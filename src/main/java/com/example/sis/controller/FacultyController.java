package com.example.sis.controller;

import com.example.sis.entity.*;
import com.example.sis.repository.*;
import com.example.sis.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/Faculty")
public class FacultyController {

    @Autowired private GiangVienRepository giangVienRepository;
    @Autowired private LopHocPhanRepository lopHocPhanRepository;
    @Autowired private HocPhanRepository hocPhanRepository;
    @Autowired private HocKyRepository hocKyRepository;
    @Autowired private KhungChuongTrinhRepository khungChuongTrinhRepository;
    @Autowired private LichHocRepository lichHocRepository;
    @Autowired private DangKyHocPhanRepository dangKyHocPhanRepository;
    @Autowired private DiemHocPhanRepository diemHocPhanRepository;
    @Autowired private DiemDanhRepository diemDanhRepository;
    @Autowired private DiemRenLuyenRepository diemRenLuyenRepository;
    @Autowired private LopDanhNghiaRepository lopDanhNghiaRepository;
    @Autowired private SinhVienRepository sinhVienRepository;
    @Autowired private YeuCauDoiLichRepository yeuCauDoiLichRepository;
    @Autowired private ThongBaoRepository thongBaoRepository;

    @Autowired private AttendanceService attendanceService;
    @Autowired private GradingService gradingService;
    @Autowired private ScheduleConflictService scheduleConflictService;
    @Autowired private NotificationService notificationService;
    @Autowired private ExcelService excelService;
    @Autowired private AuditLogService auditLogService;

    private GiangVien getFaculty(HttpSession session) {
        NguoiDung u = (NguoiDung) session.getAttribute("currentUser");
        if (u == null) return null;
        if (u.getVaiTro() != VaiTro.GIANG_VIEN && u.getVaiTro() != VaiTro.CO_VAN_HOC_TAP) return null;
        return giangVienRepository.findByNguoiDungId(u.getId()).orElse(null);
    }

    @GetMapping({"", "/", "/Dashboard"})
    public String dashboard(HttpSession session, Model model) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        model.addAttribute("faculty", gv);
        List<LopHocPhan> allClasses = lopHocPhanRepository.findByGiangVienId(gv.getId());
        List<LopHocPhan> activeClasses = allClasses.stream()
                .filter(c -> c.getTrangThai() == LopHocPhan.TrangThaiLHP.DANG_MO || c.getTrangThai() == LopHocPhan.TrangThaiLHP.DA_KHOA)
                .toList();
        long pendingProposals = allClasses.stream()
                .filter(c -> c.getTrangThai() == LopHocPhan.TrangThaiLHP.CHO_DUYET).count();

        model.addAttribute("classes", activeClasses);
        model.addAttribute("allClassesCount", allClasses.size());
        model.addAttribute("pendingProposalsCount", pendingProposals);

