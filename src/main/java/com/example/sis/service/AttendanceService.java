package com.example.sis.service;

import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AttendanceService {

    @Autowired private DiemDanhRepository diemDanhRepository;
    @Autowired private LichHocRepository lichHocRepository;
    @Autowired private SinhVienRepository sinhVienRepository;
    @Autowired private DangKyHocPhanRepository dangKyHocPhanRepository;
    @Autowired private DiemHocPhanRepository diemHocPhanRepository;
    @Autowired private GradingService gradingService;

    /**
     * ĐIỂM DANH HÀNG LOẠT CHO CẢ LỚP (BULK ATTENDANCE) VÀ TỰ ĐỘNG CẢNH BÁO CẤM THI NẾU VẮNG > 20%
     */
    @Transactional
    public void saveBulkAttendance(Long lichHocId, LocalDate ngayHoc, Map<Long, DiemDanh.TrangThaiDiemDanh> attendanceMap, Map<Long, String> noteMap) {
        LichHoc lichHoc = lichHocRepository.findById(lichHocId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lịch học"));
        LopHocPhan lhp = lichHoc.getLopHocPhan();

        for (Map.Entry<Long, DiemDanh.TrangThaiDiemDanh> entry : attendanceMap.entrySet()) {
            Long svId = entry.getKey();
            DiemDanh.TrangThaiDiemDanh status = entry.getValue();
            String note = noteMap != null ? noteMap.get(svId) : "";

            SinhVien sv = sinhVienRepository.findById(svId).orElse(null);
            if (sv == null) continue;

            // Tìm xem đã điểm danh ngày này chưa
            List<DiemDanh> existing = diemDanhRepository.findByLichHocIdAndNgayHoc(lichHocId, ngayHoc);
            Optional<DiemDanh> svRecord = existing.stream().filter(d -> d.getSinhVien().getId().equals(svId)).findFirst();

            DiemDanh dd;
            if (svRecord.isPresent()) {
                dd = svRecord.get();
                dd.setTrangThai(status);
                dd.setGhiChu(note);
            } else {
                dd = DiemDanh.builder()
                        .lichHoc(lichHoc)
                        .sinhVien(sv)
                        .ngayHoc(ngayHoc)
                        .trangThai(status)
                        .ghiChu(note)
                        .build();
            }
            diemDanhRepository.save(dd);

            // TÍNH TOÁN TỶ LỆ VẮNG MẶT TRÊN TỔNG SỐ TIẾT
            checkAndFlagCamThi(svId, lhp.getId());
        }
    }

    /**
     * TÍNH TOÁN TỶ LỆ VẮNG: NẾU VẮNG > 20% THÌ TỰ ĐỘNG GẮN CỜ CẤM THI VÀ KHÓA ĐIỂM CUỐI KỲ = 0.0
     */
    @Transactional
    public boolean checkAndFlagCamThi(Long sinhVienId, Long lopHocPhanId) {
        long absentSessions = diemDanhRepository.countVangBySinhVienAndLopHocPhan(sinhVienId, lopHocPhanId);
        long totalRecordedSessions = diemDanhRepository.countTongBuoiBySinhVienAndLopHocPhan(sinhVienId, lopHocPhanId);

        // Chuẩn học kỳ đại học: 15 tuần học
        long totalEstimatedSessions = 15;
        double absentRate = (double) absentSessions / (double) totalEstimatedSessions;

        boolean isCamThi = absentRate > 0.20; // Vượt quá 20%

        // Tìm điểm học phần của sinh viên trong lớp này
        Optional<DangKyHocPhan> dkOpt = dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(sinhVienId, lopHocPhanId);
        if (dkOpt.isPresent()) {
            Optional<DiemHocPhan> dhpOpt = diemHocPhanRepository.findByDangKyHocPhanId(dkOpt.get().getId());
            if (dhpOpt.isPresent()) {
                DiemHocPhan dhp = dhpOpt.get();
                dhp.setCamThi(isCamThi);
                if (isCamThi) {
                    dhp.setDiemCuoiKy(0.0);
                }
                gradingService.calculateComponentGrades(dhp);
            }
        }

        return isCamThi;
    }
}
