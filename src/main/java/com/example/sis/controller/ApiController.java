package com.example.sis.controller;

import com.example.sis.entity.*;
import com.example.sis.repository.*;
import com.example.sis.service.ExcelService;
import com.example.sis.service.FileStorageService;
import com.example.sis.service.ScheduleConflictService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired private SinhVienRepository sinhVienRepository;
    @Autowired private HocPhanRepository hocPhanRepository;
    @Autowired private ChungChiChuanDauRaRepository chungChiChuanDauRaRepository;
    @Autowired private ExcelService excelService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private ScheduleConflictService scheduleConflictService;

    public static class CommandItem {
        public String title;
        public String subtitle;
        public String url;
        public String category;

        public CommandItem(String title, String subtitle, String url, String category) {
            this.title = title;
            this.subtitle = subtitle;
            this.url = url;
            this.category = category;
        }
    }

    @GetMapping("/search/command-bar")
    public List<CommandItem> commandBarSearch(@RequestParam(defaultValue = "") String q, HttpSession session) {
        List<CommandItem> results = new ArrayList<>();
        String query = q.trim().toLowerCase();
        NguoiDung u = (NguoiDung) session.getAttribute("currentUser");
        VaiTro role = u != null ? u.getVaiTro() : VaiTro.SINH_VIEN;

        if (role == VaiTro.SINH_VIEN) {
            addIfMatch(results, query, "Tổng Quan & Tiến Độ", "Nhắc nhở, biểu đồ tín chỉ & GPA", "/Student/Dashboard", "Sinh Viên");
            addIfMatch(results, query, "Hồ Sơ Sinh Viên", "Lý lịch, lớp danh nghĩa, cố vấn học tập", "/Student/Profile", "Hồ Sơ");
            addIfMatch(results, query, "Chứng Chỉ Chuẩn Đầu Ra (PDF)", "Nộp PDF TOEIC/IELTS/MOS/GDQP & theo dõi duyệt", "/Student/Certificates", "Hồ Sơ");
            addIfMatch(results, query, "Chương Trình Khung", "Danh sách môn học 8 học kỳ & tiên quyết", "/Student/Curriculum", "Học Tập");
            addIfMatch(results, query, "Đăng Ký Học Phần", "Đăng ký các lớp đã được Admin duyệt", "/Student/Registration", "Học Tập");
            addIfMatch(results, query, "Lịch Tuần & Lịch Thi", "Thời khóa biểu tuần và tiến độ học kỳ", "/Student/Timetable", "Học Tập");
            addIfMatch(results, query, "Kết Quả Học Tập", "Bảng điểm chi tiết hệ 10 & hệ 4", "/Student/Grades", "Học Tập");
            addIfMatch(results, query, "Điểm Rèn Luyện", "Điểm rèn luyện theo học kỳ & tự đánh giá", "/Student/Conduct", "Học Tập");
            addIfMatch(results, query, "E-Learning HUIT", "Học tập trực tuyến 10 học phần KHDL, kỹ năng & CLO", "/ELearning", "Học Tập");
            addIfMatch(results, query, "Mô Phỏng GPA", "Tính toán mục tiêu GPA & gợi ý môn học cải thiện", "/Student/GpaSimulator", "Hỗ Trợ");
            addIfMatch(results, query, "Công Nợ & Thanh Toán", "Tra cứu học phí, VietQR & biên lai", "/Student/Tuition", "Tài Chính");
            addIfMatch(results, query, "Hộp Thư Sinh Viên", "Thông báo đổi lịch, chứng chỉ & gửi phản hồi", "/Student/Inbox", "Hộp Thư");
        } else if (role == VaiTro.GIANG_VIEN || role == VaiTro.CO_VAN_HOC_TAP) {
            addIfMatch(results, query, "Tổng Quan Giảng Viên", "Lịch dạy hôm nay và lớp phụ trách", "/Faculty/Dashboard", "Giảng Viên");
            addIfMatch(results, query, "Hộp Thư Giảng Viên", "Nhận thông báo & gửi tin hàng loạt cho lớp", "/Faculty/Inbox", "Hộp Thư");
            addIfMatch(results, query, "Đề Xuất Mở Học Phần", "Chọn môn trong chương trình khung gửi Admin duyệt", "/Faculty/Proposals", "Lớp Học Phần");
            addIfMatch(results, query, "Danh Sách Lớp & Sinh Viên", "Xem danh sách SV, liên hệ & xuất Excel", "/Faculty/Classes", "Lớp Học Phần");
            addIfMatch(results, query, "Quản Lý Lịch Giảng & Đổi Lịch", "Kiểm tra xung đột 0 SV trùng & gửi duyệt", "/Faculty/Schedule", "Lịch Giảng");
            addIfMatch(results, query, "Điểm Danh & Cấm Thi", "Điểm danh từng buổi & cảnh báo cấm thi", "/Faculty/Attendance", "Giảng Dạy");
            addIfMatch(results, query, "Nhập & Chốt Điểm", "Nhập điểm, import Excel & chốt khóa bảng điểm", "/Faculty/Grading", "Giảng Dạy");
            addIfMatch(results, query, "Lớp Cố Vấn & Điểm Rèn Luyện", "Theo dõi học vụ & duyệt điểm rèn luyện cấp lớp", "/Faculty/Advisees", "Cố Vấn");
        } else {
            addIfMatch(results, query, "Trung Tâm Phê Duyệt", "Duyệt mở lớp, duyệt đổi lịch & duyệt chứng chỉ PDF", "/Admin/Approvals", "Phê Duyệt");
            addIfMatch(results, query, "Tổng Quan Điều Hành", "Thống kê toàn trường & rà soát học vụ", "/Admin/Dashboard", "Đào Tạo");
            addIfMatch(results, query, "Chương Trình Khung & Môn Học", "Quản lý ngành, học phần & môn tiên quyết", "/Admin/Curriculum", "Đào Tạo");
            addIfMatch(results, query, "Học Kỳ & Đợt Đăng Ký", "Mở/đóng cổng ĐKHP và cổng nhập điểm", "/Admin/Semesters", "Đào Tạo");
            addIfMatch(results, query, "Lớp Học Phần & Lịch Học", "Xếp lịch gốc & can thiệp sửa trực tiếp lịch tuần", "/Admin/Classes", "Đào Tạo");
            addIfMatch(results, query, "Quản Lý Điểm & Rèn Luyện", "Phúc khảo điểm & chốt điểm rèn luyện toàn trường", "/Admin/Grades", "Đào Tạo");
            addIfMatch(results, query, "Quản Lý Tài Chính Học Phí", "Cấu hình đơn giá, công nợ & báo cáo doanh thu", "/Admin/Tuition", "Tài Chính");
            addIfMatch(results, query, "Quản Lý Người Dùng & RBAC", "Tạo tài khoản, khóa/mở khóa, reset mật khẩu", "/Admin/Users", "Hệ Thống");
            addIfMatch(results, query, "Thông Báo Toàn Trường", "Gửi thông báo SV/GV & trả lời phản hồi", "/Admin/Notifications", "Hệ Thống");
            addIfMatch(results, query, "Nhật Ký Hệ Thống (Audit Log)", "Tra cứu lịch sử thao tác nhạy cảm", "/Admin/AuditLog", "Hệ Thống");
        }

        if (!query.isEmpty()) {
            if (role != VaiTro.SINH_VIEN) {
                for (SinhVien sv : sinhVienRepository.searchSinhVien(query).stream().limit(5).toList()) {
                    results.add(new CommandItem(
                            sv.getNguoiDung().getHoTen(),
                            "MSSV: " + sv.getMssv() + " • Lớp: " + (sv.getLopDanhNghia() != null ? sv.getLopDanhNghia().getMaLop() : "") + " • GPA: " + sv.getCpaTichLuy(),
                            role == VaiTro.GIANG_VIEN || role == VaiTro.CO_VAN_HOC_TAP ? "/Faculty/Advisees" : "/Admin/Students?search=" + sv.getMssv(),
                            "Sinh Viên"
                    ));
                }
            }
            for (HocPhan hp : hocPhanRepository.searchHocPhan(query).stream().limit(5).toList()) {
                results.add(new CommandItem(
                        hp.getTenHP(),
                        "Mã HP: " + hp.getMaHP() + " • " + hp.getTongTinChi() + " Tín chỉ",
                        role == VaiTro.SINH_VIEN ? "/Student/Registration" : (role == VaiTro.GIANG_VIEN || role == VaiTro.CO_VAN_HOC_TAP ? "/Faculty/Proposals" : "/Admin/Curriculum"),
                        "Học Phần"
                ));
            }
        }
        return results;
    }

    private void addIfMatch(List<CommandItem> list, String query, String title, String subtitle, String url, String cat) {
        if (query.isEmpty() || title.toLowerCase().contains(query) || subtitle.toLowerCase().contains(query) || cat.toLowerCase().contains(query)) {
            list.add(new CommandItem(title, subtitle, url, cat));
        }
    }

    /**
     * KIỂM TRA XUNG ĐỘT LỊCH GIẢNG REALTIME (AJAX) CHO GIẢNG VIÊN
     */
    @GetMapping("/faculty/check-schedule-conflict")
    public ResponseEntity<?> checkScheduleConflict(
            @RequestParam Long lopHocPhanId,
            @RequestParam(required = false) Long lichHocId,
            @RequestParam int thuMoi,
            @RequestParam int tietBatDauMoi,
            @RequestParam int soTietMoi,
            @RequestParam String phongHocMoi,
            @RequestParam(defaultValue = "1") int tuTuanMoi,
            @RequestParam(defaultValue = "15") int denTuanMoi,
            HttpSession session) {
        NguoiDung u = (NguoiDung) session.getAttribute("currentUser");
        if (u == null) return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        ScheduleConflictService.ConflictReport report = scheduleConflictService.checkConflict(
                lopHocPhanId, lichHocId, thuMoi, tietBatDauMoi, soTietMoi, phongHocMoi, tuTuanMoi, denTuanMoi);
        return ResponseEntity.ok(report);
    }

    /**
     * XEM TRỰC TIẾP FILE PDF CHỨNG CHỈ (INLINE VIEWER CHO ADMIN & SINH VIÊN CHỦ SỞ HỮU)
     */
    @GetMapping("/certificates/{id}/pdf")
    public ResponseEntity<Resource> viewCertificatePdf(@PathVariable Long id, HttpSession session) throws IOException {
        NguoiDung u = (NguoiDung) session.getAttribute("currentUser");
        if (u == null) return ResponseEntity.status(401).build();

        ChungChiChuanDauRa cc = chungChiChuanDauRaRepository.findById(id).orElse(null);
        if (cc == null || cc.getFilePdfUrl() == null) return ResponseEntity.notFound().build();

        // Chỉ Admin/Phòng Đào Tạo hoặc chính sinh viên sở hữu chứng chỉ được xem
        boolean isAdmin = u.getVaiTro() == VaiTro.ADMIN || u.getVaiTro() == VaiTro.PHONG_DAO_TAO;
        boolean isOwner = cc.getSinhVien() != null && cc.getSinhVien().getNguoiDung() != null
                && cc.getSinhVien().getNguoiDung().getId().equals(u.getId());
        if (!isAdmin && !isOwner) return ResponseEntity.status(403).build();

        Resource resource = fileStorageService.loadCertificatePdf(cc.getFilePdfUrl());
        if (resource == null || !resource.exists()) return ResponseEntity.notFound().build();

        String fileName = cc.getTenFileGoc() != null ? cc.getTenFileGoc() : cc.getFilePdfUrl();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName.replace("\"", "") + "\"")
                .body(resource);
    }

    @GetMapping("/export/class-grades/{lhpId}")
    public ResponseEntity<byte[]> exportGrades(@PathVariable Long lhpId) throws IOException {
        byte[] excelBytes = excelService.exportClassGrades(lhpId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BangDiem_LHP_" + lhpId + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/export/class-attendance/{lhpId}")
    public ResponseEntity<byte[]> exportAttendanceRoster(@PathVariable Long lhpId) throws IOException {
        byte[] excelBytes = excelService.exportClassAttendanceRoster(lhpId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=DiemDanh_SV_LHP_" + lhpId + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }

    @GetMapping("/export/tuition-report")
    public ResponseEntity<byte[]> exportTuitionReport(@RequestParam(required = false) Long hocKyId) throws IOException {
        byte[] excelBytes = excelService.exportTuitionRevenueReport(hocKyId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=BaoCao_DoanhThu_HocPhi.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
