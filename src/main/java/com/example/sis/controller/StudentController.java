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
import java.util.stream.Collectors;

@Controller
@RequestMapping("/Student")
public class StudentController {

    @Autowired private SinhVienRepository sinhVienRepository;
    @Autowired private NguoiDungRepository nguoiDungRepository;
    @Autowired private LopHocPhanRepository lopHocPhanRepository;
    @Autowired private DangKyHocPhanRepository dangKyHocPhanRepository;
    @Autowired private DiemHocPhanRepository diemHocPhanRepository;
    @Autowired private LichHocRepository lichHocRepository;
    @Autowired private HocKyRepository hocKyRepository;
    @Autowired private CongNoHocKyRepository congNoHocKyRepository;
    @Autowired private PhieuThuHocPhiRepository phieuThuHocPhiRepository;
    @Autowired private KhungChuongTrinhRepository khungChuongTrinhRepository;
    @Autowired private DieuKienHocPhanRepository dieuKienHocPhanRepository;
    @Autowired private YeuCauDichVuMotCuaRepository yeuCauDichVuMotCuaRepository;
    @Autowired private ChungChiChuanDauRaRepository chungChiChuanDauRaRepository;
    @Autowired private DiemRenLuyenRepository diemRenLuyenRepository;
    @Autowired private ThongBaoRepository thongBaoRepository;

    @Autowired private CourseRegistrationService courseRegistrationService;
    @Autowired private GradingService gradingService;
    @Autowired private TuitionService tuitionService;
    @Autowired private AcademicStandingService academicStandingService;
    @Autowired private GpaSimulatorService gpaSimulatorService;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private NotificationService notificationService;
    @Autowired private AuditLogService auditLogService;

    private SinhVien getStudent(HttpSession session) {
        NguoiDung u = (NguoiDung) session.getAttribute("currentUser");
        if (u == null || u.getVaiTro() != VaiTro.SINH_VIEN) return null;
        return sinhVienRepository.findByNguoiDungId(u.getId()).orElse(null);
    }

    @GetMapping({"", "/", "/Dashboard"})
    public String dashboard(HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        gradingService.updateStudentCpa(sv.getId());
        sv = sinhVienRepository.findById(sv.getId()).orElse(sv);
        model.addAttribute("student", sv);

        int earned = sv.getTongTcTichLuy() != null ? sv.getTongTcTichLuy() : 0;
        int required = sv.getNganhHoc().getTongTinChiYeuCau();
        int progressPercent = (int) Math.min(100, Math.round(((double) earned / required) * 100));
        model.addAttribute("creditsEarned", earned);
        model.addAttribute("creditsRequired", required);
        model.addAttribute("progressPercent", progressPercent);

        HocKy activeSemester = hocKyRepository.findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO)
                .orElse(hocKyRepository.findAll().stream().findFirst().orElse(null));
        model.addAttribute("activeSemester", activeSemester);

        List<DangKyHocPhan> currentRegs = new ArrayList<>();
        List<LichHoc> upcomingSchedules = new ArrayList<>();
        List<LichHoc> examSchedules = new ArrayList<>();
        CongNoHocKy currentDebt = null;

        if (activeSemester != null) {
            currentRegs = dangKyHocPhanRepository.findActiveRegistrations(sv.getId(), activeSemester.getId());
            model.addAttribute("currentRegs", currentRegs);
            model.addAttribute("currentClasses", currentRegs);
            int curCredits = currentRegs.stream().mapToInt(r -> r.getLopHocPhan().getHocPhan().getTongTinChi()).sum();
            model.addAttribute("currentCredits", curCredits);

            currentDebt = congNoHocKyRepository.findBySinhVienIdAndHocKyId(sv.getId(), activeSemester.getId()).orElse(null);
            model.addAttribute("currentDebt", currentDebt);

            List<LichHoc> allScheds = lichHocRepository.findLichHocBySinhVienAndHocKy(sv.getId(), activeSemester.getId());
            for (LichHoc lh : allScheds) {
                if (lh.getLoaiLich() == LichHoc.LoaiLich.LICH_THI) {
                    examSchedules.add(lh);
                } else {
                    upcomingSchedules.add(lh);
                }
            }
        }
        model.addAttribute("upcomingSchedules", upcomingSchedules);
        model.addAttribute("examSchedules", examSchedules);

