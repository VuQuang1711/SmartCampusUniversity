package com.example.sis.service;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ScheduleConflictService {

    private final LopHocPhanRepository lopHocPhanRepository;
    private final LichHocRepository lichHocRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final YeuCauDoiLichRepository yeuCauDoiLichRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public static class ConflictReport {
        public boolean canSubmit;
        public int totalStudentsChecked;
        public int studentConflictCount;
        public List<String> studentConflicts = new ArrayList<>();
        public List<String> roomConflicts = new ArrayList<>();
        public List<String> lecturerConflicts = new ArrayList<>();
        public String summary;
    }

    private boolean overlapsWithExisting(int thuMoi, int tuTuanMoi, int denTuanMoi, int tietBdMoi, int tietKtMoi, LichHoc existing) {
        if (existing.getThu() == null || thuMoi != existing.getThu()) return false;
        boolean periodOverlap = !(tietKtMoi < existing.getTietBatDau() || tietBdMoi > existing.getTietKetThuc());
        if (!periodOverlap) return false;
        for (int w : existing.getWeekSet()) {
            if (w >= tuTuanMoi && w <= denTuanMoi) return true;
        }
        return false;
    }

    /**
     * KIỂM TRA XUNG ĐỘT TOÀN DIỆN (0 SINH VIÊN TRÙNG + TRỐNG PHÒNG + TRỐNG LỊCH GV)
     */
    public ConflictReport checkConflict(Long lopHocPhanId, Long ignoreLichHocId,
                                        int thuMoi, int tietBatDauMoi, int soTietMoi,
                                        String phongHocMoi, int tuTuanMoi, int denTuanMoi) {
        ConflictReport report = new ConflictReport();
        LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học phần"));

        Long hocKyId = lhp.getHocKy().getId();
        int tietKetThucMoi = tietBatDauMoi + soTietMoi - 1;
        if (tietBatDauMoi < 1 || tietKetThucMoi > 15 || soTietMoi < 1 || thuMoi < 2 || thuMoi > 8) {
            report.canSubmit = false;
            report.summary = "Khung giờ không hợp lệ (Thứ 2-8, Tiết 1-15).";
            return report;
        }

        // 1. Kiểm tra trùng lịch trên TOÀN BỘ sinh viên đang đăng ký lớp học phần này
        List<DangKyHocPhan> enrollments = dangKyHocPhanRepository.findByLopHocPhanId(lopHocPhanId);
        Set<Long> conflictedStudentIds = new HashSet<>();
        int activeStudentCount = 0;

        for (DangKyHocPhan dk : enrollments) {
            if (dk.getTrangThai() != DangKyHocPhan.TrangThaiDangKy.THANH_CONG) continue;
            activeStudentCount++;
            SinhVien sv = dk.getSinhVien();
            List<LichHoc> svSchedules = lichHocRepository.findLichHocBySinhVienAndHocKy(sv.getId(), hocKyId);
            for (LichHoc existing : svSchedules) {
                if (ignoreLichHocId != null && existing.getId().equals(ignoreLichHocId)) continue;
                if (existing.getLopHocPhan().getId().equals(lopHocPhanId) && ignoreLichHocId != null) continue;
                if (overlapsWithExisting(thuMoi, tuTuanMoi, denTuanMoi, tietBatDauMoi, tietKetThucMoi, existing)) {
                    conflictedStudentIds.add(sv.getId());
                    report.studentConflicts.add(String.format(
                            "SV %s (%s) bị trùng với lớp %s (%s) - Thứ %d, Tiết %d-%d, Phòng %s",
                            sv.getNguoiDung().getHoTen(), sv.getMssv(),
                            existing.getLopHocPhan().getMaLHP(),
                            existing.getLopHocPhan().getHocPhan().getTenHP(),
                            existing.getThu(), existing.getTietBatDau(), existing.getTietKetThuc(),
                            existing.getPhongHoc()));
                }
            }
        }
        report.totalStudentsChecked = activeStudentCount;
        report.studentConflictCount = conflictedStudentIds.size();

        // 2. Kiểm tra trống phòng học trong toàn trường ở học kỳ đó
        String targetRoom = phongHocMoi != null ? phongHocMoi.trim() : "";
        List<LichHoc> allSemesterSchedules = lichHocRepository.findByLopHocPhanHocKyId(hocKyId);
        for (LichHoc existing : allSemesterSchedules) {
            if (ignoreLichHocId != null && existing.getId().equals(ignoreLichHocId)) continue;
            if (existing.getLopHocPhan().getTrangThai() == LopHocPhan.TrangThaiLHP.HUY_LOP
                    || existing.getLopHocPhan().getTrangThai() == LopHocPhan.TrangThaiLHP.TU_CHOI) continue;

            if (!targetRoom.isEmpty() && existing.getPhongHoc() != null
                    && existing.getPhongHoc().trim().equalsIgnoreCase(targetRoom)) {
                if (overlapsWithExisting(thuMoi, tuTuanMoi, denTuanMoi, tietBatDauMoi, tietKetThucMoi, existing)) {
                    report.roomConflicts.add(String.format(
                            "Phòng %s đã có lớp %s (%s) sử dụng vào Thứ %d, Tiết %d-%d (Tuần %d-%d)",
                            targetRoom, existing.getLopHocPhan().getMaLHP(),
                            existing.getLopHocPhan().getHocPhan().getTenHP(),
                            existing.getThu(), existing.getTietBatDau(), existing.getTietKetThuc(),
                            existing.getTuTuan(), existing.getDenTuan()));
                }
            }
        }

        // 3. Kiểm tra trống lịch Giảng viên
        if (lhp.getGiangVien() != null) {
            List<LichHoc> gvSchedules = lichHocRepository.findLichHocByGiangVienAndHocKy(lhp.getGiangVien().getId(), hocKyId);
            for (LichHoc existing : gvSchedules) {
                if (ignoreLichHocId != null && existing.getId().equals(ignoreLichHocId)) continue;
                if (overlapsWithExisting(thuMoi, tuTuanMoi, denTuanMoi, tietBatDauMoi, tietKetThucMoi, existing)) {
                    report.lecturerConflicts.add(String.format(
                            "Giảng viên bị trùng lịch dạy lớp %s (%s) vào Thứ %d, Tiết %d-%d",
                            existing.getLopHocPhan().getMaLHP(),
                            existing.getLopHocPhan().getHocPhan().getTenHP(),
                            existing.getThu(), existing.getTietBatDau(), existing.getTietKetThuc()));
                }
            }
        }

        report.canSubmit = report.studentConflictCount == 0
                && report.roomConflicts.isEmpty()
                && report.lecturerConflicts.isEmpty();

        if (report.canSubmit) {
            report.summary = String.format(
                    "Đạt chuẩn: 0/%d sinh viên trùng lịch, phòng %s trống, lịch giảng viên trống.",
                    activeStudentCount, targetRoom);
        } else {
            List<String> parts = new ArrayList<>();
            if (report.studentConflictCount > 0) parts.add(report.studentConflictCount + " sinh viên bị trùng lịch");
            if (!report.roomConflicts.isEmpty()) parts.add("phòng " + targetRoom + " đã bận");
            if (!report.lecturerConflicts.isEmpty()) parts.add("giảng viên bị trùng lịch dạy");
            report.summary = "BỊ CHẶN: " + String.join(" • ", parts) + ".";
        }

        return report;
    }

    @Transactional
    public YeuCauDoiLich submitChangeRequest(GiangVien gv, Long lopHocPhanId, Long lichHocId,
                                             YeuCauDoiLich.LoaiYeuCau loaiYeuCau,
                                             int thuMoi, int tietBatDauMoi, int soTietMoi,
                                             String phongHocMoi, int tuTuanMoi, int denTuanMoi, String lyDo) {
        ConflictReport report = checkConflict(lopHocPhanId, lichHocId, thuMoi, tietBatDauMoi, soTietMoi, phongHocMoi, tuTuanMoi, denTuanMoi);
        if (!report.canSubmit) {
            throw new IllegalStateException(report.summary);
        }

        LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId).orElseThrow();
        LichHoc lichHoc = (lichHocId != null) ? lichHocRepository.findById(lichHocId).orElse(null) : null;

        YeuCauDoiLich req = YeuCauDoiLich.builder()
                .lopHocPhan(lhp)
                .lichHoc(lichHoc)
                .giangVien(gv)
                .loaiYeuCau(loaiYeuCau)
                .thuMoi(thuMoi)
                .tietBatDauMoi(tietBatDauMoi)
                .soTietMoi(soTietMoi)
                .phongHocMoi(phongHocMoi.trim())
                .tuTuanMoi(tuTuanMoi)
                .denTuanMoi(denTuanMoi)
                .lyDo(lyDo)
                .ketQuaKiemTra(report.summary)
                .soSvTrung(0)
                .trangThai(YeuCauDoiLich.TrangThaiDuyet.CHO_DUYET)
                .ngayTao(LocalDateTime.now())
                .build();

        req = yeuCauDoiLichRepository.save(req);

        notificationService.sendToAdmins(
                gv.getNguoiDung(),
                "Yêu cầu đổi lịch mới: " + lhp.getMaLHP(),
                String.format("GV %s đề xuất %s cho lớp %s (%s) sang Thứ %d, Tiết %d-%d, Phòng %s (Tuần %d-%d). %s",
                        gv.getNguoiDung().getHoTen(), loaiYeuCau.getLabel(), lhp.getMaLHP(), lhp.getHocPhan().getTenHP(),
                        thuMoi, tietBatDauMoi, tietBatDauMoi + soTietMoi - 1, phongHocMoi, tuTuanMoi, denTuanMoi, report.summary),
                ThongBao.LoaiThongBao.DOI_LICH,
                "/Admin/Approvals?tab=schedules"
        );
        auditLogService.log(gv.getNguoiDung(), "DE_XUAT_DOI_LICH", "YeuCauDoiLich#" + req.getId(),
                "Đề xuất " + loaiYeuCau.getLabel() + " lớp " + lhp.getMaLHP() + " -> Thứ " + thuMoi + " Tiết " + tietBatDauMoi + "-" + (tietBatDauMoi + soTietMoi - 1) + " P." + phongHocMoi);
        return req;
    }

    @Transactional
    public void approveRequest(Long requestId, NguoiDung admin, String adminNote) {
        YeuCauDoiLich req = yeuCauDoiLichRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đổi lịch"));
        if (req.getTrangThai() != YeuCauDoiLich.TrangThaiDuyet.CHO_DUYET) {
            throw new IllegalStateException("Yêu cầu này đã được xử lý trước đó.");
        }

        // Re-check conflict before applying
        Long ignoreId = req.getLichHoc() != null ? req.getLichHoc().getId() : null;
        ConflictReport report = checkConflict(req.getLopHocPhan().getId(), ignoreId,
                req.getThuMoi(), req.getTietBatDauMoi(), req.getSoTietMoi(),
                req.getPhongHocMoi(), req.getTuTuanMoi(), req.getDenTuanMoi());
        if (!report.canSubmit) {
            throw new IllegalStateException("Không thể duyệt vì phát sinh xung đột mới: " + report.summary);
        }

        // Apply to LichHoc
        LichHoc lh;
        if (req.getLichHoc() != null && req.getLoaiYeuCau() != YeuCauDoiLich.LoaiYeuCau.DAY_BU) {
            lh = req.getLichHoc();
            lh.setThu(req.getThuMoi());
            lh.setTietBatDau(req.getTietBatDauMoi());
            lh.setSoTiet(req.getSoTietMoi());
            lh.setPhongHoc(req.getPhongHocMoi());
            lh.setTuTuan(req.getTuTuanMoi());
            lh.setDenTuan(req.getDenTuanMoi());
        } else {
            lh = LichHoc.builder()
                    .lopHocPhan(req.getLopHocPhan())
                    .thu(req.getThuMoi())
                    .tietBatDau(req.getTietBatDauMoi())
                    .soTiet(req.getSoTietMoi())
                    .phongHoc(req.getPhongHocMoi())
                    .tuTuan(req.getTuTuanMoi())
                    .denTuan(req.getDenTuanMoi())
                    .loaiLich(LichHoc.LoaiLich.LY_THUYET)
                    .build();
        }
        lichHocRepository.save(lh);

        req.setTrangThai(YeuCauDoiLich.TrangThaiDuyet.DA_DUYET);
        req.setPhanHoiAdmin(adminNote != null && !adminNote.isBlank() ? adminNote : "Đã phê duyệt và cập nhật thời khóa biểu chính thức.");
        req.setNgayXuLy(LocalDateTime.now());
        yeuCauDoiLichRepository.save(req);

        LopHocPhan lhp = req.getLopHocPhan();
        String msg = String.format(
                "Lịch học lớp %s (%s) đã được cập nhật (%s): Thứ %d, Tiết %d-%d, Phòng %s (Tuần %d-%d). Lý do: %s",
                lhp.getMaLHP(), lhp.getHocPhan().getTenHP(), req.getLoaiYeuCau().getLabel(),
                req.getThuMoi(), req.getTietBatDauMoi(), req.getTietKetThucMoi(),
                req.getPhongHocMoi(), req.getTuTuanMoi(), req.getDenTuanMoi(),
                req.getLyDo() != null ? req.getLyDo() : "Theo điều chỉnh giảng dạy");

        // Thông báo tới Giảng viên
        notificationService.sendToUser(admin, req.getGiangVien().getNguoiDung(),
                "[Đã duyệt] Đổi lịch lớp " + lhp.getMaLHP(), msg,
                ThongBao.LoaiThongBao.DOI_LICH, "/Faculty/Schedule");

        // Thông báo tới TOÀN BỘ Sinh viên thuộc lớp học phần
        int countSv = notificationService.sendToClassStudents(admin, lhp.getId(),
                "[Cập nhật lịch học] Lớp " + lhp.getMaLHP() + " - " + lhp.getHocPhan().getTenHP(),
                msg, ThongBao.LoaiThongBao.DOI_LICH, "/Student/Timetable");

        auditLogService.log(admin, "DUYET_DOI_LICH", "YeuCauDoiLich#" + req.getId(),
                "Duyệt đổi lịch lớp " + lhp.getMaLHP() + " -> Thứ " + req.getThuMoi() + " Tiết " + req.getTietBatDauMoi() + "-" + req.getTietKetThucMoi() + " P." + req.getPhongHocMoi() + " (Đã gửi TB cho GV & " + countSv + " SV)");
    }

    @Transactional
    public void rejectRequest(Long requestId, NguoiDung admin, String reason) {
        YeuCauDoiLich req = yeuCauDoiLichRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đổi lịch"));
        req.setTrangThai(YeuCauDoiLich.TrangThaiDuyet.TU_CHOI);
        req.setPhanHoiAdmin(reason != null && !reason.isBlank() ? reason : "Không phù hợp kế hoạch giảng dạy.");
        req.setNgayXuLy(LocalDateTime.now());
        yeuCauDoiLichRepository.save(req);

        notificationService.sendToUser(admin, req.getGiangVien().getNguoiDung(),
                "[Từ chối] Đề xuất đổi lịch lớp " + req.getLopHocPhan().getMaLHP(),
                "Yêu cầu đổi lịch của thầy/cô không được phê duyệt. Lý do: " + req.getPhanHoiAdmin(),
                ThongBao.LoaiThongBao.DOI_LICH, "/Faculty/Schedule");

        auditLogService.log(admin, "TU_CHOI_DOI_LICH", "YeuCauDoiLich#" + req.getId(),
                "Từ chối đổi lịch lớp " + req.getLopHocPhan().getMaLHP() + ". Lý do: " + req.getPhanHoiAdmin());
    }
}
