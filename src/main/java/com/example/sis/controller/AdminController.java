package com.example.sis.controller;

import com.example.sis.entity.*;
import com.example.sis.repository.*;
import com.example.sis.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/Admin")
public class AdminController {

    @Autowired private SinhVienRepository sinhVienRepository;
    @Autowired private LopHocPhanRepository lopHocPhanRepository;
    @Autowired private HocPhanRepository hocPhanRepository;
    @Autowired private GiangVienRepository giangVienRepository;
    @Autowired private HocKyRepository hocKyRepository;
    @Autowired private KhoaRepository khoaRepository;
    @Autowired private NganhHocRepository nganhHocRepository;
    @Autowired private KhungChuongTrinhRepository khungChuongTrinhRepository;
    @Autowired private DieuKienHocPhanRepository dieuKienHocPhanRepository;
    @Autowired private LichHocRepository lichHocRepository;
    @Autowired private DiemHocPhanRepository diemHocPhanRepository;
    @Autowired private DiemRenLuyenRepository diemRenLuyenRepository;
    @Autowired private CongNoHocKyRepository congNoHocKyRepository;
    @Autowired private PhieuThuHocPhiRepository phieuThuHocPhiRepository;
    @Autowired private ChungChiChuanDauRaRepository chungChiChuanDauRaRepository;
    @Autowired private YeuCauDoiLichRepository yeuCauDoiLichRepository;
    @Autowired private YeuCauDichVuMotCuaRepository yeuCauDichVuMotCuaRepository;
    @Autowired private NguoiDungRepository nguoiDungRepository;
    @Autowired private LopDanhNghiaRepository lopDanhNghiaRepository;
    @Autowired private ThongBaoRepository thongBaoRepository;
    @Autowired private NhatKyHeThongRepository nhatKyHeThongRepository;

    @Autowired private ExcelService excelService;
    @Autowired private AcademicStandingService academicStandingService;
    @Autowired private TuitionService tuitionService;
    @Autowired private GradingService gradingService;
    @Autowired private ScheduleConflictService scheduleConflictService;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditLogService auditLogService;

    private NguoiDung getAdmin(HttpSession session) {
        NguoiDung u = (NguoiDung) session.getAttribute("currentUser");
        if (u == null) return null;
        if (u.getVaiTro() == VaiTro.ADMIN || u.getVaiTro() == VaiTro.PHONG_DAO_TAO || u.getVaiTro() == VaiTro.KE_TOAN) {
            return u;
        }
        return null;
    }

    @GetMapping({"", "/", "/Dashboard"})
    public String dashboard(HttpSession session, Model model) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";

        model.addAttribute("totalStudents", sinhVienRepository.count());
        model.addAttribute("totalClasses", lopHocPhanRepository.count());
        model.addAttribute("totalFaculty", giangVienRepository.count());
        model.addAttribute("totalSubjects", hocPhanRepository.count());

        long pendingClasses = lopHocPhanRepository.countByTrangThai(LopHocPhan.TrangThaiLHP.CHO_DUYET);
        long pendingSchedules = yeuCauDoiLichRepository.countByTrangThai(YeuCauDoiLich.TrangThaiDuyet.CHO_DUYET);
        long pendingCerts = chungChiChuanDauRaRepository.countByTrangThai(ChungChiChuanDauRa.TrangThaiChungChi.CHO_DUYET);
        model.addAttribute("pendingClasses", pendingClasses);
        model.addAttribute("pendingSchedules", pendingSchedules);
        model.addAttribute("pendingCerts", pendingCerts);

