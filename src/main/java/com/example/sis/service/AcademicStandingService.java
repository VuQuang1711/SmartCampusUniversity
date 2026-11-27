package com.example.sis.service;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AcademicStandingService {

    private final SinhVienRepository sinhVienRepository;
    private final DiemHocPhanRepository diemHocPhanRepository;
    private final KhungChuongTrinhRepository khungChuongTrinhRepository;
    private final ChungChiChuanDauRaRepository chungChiChuanDauRaRepository;
    private final CongNoHocKyRepository congNoHocKyRepository;

    public static class GraduationAuditResult {
        public boolean eligible;
        public List<String> reasons = new ArrayList<>();
        public int creditsEarned;
        public int creditsRequired;
        public double currentCpa;
        public boolean allPrerequisitesPassed;
        public boolean certificatesPassed;
        public boolean debtFree;
    }

    /**
     * TỰ ĐỘNG QUÉT VÀ CẬP NHẬT CẢNH BÁO HỌC VỤ
     */
    @Transactional
    public List<SinhVien> auditAcademicWarnings() {
        List<SinhVien> allStudents = sinhVienRepository.findAll();
        List<SinhVien> warnedStudents = new ArrayList<>();

        for (SinhVien sv : allStudents) {
            double cpa = sv.getCpaTichLuy() != null ? sv.getCpaTichLuy() : 0.0;
            String previousStatus = sv.getTrangThaiHocVu();

            if (cpa < 1.0) {
                sv.setTrangThaiHocVu("BUOC_THOI_HOC");
                warnedStudents.add(sv);
            } else if (cpa < 1.5) {
                if ("CANH_BAO_1".equals(previousStatus)) {
                    sv.setTrangThaiHocVu("CANH_BAO_2");
                } else {
                    sv.setTrangThaiHocVu("CANH_BAO_1");
                }
                warnedStudents.add(sv);
            } else if (cpa < 1.8) {
                sv.setTrangThaiHocVu("CANH_BAO_1");
                warnedStudents.add(sv);
            } else if (!"DA_TOT_NGHIEP".equals(previousStatus)) {
                sv.setTrangThaiHocVu("BINH_THUONG");
            }
            sinhVienRepository.save(sv);
        }
        return warnedStudents;
    }

    /**
     * KIỂM TRA ĐIỀU KIỆN XÉT TỐT NGHIỆP TỰ ĐỘNG (DEGREE AUDIT)
     */
    public GraduationAuditResult checkGraduationEligibility(Long sinhVienId) {
        SinhVien sv = sinhVienRepository.findById(sinhVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));

        GraduationAuditResult result = new GraduationAuditResult();
        result.creditsEarned = sv.getTongTcTichLuy() != null ? sv.getTongTcTichLuy() : 0;
        result.creditsRequired = sv.getNganhHoc().getTongTinChiYeuCau();
        result.currentCpa = sv.getCpaTichLuy() != null ? sv.getCpaTichLuy() : 0.0;

        boolean pass = true;

        // 1. Kiểm tra tổng tín chỉ
        if (result.creditsEarned < result.creditsRequired) {
            pass = false;
            result.reasons.add("Chưa tích lũy đủ số tín chỉ yêu cầu: Đạt " + result.creditsEarned + "/" + result.creditsRequired + " TC");
        }

        // 2. Kiểm tra GPA >= 2.0
        if (result.currentCpa < 2.0) {
            pass = false;
            result.reasons.add("Điểm trung bình tích lũy GPA chưa đạt chuẩn (Hiện tại: " + result.currentCpa + " / Yêu cầu: >= 2.0)");
        }

        // 3. Kiểm tra các môn bắt buộc trong Khung chương trình
        List<KhungChuongTrinh> kctList = khungChuongTrinhRepository.findByNganhHocId(sv.getNganhHoc().getId());
        List<DiemHocPhan> allGrades = diemHocPhanRepository.findAllBySinhVienId(sinhVienId);
        Set<Long> passedSubjectIds = allGrades.stream()
                .filter(d -> Boolean.TRUE.equals(d.getKetQuaQuaMon()))
                .map(d -> d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getId())
                .collect(Collectors.toSet());

        List<String> missingSubjects = new ArrayList<>();
        for (KhungChuongTrinh kct : kctList) {
            if (Boolean.TRUE.equals(kct.getBatBuoc()) && !passedSubjectIds.contains(kct.getHocPhan().getId())) {
                missingSubjects.add(kct.getHocPhan().getTenHP() + " (" + kct.getHocPhan().getMaHP() + ")");
            }
        }
        if (!missingSubjects.isEmpty()) {
            pass = false;
            result.allPrerequisitesPassed = false;
            result.reasons.add("Chưa hoàn thành các môn bắt buộc: " + String.join(", ", missingSubjects));
        } else {
            result.allPrerequisitesPassed = true;
        }

        // 4. Kiểm tra Chuẩn đầu ra
        List<ChungChiChuanDauRa> certs = chungChiChuanDauRaRepository.findBySinhVienIdAndTrangThai(
                sinhVienId, ChungChiChuanDauRa.TrangThaiChungChi.DA_DAT);
        Set<ChungChiChuanDauRa.LoaiChungChi> achievedCerts = certs.stream()
                .map(ChungChiChuanDauRa::getLoaiChungChi)
                .collect(Collectors.toSet());

        boolean hasEnglish = achievedCerts.contains(ChungChiChuanDauRa.LoaiChungChi.TIENG_ANH_B1) ||
                             achievedCerts.contains(ChungChiChuanDauRa.LoaiChungChi.TIENG_ANH_IELTS) ||
                             achievedCerts.contains(ChungChiChuanDauRa.LoaiChungChi.TIENG_ANH_TOEIC);
        boolean hasGDQP = achievedCerts.contains(ChungChiChuanDauRa.LoaiChungChi.GIAO_DUC_QUOC_PHONG);

        if (!hasEnglish || !hasGDQP) {
            pass = false;
            result.certificatesPassed = false;
            result.reasons.add("Chưa hoàn thành đủ chuẩn đầu ra (Yêu cầu Chứng chỉ Ngoại ngữ & Giáo dục Quốc phòng)");
        } else {
            result.certificatesPassed = true;
        }

        // 5. Kiểm tra công nợ học phí
        List<CongNoHocKy> debts = congNoHocKyRepository.findBySinhVienId(sinhVienId);
        double totalDebt = debts.stream().mapToDouble(cn -> cn.getConNo() != null ? cn.getConNo() : 0.0).sum();
        if (totalDebt > 0) {
            pass = false;
            result.debtFree = false;
            result.reasons.add("Còn nợ học phí chưa thanh toán: " + String.format("%,.0f VNĐ", totalDebt));
        } else {
            result.debtFree = true;
        }

        result.eligible = pass;
        return result;
    }
}