        // Chi tiết tiến độ từng môn trong Chương trình khung (Đã học / Đang học / Đang nợ / Chưa học)
        List<KhungChuongTrinh> kctList = khungChuongTrinhRepository.findByNganhHocIdOrderByHocKyDuKienAsc(sv.getNganhHoc().getId());
        if (kctList.isEmpty()) kctList = khungChuongTrinhRepository.findByNganhHocIdOrderByHocKyDuKienAsc(4L);

        List<DiemHocPhan> allGrades = diemHocPhanRepository.findAllBySinhVienId(sv.getId());
        Map<Long, DiemHocPhan> bestGradeMap = new HashMap<>();
        for (DiemHocPhan d : allGrades) {
            Long hpId = d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getId();
            if (!bestGradeMap.containsKey(hpId) ||
                    (d.getDiemHe4() != null && (bestGradeMap.get(hpId).getDiemHe4() == null || d.getDiemHe4() > bestGradeMap.get(hpId).getDiemHe4()))) {
                bestGradeMap.put(hpId, d);
            }
        }
        Set<Long> takingIds = currentRegs.stream()
                .map(r -> r.getLopHocPhan().getHocPhan().getId())
                .collect(Collectors.toSet());

        List<Map<String, Object>> passedSubjects = new ArrayList<>();
        List<Map<String, Object>> takingSubjects = new ArrayList<>();
        List<Map<String, Object>> failedSubjects = new ArrayList<>();
        List<Map<String, Object>> remainingSubjects = new ArrayList<>();

        int passedCredits = 0, takingCredits = 0, failedCredits = 0, remainingCredits = 0;

        for (KhungChuongTrinh kct : kctList) {
            HocPhan hp = kct.getHocPhan();
            DiemHocPhan g = bestGradeMap.get(hp.getId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("maHP", hp.getMaHP());
            row.put("tenHP", hp.getTenHP());
            row.put("tinChi", hp.getTongTinChi());
            row.put("hocKy", kct.getHocKyDuKien());
            row.put("diemChu", g != null ? g.getDiemChu() : "-");
            row.put("diemHe10", g != null ? g.getDiemTongKetHe10() : null);

            if (g != null && Boolean.TRUE.equals(g.getKetQuaQuaMon())) {
                passedSubjects.add(row);
                passedCredits += hp.getTongTinChi();
            } else if (g != null && Boolean.FALSE.equals(g.getKetQuaQuaMon())) {
                failedSubjects.add(row);
                failedCredits += hp.getTongTinChi();
            } else if (takingIds.contains(hp.getId())) {
                takingSubjects.add(row);
                takingCredits += hp.getTongTinChi();
            } else {
                remainingSubjects.add(row);
                remainingCredits += hp.getTongTinChi();
            }
        }

        model.addAttribute("passedSubjects", passedSubjects);
        model.addAttribute("takingSubjects", takingSubjects);
        model.addAttribute("failedSubjects", failedSubjects);
        model.addAttribute("remainingSubjects", remainingSubjects);
        model.addAttribute("passedCredits", passedCredits);
        model.addAttribute("takingCredits", takingCredits);
        model.addAttribute("failedCredits", failedCredits);
        model.addAttribute("remainingCredits", Math.max(0, required - passedCredits - takingCredits - failedCredits));

        // Nhắc nhở thông minh (Lịch học/thi, Công nợ, Chứng chỉ)
        List<ChungChiChuanDauRa> certs = chungChiChuanDauRaRepository.findBySinhVienIdOrderByNgayNopDesc(sv.getId());
        model.addAttribute("certs", certs);

        AcademicStandingService.GraduationAuditResult gradAudit = academicStandingService.checkGraduationEligibility(sv.getId());
        model.addAttribute("gradAudit", gradAudit);

        List<ThongBao> recentInbox = thongBaoRepository.findByNguoiNhanIdOrderByNgayGuiDesc(sv.getNguoiDung().getId())
                .stream().limit(4).toList();
        model.addAttribute("recentInbox", recentInbox);

        return "student/dashboard";
    }

    // ==================== HỒ SƠ & CHỨNG CHỈ ====================

