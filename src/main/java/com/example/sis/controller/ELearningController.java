package com.example.sis.controller;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.*;
import com.example.sis.repository.*;
import com.example.sis.service.ELearningService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping({"/ELearning", "/elearning"})
@RequiredArgsConstructor
public class ELearningController {

    private final ELearningService eLearningService;
    private final NguoiDungRepository nguoiDungRepository;
    private final SinhVienRepository sinhVienRepository;

    /**
     * Cổng chính E-Learning (có màn hình chờ nền trắng + logo HUIT trước khi vào giao diện)
     * Hỗ trợ tự động đăng nhập liên kết SSO từ Cổng Sinh viên / SIS nếu đã đăng nhập
     */
    @GetMapping({"", "/"})
    public String index(@RequestParam(required = false, defaultValue = "ALL") String category,
                        @RequestParam(required = false, defaultValue = "") String q,
                        @RequestParam(required = false, defaultValue = "ALL") String track,
                        @RequestParam(required = false, defaultValue = "true") boolean splash,
                        HttpSession session,
                        RedirectAttributes ra,
                        Model model) {
        NguoiDung currentUser = (NguoiDung) session.getAttribute("currentUser");
        Boolean eLearningAuthenticated = (Boolean) session.getAttribute("eLearningAuth");

        // E-Learning chỉ dành cho Sinh viên
        if (currentUser != null && currentUser.getVaiTro() != VaiTro.SINH_VIEN) {
            ra.addFlashAttribute("error", "HUIT E-Learning chỉ dành cho tài khoản Sinh viên! Giảng viên/Quản trị vui lòng quay lại Cổng Thông Tin HUIT.");
            return "redirect:/ELearning/Login?splash=false";
        }

        // Nếu đã đăng nhập Cổng Sinh viên -> tự động liên kết phiên đăng nhập sang E-Learning
        if (currentUser != null && !Boolean.TRUE.equals(eLearningAuthenticated)) {
            session.setAttribute("eLearningAuth", true);
            session.setAttribute("eLearningSsoLinked", true);
        }

        // Nếu chưa đăng nhập -> chuyển sang trang đăng nhập E-Learning (vẫn giữ splash nền trắng logo HUIT)
        if (currentUser == null) {
            return "redirect:/ELearning/Login?splash=" + splash;
        }

        List<ELearningService.ELearningCourse> filteredCourses = eLearningService.filterCourses(category, q, track);
        model.addAttribute("courses", filteredCourses);
        model.addAttribute("totalCoursesCount", eLearningService.getAllCourses().size());
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedTrack", track);
        model.addAttribute("searchQuery", q);
        model.addAttribute("showSplash", splash);
        model.addAttribute("ssoLinked", Boolean.TRUE.equals(session.getAttribute("eLearningSsoLinked")));
        model.addAttribute("portalReturnUrl", "/Student/Dashboard");
        return "elearning/index";
    }

    /**
     * Trang đăng nhập riêng của HUIT E-Learning (chỉ dành cho Sinh viên, hỗ trợ liên kết 1 chạm từ Cổng Sinh viên)
     */
    @GetMapping("/Login")
    public String loginPage(@RequestParam(required = false, defaultValue = "true") boolean splash,
                            HttpSession session, Model model) {
        NguoiDung existingSisUser = (NguoiDung) session.getAttribute("currentUser");
        // Chỉ hiển thị nút liên kết nhanh nếu phiên SIS đang đăng nhập là Sinh viên
        model.addAttribute("existingSisUser",
                (existingSisUser != null && existingSisUser.getVaiTro() == VaiTro.SINH_VIEN) ? existingSisUser : null);
        model.addAttribute("showSplash", splash);
        return "elearning/login";
    }

    @PostMapping("/Login")
    public String handleLogin(@RequestParam String identifier,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes ra) {
        Optional<NguoiDung> userOpt = nguoiDungRepository.findByMaDinhDanh(identifier.trim());
        if (userOpt.isEmpty()) {
            userOpt = nguoiDungRepository.findByEmail(identifier.trim());
        }

        if (userOpt.isPresent()) {
            NguoiDung user = userOpt.get();
            if (user.getTrangThai() == NguoiDung.TrangThaiTaiKhoan.BI_KHOA) {
                ra.addFlashAttribute("error", "Tài khoản của bạn đang bị khóa. Vui lòng liên hệ Phòng Đào Tạo!");
                return "redirect:/ELearning/Login?splash=false";
            }
            if (user.getMatKhau().equals(password)) {
                if (user.getVaiTro() != VaiTro.SINH_VIEN) {
                    ra.addFlashAttribute("error", "HUIT E-Learning chỉ dành cho tài khoản Sinh viên!");
                    return "redirect:/ELearning/Login?splash=false";
                }
                session.setAttribute("currentUser", user);
                session.setAttribute("eLearningAuth", true);
                session.setAttribute("eLearningSsoLinked", false);
                session.removeAttribute("currentFaculty");

                sinhVienRepository.findByNguoiDungId(user.getId())
                        .ifPresent(sv -> session.setAttribute("currentStudent", sv));
                return "redirect:/ELearning?splash=false";
            }
        }

        ra.addFlashAttribute("error", "MSSV/Email hoặc mật khẩu không chính xác!");
        return "redirect:/ELearning/Login?splash=false";
    }

