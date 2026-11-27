package com.example.sis.controller;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.*;
import com.example.sis.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final SinhVienRepository sinhVienRepository;
    private final ThongBaoRepository thongBaoRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final YeuCauDoiLichRepository yeuCauDoiLichRepository;
    private final ChungChiChuanDauRaRepository chungChiChuanDauRaRepository;

    @ModelAttribute("currentUser")
    public NguoiDung getCurrentUser(HttpSession session) {
        return (NguoiDung) session.getAttribute("currentUser");
    }

    @ModelAttribute("currentStudent")
    public SinhVien getCurrentStudent(HttpSession session) {
        NguoiDung u = (NguoiDung) session.getAttribute("currentUser");
        if (u != null && u.getVaiTro() == VaiTro.SINH_VIEN) {
            return sinhVienRepository.findByNguoiDungId(u.getId()).orElse(null);
        }
        return null;
    }

    @ModelAttribute("unreadInboxCount")
    public long getUnreadInboxCount(HttpSession session) {
        NguoiDung u = (NguoiDung) session.getAttribute("currentUser");
        if (u == null) return 0;
        return thongBaoRepository.countByNguoiNhanIdAndDaDocFalse(u.getId());
    }

    @ModelAttribute("pendingApprovalsCount")
    public long getPendingApprovalsCount(HttpSession session) {
        NguoiDung u = (NguoiDung) session.getAttribute("currentUser");
        if (u == null) return 0;
        if (u.getVaiTro() == VaiTro.ADMIN || u.getVaiTro() == VaiTro.PHONG_DAO_TAO || u.getVaiTro() == VaiTro.KE_TOAN) {
            return lopHocPhanRepository.countByTrangThai(LopHocPhan.TrangThaiLHP.CHO_DUYET)
                    + yeuCauDoiLichRepository.countByTrangThai(YeuCauDoiLich.TrangThaiDuyet.CHO_DUYET)
                    + chungChiChuanDauRaRepository.countByTrangThai(ChungChiChuanDauRa.TrangThaiChungChi.CHO_DUYET);
        }
        return 0;
    }
}
