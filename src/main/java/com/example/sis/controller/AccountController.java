package com.example.sis.controller;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.*;
import com.example.sis.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/Account")
@RequiredArgsConstructor
public class AccountController {

    private final NguoiDungRepository nguoiDungRepository;
    private final SinhVienRepository sinhVienRepository;
    private final GiangVienRepository giangVienRepository;

    @GetMapping("/Login")
    public String loginPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return redirectByRole((NguoiDung) session.getAttribute("currentUser"));
        }
        return "account/login";
    }

    @PostMapping("/Login")
    public String handleLogin(@RequestParam String identifier,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes ra) {
        // Hỗ trợ đăng nhập bằng Mã định danh (MSSV/MSGV) hoặc Email
        Optional<NguoiDung> userOpt = nguoiDungRepository.findByMaDinhDanh(identifier);
        if (userOpt.isEmpty()) {
            userOpt = nguoiDungRepository.findByEmail(identifier);
        }

        if (userOpt.isPresent()) {
            NguoiDung user = userOpt.get();
            if (user.getTrangThai() == NguoiDung.TrangThaiTaiKhoan.BI_KHOA) {
                ra.addFlashAttribute("error", "Tài khoản của bạn đã bị khóa bởi Quản trị viên! Vui lòng liên hệ Phòng Đào Tạo.");
                return "redirect:/Account/Login";
            }
            if (user.getMatKhau().equals(password)) {
                session.setAttribute("currentUser", user);

                // Gắn thêm profile cụ thể vào session
                if (user.getVaiTro() == VaiTro.SINH_VIEN) {
                    sinhVienRepository.findByNguoiDungId(user.getId())
                            .ifPresent(sv -> session.setAttribute("currentStudent", sv));
                } else if (user.getVaiTro() == VaiTro.GIANG_VIEN || user.getVaiTro() == VaiTro.CO_VAN_HOC_TAP) {
                    giangVienRepository.findByNguoiDungId(user.getId())
                            .ifPresent(gv -> session.setAttribute("currentFaculty", gv));
                }

                return redirectByRole(user);
            }
        }

        ra.addFlashAttribute("error", "Mã định danh/Email hoặc mật khẩu không chính xác!");
        return "redirect:/Account/Login";
    }

    @GetMapping("/Logout")
    public String handleLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/Account/Login";
    }

    private String redirectByRole(NguoiDung user) {
        if (user == null) return "redirect:/Account/Login";
        switch (user.getVaiTro()) {
            case SINH_VIEN:
                return "redirect:/Student/Dashboard";
            case GIANG_VIEN:
            case CO_VAN_HOC_TAP:
                return "redirect:/Faculty/Dashboard";
            case ADMIN:
            case PHONG_DAO_TAO:
            case KE_TOAN:
            default:
                return "redirect:/Admin/Dashboard";
        }
    }
}