        HocKy activeHocKy = hocKyRepository.findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO)
                .orElse(hocKyRepository.findAll().stream().findFirst().orElse(null));
        model.addAttribute("activeHocKy", activeHocKy);

        List<SinhVien> warnedList = new ArrayList<>();
        warnedList.addAll(sinhVienRepository.findByTrangThaiHocVu("CANH_BAO_1"));
        warnedList.addAll(sinhVienRepository.findByTrangThaiHocVu("CANH_BAO_2"));
        warnedList.addAll(sinhVienRepository.findByTrangThaiHocVu("BUOC_THOI_HOC"));
        model.addAttribute("warnedStudents", warnedList);

        if (activeHocKy != null) {
            List<CongNoHocKy> debtList = congNoHocKyRepository.findByHocKyId(activeHocKy.getId());
            double totalDebt = debtList.stream().mapToDouble(c -> c.getConNo() != null ? c.getConNo() : 0.0).sum();
            double totalCollected = debtList.stream().mapToDouble(c -> c.getDaDong() != null ? c.getDaDong() : 0.0).sum();
            model.addAttribute("totalDebt", totalDebt);
            model.addAttribute("totalCollected", totalCollected);
            model.addAttribute("debtList", debtList);
        }

        model.addAttribute("recentLogs", nhatKyHeThongRepository.findTop100ByOrderByThoiGianDesc().stream().limit(8).toList());
        return "admin/dashboard";
    }

    // ==================== 1. TRUNG TÂM PHÊ DUYỆT (APPROVAL CENTER) ====================

    @GetMapping("/Approvals")
    public String approvals(HttpSession session, Model model, @RequestParam(defaultValue = "classes") String tab) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";

        model.addAttribute("activeTab", tab);
        model.addAttribute("pendingClasses", lopHocPhanRepository.findByTrangThaiOrderByNgayDeXuatDesc(LopHocPhan.TrangThaiLHP.CHO_DUYET));
        model.addAttribute("pendingSchedules", yeuCauDoiLichRepository.findByTrangThaiOrderByNgayTaoDesc(YeuCauDoiLich.TrangThaiDuyet.CHO_DUYET));
        model.addAttribute("allSchedules", yeuCauDoiLichRepository.findAllByOrderByNgayTaoDesc());
        model.addAttribute("pendingCerts", chungChiChuanDauRaRepository.findByTrangThaiOrderByNgayNopDesc(ChungChiChuanDauRa.TrangThaiChungChi.CHO_DUYET));
        model.addAttribute("allCerts", chungChiChuanDauRaRepository.findAllByOrderByNgayNopDesc());
        return "admin/approvals";
    }

    @PostMapping("/Approvals/Class/{id}/Approve")
    public String approveClassProposal(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        LopHocPhan lhp = lopHocPhanRepository.findById(id).orElseThrow();
        lhp.setTrangThai(LopHocPhan.TrangThaiLHP.DANG_MO);
        lhp.setLyDoTuChoi(null);
        lopHocPhanRepository.save(lhp);

        // Tự động tạo lịch học chính thức từ khung giờ giảng viên đề xuất
        if (lhp.getDanhSachLichHoc().isEmpty() && lhp.getThuDuKien() != null && lhp.getTietBatDauDuKien() != null) {
            LichHoc lh = LichHoc.builder()
                    .lopHocPhan(lhp)
                    .thu(lhp.getThuDuKien())
                    .tietBatDau(lhp.getTietBatDauDuKien())
                    .soTiet(lhp.getSoTietDuKien() != null ? lhp.getSoTietDuKien() : 3)
                    .phongHoc(lhp.getPhongMacDinh() != null ? lhp.getPhongMacDinh() : "A101")
                    .tuTuan(1).denTuan(15)
                    .loaiLich(LichHoc.LoaiLich.LY_THUYET)
                    .build();
            lichHocRepository.save(lh);
        }

        if (lhp.getGiangVien() != null) {
            notificationService.sendToUser(admin, lhp.getGiangVien().getNguoiDung(),
                    "[Đã duyệt mở lớp] " + lhp.getMaLHP() + " - " + lhp.getHocPhan().getTenHP(),
                    "Phòng Đào Tạo đã phê duyệt mở lớp học phần " + lhp.getMaLHP() + ". Lớp đã được mở trên cổng Đăng ký Học phần cho sinh viên đăng ký!",
                    ThongBao.LoaiThongBao.MO_LOP, "/Faculty/Classes");
        }
        auditLogService.log(admin, "DUYET_MO_LOP", "LopHocPhan#" + lhp.getMaLHP(),
                "Phê duyệt mở lớp học phần " + lhp.getMaLHP() + " (" + lhp.getHocPhan().getTenHP() + ") -> DANG_MO");

        ra.addFlashAttribute("success", "Đã duyệt mở lớp " + lhp.getMaLHP() + "! Lớp lập tức hiển thị trên Cổng Đăng Ký Học Phần của Sinh viên.");
        return "redirect:/Admin/Approvals?tab=classes";
    }

    @PostMapping("/Approvals/Class/{id}/Reject")
    public String rejectClassProposal(@PathVariable Long id, @RequestParam String lyDo, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        LopHocPhan lhp = lopHocPhanRepository.findById(id).orElseThrow();
        lhp.setTrangThai(LopHocPhan.TrangThaiLHP.TU_CHOI);
        lhp.setLyDoTuChoi(lyDo);
        lopHocPhanRepository.save(lhp);

        if (lhp.getGiangVien() != null) {
            notificationService.sendToUser(admin, lhp.getGiangVien().getNguoiDung(),
                    "[Từ chối mở lớp] " + lhp.getMaLHP(),
                    "Đề xuất mở lớp " + lhp.getMaLHP() + " (" + lhp.getHocPhan().getTenHP() + ") bị từ chối. Lý do: " + lyDo,
                    ThongBao.LoaiThongBao.MO_LOP, "/Faculty/Proposals");
        }
        auditLogService.log(admin, "TU_CHOI_MO_LOP", "LopHocPhan#" + lhp.getMaLHP(), "Từ chối lớp " + lhp.getMaLHP() + ": " + lyDo);

        ra.addFlashAttribute("success", "Đã từ chối đề xuất mở lớp " + lhp.getMaLHP() + " và gửi thông báo cho Giảng viên.");
        return "redirect:/Admin/Approvals?tab=classes";
    }

    @PostMapping("/Approvals/Schedule/{id}/Approve")
    public String approveScheduleChange(@PathVariable Long id, @RequestParam(required = false) String phanHoi,
                                        HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        try {
            scheduleConflictService.approveRequest(id, admin, phanHoi);
            ra.addFlashAttribute("success", "Đã phê duyệt đổi lịch! Thời khóa biểu chính thức đã được cập nhật và tự động gửi thông báo tới Giảng viên & toàn bộ Sinh viên lớp.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể duyệt đổi lịch: " + e.getMessage());
        }
        return "redirect:/Admin/Approvals?tab=schedules";
    }

    @PostMapping("/Approvals/Schedule/{id}/Reject")
    public String rejectScheduleChange(@PathVariable Long id, @RequestParam String lyDo,
                                       HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        scheduleConflictService.rejectRequest(id, admin, lyDo);
        ra.addFlashAttribute("success", "Đã từ chối yêu cầu đổi lịch và thông báo tới Giảng viên.");
        return "redirect:/Admin/Approvals?tab=schedules";
    }

    @PostMapping("/Approvals/Certificate/{id}/Approve")
    public String approveCertificate(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        ChungChiChuanDauRa cc = chungChiChuanDauRaRepository.findById(id).orElseThrow();
        cc.setTrangThai(ChungChiChuanDauRa.TrangThaiChungChi.DA_DAT);
        cc.setLyDoTuChoi(null);
        cc.setNgayDuyet(LocalDateTime.now());
        chungChiChuanDauRaRepository.save(cc);

        notificationService.sendToUser(admin, cc.getSinhVien().getNguoiDung(),
                "[Đã duyệt chứng chỉ] " + cc.getLoaiChungChi().getLabel(),
                "Chứng chỉ " + cc.getLoaiChungChi().getLabel() + " (Số hiệu: " + cc.getSoHieu() + ") của bạn đã được Phòng Đào Tạo phê duyệt ĐẠT CHUẨN ĐẦU RA.",
                ThongBao.LoaiThongBao.CHUNG_CHI, "/Student/Certificates");

        auditLogService.log(admin, "DUYET_CHUNG_CHI", "ChungChi#" + cc.getId(),
                "Duyệt đạt chuẩn đầu ra " + cc.getLoaiChungChi().getLabel() + " cho SV " + cc.getSinhVien().getMssv());

        ra.addFlashAttribute("success", "Đã phê duyệt chứng chỉ ĐẠT chuẩn đầu ra cho SV " + cc.getSinhVien().getMssv() + "!");
        return "redirect:/Admin/Approvals?tab=certificates";
    }

    @PostMapping("/Approvals/Certificate/{id}/Reject")
    public String rejectCertificate(@PathVariable Long id, @RequestParam String lyDo, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        ChungChiChuanDauRa cc = chungChiChuanDauRaRepository.findById(id).orElseThrow();
        cc.setTrangThai(ChungChiChuanDauRa.TrangThaiChungChi.KHONG_DAT);
        cc.setLyDoTuChoi(lyDo);
        cc.setNgayDuyet(LocalDateTime.now());
        chungChiChuanDauRaRepository.save(cc);

        notificationService.sendToUser(admin, cc.getSinhVien().getNguoiDung(),
                "[Từ chối chứng chỉ] " + cc.getLoaiChungChi().getLabel(),
                "Chứng chỉ " + cc.getLoaiChungChi().getLabel() + " (Số hiệu: " + cc.getSoHieu() + ") chưa được duyệt. Lý do: " + lyDo,
                ThongBao.LoaiThongBao.CHUNG_CHI, "/Student/Certificates");

        auditLogService.log(admin, "TU_CHOI_CHUNG_CHI", "ChungChi#" + cc.getId(),
                "Từ chối chứng chỉ SV " + cc.getSinhVien().getMssv() + ". Lý do: " + lyDo);

        ra.addFlashAttribute("success", "Đã từ chối chứng chỉ và gửi lý do vào hộp thư Sinh viên.");
        return "redirect:/Admin/Approvals?tab=certificates";
    }

    // ==================== 2. QUẢN LÝ ĐÀO TẠO ====================

    @GetMapping("/Curriculum")
    public String curriculumManage(HttpSession session, Model model, @RequestParam(required = false) Long nganhId) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";

        List<NganhHoc> majors = nganhHocRepository.findAll();
        NganhHoc selectedMajor = nganhId != null
                ? nganhHocRepository.findById(nganhId).orElse(majors.get(0))
                : majors.stream().filter(m -> m.getId().equals(4L)).findFirst().orElse(majors.isEmpty() ? null : majors.get(0));

        model.addAttribute("majors", majors);
        model.addAttribute("selectedMajor", selectedMajor);
        model.addAttribute("khoas", khoaRepository.findAll());
        model.addAttribute("subjects", hocPhanRepository.findAll());
        model.addAttribute("prereqs", dieuKienHocPhanRepository.findAll());
        if (selectedMajor != null) {
            model.addAttribute("kctList", khungChuongTrinhRepository.findByNganhHocIdOrderByHocKyDuKienAsc(selectedMajor.getId()));
        }
        return "admin/curriculum";
    }

    @PostMapping("/Curriculum/Major/Save")
    public String saveMajor(@RequestParam(required = false) Long id,
                            @RequestParam String maNganh, @RequestParam String tenNganh,
                            @RequestParam Long khoaId, @RequestParam Integer tongTinChiYeuCau,
                            @RequestParam(required = false) Double donGiaTinChiRieng,
                            HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        Khoa khoa = khoaRepository.findById(khoaId).orElseThrow();
        NganhHoc nh = (id != null)
                ? nganhHocRepository.findById(id).orElse(new NganhHoc())
                : nganhHocRepository.findByMaNganh(maNganh.trim()).orElse(new NganhHoc());
        nh.setMaNganh(maNganh.trim().toUpperCase());
        nh.setTenNganh(tenNganh.trim());
        nh.setKhoa(khoa);
        nh.setTongTinChiYeuCau(tongTinChiYeuCau);
        nh.setDonGiaTinChiRieng(donGiaTinChiRieng);
        nganhHocRepository.save(nh);

        auditLogService.log(admin, "CAP_NHAT_NGANH", "NganhHoc#" + nh.getMaNganh(), "Lưu ngành " + nh.getTenNganh() + " (" + tongTinChiYeuCau + " TC)");
        ra.addFlashAttribute("success", "Đã lưu ngành đào tạo [" + nh.getMaNganh() + " - " + nh.getTenNganh() + "] thành công!");
        return "redirect:/Admin/Curriculum?nganhId=" + nh.getId();
    }

    @PostMapping("/Curriculum/Subject/Save")
    public String saveSubject(@RequestParam(required = false) Long id,
                              @RequestParam String maHP, @RequestParam String tenHP,
                              @RequestParam Integer soTCLyThuyet, @RequestParam Integer soTCThucHanh,
                              @RequestParam(defaultValue = "1.0") Double heSoHocPhi,
                              @RequestParam Long khoaId,
                              HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        Khoa khoa = khoaRepository.findById(khoaId).orElseThrow();
        HocPhan hp = (id != null)
                ? hocPhanRepository.findById(id).orElse(new HocPhan())
                : hocPhanRepository.findByMaHP(maHP.trim()).orElse(new HocPhan());
        hp.setMaHP(maHP.trim().toUpperCase());
        hp.setTenHP(tenHP.trim());
        hp.setSoTCLyThuyet(soTCLyThuyet);
        hp.setSoTCThucHanh(soTCThucHanh);
        hp.setTongTinChi(soTCLyThuyet + soTCThucHanh);
        hp.setHeSoHocPhi(heSoHocPhi);
        hp.setKhoa(khoa);
        hocPhanRepository.save(hp);

        auditLogService.log(admin, "CAP_NHAT_HOC_PHAN", "HocPhan#" + hp.getMaHP(), "Lưu học phần " + hp.getTenHP() + " (" + hp.getTongTinChi() + " TC)");
        ra.addFlashAttribute("success", "Đã lưu học phần [" + hp.getMaHP() + " - " + hp.getTenHP() + "]!");
        return "redirect:/Admin/Curriculum";
    }

    @PostMapping("/Curriculum/Assign")
    public String assignToCurriculum(@RequestParam Long nganhHocId, @RequestParam Long hocPhanId,
                                     @RequestParam Integer hocKyDuKien, @RequestParam(defaultValue = "true") Boolean batBuoc,
                                     HttpSession session, RedirectAttributes ra) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";
        NganhHoc nh = nganhHocRepository.findById(nganhHocId).orElseThrow();
        HocPhan hp = hocPhanRepository.findById(hocPhanId).orElseThrow();

        boolean exists = khungChuongTrinhRepository.findByNganhHocId(nganhHocId).stream()
                .anyMatch(k -> k.getHocPhan() != null && k.getHocPhan().getId().equals(hocPhanId));
        if (exists) {
            ra.addFlashAttribute("error", "Môn [" + hp.getMaHP() + " - " + hp.getTenHP() + "] đã có trong chương trình khung ngành " + nh.getTenNganh() + "!");
            return "redirect:/Admin/Curriculum?nganhId=" + nganhHocId;
        }
        KhungChuongTrinh kct = KhungChuongTrinh.builder()
                .nganhHoc(nh).hocPhan(hp).hocKyDuKien(hocKyDuKien).batBuoc(batBuoc).build();
        khungChuongTrinhRepository.save(kct);
        ra.addFlashAttribute("success", "Đã gán môn [" + hp.getMaHP() + "] vào HK" + hocKyDuKien + " của ngành " + nh.getTenNganh() + "!");
        return "redirect:/Admin/Curriculum?nganhId=" + nganhHocId;
    }

    @PostMapping("/Curriculum/Remove/{kctId}")
    public String removeFromCurriculum(@PathVariable Long kctId, @RequestParam Long nganhHocId,
                                       HttpSession session, RedirectAttributes ra) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";
        khungChuongTrinhRepository.deleteById(kctId);
        ra.addFlashAttribute("success", "Đã gỡ học phần khỏi chương trình khung!");
        return "redirect:/Admin/Curriculum?nganhId=" + nganhHocId;
    }

    @PostMapping("/Curriculum/Prerequisite/Add")
    public String addPrerequisite(@RequestParam Long hocPhanId, @RequestParam Long hocPhanDieuKienId,
                                  @RequestParam(defaultValue = "TIEN_QUYET") DieuKienHocPhan.LoaiDieuKien loaiDieuKien,
                                  HttpSession session, RedirectAttributes ra) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";
        if (hocPhanId.equals(hocPhanDieuKienId)) {
            ra.addFlashAttribute("error", "Một môn học không thể là tiên quyết của chính nó!");
            return "redirect:/Admin/Curriculum";
        }
        DieuKienHocPhan dk = DieuKienHocPhan.builder()
                .hocPhan(hocPhanRepository.findById(hocPhanId).orElseThrow())
                .hocPhanDieuKien(hocPhanRepository.findById(hocPhanDieuKienId).orElseThrow())
                .loaiDieuKien(loaiDieuKien).build();
        dieuKienHocPhanRepository.save(dk);
        ra.addFlashAttribute("success", "Đã thêm ràng buộc tiên quyết thành công!");
        return "redirect:/Admin/Curriculum";
    }

    @GetMapping("/Semesters")
    public String semesters(HttpSession session, Model model) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";
        model.addAttribute("semesters", hocKyRepository.findAll());
        return "admin/semesters";
    }

    @PostMapping("/Semesters/Create")
    public String createSemester(@RequestParam String maHocKy, @RequestParam String tenHocKy,
                                 @RequestParam String namHoc, @RequestParam HocKy.LoaiHocKy loaiHocKy,
                                 @RequestParam Double donGiaTinChi,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayBatDauDkhp,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayKetThucDkhp,
                                 HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        HocKy hk = HocKy.builder()
                .maHocKy(maHocKy.trim())
                .tenHocKy(tenHocKy.trim())
                .namHoc(namHoc.trim())
                .loaiHocKy(loaiHocKy)
                .donGiaTinChi(donGiaTinChi)
                .ngayBatDauDkhp(ngayBatDauDkhp)
                .ngayKetThucDkhp(ngayKetThucDkhp)
                .trangThaiDangKy(HocKy.TrangThaiDangKy.CHUA_MO)
                .choPhepNhapDiem(true)
                .build();
        hocKyRepository.save(hk);
        auditLogService.log(admin, "TAO_HOC_KY", "HocKy#" + maHocKy, "Tạo học kỳ " + tenHocKy);
        ra.addFlashAttribute("success", "Đã tạo mới học kỳ " + tenHocKy + "!");
        return "redirect:/Admin/Semesters";
    }

    @PostMapping("/Semesters/{id}/RegistrationStatus")
    public String updateRegistrationStatus(@PathVariable Long id, @RequestParam HocKy.TrangThaiDangKy status,
                                           HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        HocKy hk = hocKyRepository.findById(id).orElseThrow();
        hk.setTrangThaiDangKy(status);
        hocKyRepository.save(hk);
        auditLogService.log(admin, "CAP_NHAT_CONG_DKHP", "HocKy#" + hk.getMaHocKy(), "Đổi cổng ĐKHP -> " + status.name());
        ra.addFlashAttribute("success", "Đã cập nhật trạng thái cổng ĐKHP " + hk.getTenHocKy() + " -> " + status.name());
        return "redirect:/Admin/Semesters";
    }

    @PostMapping("/Semesters/{id}/ToggleGrading")
    public String toggleGradingPortal(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        HocKy hk = hocKyRepository.findById(id).orElseThrow();
        boolean next = Boolean.FALSE.equals(hk.getChoPhepNhapDiem());
        hk.setChoPhepNhapDiem(next);
        hocKyRepository.save(hk);
        auditLogService.log(admin, next ? "MO_CONG_NHAP_DIEM" : "DONG_CONG_NHAP_DIEM", "HocKy#" + hk.getMaHocKy(),
                (next ? "Mở" : "Đóng") + " cổng nhập điểm " + hk.getTenHocKy());
        ra.addFlashAttribute("success", "Đã " + (next ? "MỞ" : "ĐÓNG") + " cổng nhập điểm cho " + hk.getTenHocKy() + "!");
        return "redirect:/Admin/Semesters";
    }

    @GetMapping("/Students")
    public String studentList(HttpSession session, Model model, @RequestParam(required = false) String search) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";

        List<SinhVien> list = (search != null && !search.trim().isEmpty())
                ? sinhVienRepository.searchSinhVien(search.trim())
                : sinhVienRepository.findAll();
        model.addAttribute("students", list);
        model.addAttribute("searchQuery", search);
        return "admin/students";
    }

    @PostMapping("/Students/Import")
    public String importStudents(@RequestParam("file") MultipartFile file, HttpSession session, RedirectAttributes ra) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";
        if (file.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn file Excel .xlsx");
            return "redirect:/Admin/Students";
        }
        try {
            List<SinhVien> imported = excelService.importStudents(file);
            ra.addFlashAttribute("success", "Đã import thành công " + imported.size() + " sinh viên từ file Excel!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi khi import file Excel: " + e.getMessage());
        }
        return "redirect:/Admin/Students";
    }

    @GetMapping("/Classes")
    public String classList(HttpSession session, Model model) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";

        model.addAttribute("classes", lopHocPhanRepository.findAll());
        model.addAttribute("subjects", hocPhanRepository.findAll());
        model.addAttribute("semesters", hocKyRepository.findAll());
        model.addAttribute("faculties", giangVienRepository.findAll());
        model.addAttribute("schedules", lichHocRepository.findAll());
        return "admin/classes";
    }

    @PostMapping("/Classes/Create")
    public String createClass(@ModelAttribute LopHocPhan lhp,
                              @RequestParam Long hocPhanId,
                              @RequestParam Long hocKyId,
                              @RequestParam(required = false) Long giangVienId,
                              HttpSession session,
                              RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        HocPhan hp = hocPhanRepository.findById(hocPhanId).orElseThrow();
        HocKy hk = hocKyRepository.findById(hocKyId).orElseThrow();
        GiangVien gv = giangVienId != null ? giangVienRepository.findById(giangVienId).orElse(null) : null;

        lhp.setHocPhan(hp);
        lhp.setHocKy(hk);
        lhp.setGiangVien(gv);
        lhp.setSiSoHienTai(0);
        lhp.setTrangThai(LopHocPhan.TrangThaiLHP.DANG_MO);
        lopHocPhanRepository.save(lhp);

        auditLogService.log(admin, "TAO_LOP_HOC_PHAN", "LopHocPhan#" + lhp.getMaLHP(), "Tạo lớp " + lhp.getTenLHP());
        ra.addFlashAttribute("success", "Tạo mới lớp học phần " + lhp.getMaLHP() + " thành công!");
        return "redirect:/Admin/Classes";
    }

    @PostMapping("/Classes/Schedule/Save")
    public String adminSaveSchedule(@RequestParam(required = false) Long lichHocId,
                                    @RequestParam Long lopHocPhanId,
                                    @RequestParam Integer thu,
                                    @RequestParam Integer tietBatDau,
                                    @RequestParam Integer soTiet,
                                    @RequestParam String phongHoc,
                                    @RequestParam(defaultValue = "1") Integer tuTuan,
                                    @RequestParam(defaultValue = "15") Integer denTuan,
                                    @RequestParam(defaultValue = "LY_THUYET") LichHoc.LoaiLich loaiLich,
                                    HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId).orElseThrow();

        // Kiểm tra xung đột phòng học, giảng viên và sinh viên trước khi lưu lịch trực tiếp
        ScheduleConflictService.ConflictReport conflict = scheduleConflictService.checkConflict(
                lopHocPhanId, lichHocId, thu, tietBatDau, soTiet, phongHoc, tuTuan, denTuan);
        if (!conflict.canSubmit) {
            ra.addFlashAttribute("error", "Không thể lưu lịch học do xung đột: " + conflict.summary);
            return "redirect:/Admin/Classes";
        }

        LichHoc lh = (lichHocId != null)
                ? lichHocRepository.findById(lichHocId).orElse(new LichHoc())
                : new LichHoc();
        lh.setLopHocPhan(lhp);
        lh.setThu(thu);
        lh.setTietBatDau(tietBatDau);
        lh.setSoTiet(soTiet);
        lh.setPhongHoc(phongHoc.trim());
        lh.setTuTuan(tuTuan);
        lh.setDenTuan(denTuan);
        lh.setLoaiLich(loaiLich);
        lichHocRepository.save(lh);

        String msg = String.format("Phòng Đào Tạo đã xếp/điều chỉnh lịch (%s) lớp %s (%s): Thứ %d, Tiết %d-%d, Phòng %s (Tuần %d-%d).",
                loaiLich.name(), lhp.getMaLHP(), lhp.getHocPhan().getTenHP(), thu, tietBatDau, tietBatDau + soTiet - 1, phongHoc, tuTuan, denTuan);
        if (lhp.getGiangVien() != null) {
            notificationService.sendToUser(admin, lhp.getGiangVien().getNguoiDung(), "[Điều chỉnh lịch] " + lhp.getMaLHP(), msg, ThongBao.LoaiThongBao.DOI_LICH, "/Faculty/Schedule");
        }
        notificationService.sendToClassStudents(admin, lhp.getId(), "[Cập nhật lịch] " + lhp.getMaLHP(), msg, ThongBao.LoaiThongBao.DOI_LICH, "/Student/Timetable");
        auditLogService.log(admin, "SUA_LICH_TRUC_TIEP", "LopHocPhan#" + lhp.getMaLHP(), msg);

        ra.addFlashAttribute("success", "Đã cập nhật lịch trực tiếp cho lớp " + lhp.getMaLHP() + " và gửi thông báo tới GV & Sinh viên!");
        return "redirect:/Admin/Classes";
    }

    @PostMapping("/Classes/Schedule/Delete/{id}")
    public String deleteSchedule(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        lichHocRepository.deleteById(id);
        auditLogService.log(admin, "XOA_LICH_HOC", "LichHoc#" + id, "Xóa buổi học ID " + id);
        ra.addFlashAttribute("success", "Đã xóa buổi học khỏi thời khóa biểu.");
        return "redirect:/Admin/Classes";
    }

    @GetMapping("/Grades")
    public String adminGrades(HttpSession session, Model model, @RequestParam(required = false) Long lopHocPhanId) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";

        model.addAttribute("classes", lopHocPhanRepository.findAll());
        if (lopHocPhanId != null) {
            LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId).orElse(null);
            model.addAttribute("selectedClass", lhp);
            model.addAttribute("gradeList", diemHocPhanRepository.findByLopHocPhanId(lopHocPhanId));
        }
        model.addAttribute("conductList", diemRenLuyenRepository.findAll());
        return "admin/grades";
    }

    @PostMapping("/Grades/Update")
    public String adminUpdateGrade(@RequestParam Long diemId, @RequestParam Long lopHocPhanId,
                                   @RequestParam(required = false) Double cc,
                                   @RequestParam(required = false) Double qt,
                                   @RequestParam(required = false) Double th,
                                   @RequestParam(required = false) Double ck,
                                   @RequestParam(required = false) String lyDoPhucKhao,
                                   HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        DiemHocPhan d = diemHocPhanRepository.findById(diemId).orElseThrow();
        Double oldTotal = d.getDiemTongKetHe10();
        if (cc != null) d.setDiemChuyenCan(cc);
        if (qt != null) d.setDiemQuaTrinh(qt);
        if (th != null) d.setDiemThucHanh(th);
        if (ck != null) d.setDiemCuoiKy(ck);
        gradingService.calculateComponentGrades(d);

        SinhVien sv = d.getDangKyHocPhan().getSinhVien();
        auditLogService.log(admin, "SUA_DIEM_PHUC_KHAO", "DiemHocPhan#" + d.getId(),
                String.format("Phúc khảo điểm SV %s lớp %s: %.1f -> %.1f (%s). Lý do: %s",
                        sv.getMssv(), d.getDangKyHocPhan().getLopHocPhan().getMaLHP(),
                        oldTotal != null ? oldTotal : 0.0, d.getDiemTongKetHe10(), d.getDiemChu(), lyDoPhucKhao));
        notificationService.sendToUser(admin, sv.getNguoiDung(),
                "Cập nhật kết quả phúc khảo điểm: " + d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getTenHP(),
                "Điểm tổng kết mới của bạn là " + d.getDiemTongKetHe10() + " (Điểm chữ: " + d.getDiemChu() + "). Ghi chú: " + lyDoPhucKhao,
                ThongBao.LoaiThongBao.HE_THONG, "/Student/Grades");

        ra.addFlashAttribute("success", "Đã điều chỉnh điểm phúc khảo cho SV " + sv.getMssv() + " và ghi nhận vào Nhật ký hệ thống!");
        return "redirect:/Admin/Grades?lopHocPhanId=" + lopHocPhanId;
    }

    @PostMapping("/Grades/Unlock/{lhpId}")
    public String unlockClassGrades(@PathVariable Long lhpId, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        List<DiemHocPhan> grades = diemHocPhanRepository.findByLopHocPhanId(lhpId);
        for (DiemHocPhan d : grades) {
            d.setKhoaDiem(false);
            diemHocPhanRepository.save(d);
        }
        auditLogService.log(admin, "MO_KHOA_DIEM", "LopHocPhan#" + lhpId, "Mở khóa bảng điểm cho giảng viên chỉnh sửa");
        ra.addFlashAttribute("success", "Đã mở khóa bảng điểm lớp học phần thành công!");
        return "redirect:/Admin/Grades?lopHocPhanId=" + lhpId;
    }

    @PostMapping("/Conduct/Finalize")
    public String finalizeConduct(@RequestParam Long conductId, @RequestParam Integer diemKhoaDuyet,
                                  HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        DiemRenLuyen drl = diemRenLuyenRepository.findById(conductId).orElseThrow();
        int score = Math.max(0, Math.min(100, diemKhoaDuyet));
        drl.setDiemKhoaDuyet(score);
        drl.setXepLoai(DiemRenLuyen.classify(score));
        drl.setTrangThai("DA_CHOT");
        diemRenLuyenRepository.save(drl);

        auditLogService.log(admin, "CHOT_DIEM_REN_LUYEN", "DiemRenLuyen#" + drl.getId(),
                "Chốt DRL SV " + drl.getSinhVien().getMssv() + " HK " + drl.getHocKy().getMaHocKy() + ": " + score + " (" + drl.getXepLoai() + ")");
        ra.addFlashAttribute("success", "Đã chốt điểm rèn luyện chính thức cho SV " + drl.getSinhVien().getMssv() + "!");
        return "redirect:/Admin/Grades";
    }

    @PostMapping("/Warnings/Audit")
    public String auditAcademicWarnings(HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        List<SinhVien> warned = academicStandingService.auditAcademicWarnings();
        auditLogService.log(admin, "RA_SOAT_HOC_VU", "ToanTruong", "Phát hiện " + warned.size() + " SV diện cảnh báo/buộc thôi học");
        ra.addFlashAttribute("success", "Đã hoàn thành rà soát học vụ! Phát hiện " + warned.size() + " sinh viên thuộc diện cảnh báo/buộc thôi học.");
        return "redirect:/Admin/Dashboard";
    }

    // ==================== 3. QUẢN LÝ TÀI CHÍNH ====================

    @GetMapping("/Tuition")
    public String tuitionManage(HttpSession session, Model model, @RequestParam(required = false) String statusFilter) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";

        List<CongNoHocKy> allDebts = congNoHocKyRepository.findAll();
        List<CongNoHocKy> filteredDebts = (statusFilter != null && !statusFilter.isBlank() && !"ALL".equals(statusFilter))
                ? allDebts.stream().filter(d -> d.getTrangThai().name().equals(statusFilter)).toList()
                : allDebts;

        // Thống kê doanh thu theo học kỳ
        List<HocKy> semesters = hocKyRepository.findAll();
        List<Map<String, Object>> revenueBySemester = new ArrayList<>();
        for (HocKy hk : semesters) {
            List<CongNoHocKy> hkDebts = congNoHocKyRepository.findByHocKyId(hk.getId());
            double tongPhatSinh = hkDebts.stream().mapToDouble(c -> c.getTongHocPhi() != null ? c.getTongHocPhi() : 0).sum();
            double daThu = hkDebts.stream().mapToDouble(c -> c.getDaDong() != null ? c.getDaDong() : 0).sum();
            double conNo = hkDebts.stream().mapToDouble(c -> c.getConNo() != null ? c.getConNo() : 0).sum();
            Map<String, Object> stat = new LinkedHashMap<>();
            stat.put("hocKy", hk);
            stat.put("tongPhatSinh", tongPhatSinh);
            stat.put("daThu", daThu);
            stat.put("conNo", conNo);
            stat.put("soHoSo", hkDebts.size());
            revenueBySemester.add(stat);
        }

        model.addAttribute("debts", filteredDebts);
        model.addAttribute("allDebts", allDebts);
        model.addAttribute("receipts", phieuThuHocPhiRepository.findAll());
        model.addAttribute("semesters", semesters);
        model.addAttribute("majors", nganhHocRepository.findAll());
        model.addAttribute("revenueBySemester", revenueBySemester);
        model.addAttribute("statusFilter", statusFilter != null ? statusFilter : "ALL");
        return "admin/tuition_manage";
    }

    @PostMapping("/Tuition/Config")
    public String configTuition(@RequestParam Long hocKyId,
                                @RequestParam Double donGiaTinChi,
                                @RequestParam(defaultValue = "1.5") Double heSoHocLai,
                                @RequestParam(defaultValue = "1.2") Double heSoCaiThien,
                                @RequestParam(defaultValue = "true") Boolean syncNow,
                                HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        HocKy hk = hocKyRepository.findById(hocKyId).orElseThrow();
        hk.setDonGiaTinChi(donGiaTinChi);
        hk.setHeSoHocLai(heSoHocLai);
        hk.setHeSoCaiThien(heSoCaiThien);
        hocKyRepository.save(hk);

        int synced = Boolean.TRUE.equals(syncNow) ? tuitionService.syncAllTuitionForSemester(hocKyId) : 0;
        auditLogService.log(admin, "CAU_HINH_HOC_PHI", "HocKy#" + hk.getMaHocKy(),
                String.format("Đơn giá: %,.0f đ/TC • HS Học lại: %.1f • HS Cải thiện: %.1f • Đồng bộ %d SV",
                        donGiaTinChi, heSoHocLai, heSoCaiThien, synced));
        ra.addFlashAttribute("success", "Đã cập nhật cấu hình học phí " + hk.getTenHocKy() + " và đồng bộ công nợ cho " + synced + " sinh viên!");
        return "redirect:/Admin/Tuition";
    }

    @PostMapping("/Tuition/RecordPayment")
    public String recordPayment(@RequestParam Long congNoId,
                                @RequestParam Double soTien,
                                @RequestParam PhieuThuHocPhi.PhuongThucThanhToan phuongThuc,
                                @RequestParam(required = false) String ghiChu,
                                HttpSession session,
                                RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        try {
            PhieuThuHocPhi pt = tuitionService.createPayment(congNoId, soTien, phuongThuc, admin.getHoTen(), ghiChu);
            CongNoHocKy cn = pt.getCongNoHocKy();
            notificationService.sendToUser(admin, cn.getSinhVien().getNguoiDung(),
                    "Xác nhận phiếu thu học phí: " + pt.getMaPhieuThu(),
                    String.format("Phòng Tài chính Kế toán đã xác nhận thu %,.0f VNĐ cho %s. Số dư còn nợ: %,.0f VNĐ.",
                            soTien, cn.getHocKy().getTenHocKy(), cn.getConNo()),
                    ThongBao.LoaiThongBao.HOC_PHI, "/Student/Receipts");
            auditLogService.log(admin, "XAC_NHAN_THANH_TOAN", "PhieuThu#" + pt.getMaPhieuThu(),
                    "Xác nhận thu " + String.format("%,.0f", soTien) + " đ của SV " + cn.getSinhVien().getMssv() + " (" + phuongThuc + ")");
            ra.addFlashAttribute("success", "Thu học phí thành công! Mã phiếu thu: " + pt.getMaPhieuThu());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/Admin/Tuition";
    }

    @PostMapping("/Tuition/RemindDebt/{congNoId}")
    public String remindDebt(@PathVariable Long congNoId, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        CongNoHocKy cn = congNoHocKyRepository.findById(congNoId).orElseThrow();
        notificationService.sendToUser(admin, cn.getSinhVien().getNguoiDung(),
                "[Nhắc nợ học phí] " + cn.getHocKy().getTenHocKy(),
                String.format("Bạn hiện còn công nợ học phí %,.0f VNĐ của %s. Vui lòng thanh toán trực tuyến qua VietQR/chuyển khoản trước hạn quy định.",
                        cn.getConNo(), cn.getHocKy().getTenHocKy()),
                ThongBao.LoaiThongBao.HOC_PHI, "/Student/Tuition");
        ra.addFlashAttribute("success", "Đã gửi thông báo nhắc nợ học phí vào hộp thư SV " + cn.getSinhVien().getMssv() + "!");
        return "redirect:/Admin/Tuition";
    }

    // ==================== 4. NGƯỜI DÙNG, THÔNG BÁO & AUDIT LOG ====================

    @GetMapping("/Users")
    public String usersManage(HttpSession session, Model model) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";

        model.addAttribute("users", nguoiDungRepository.findAll());
        model.addAttribute("roles", VaiTro.values());
        model.addAttribute("khoas", khoaRepository.findAll());
        model.addAttribute("majors", nganhHocRepository.findAll());
        model.addAttribute("classes", lopDanhNghiaRepository.findAll());
        return "admin/users";
    }

    @PostMapping("/Users/Create")
    public String createUser(@RequestParam String maDinhDanh, @RequestParam String hoTen,
                             @RequestParam String email, @RequestParam String matKhau,
                             @RequestParam(required = false) String soDienThoai,
                             @RequestParam VaiTro vaiTro,
                             @RequestParam(required = false) Long khoaId,
                             @RequestParam(required = false) Long nganhHocId,
                             @RequestParam(required = false) Long lopDanhNghiaId,
                             HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        String cleanId = maDinhDanh.trim();
        String cleanEmail = email.trim();
        if (nguoiDungRepository.findByMaDinhDanh(cleanId).isPresent()) {
            ra.addFlashAttribute("error", "Không thể tạo tài khoản: Mã định danh [" + cleanId + "] đã tồn tại!");
            return "redirect:/Admin/Users";
        }
        if (nguoiDungRepository.findByEmail(cleanEmail).isPresent()) {
            ra.addFlashAttribute("error", "Không thể tạo tài khoản: Email [" + cleanEmail + "] đã được sử dụng!");
            return "redirect:/Admin/Users";
        }

        try {
            NguoiDung user = NguoiDung.builder()
                    .maDinhDanh(cleanId)
                    .hoTen(hoTen.trim())
                    .email(cleanEmail)
                    .matKhau(matKhau.trim())
                    .soDienThoai(soDienThoai)
                    .vaiTro(vaiTro)
                    .trangThai(NguoiDung.TrangThaiTaiKhoan.HOAT_DONG)
                    .build();
            user = nguoiDungRepository.save(user);

            if (vaiTro == VaiTro.SINH_VIEN) {
                NganhHoc nh = nganhHocId != null ? nganhHocRepository.findById(nganhHocId).orElse(nganhHocRepository.findAll().get(0))
                        : nganhHocRepository.findAll().get(0);
                LopDanhNghia lop = lopDanhNghiaId != null ? lopDanhNghiaRepository.findById(lopDanhNghiaId).orElse(null) : null;
                SinhVien sv = SinhVien.builder()
                        .mssv(maDinhDanh.trim()).nganhHoc(nh).lopDanhNghia(lop)
                        .khoaTuyenSinh(2025).trangThaiHocVu("BINH_THUONG")
                        .cpaTichLuy(0.0).tongTcTichLuy(0).nguoiDung(user).build();
                sinhVienRepository.save(sv);
            } else if (vaiTro == VaiTro.GIANG_VIEN || vaiTro == VaiTro.CO_VAN_HOC_TAP) {
                Khoa k = khoaId != null ? khoaRepository.findById(khoaId).orElse(khoaRepository.findAll().get(0))
                        : khoaRepository.findAll().get(0);
                GiangVien gv = GiangVien.builder()
                        .maGV(maDinhDanh.trim()).hocHamHocVi("ThS.").chuyenMon("Giảng dạy Đại học")
                        .khoa(k).nguoiDung(user).build();
                giangVienRepository.save(gv);
            }

            auditLogService.log(admin, "TAO_TAI_KHOAN", "NguoiDung#" + user.getMaDinhDanh(),
                    "Tạo tài khoản " + user.getHoTen() + " • Vai trò: " + vaiTro.name());
            ra.addFlashAttribute("success", "Đã tạo mới tài khoản [" + user.getMaDinhDanh() + " - " + user.getHoTen() + "] thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể tạo tài khoản (trùng mã định danh hoặc email): " + e.getMessage());
        }
        return "redirect:/Admin/Users";
    }

    @PostMapping("/Users/{id}/ToggleLock")
    public String toggleUserLock(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        if (admin.getId().equals(id)) {
            ra.addFlashAttribute("error", "Không thể tự khóa tài khoản đang đăng nhập!");
            return "redirect:/Admin/Users";
        }
        NguoiDung u = nguoiDungRepository.findById(id).orElseThrow();
        NguoiDung.TrangThaiTaiKhoan next = u.getTrangThai() == NguoiDung.TrangThaiTaiKhoan.BI_KHOA
                ? NguoiDung.TrangThaiTaiKhoan.HOAT_DONG : NguoiDung.TrangThaiTaiKhoan.BI_KHOA;
        u.setTrangThai(next);
        nguoiDungRepository.save(u);
        auditLogService.log(admin, next == NguoiDung.TrangThaiTaiKhoan.BI_KHOA ? "KHOA_TAI_KHOAN" : "MO_KHOA_TAI_KHOAN",
                "NguoiDung#" + u.getMaDinhDanh(), "Đổi trạng thái tài khoản " + u.getHoTen() + " -> " + next.name());
        ra.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản " + u.getMaDinhDanh() + " -> " + next.name());
        return "redirect:/Admin/Users";
    }

    @PostMapping("/Users/{id}/ResetPassword")
    public String resetPassword(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        NguoiDung u = nguoiDungRepository.findById(id).orElseThrow();
        u.setMatKhau("123456");
        nguoiDungRepository.save(u);
        auditLogService.log(admin, "RESET_MAT_KHAU", "NguoiDung#" + u.getMaDinhDanh(), "Reset mật khẩu về mặc định 123456");
        ra.addFlashAttribute("success", "Đã đặt lại mật khẩu tài khoản " + u.getMaDinhDanh() + " về 123456!");
        return "redirect:/Admin/Users";
    }

    @PostMapping("/Users/{id}/ChangeRole")
    public String changeRole(@PathVariable Long id, @RequestParam VaiTro vaiTro, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        NguoiDung u = nguoiDungRepository.findById(id).orElseThrow();
        VaiTro oldRole = u.getVaiTro();
        u.setVaiTro(vaiTro);
        nguoiDungRepository.save(u);
        auditLogService.log(admin, "PHAN_QUYEN_RBAC", "NguoiDung#" + u.getMaDinhDanh(),
                "Đổi quyền " + oldRole.name() + " -> " + vaiTro.name());
        ra.addFlashAttribute("success", "Đã cập nhật quyền RBAC cho " + u.getHoTen() + " -> " + vaiTro.getTenHienThi());
        return "redirect:/Admin/Users";
    }

    @GetMapping("/Notifications")
    public String notifications(HttpSession session, Model model) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        model.addAttribute("feedbacks", thongBaoRepository.findByNguoiNhanIdOrderByNgayGuiDesc(admin.getId()));
        model.addAttribute("oneStopRequests", yeuCauDichVuMotCuaRepository.findAll());
        return "admin/notifications";
    }

    @PostMapping("/Notifications/Broadcast")
    public String broadcastNotification(@RequestParam String targetGroup,
                                        @RequestParam String tieuDe,
                                        @RequestParam String noiDung,
                                        HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";

        int count = notificationService.broadcast(admin, targetGroup, tieuDe.trim(), noiDung.trim(), null);
        auditLogService.log(admin, "GUI_THONG_BAO_TOAN_TRUONG", "Group#" + targetGroup,
                "Gửi thông báo [" + tieuDe + "] tới " + count + " tài khoản");
        ra.addFlashAttribute("success", "Đã gửi thông báo toàn trường tới " + count + " người dùng (" + targetGroup + ")!");
        return "redirect:/Admin/Notifications";
    }

    @PostMapping("/Notifications/Reply")
    public String replyFeedback(@RequestParam Long recipientId, @RequestParam String tieuDe,
                                @RequestParam String noiDung, HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        NguoiDung recipient = nguoiDungRepository.findById(recipientId).orElseThrow();
        notificationService.sendToUser(admin, recipient, "[Phản hồi P.ĐT] " + tieuDe, noiDung, ThongBao.LoaiThongBao.PHAN_HOI,
                recipient.getVaiTro() == VaiTro.SINH_VIEN ? "/Student/Inbox" : "/Faculty/Inbox");
        ra.addFlashAttribute("success", "Đã gửi phản hồi tới " + recipient.getHoTen() + "!");
        return "redirect:/Admin/Notifications";
    }

    @PostMapping("/OneStop/{id}/Process")
    public String processOneStop(@PathVariable Long id,
                                 @RequestParam YeuCauDichVuMotCua.TrangThaiYeuCau status,
                                 @RequestParam(required = false) String phanHoi,
                                 HttpSession session, RedirectAttributes ra) {
        NguoiDung admin = getAdmin(session);
        if (admin == null) return "redirect:/Account/Login";
        YeuCauDichVuMotCua yc = yeuCauDichVuMotCuaRepository.findById(id).orElseThrow();
        yc.setTrangThai(status);
        yc.setPhanHoiAdmin(phanHoi);
        yc.setNgayXuLy(LocalDateTime.now());
        yeuCauDichVuMotCuaRepository.save(yc);
        notificationService.sendToUser(admin, yc.getSinhVien().getNguoiDung(),
                "[Dịch vụ 1 cửa] Kết quả yêu cầu " + yc.getLoaiDichVu().name(),
                "Yêu cầu của bạn đã được cập nhật trạng thái: " + status.name() + (phanHoi != null ? " • Phản hồi: " + phanHoi : ""),
                ThongBao.LoaiThongBao.HE_THONG, "/Student/OneStop");
        ra.addFlashAttribute("success", "Đã xử lý yêu cầu dịch vụ một cửa #" + id);
        return "redirect:/Admin/Notifications";
    }

    @GetMapping("/AuditLog")
    public String auditLog(HttpSession session, Model model, @RequestParam(required = false) String action) {
        if (getAdmin(session) == null) return "redirect:/Account/Login";
        List<NhatKyHeThong> logs = (action != null && !action.isBlank() && !"ALL".equals(action))
                ? nhatKyHeThongRepository.findByHanhDongOrderByThoiGianDesc(action)
                : nhatKyHeThongRepository.findTop100ByOrderByThoiGianDesc();
        model.addAttribute("logs", logs);
        model.addAttribute("selectedAction", action != null ? action : "ALL");
        return "admin/audit_log";
    }
}