        HocKy activeHocKy = hocKyRepository.findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO)
                .orElse(hocKyRepository.findAll().stream().findFirst().orElse(null));
        model.addAttribute("activeHocKy", activeHocKy);

        int todayThu = LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY ? 8 : LocalDate.now().getDayOfWeek().getValue() + 1;
        List<LichHoc> weekSchedules = activeHocKy != null
                ? lichHocRepository.findLichHocByGiangVienAndHocKy(gv.getId(), activeHocKy.getId())
                : List.of();
        List<LichHoc> todaySchedules = weekSchedules.stream()
                .filter(lh -> lh.getThu() != null && lh.getThu() == todayThu).toList();

        model.addAttribute("weekSchedules", weekSchedules);
        model.addAttribute("todaySchedules", todaySchedules);
        model.addAttribute("todayThu", todayThu);

        List<YeuCauDoiLich> scheduleRequests = yeuCauDoiLichRepository.findByGiangVienIdOrderByNgayTaoDesc(gv.getId());
        model.addAttribute("pendingScheduleChanges", scheduleRequests.stream()
                .filter(r -> r.getTrangThai() == YeuCauDoiLich.TrangThaiDuyet.CHO_DUYET).count());

        List<LopDanhNghia> adviseeClasses = lopDanhNghiaRepository.findByCoVanHocTapId(gv.getId());
        model.addAttribute("adviseeClasses", adviseeClasses);

        List<ThongBao> recentInbox = thongBaoRepository.findByNguoiNhanIdOrderByNgayGuiDesc(gv.getNguoiDung().getId())
                .stream().limit(5).toList();
        model.addAttribute("recentInbox", recentInbox);

        return "faculty/dashboard";
    }

    // ==================== HỘP THƯ GIẢNG VIÊN ====================

    @GetMapping("/Inbox")
    public String inbox(HttpSession session, Model model) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        List<ThongBao> messages = thongBaoRepository.findByNguoiNhanIdOrderByNgayGuiDesc(gv.getNguoiDung().getId());
        List<ThongBao> sentMessages = thongBaoRepository.findByNguoiGuiIdOrderByNgayGuiDesc(gv.getNguoiDung().getId());
        List<LopHocPhan> activeClasses = lopHocPhanRepository.findByGiangVienId(gv.getId()).stream()
                .filter(c -> c.getTrangThai() == LopHocPhan.TrangThaiLHP.DANG_MO || c.getTrangThai() == LopHocPhan.TrangThaiLHP.DA_KHOA)
                .toList();

        model.addAttribute("faculty", gv);
        model.addAttribute("messages", messages);
        model.addAttribute("sentMessages", sentMessages);
        model.addAttribute("classes", activeClasses);
        return "faculty/inbox";
    }

    @PostMapping("/Inbox/Read/{id}")
    public String markRead(@PathVariable Long id, HttpSession session) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";
        notificationService.markAsRead(id, gv.getNguoiDung().getId());
        return "redirect:/Faculty/Inbox";
    }

    @PostMapping("/Inbox/ReadAll")
    public String markAllRead(HttpSession session, RedirectAttributes ra) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";
        notificationService.markAllAsRead(gv.getNguoiDung().getId());
        ra.addFlashAttribute("success", "Đã đánh dấu tất cả thông báo là đã đọc.");
        return "redirect:/Faculty/Inbox";
    }

    @PostMapping("/Inbox/SendClassMessage")
    public String sendClassAnnouncement(@RequestParam Long lopHocPhanId,
                                        @RequestParam String tieuDe,
                                        @RequestParam String noiDung,
                                        HttpSession session, RedirectAttributes ra) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId).orElseThrow();
        int sentCount = notificationService.sendToClassStudents(
                gv.getNguoiDung(),
                lopHocPhanId,
                "[" + lhp.getMaLHP() + "] " + tieuDe.trim(),
                noiDung.trim(),
                ThongBao.LoaiThongBao.LOP_HOC_PHAN,
                "/Student/Inbox"
        );
        ra.addFlashAttribute("success", "Đã gửi thông báo hàng loạt tới " + sentCount + " sinh viên lớp " + lhp.getMaLHP() + "!");
        return "redirect:/Faculty/Inbox";
    }

    // ==================== ĐỀ XUẤT MỞ HỌC PHẦN & QUẢN LÝ LỚP ====================

    @GetMapping("/Proposals")
    public String proposals(HttpSession session, Model model) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        // Chỉ hiển thị các môn có trong Chương trình khung
        List<KhungChuongTrinh> kctAll = khungChuongTrinhRepository.findAll();
        Map<Long, HocPhan> curriculumSubjectsMap = new LinkedHashMap<>();
        for (KhungChuongTrinh kct : kctAll) {
            if (kct.getHocPhan() != null) {
                curriculumSubjectsMap.putIfAbsent(kct.getHocPhan().getId(), kct.getHocPhan());
            }
        }
        List<HocPhan> subjects = curriculumSubjectsMap.isEmpty()
                ? hocPhanRepository.findAll()
                : new ArrayList<>(curriculumSubjectsMap.values());

        List<LopHocPhan> myProposals = lopHocPhanRepository.findByGiangVienId(gv.getId()).stream()
                .sorted(Comparator.comparing(LopHocPhan::getId).reversed())
                .toList();

        model.addAttribute("faculty", gv);
        model.addAttribute("subjects", subjects);
        model.addAttribute("semesters", hocKyRepository.findAll());
        model.addAttribute("proposals", myProposals);
        return "faculty/proposals";
    }

    @PostMapping("/Proposals/Create")
    public String createProposal(@RequestParam Long hocPhanId,
                                 @RequestParam Long hocKyId,
                                 @RequestParam Integer siSoToiDa,
                                 @RequestParam Integer thuDuKien,
                                 @RequestParam Integer tietBatDauDuKien,
                                 @RequestParam Integer soTietDuKien,
                                 @RequestParam String phongDuKien,
                                 @RequestParam(required = false) String tenLhpMoRong,
                                 HttpSession session, RedirectAttributes ra) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        HocPhan hp = hocPhanRepository.findById(hocPhanId).orElseThrow();
        HocKy hk = hocKyRepository.findById(hocKyId).orElseThrow();

        // Validation: kiểm tra môn có thuộc chương trình khung
        boolean inCurriculum = khungChuongTrinhRepository.findAll().stream()
                .anyMatch(k -> k.getHocPhan() != null && k.getHocPhan().getId().equals(hp.getId()));
        if (!inCurriculum) {
            ra.addFlashAttribute("error", "Môn học [" + hp.getMaHP() + "] chưa nằm trong Chương trình khung đào tạo!");
            return "redirect:/Faculty/Proposals";
        }
        if (tietBatDauDuKien + soTietDuKien - 1 > 15) {
            ra.addFlashAttribute("error", "Khung tiết dự kiến vượt quá tiết 15 trong ngày!");
            return "redirect:/Faculty/Proposals";
        }

        String maLhp = hp.getMaHP() + "_" + hk.getMaHocKy() + "_" + String.format("%02d", (int) (System.currentTimeMillis() % 90 + 10));
        String tenLhp = (tenLhpMoRong != null && !tenLhpMoRong.isBlank())
                ? hp.getTenHP() + " - " + tenLhpMoRong.trim()
                : hp.getTenHP() + " - Nhóm " + gv.getMaGV();

        LopHocPhan lhp = LopHocPhan.builder()
                .maLHP(maLhp)
                .tenLHP(tenLhp)
                .hocPhan(hp)
                .hocKy(hk)
                .giangVien(gv)
                .siSoToiDa(Math.max(10, Math.min(200, siSoToiDa)))
                .siSoHienTai(0)
                .phongMacDinh(phongDuKien.trim())
                .thuDuKien(thuDuKien)
                .tietBatDauDuKien(tietBatDauDuKien)
                .soTietDuKien(soTietDuKien)
                .trangThai(LopHocPhan.TrangThaiLHP.CHO_DUYET)
                .ngayDeXuat(LocalDateTime.now())
                .build();
        lhp = lopHocPhanRepository.save(lhp);

        notificationService.sendToAdmins(
                gv.getNguoiDung(),
                "Đề xuất mở lớp học phần mới: " + maLhp,
                String.format("GV %s đề xuất mở lớp %s (%s) học kỳ %s • Sĩ số: %d • Dự kiến: Thứ %d, Tiết %d-%d, Phòng %s.",
                        gv.getNguoiDung().getHoTen(), maLhp, hp.getTenHP(), hk.getTenHocKy(),
                        lhp.getSiSoToiDa(), thuDuKien, tietBatDauDuKien, tietBatDauDuKien + soTietDuKien - 1, phongDuKien),
                ThongBao.LoaiThongBao.MO_LOP,
                "/Admin/Approvals?tab=classes"
        );
        auditLogService.log(gv.getNguoiDung(), "DE_XUAT_MO_LOP", "LopHocPhan#" + maLhp,
                "GV " + gv.getMaGV() + " đề xuất mở lớp " + hp.getTenHP() + " (" + hk.getMaHocKy() + ")");

        ra.addFlashAttribute("success", "Đã gửi đề xuất mở lớp [" + maLhp + "] lên Admin! Trạng thái: Chờ duyệt.");
        return "redirect:/Faculty/Proposals";
    }

    @GetMapping("/Classes")
    public String classList(HttpSession session, Model model, @RequestParam(required = false) Long lopHocPhanId) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        List<LopHocPhan> allMyClasses = lopHocPhanRepository.findByGiangVienId(gv.getId()).stream()
                .filter(c -> c.getTrangThai() != LopHocPhan.TrangThaiLHP.CHO_DUYET && c.getTrangThai() != LopHocPhan.TrangThaiLHP.TU_CHOI)
                .toList();
        List<LopHocPhan> activeClasses = allMyClasses.stream()
                .filter(c -> c.getTrangThai() == LopHocPhan.TrangThaiLHP.DANG_MO || c.getTrangThai() == LopHocPhan.TrangThaiLHP.DA_KHOA)
                .toList();
        List<LopHocPhan> finishedClasses = allMyClasses.stream()
                .filter(c -> c.getTrangThai() == LopHocPhan.TrangThaiLHP.DA_KET_THUC)
                .toList();

        model.addAttribute("faculty", gv);
        model.addAttribute("activeClasses", activeClasses);
        model.addAttribute("finishedClasses", finishedClasses);

        if (lopHocPhanId != null) {
            LopHocPhan selected = lopHocPhanRepository.findById(lopHocPhanId).orElse(null);
            if (selected != null) {
                model.addAttribute("selectedClass", selected);
                List<DangKyHocPhan> roster = dangKyHocPhanRepository.findByLopHocPhanId(lopHocPhanId).stream()
                        .filter(d -> d.getTrangThai() == DangKyHocPhan.TrangThaiDangKy.THANH_CONG)
                        .toList();
                model.addAttribute("roster", roster);
            }
        }

        return "faculty/classes";
    }

    // ==================== QUẢN LÝ LỊCH GIẢNG & CONFLICT CHECK ====================

    @GetMapping("/Schedule")
    public String schedule(HttpSession session, Model model, @RequestParam(required = false) Long hocKyId) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        List<HocKy> semesters = hocKyRepository.findAll();
        HocKy selectedSemester = hocKyId != null
                ? hocKyRepository.findById(hocKyId).orElse(semesters.get(0))
                : hocKyRepository.findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO)
                        .orElse(semesters.isEmpty() ? null : semesters.get(0));

        model.addAttribute("faculty", gv);
        model.addAttribute("semesters", semesters);
        model.addAttribute("selectedSemester", selectedSemester);

        if (selectedSemester != null) {
            List<LichHoc> schedules = lichHocRepository.findLichHocByGiangVienAndHocKy(gv.getId(), selectedSemester.getId());
            model.addAttribute("schedules", schedules);

            Map<String, LichHoc> grid = new HashMap<>();
            for (LichHoc lh : schedules) {
                for (int t = lh.getTietBatDau(); t <= lh.getTietKetThuc(); t++) {
                    grid.put(lh.getThu() + "_" + t, lh);
                }
            }
            model.addAttribute("grid", grid);
        }

        List<LopHocPhan> teachingClasses = lopHocPhanRepository.findByGiangVienId(gv.getId()).stream()
                .filter(c -> c.getTrangThai() == LopHocPhan.TrangThaiLHP.DANG_MO || c.getTrangThai() == LopHocPhan.TrangThaiLHP.DA_KHOA)
                .toList();
        model.addAttribute("teachingClasses", teachingClasses);
        model.addAttribute("changeRequests", yeuCauDoiLichRepository.findByGiangVienIdOrderByNgayTaoDesc(gv.getId()));

        return "faculty/schedule";
    }

    @PostMapping("/Schedule/RequestChange")
    public String requestScheduleChange(@RequestParam Long lopHocPhanId,
                                        @RequestParam(required = false) Long lichHocId,
                                        @RequestParam YeuCauDoiLich.LoaiYeuCau loaiYeuCau,
                                        @RequestParam int thuMoi,
                                        @RequestParam int tietBatDauMoi,
                                        @RequestParam int soTietMoi,
                                        @RequestParam String phongHocMoi,
                                        @RequestParam(defaultValue = "1") int tuTuanMoi,
                                        @RequestParam(defaultValue = "15") int denTuanMoi,
                                        @RequestParam String lyDo,
                                        HttpSession session, RedirectAttributes ra) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        try {
            YeuCauDoiLich req = scheduleConflictService.submitChangeRequest(
                    gv, lopHocPhanId, lichHocId, loaiYeuCau,
                    thuMoi, tietBatDauMoi, soTietMoi, phongHocMoi, tuTuanMoi, denTuanMoi, lyDo);
            ra.addFlashAttribute("success", "Đã kiểm tra 0 sinh viên trùng lịch & gửi yêu cầu đổi lịch (#" + req.getId() + ") lên Admin chờ duyệt!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể gửi yêu cầu đổi lịch: " + e.getMessage());
        }
        return "redirect:/Faculty/Schedule";
    }

    // ==================== ĐIỂM DANH & CẤM THI ====================

    @GetMapping("/Attendance")
    public String attendance(HttpSession session, Model model,
                             @RequestParam(required = false) Long lopHocPhanId,
                             @RequestParam(required = false) String date) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        List<LopHocPhan> classes = lopHocPhanRepository.findByGiangVienId(gv.getId()).stream()
                .filter(c -> c.getTrangThai() != LopHocPhan.TrangThaiLHP.CHO_DUYET && c.getTrangThai() != LopHocPhan.TrangThaiLHP.TU_CHOI)
                .toList();
        model.addAttribute("classes", classes);

        LocalDate ngayHoc = (date != null && !date.isEmpty()) ? LocalDate.parse(date) : LocalDate.now();
        model.addAttribute("currentDate", ngayHoc);

        if (lopHocPhanId != null) {
            LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId).orElseThrow();
            model.addAttribute("selectedClass", lhp);

            List<DangKyHocPhan> enrollments = dangKyHocPhanRepository.findByLopHocPhanId(lopHocPhanId).stream()
                    .filter(d -> d.getTrangThai() == DangKyHocPhan.TrangThaiDangKy.THANH_CONG).toList();
            model.addAttribute("enrollments", enrollments);

            // Số buổi vắng & trạng thái cấm thi từng SV
            Map<Long, Long> absentCountMap = new HashMap<>();
            Map<Long, Boolean> camThiMap = new HashMap<>();
            for (DangKyHocPhan dk : enrollments) {
                Long svId = dk.getSinhVien().getId();
                long vang = diemDanhRepository.countVangBySinhVienAndLopHocPhan(svId, lopHocPhanId);
                absentCountMap.put(svId, vang);
                boolean camThi = diemHocPhanRepository.findByDangKyHocPhanId(dk.getId())
                        .map(d -> Boolean.TRUE.equals(d.getCamThi())).orElse(false);
                camThiMap.put(svId, camThi);
            }
            model.addAttribute("absentCountMap", absentCountMap);
            model.addAttribute("camThiMap", camThiMap);

            List<LichHoc> schedules = lichHocRepository.findByLopHocPhanId(lopHocPhanId);
            if (!schedules.isEmpty()) {
                LichHoc currentSchedule = schedules.get(0);
                model.addAttribute("schedule", currentSchedule);

                List<DiemDanh> attendanceRecords = diemDanhRepository.findByLichHocIdAndNgayHoc(currentSchedule.getId(), ngayHoc);
                Map<Long, DiemDanh> attendanceMap = new HashMap<>();
                for (DiemDanh d : attendanceRecords) {
                    attendanceMap.put(d.getSinhVien().getId(), d);
                }
                model.addAttribute("attendanceMap", attendanceMap);
            }
        }

        return "faculty/attendance";
    }

    @PostMapping("/Attendance/Save")
    public String saveAttendance(@RequestParam Long lichHocId,
                                 @RequestParam String date,
                                 @RequestParam Long lopHocPhanId,
                                 @RequestParam Map<String, String> params,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        LocalDate ngayHoc = LocalDate.parse(date);
        Map<Long, DiemDanh.TrangThaiDiemDanh> attMap = new HashMap<>();
        Map<Long, String> noteMap = new HashMap<>();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("status_")) {
                Long svId = Long.parseLong(entry.getKey().replace("status_", ""));
                DiemDanh.TrangThaiDiemDanh status = DiemDanh.TrangThaiDiemDanh.valueOf(entry.getValue());
                attMap.put(svId, status);
            } else if (entry.getKey().startsWith("note_")) {
                Long svId = Long.parseLong(entry.getKey().replace("note_", ""));
                noteMap.put(svId, entry.getValue());
            }
        }

        attendanceService.saveBulkAttendance(lichHocId, ngayHoc, attMap, noteMap);
        ra.addFlashAttribute("success", "Đã lưu điểm danh ngày " + date + " thành công! Hệ thống tự động cảnh báo cấm thi nếu sinh viên vắng > 20%.");
        return "redirect:/Faculty/Attendance?lopHocPhanId=" + lopHocPhanId + "&date=" + date;
    }

    // ==================== NHẬP, IMPORT EXCEL & CHỐT ĐIỂM ====================

    @GetMapping("/Grading")
    public String grading(HttpSession session, Model model, @RequestParam(required = false) Long lopHocPhanId) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        List<LopHocPhan> classes = lopHocPhanRepository.findByGiangVienId(gv.getId()).stream()
                .filter(c -> c.getTrangThai() != LopHocPhan.TrangThaiLHP.CHO_DUYET && c.getTrangThai() != LopHocPhan.TrangThaiLHP.TU_CHOI)
                .toList();
        model.addAttribute("classes", classes);

        if (lopHocPhanId != null) {
            LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId).orElseThrow();
            model.addAttribute("selectedClass", lhp);

            List<DiemHocPhan> gradeList = diemHocPhanRepository.findByLopHocPhanId(lopHocPhanId);
            model.addAttribute("gradeList", gradeList);

            boolean isLocked = !gradeList.isEmpty() && gradeList.stream().allMatch(d -> Boolean.TRUE.equals(d.getKhoaDiem()));
            boolean portalOpen = !Boolean.FALSE.equals(lhp.getHocKy().getChoPhepNhapDiem());
            model.addAttribute("isLocked", isLocked);
            model.addAttribute("portalOpen", portalOpen);
        }

        return "faculty/grading";
    }

    @PostMapping("/Grading/Save")
    public String saveGrades(@RequestParam Long lopHocPhanId,
                             @RequestParam Map<String, String> params,
                             HttpSession session,
                             RedirectAttributes ra) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId).orElseThrow();
        if (Boolean.FALSE.equals(lhp.getHocKy().getChoPhepNhapDiem())) {
            ra.addFlashAttribute("error", "Cổng nhập điểm học kỳ " + lhp.getHocKy().getTenHocKy() + " đang bị Admin đóng!");
            return "redirect:/Faculty/Grading?lopHocPhanId=" + lopHocPhanId;
        }

        List<DiemHocPhan> gradeList = diemHocPhanRepository.findByLopHocPhanId(lopHocPhanId);
        int updated = 0;
        for (DiemHocPhan d : gradeList) {
            if (Boolean.TRUE.equals(d.getKhoaDiem())) continue;
            String ccKey = "cc_" + d.getId();
            String qtKey = "qt_" + d.getId();
            String thKey = "th_" + d.getId();
            String ckKey = "ck_" + d.getId();

            if (params.containsKey(ccKey) && !params.get(ccKey).isEmpty()) d.setDiemChuyenCan(Double.parseDouble(params.get(ccKey)));
            if (params.containsKey(qtKey) && !params.get(qtKey).isEmpty()) d.setDiemQuaTrinh(Double.parseDouble(params.get(qtKey)));
            if (params.containsKey(thKey) && !params.get(thKey).isEmpty()) d.setDiemThucHanh(Double.parseDouble(params.get(thKey)));
            if (params.containsKey(ckKey) && !params.get(ckKey).isEmpty() && !Boolean.TRUE.equals(d.getCamThi())) {
                d.setDiemCuoiKy(Double.parseDouble(params.get(ckKey)));
            }
            gradingService.calculateComponentGrades(d);
            updated++;
        }

        auditLogService.log(gv.getNguoiDung(), "NHAP_DIEM", "LopHocPhan#" + lhp.getMaLHP(),
                "GV " + gv.getMaGV() + " cập nhật điểm cho " + updated + " sinh viên lớp " + lhp.getMaLHP());
        ra.addFlashAttribute("success", "Cập nhật bảng điểm thành công cho " + updated + " sinh viên! Điểm hệ 10, thang 4 và điểm chữ đã tự động quy đổi.");
        return "redirect:/Faculty/Grading?lopHocPhanId=" + lopHocPhanId;
    }

    @PostMapping("/Grading/Import")
    public String importGrades(@RequestParam Long lopHocPhanId,
                               @RequestParam("file") MultipartFile file,
                               HttpSession session, RedirectAttributes ra) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";
        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("error", "Vui lòng chọn file Excel (.xlsx) bảng điểm!");
            return "redirect:/Faculty/Grading?lopHocPhanId=" + lopHocPhanId;
        }
        try {
            int count = excelService.importClassGrades(lopHocPhanId, file);
            auditLogService.log(gv.getNguoiDung(), "IMPORT_DIEM_EXCEL", "LopHocPhan#" + lopHocPhanId,
                    "Import Excel điểm cho " + count + " sinh viên");
            ra.addFlashAttribute("success", "Đã import điểm từ Excel thành công cho " + count + " sinh viên!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi import file Excel: " + e.getMessage());
        }
        return "redirect:/Faculty/Grading?lopHocPhanId=" + lopHocPhanId;
    }

    @PostMapping("/Grading/Lock")
    public String lockGrades(@RequestParam Long lopHocPhanId, HttpSession session, RedirectAttributes ra) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId).orElseThrow();
        List<DiemHocPhan> gradeList = diemHocPhanRepository.findByLopHocPhanId(lopHocPhanId);
        for (DiemHocPhan d : gradeList) {
            gradingService.calculateComponentGrades(d);
            d.setKhoaDiem(true);
            diemHocPhanRepository.save(d);
        }
        lhp.setTrangThai(LopHocPhan.TrangThaiLHP.DA_KET_THUC);
        lopHocPhanRepository.save(lhp);

        int notified = notificationService.sendToClassStudents(
                gv.getNguoiDung(), lopHocPhanId,
                "[Chốt điểm chính thức] Môn " + lhp.getHocPhan().getTenHP() + " (" + lhp.getMaLHP() + ")",
                "Giảng viên đã chốt và khóa bảng điểm lớp " + lhp.getMaLHP() + ". Sinh viên vui lòng vào mục Kết quả học tập để kiểm tra điểm tổng kết hệ 10 & hệ 4.",
                ThongBao.LoaiThongBao.LOP_HOC_PHAN, "/Student/Grades");

        auditLogService.log(gv.getNguoiDung(), "CHOT_DIEM", "LopHocPhan#" + lhp.getMaLHP(),
                "GV " + gv.getMaGV() + " chốt khóa bảng điểm lớp " + lhp.getMaLHP() + " (" + gradeList.size() + " SV, đã TB " + notified + " SV)");

        ra.addFlashAttribute("success", "Đã CHỐT & KHÓA bảng điểm lớp " + lhp.getMaLHP() + "! Kết quả đã được đẩy về Cổng Sinh viên và lưu trữ tại Phòng Đào Tạo.");
        return "redirect:/Faculty/Grading?lopHocPhanId=" + lopHocPhanId;
    }

    // ==================== CỐ VẤN HỌC TẬP & DUYỆT ĐIỂM RÈN LUYỆN ====================

    @GetMapping("/Advisees")
    public String advisees(HttpSession session, Model model) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        List<LopDanhNghia> adviseeClasses = lopDanhNghiaRepository.findByCoVanHocTapId(gv.getId());
        model.addAttribute("classes", adviseeClasses);

        List<SinhVien> allAdvisees = new ArrayList<>();
        List<DiemRenLuyen> conductRecords = new ArrayList<>();
        for (LopDanhNghia lop : adviseeClasses) {
            allAdvisees.addAll(sinhVienRepository.findByLopDanhNghiaId(lop.getId()));
            conductRecords.addAll(diemRenLuyenRepository.findBySinhVienLopDanhNghiaId(lop.getId()));
        }
        model.addAttribute("students", allAdvisees);
        model.addAttribute("conductRecords", conductRecords);
        model.addAttribute("semesters", hocKyRepository.findAll());

        return "faculty/advisees";
    }

    @PostMapping("/Advisees/ApproveConduct")
    public String approveClassConduct(@RequestParam Long conductId,
                                      @RequestParam Integer diemLopCham,
                                      @RequestParam(required = false) String ghiChu,
                                      HttpSession session, RedirectAttributes ra) {
        GiangVien gv = getFaculty(session);
        if (gv == null) return "redirect:/Account/Login";

        DiemRenLuyen drl = diemRenLuyenRepository.findById(conductId).orElseThrow();
        if ("DA_CHOT".equals(drl.getTrangThai())) {
            ra.addFlashAttribute("error", "Điểm rèn luyện này đã được Admin chốt toàn trường!");
            return "redirect:/Faculty/Advisees";
        }
        int score = Math.max(0, Math.min(100, diemLopCham));
        drl.setDiemLopCham(score);
        drl.setXepLoai(DiemRenLuyen.classify(score));
        drl.setTrangThai("CVHT_DA_DUYET");
        if (ghiChu != null && !ghiChu.isBlank()) drl.setGhiChu(ghiChu.trim());
        diemRenLuyenRepository.save(drl);

        auditLogService.log(gv.getNguoiDung(), "DUYET_DRL_LOP", "DiemRenLuyen#" + drl.getId(),
                "CVHT duyệt điểm rèn luyện cấp lớp cho SV " + drl.getSinhVien().getMssv() + ": " + score + " điểm");

        ra.addFlashAttribute("success", "Đã duyệt điểm rèn luyện cấp lớp cho SV " + drl.getSinhVien().getMssv() + " (" + score + " điểm)!");
        return "redirect:/Faculty/Advisees";
    }
}