    @PostMapping("/SsoLink")
    public String ssoLinkFromStudentPortal(HttpSession session, RedirectAttributes ra) {
        NguoiDung existing = (NguoiDung) session.getAttribute("currentUser");
        if (existing == null || existing.getVaiTro() != VaiTro.SINH_VIEN) {
            ra.addFlashAttribute("error", "Vui lòng đăng nhập bằng tài khoản Sinh viên HUIT!");
            return "redirect:/ELearning/Login?splash=false";
        }
        session.setAttribute("eLearningAuth", true);
        session.setAttribute("eLearningSsoLinked", true);
        return "redirect:/ELearning?splash=false";
    }

    @GetMapping("/Logout")
    public String logoutELearning(HttpSession session) {
        session.removeAttribute("eLearningAuth");
        session.removeAttribute("eLearningSsoLinked");
        session.removeAttribute("currentUser");
        session.removeAttribute("currentStudent");
        session.removeAttribute("currentFaculty");
        return "redirect:/ELearning/Login?splash=false";
    }

    /**
     * Chi tiết môn học: Giới thiệu môn học, kỹ năng, CLO/PLO, đề cương, giáo trình & nút "Bắt đầu học"
     */
    @GetMapping("/Course/{slug}")
    public String courseDetail(@PathVariable String slug, HttpSession session, Model model) {
        NguoiDung currentUser = (NguoiDung) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getVaiTro() != VaiTro.SINH_VIEN) {
            return "redirect:/ELearning/Login?splash=false";
        }

        ELearningService.ELearningCourse course = eLearningService.findBySlug(slug).orElse(null);
        if (course == null) return "redirect:/ELearning?splash=false";

        model.addAttribute("course", course);
        model.addAttribute("courseDocuments", eLearningService.getDocumentsForCourse(slug));
        model.addAttribute("relatedCourses", eLearningService.getAllCourses().stream()
                .filter(c -> !c.slug.equals(slug) && c.category.equals(course.category))
                .limit(3).toList());
        model.addAttribute("portalReturnUrl", "/Student/Dashboard");
        return "elearning/course_detail";
    }

    /**
     * Phòng học trực tuyến E-Learning: đọc trực tiếp PDF slide & tải tài liệu thật từ class_database/elearningdocument/
     */
    @GetMapping("/Course/{slug}/Learn")
    public String startLearning(@PathVariable String slug, HttpSession session, Model model) {
        NguoiDung currentUser = (NguoiDung) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getVaiTro() != VaiTro.SINH_VIEN) {
            return "redirect:/ELearning/Login?splash=false";
        }

        ELearningService.ELearningCourse course = eLearningService.findBySlug(slug).orElse(null);
        if (course == null) return "redirect:/ELearning?splash=false";

        List<ELearningService.DocumentItem> docs = eLearningService.getDocumentsForCourse(slug);
        model.addAttribute("course", course);
        model.addAttribute("documents", docs);
        model.addAttribute("backUrl", "/ELearning/Course/" + course.slug);
        return "elearning/learn";
    }

    @GetMapping("/Document/View")
    public ResponseEntity<Resource> viewDocument(@RequestParam("path") String relativePath, HttpSession session) {
        NguoiDung currentUser = (NguoiDung) session.getAttribute("currentUser");
        if (currentUser == null) return ResponseEntity.status(401).build();

        Optional<Path> fileOpt = eLearningService.resolveDocumentFile(relativePath);
        if (fileOpt.isEmpty()) return ResponseEntity.notFound().build();

        Path file = fileOpt.get();
        String fileName = file.getFileName().toString();
        MediaType mediaType = fileName.toLowerCase().endsWith(".pdf")
                ? MediaType.APPLICATION_PDF
                : MediaType.APPLICATION_OCTET_STREAM;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new FileSystemResource(file));
    }

    @GetMapping("/Document/Download")
    public ResponseEntity<Resource> downloadDocument(@RequestParam("path") String relativePath, HttpSession session) {
        NguoiDung currentUser = (NguoiDung) session.getAttribute("currentUser");
        if (currentUser == null) return ResponseEntity.status(401).build();

        Optional<Path> fileOpt = eLearningService.resolveDocumentFile(relativePath);
        if (fileOpt.isEmpty()) return ResponseEntity.notFound().build();

        Path file = fileOpt.get();
        String fileName = file.getFileName().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new FileSystemResource(file));
    }
}