    @GetMapping("/Profile")
    public String profile(HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";
        model.addAttribute("student", sv);
        model.addAttribute("certs", chungChiChuanDauRaRepository.findBySinhVienIdOrderByNgayNopDesc(sv.getId()));
        return "student/profile";
    }

    @PostMapping("/Profile/Update")
    public String updateProfile(@RequestParam(required = false) String soDienThoai,
                                @RequestParam(required = false) String diaChi,
                                @RequestParam(required = false) String noiSinh,
                                HttpSession session, RedirectAttributes ra) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        NguoiDung u = sv.getNguoiDung();
        if (soDienThoai != null) u.setSoDienThoai(soDienThoai.trim());
        nguoiDungRepository.save(u);
        session.setAttribute("currentUser", u);

        if (diaChi != null) sv.setDiaChi(diaChi.trim());
        if (noiSinh != null) sv.setNoiSinh(noiSinh.trim());
        sinhVienRepository.save(sv);

        ra.addFlashAttribute("success", "Đã cập nhật thông tin liên hệ hồ sơ sinh viên thành công!");
        return "redirect:/Student/Profile";
    }

    @GetMapping("/Certificates")
    public String certificates(HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        List<ChungChiChuanDauRa> certs = chungChiChuanDauRaRepository.findBySinhVienIdOrderByNgayNopDesc(sv.getId());
        model.addAttribute("student", sv);
        model.addAttribute("certs", certs);
        model.addAttribute("certTypes", ChungChiChuanDauRa.LoaiChungChi.values());
        return "student/certificates";
    }

    @PostMapping("/Certificates/Submit")
    public String submitCertificate(@RequestParam ChungChiChuanDauRa.LoaiChungChi loaiChungChi,
                                    @RequestParam String soHieu,
                                    @RequestParam(required = false) String diemSo,
                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayCap,
                                    @RequestParam String donViCap,
                                    @RequestParam("pdfFile") MultipartFile pdfFile,
                                    HttpSession session, RedirectAttributes ra) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        try {
            String storedFilename = fileStorageService.storeCertificatePdf(pdfFile, sv.getMssv());
            ChungChiChuanDauRa cc = ChungChiChuanDauRa.builder()
                    .sinhVien(sv)
                    .loaiChungChi(loaiChungChi)
                    .soHieu(soHieu.trim())
                    .diemSo(diemSo != null ? diemSo.trim() : "")
                    .ngayCap(ngayCap)
                    .donViCap(donViCap.trim())
                    .filePdfUrl(storedFilename)
                    .tenFileGoc(pdfFile.getOriginalFilename())
                    .trangThai(ChungChiChuanDauRa.TrangThaiChungChi.CHO_DUYET)
                    .ngayNop(LocalDateTime.now())
                    .build();
            cc = chungChiChuanDauRaRepository.save(cc);

            notificationService.sendToAdmins(
                    sv.getNguoiDung(),
                    "Hồ sơ chứng chỉ mới cần duyệt: " + sv.getMssv(),
                    String.format("Sinh viên %s (%s) vừa nộp chứng chỉ %s (Số hiệu: %s, File PDF: %s). Vui lòng kiểm tra và phê duyệt.",
                            sv.getNguoiDung().getHoTen(), sv.getMssv(), loaiChungChi.getLabel(), soHieu, pdfFile.getOriginalFilename()),
                    ThongBao.LoaiThongBao.CHUNG_CHI,
                    "/Admin/Approvals?tab=certificates"
            );
            auditLogService.log(sv.getNguoiDung(), "NOP_CHUNG_CHI", "ChungChi#" + cc.getId(),
                    "SV " + sv.getMssv() + " nộp chứng chỉ " + loaiChungChi.getLabel() + " (" + storedFilename + ")");

            ra.addFlashAttribute("success", "Đã gửi hồ sơ chứng chỉ kèm file PDF thành công! Trạng thái hiện tại: Chờ Admin phê duyệt.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể nộp chứng chỉ: " + e.getMessage());
        }
        return "redirect:/Student/Certificates";
    }

    // ==================== HỌC TẬP ====================

    @GetMapping("/Timetable")
    public String timetable(HttpSession session, Model model, @RequestParam(required = false) Long hocKyId) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        List<HocKy> allSemesters = hocKyRepository.findAll();
        HocKy selectedSemester;
        if (hocKyId != null) {
            selectedSemester = hocKyRepository.findById(hocKyId).orElse(allSemesters.get(0));
        } else {
            selectedSemester = hocKyRepository.findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO)
                    .orElse(allSemesters.isEmpty() ? null : allSemesters.get(0));
        }

        model.addAttribute("semesters", allSemesters);
        model.addAttribute("selectedSemester", selectedSemester);

        if (selectedSemester != null) {
            List<LichHoc> scheduleList = lichHocRepository.findLichHocBySinhVienAndHocKy(sv.getId(), selectedSemester.getId());
            List<LichHoc> regularSchedules = scheduleList.stream()
                    .filter(lh -> lh.getLoaiLich() != LichHoc.LoaiLich.LICH_THI).toList();
            List<LichHoc> examSchedules = scheduleList.stream()
                    .filter(lh -> lh.getLoaiLich() == LichHoc.LoaiLich.LICH_THI).toList();

            model.addAttribute("schedules", regularSchedules);
            model.addAttribute("examSchedules", examSchedules);
            model.addAttribute("allSchedules", scheduleList);

            Map<String, LichHoc> timetableGrid = new HashMap<>();
            for (LichHoc lh : regularSchedules) {
                for (int t = lh.getTietBatDau(); t <= lh.getTietKetThuc(); t++) {
                    timetableGrid.put(lh.getThu() + "_" + t, lh);
                }
            }
            model.addAttribute("grid", timetableGrid);
        }

        return "student/timetable";
    }

    @GetMapping("/Registration")
    public String registration(HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        HocKy activeHocKy = hocKyRepository.findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO).orElse(null);
        model.addAttribute("activeHocKy", activeHocKy);
        model.addAttribute("student", sv);

        if (activeHocKy != null) {
            // CHỈ hiển thị các lớp học phần ĐÃ ĐƯỢC ADMIN DUYỆT MỞ (DANG_MO)
            List<LopHocPhan> openClasses = lopHocPhanRepository.findByHocKyIdAndTrangThai(
                    activeHocKy.getId(), LopHocPhan.TrangThaiLHP.DANG_MO);
            List<DangKyHocPhan> registeredClasses = dangKyHocPhanRepository.findActiveRegistrations(sv.getId(), activeHocKy.getId());

            Set<Long> registeredLhpIds = registeredClasses.stream()
                    .map(d -> d.getLopHocPhan().getId())
                    .collect(Collectors.toSet());

            model.addAttribute("openClasses", openClasses);
            model.addAttribute("registeredClasses", registeredClasses);
            model.addAttribute("registeredLhpIds", registeredLhpIds);

            int totalRegisteredCredits = registeredClasses.stream()
                    .mapToInt(d -> d.getLopHocPhan().getHocPhan().getTongTinChi())
                    .sum();
            model.addAttribute("totalRegisteredCredits", totalRegisteredCredits);

            boolean isWarning = sv.getTrangThaiHocVu() != null && sv.getTrangThaiHocVu().startsWith("CANH_BAO");
            model.addAttribute("maxCredits", isWarning ? 18 : 25);
            model.addAttribute("isWarning", isWarning);
        }

        return "student/registration";
    }

    @PostMapping("/Registration/Register")
    public String registerCourse(@RequestParam Long lopHocPhanId, HttpSession session, RedirectAttributes ra) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        CourseRegistrationService.RegistrationResult result = courseRegistrationService.registerCourse(sv.getId(), lopHocPhanId);
        if (result.isSuccess()) {
            ra.addFlashAttribute("success", result.getMessage() + " • Hệ thống đã tự động cập nhật Công nợ học phí!");
        } else {
            ra.addFlashAttribute("error", result.getMessage());
        }
        return "redirect:/Student/Registration";
    }

    @PostMapping("/Registration/Drop")
    public String dropCourse(@RequestParam Long lopHocPhanId, HttpSession session, RedirectAttributes ra) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        CourseRegistrationService.RegistrationResult result = courseRegistrationService.dropCourse(sv.getId(), lopHocPhanId);
        if (result.isSuccess()) {
            ra.addFlashAttribute("success", result.getMessage() + " • Công nợ học phí đã được cập nhật lại!");
        } else {
            ra.addFlashAttribute("error", result.getMessage());
        }
        return "redirect:/Student/Registration";
    }

    @GetMapping("/Grades")
    public String grades(HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        gradingService.updateStudentCpa(sv.getId());
        sv = sinhVienRepository.findById(sv.getId()).orElse(sv);
        model.addAttribute("student", sv);

        List<DiemHocPhan> allGrades = diemHocPhanRepository.findAllBySinhVienId(sv.getId());
        Map<HocKy, List<DiemHocPhan>> gradesBySemester = allGrades.stream()
                .collect(Collectors.groupingBy(d -> d.getDangKyHocPhan().getLopHocPhan().getHocKy()));
        model.addAttribute("gradesBySemester", gradesBySemester);

        return "student/grades";
    }

    @GetMapping("/Conduct")
    public String conduct(HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        List<DiemRenLuyen> conductList = diemRenLuyenRepository.findBySinhVienId(sv.getId());
        List<HocKy> semesters = hocKyRepository.findAll();
        model.addAttribute("student", sv);
        model.addAttribute("conductList", conductList);
        model.addAttribute("semesters", semesters);
        return "student/conduct";
    }

    @PostMapping("/Conduct/SelfAssess")
    public String submitSelfConduct(@RequestParam Long hocKyId,
                                    @RequestParam Integer diemTuCham,
                                    @RequestParam(required = false) String ghiChu,
                                    HttpSession session, RedirectAttributes ra) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        if (diemTuCham == null || diemTuCham < 0 || diemTuCham > 100) {
            ra.addFlashAttribute("error", "Điểm rèn luyện tự đánh giá phải nằm trong khoảng từ 0 đến 100!");
            return "redirect:/Student/Conduct";
        }
        int score = diemTuCham;
        HocKy hk = hocKyRepository.findById(hocKyId).orElseThrow();
        DiemRenLuyen drl = diemRenLuyenRepository.findBySinhVienIdAndHocKyId(sv.getId(), hocKyId)
                .orElse(DiemRenLuyen.builder().sinhVien(sv).hocKy(hk).build());

        if ("DA_CHOT".equals(drl.getTrangThai())) {
            ra.addFlashAttribute("error", "Điểm rèn luyện học kỳ này đã được Nhà trường chốt, không thể chỉnh sửa!");
            return "redirect:/Student/Conduct";
        }
        drl.setDiemTuCham(score);
        drl.setXepLoai(DiemRenLuyen.classify(score));
        drl.setTrangThai("CHO_CVHT_DUYET");
        drl.setGhiChu(ghiChu);
        diemRenLuyenRepository.save(drl);

        ra.addFlashAttribute("success", "Đã gửi phiếu tự đánh giá điểm rèn luyện (" + score + " điểm) lên Cố vấn học tập!");
        return "redirect:/Student/Conduct";
    }

    // ==================== TÀI CHÍNH ====================

    @GetMapping("/Tuition")
    public String tuition(HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        List<CongNoHocKy> allDebts = congNoHocKyRepository.findBySinhVienId(sv.getId());
        model.addAttribute("debts", allDebts);
        model.addAttribute("student", sv);

        CongNoHocKy latestDebt = allDebts.isEmpty() ? null : allDebts.get(allDebts.size() - 1);
        model.addAttribute("latestDebt", latestDebt);

        if (latestDebt != null && latestDebt.getConNo() > 0) {
            model.addAttribute("vietQrUrl", tuitionService.generateVietQrUrl(latestDebt));
        }

        return "student/tuition";
    }

    @PostMapping("/Tuition/PayOnline")
    public String payOnline(@RequestParam Long congNoId,
                            @RequestParam Double soTien,
                            @RequestParam(defaultValue = "VIETQR") PhieuThuHocPhi.PhuongThucThanhToan phuongThuc,
                            HttpSession session, RedirectAttributes ra) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        try {
            CongNoHocKy cn = congNoHocKyRepository.findById(congNoId).orElseThrow();
            if (!cn.getSinhVien().getId().equals(sv.getId())) {
                ra.addFlashAttribute("error", "Không có quyền thanh toán khoản công nợ này!");
                return "redirect:/Student/Tuition";
            }
            if (cn.getConNo() == null || cn.getConNo() <= 0) {
                ra.addFlashAttribute("error", "Học kỳ này đã hoàn thành học phí, không còn công nợ cần thanh toán!");
                return "redirect:/Student/Tuition";
            }
            if (soTien == null || soTien <= 0) {
                ra.addFlashAttribute("error", "Số tiền thanh toán phải lớn hơn 0 VNĐ!");
                return "redirect:/Student/Tuition";
            }
            if (soTien > cn.getConNo()) {
                ra.addFlashAttribute("error", "Số tiền thanh toán vượt quá công nợ còn lại (" + String.format("%,.0f VNĐ", cn.getConNo()) + ")!");
                return "redirect:/Student/Tuition";
            }
            double payAmount = soTien;
            PhieuThuHocPhi pt = tuitionService.createPayment(
                    congNoId, payAmount, phuongThuc,
                    "Cổng thanh toán trực tuyến (" + phuongThuc.name() + ")",
                    "Sinh viên thanh toán học phí trực tuyến HK " + cn.getHocKy().getMaHocKy());

            notificationService.sendToUser(null, sv.getNguoiDung(),
                    "Xác nhận thanh toán học phí thành công - " + pt.getMaPhieuThu(),
                    String.format("Hệ thống đã ghi nhận giao dịch %,.0f VNĐ (Mã GD: %s) cho %s. Bạn có thể tải biên lai điện tử ngay.",
                            payAmount, pt.getMaGiaoDich(), cn.getHocKy().getTenHocKy()),
                    ThongBao.LoaiThongBao.HOC_PHI, "/Student/Receipts");

            auditLogService.log(sv.getNguoiDung(), "THANH_TOAN_ONLINE", "PhieuThu#" + pt.getMaPhieuThu(),
                    "SV " + sv.getMssv() + " thanh toán " + String.format("%,.0f", payAmount) + " đ qua " + phuongThuc.name());

            ra.addFlashAttribute("success", "Thanh toán trực tuyến thành công! Mã biên lai: " + pt.getMaPhieuThu());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Lỗi thanh toán: " + e.getMessage());
        }
        return "redirect:/Student/Tuition";
    }

    @GetMapping("/Tuition/PrintReceipt/{id}")
    public String printReceipt(@PathVariable Long id, HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        CongNoHocKy cn = congNoHocKyRepository.findById(id).orElse(null);
        if (cn == null) return "redirect:/Student/Tuition";

        model.addAttribute("debt", cn);
        model.addAttribute("student", sv);
        if (cn.getDanhSachPhieuThu() != null && !cn.getDanhSachPhieuThu().isEmpty()) {
            model.addAttribute("receipt", cn.getDanhSachPhieuThu().get(0));
        }
        return "student/tuition_receipt";
    }

    @GetMapping("/Receipts")
    public String receipts(HttpSession session, Model model, @RequestParam(required = false) Long congNoId) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        List<PhieuThuHocPhi> allReceipts = phieuThuHocPhiRepository.findBySinhVienId(sv.getId());
        if (congNoId != null) {
            allReceipts = allReceipts.stream()
                    .filter(p -> p.getCongNoHocKy() != null && p.getCongNoHocKy().getId().equals(congNoId))
                    .collect(Collectors.toList());
        }

        double totalPaid = allReceipts.stream().mapToDouble(p -> p.getSoTien() != null ? p.getSoTien() : 0.0).sum();
        List<CongNoHocKy> allDebts = congNoHocKyRepository.findBySinhVienId(sv.getId());

        model.addAttribute("receipts", allReceipts);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("debts", allDebts);
        model.addAttribute("selectedCongNoId", congNoId);
        model.addAttribute("student", sv);

        return "student/receipts";
    }

    @GetMapping("/Receipts/Print/{id}")
    public String printReceiptByPhieuId(@PathVariable Long id, HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        PhieuThuHocPhi pt = phieuThuHocPhiRepository.findById(id).orElse(null);
        if (pt == null) return "redirect:/Student/Receipts";

        model.addAttribute("receipt", pt);
        model.addAttribute("debt", pt.getCongNoHocKy());
        model.addAttribute("student", sv);
        return "student/tuition_receipt";
    }

    // ==================== CHƯƠNG TRÌNH KHUNG & TIỆN ÍCH ====================

    @GetMapping("/Curriculum")
    public String curriculum(HttpSession session, Model model, @RequestParam(required = false) Integer hocKy) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        NganhHoc nganh = sv.getNganhHoc();
        List<KhungChuongTrinh> allKct = khungChuongTrinhRepository.findByNganhHocIdOrderByHocKyDuKienAsc(nganh.getId());
        if (allKct.isEmpty()) {
            allKct = khungChuongTrinhRepository.findByNganhHocIdOrderByHocKyDuKienAsc(4L);
        }

        List<DiemHocPhan> studentGrades = diemHocPhanRepository.findAllBySinhVienId(sv.getId());
        Map<Long, DiemHocPhan> gradeByCourseId = new HashMap<>();
        for (DiemHocPhan d : studentGrades) {
            if (d.getDangKyHocPhan() != null && d.getDangKyHocPhan().getLopHocPhan() != null) {
                Long hpId = d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getId();
                if (!gradeByCourseId.containsKey(hpId) ||
                        (d.getDiemHe4() != null && gradeByCourseId.get(hpId).getDiemHe4() != null && d.getDiemHe4() > gradeByCourseId.get(hpId).getDiemHe4())) {
                    gradeByCourseId.put(hpId, d);
                }
            }
        }

        HocKy activeHocKy = hocKyRepository.findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO).orElse(null);
        Set<Long> takingCourseIds = new HashSet<>();
        if (activeHocKy != null) {
            List<DangKyHocPhan> activeRegs = dangKyHocPhanRepository.findActiveRegistrations(sv.getId(), activeHocKy.getId());
            for (DangKyHocPhan reg : activeRegs) {
                if (reg.getLopHocPhan() != null && reg.getLopHocPhan().getHocPhan() != null) {
                    takingCourseIds.add(reg.getLopHocPhan().getHocPhan().getId());
                }
            }
        }

        List<DieuKienHocPhan> allPrereqs = dieuKienHocPhanRepository.findAll();
        Map<Long, List<String>> prereqMap = new HashMap<>();
        for (DieuKienHocPhan dk : allPrereqs) {
            if (dk.getHocPhan() != null && dk.getHocPhanDieuKien() != null) {
                prereqMap.computeIfAbsent(dk.getHocPhan().getId(), k -> new ArrayList<>())
                        .add(dk.getHocPhanDieuKien().getMaHP() + " - " + dk.getHocPhanDieuKien().getTenHP());
            }
        }

        Map<Integer, List<KhungChuongTrinh>> curriculumBySemester = new TreeMap<>();
        for (KhungChuongTrinh item : allKct) {
            curriculumBySemester.computeIfAbsent(item.getHocKyDuKien(), k -> new ArrayList<>()).add(item);
        }

        int totalRequiredCredits = nganh.getTongTinChiYeuCau() != null ? nganh.getTongTinChiYeuCau() : 123;
        int totalPassedCredits = 0;
        int totalPassedCourses = 0;
        for (KhungChuongTrinh item : allKct) {
            Long hpId = item.getHocPhan().getId();
            DiemHocPhan gr = gradeByCourseId.get(hpId);
            if (gr != null && Boolean.TRUE.equals(gr.getKetQuaQuaMon())) {
                totalPassedCredits += (item.getHocPhan().getTongTinChi() != null ? item.getHocPhan().getTongTinChi() : 0);
                totalPassedCourses++;
            }
        }

        int progressPercentage = (int) Math.min(100, Math.round(((double) totalPassedCredits / totalRequiredCredits) * 100));

        model.addAttribute("student", sv);
        model.addAttribute("nganh", nganh);
        model.addAttribute("curriculumBySemester", curriculumBySemester);
        model.addAttribute("selectedSemester", hocKy);
        model.addAttribute("gradeByCourseId", gradeByCourseId);
        model.addAttribute("takingCourseIds", takingCourseIds);
        model.addAttribute("prereqMap", prereqMap);
        model.addAttribute("totalRequiredCredits", totalRequiredCredits);
        model.addAttribute("totalPassedCredits", totalPassedCredits);
        model.addAttribute("totalPassedCourses", totalPassedCourses);
        model.addAttribute("totalCourses", allKct.size());
        model.addAttribute("progressPercentage", progressPercentage);

        return "student/curriculum";
    }

    @GetMapping("/GpaSimulator")
    public String gpaSimulator(HttpSession session, Model model, @RequestParam(required = false, defaultValue = "3.2") Double targetCpa) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        GpaSimulatorService.SimulationResult simResult = gpaSimulatorService.simulateTargetCpa(sv.getId(), targetCpa);
        model.addAttribute("simResult", simResult);
        model.addAttribute("targetCpa", targetCpa);
        model.addAttribute("student", sv);
        return "student/gpa_simulator";
    }

    // ==================== HỘP THƯ SINH VIÊN & PHẢN HỒI ====================

    @GetMapping("/Inbox")
    public String inbox(HttpSession session, Model model, @RequestParam(required = false) String filter) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        List<ThongBao> messages = thongBaoRepository.findByNguoiNhanIdOrderByNgayGuiDesc(sv.getNguoiDung().getId());
        if (filter != null && !filter.isBlank() && !"ALL".equals(filter)) {
            messages = messages.stream()
                    .filter(m -> m.getLoaiThongBao().name().equals(filter))
                    .toList();
        }
        List<ThongBao> sentMessages = thongBaoRepository.findByNguoiGuiIdOrderByNgayGuiDesc(sv.getNguoiDung().getId());

        model.addAttribute("student", sv);
        model.addAttribute("messages", messages);
        model.addAttribute("sentMessages", sentMessages);
        model.addAttribute("selectedFilter", filter != null ? filter : "ALL");
        return "student/inbox";
    }

    @PostMapping("/Inbox/Read/{id}")
    public String markRead(@PathVariable Long id, HttpSession session) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";
        notificationService.markAsRead(id, sv.getNguoiDung().getId());
        return "redirect:/Student/Inbox";
    }

    @PostMapping("/Inbox/ReadAll")
    public String markAllRead(HttpSession session, RedirectAttributes ra) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";
        notificationService.markAllAsRead(sv.getNguoiDung().getId());
        ra.addFlashAttribute("success", "Đã đánh dấu tất cả thông báo là đã đọc.");
        return "redirect:/Student/Inbox";
    }

    @PostMapping("/Inbox/SendFeedback")
    public String sendFeedback(@RequestParam String tieuDe,
                               @RequestParam String noiDung,
                               HttpSession session, RedirectAttributes ra) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        notificationService.sendToAdmins(
                sv.getNguoiDung(),
                "[Phản hồi SV " + sv.getMssv() + "] " + tieuDe.trim(),
                noiDung.trim(),
                ThongBao.LoaiThongBao.PHAN_HOI,
                "/Admin/Notifications"
        );
        ra.addFlashAttribute("success", "Đã gửi phản hồi / đơn từ tới Phòng Đào Tạo thành công!");
        return "redirect:/Student/Inbox";
    }

    @GetMapping("/OneStop")
    public String oneStop(HttpSession session, Model model) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        model.addAttribute("requests", yeuCauDichVuMotCuaRepository.findBySinhVienId(sv.getId()));
        model.addAttribute("student", sv);
        return "student/onestop";
    }

    @PostMapping("/OneStop/Submit")
    public String submitOneStop(@RequestParam YeuCauDichVuMotCua.LoaiDichVu loaiDichVu,
                                @RequestParam String lyDo,
                                HttpSession session,
                                RedirectAttributes ra) {
        SinhVien sv = getStudent(session);
        if (sv == null) return "redirect:/Account/Login";

        String qrCode = "VERIFY-QR-" + sv.getMssv() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        YeuCauDichVuMotCua yc = YeuCauDichVuMotCua.builder()
                .sinhVien(sv)
                .loaiDichVu(loaiDichVu)
                .lyDo(lyDo)
                .trangThai(YeuCauDichVuMotCua.TrangThaiYeuCau.CHO_DUYET)
                .maXacThucQr(qrCode)
                .ngayTao(LocalDateTime.now())
                .build();

        yeuCauDichVuMotCuaRepository.save(yc);
        ra.addFlashAttribute("success", "Đã gửi yêu cầu trực tuyến thành công! Mã xác thực: " + qrCode);
        return "redirect:/Student/OneStop";
    }
}
