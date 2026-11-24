package com.example.sis.service;

import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class GradingService {

    @Autowired private DiemHocPhanRepository diemHocPhanRepository;
    @Autowired private SinhVienRepository sinhVienRepository;

    public static class GradeScaleResult {
        public final double he4;
        public final String diemChu;
        public final boolean quaMon;

        public GradeScaleResult(double he4, String diemChu, boolean quaMon) {
            this.he4 = he4;
            this.diemChu = diemChu;
            this.quaMon = quaMon;
        }
    }

    /**
     * QUY ĐỔI THANG 10 SANG THANG 4 VÀ ĐIỂM CHỮ THEO CHUẨN BỘ GD&ĐT
     */
    public GradeScaleResult convertToScale4AndLetter(double diemHe10) {
        if (diemHe10 >= 8.5) return new GradeScaleResult(4.0, "A", true);
        if (diemHe10 >= 8.0) return new GradeScaleResult(3.5, "B+", true);
        if (diemHe10 >= 7.0) return new GradeScaleResult(3.0, "B", true);
        if (diemHe10 >= 6.5) return new GradeScaleResult(2.5, "C+", true);
        if (diemHe10 >= 5.5) return new GradeScaleResult(2.0, "C", true);
        if (diemHe10 >= 5.0) return new GradeScaleResult(1.5, "D+", true);
        if (diemHe10 >= 4.0) return new GradeScaleResult(1.0, "D", true);
        return new GradeScaleResult(0.0, "F", false);
    }

    /**
     * TÍNH ĐIỂM TỔNG KẾT VÀ QUY ĐỔI CHO MỘT BẢN GHI ĐIỂM HỌC PHẦN
     */
    @Transactional
    public DiemHocPhan calculateComponentGrades(DiemHocPhan dhp) {
        HocPhan hp = dhp.getDangKyHocPhan().getLopHocPhan().getHocPhan();

        // Nếu bị cấm thi thì điểm cuối kỳ = 0.0
        if (Boolean.TRUE.equals(dhp.getCamThi())) {
            dhp.setDiemCuoiKy(0.0);
        }

        double cc = dhp.getDiemChuyenCan() != null ? dhp.getDiemChuyenCan() : 0.0;
        double qt = dhp.getDiemQuaTrinh() != null ? dhp.getDiemQuaTrinh() : 0.0;
        double th = dhp.getDiemThucHanh() != null ? dhp.getDiemThucHanh() : 0.0;
        double ck = dhp.getDiemCuoiKy() != null ? dhp.getDiemCuoiKy() : 0.0;

        double wCc = hp.getTyLeChuyenCan() != null ? hp.getTyLeChuyenCan() : 0.10;
        double wQt = hp.getTyLeQuaTrinh() != null ? hp.getTyLeQuaTrinh() : 0.20;
        double wTh = hp.getTyLeThucHanh() != null ? hp.getTyLeThucHanh() : 0.20;
        double wCk = hp.getTyLeCuoiKy() != null ? hp.getTyLeCuoiKy() : 0.50;

        double tong10 = (cc * wCc) + (qt * wQt) + (th * wTh) + (ck * wCk);
        tong10 = Math.round(tong10 * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân

        GradeScaleResult scaleResult = convertToScale4AndLetter(tong10);

        dhp.setDiemTongKetHe10(tong10);
        dhp.setDiemHe4(scaleResult.he4);
        dhp.setDiemChu(scaleResult.diemChu);
        dhp.setKetQuaQuaMon(scaleResult.quaMon);

        diemHocPhanRepository.save(dhp);

        // Tự động tính lại CPA tích lũy cho sinh viên
        updateStudentCpa(dhp.getDangKyHocPhan().getSinhVien().getId());

        return dhp;
    }

    /**
     * TÍNH GPA HỌC KỲ (TẤT CẢ CÁC MÔN TRONG HỌC KỲ ĐÓ)
     */
    public double calculateSemesterGpa(Long sinhVienId, Long hocKyId) {
        List<DiemHocPhan> semesterGrades = diemHocPhanRepository.findBySinhVienIdAndHocKyId(sinhVienId, hocKyId);
        double totalWeightedPoints = 0.0;
        int totalCredits = 0;

        for (DiemHocPhan d : semesterGrades) {
            if (d.getDiemHe4() != null) {
                int credits = d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getTongTinChi();
                totalWeightedPoints += d.getDiemHe4() * credits;
                totalCredits += credits;
            }
        }

        if (totalCredits == 0) return 0.0;
        return Math.round((totalWeightedPoints / totalCredits) * 100.0) / 100.0;
    }

    /**
     * TÍNH CPA TÍCH LŨY TOÀN KHÓA CHUẨN TÍN CHỈ
     * ĐẶC BIỆT: Với môn học lại / học cải thiện, CPA CHỈ LẤY ĐIỂM CAO NHẤT trong các lần học của cùng một HocPhan!
     */
    @Transactional
    public double updateStudentCpa(Long sinhVienId) {
        SinhVien sv = sinhVienRepository.findById(sinhVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));

        List<DiemHocPhan> allGrades = diemHocPhanRepository.findAllBySinhVienId(sinhVienId);

        // Map HocPhanId -> Bản ghi điểm cao nhất của môn đó
        Map<Long, DiemHocPhan> bestGradeBySubject = new HashMap<>();

        for (DiemHocPhan d : allGrades) {
            if (d.getDiemHe4() == null) continue;
            Long hpId = d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getId();

            if (!bestGradeBySubject.containsKey(hpId)) {
                bestGradeBySubject.put(hpId, d);
            } else {
                DiemHocPhan currentBest = bestGradeBySubject.get(hpId);
                if (d.getDiemHe4() > currentBest.getDiemHe4()) {
                    bestGradeBySubject.put(hpId, d);
                }
            }
        }

        double totalWeightedPoints = 0.0;
        int totalCreditsEarned = 0;

        for (DiemHocPhan best : bestGradeBySubject.values()) {
            int credits = best.getDangKyHocPhan().getLopHocPhan().getHocPhan().getTongTinChi();
            totalWeightedPoints += best.getDiemHe4() * credits;

            // Chỉ tính tín chỉ tích lũy nếu môn ĐÃ QUA (khác F)
            if (Boolean.TRUE.equals(best.getKetQuaQuaMon())) {
                totalCreditsEarned += credits;
            }
        }

        int totalSubjectCredits = bestGradeBySubject.values().stream()
                .mapToInt(d -> d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getTongTinChi())
                .sum();

        double cpa = totalSubjectCredits > 0 ? (totalWeightedPoints / totalSubjectCredits) : 0.0;
        cpa = Math.round(cpa * 100.0) / 100.0;

        sv.setCpaTichLuy(cpa);
        sv.setTongTcTichLuy(totalCreditsEarned);
        sinhVienRepository.save(sv);

        return cpa;
    }
}
