package com.example.sis.service;

import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired private ThongBaoRepository thongBaoRepository;
    @Autowired private NguoiDungRepository nguoiDungRepository;
    @Autowired private DangKyHocPhanRepository dangKyHocPhanRepository;

    public ThongBao sendToUser(NguoiDung sender, NguoiDung recipient, String title, String content,
                               ThongBao.LoaiThongBao type, String link) {
        if (recipient == null) return null;
        ThongBao tb = ThongBao.builder()
                .nguoiGui(sender)
                .nguoiNhan(recipient)
                .tieuDe(title)
                .noiDung(content)
                .loaiThongBao(type != null ? type : ThongBao.LoaiThongBao.HE_THONG)
                .daDoc(false)
                .lienKet(link)
                .ngayGui(LocalDateTime.now())
                .build();
        return thongBaoRepository.save(tb);
    }

    @Transactional
    public int sendToClassStudents(NguoiDung sender, Long lopHocPhanId, String title, String content,
                                   ThongBao.LoaiThongBao type, String link) {
        List<DangKyHocPhan> enrollments = dangKyHocPhanRepository.findByLopHocPhanId(lopHocPhanId);
        int sent = 0;
        for (DangKyHocPhan dk : enrollments) {
            if (dk.getTrangThai() == DangKyHocPhan.TrangThaiDangKy.THANH_CONG
                    && dk.getSinhVien() != null && dk.getSinhVien().getNguoiDung() != null) {
                sendToUser(sender, dk.getSinhVien().getNguoiDung(), title, content, type, link);
                sent++;
            }
        }
        return sent;
    }

    @Transactional
    public int sendToAdmins(NguoiDung sender, String title, String content,
                            ThongBao.LoaiThongBao type, String link) {
        List<NguoiDung> admins = nguoiDungRepository.findByVaiTroIn(
                List.of(VaiTro.ADMIN, VaiTro.PHONG_DAO_TAO));
        int sent = 0;
        for (NguoiDung admin : admins) {
            sendToUser(sender, admin, title, content, type, link);
            sent++;
        }
        return sent;
    }

    @Transactional
    public int broadcast(NguoiDung sender, String targetGroup, String title, String content, String link) {
        List<NguoiDung> recipients;
        switch (targetGroup) {
            case "SINH_VIEN":
                recipients = nguoiDungRepository.findByVaiTro(VaiTro.SINH_VIEN);
                break;
            case "GIANG_VIEN":
                recipients = nguoiDungRepository.findByVaiTroIn(List.of(VaiTro.GIANG_VIEN, VaiTro.CO_VAN_HOC_TAP));
                break;
            default:
                recipients = nguoiDungRepository.findAll();
                break;
        }
        int sent = 0;
        for (NguoiDung u : recipients) {
            if (sender != null && u.getId().equals(sender.getId())) continue;
            sendToUser(sender, u, title, content, ThongBao.LoaiThongBao.TOAN_TRUONG, link);
            sent++;
        }
        return sent;
    }

    @Transactional
    public void markAsRead(Long thongBaoId, Long currentUserId) {
        thongBaoRepository.findById(thongBaoId).ifPresent(tb -> {
            if (tb.getNguoiNhan() != null && tb.getNguoiNhan().getId().equals(currentUserId)) {
                tb.setDaDoc(true);
                thongBaoRepository.save(tb);
            }
        });
    }

    @Transactional
    public void markAllAsRead(Long currentUserId) {
        List<ThongBao> list = thongBaoRepository.findByNguoiNhanIdOrderByNgayGuiDesc(currentUserId);
        for (ThongBao tb : list) {
            if (!Boolean.TRUE.equals(tb.getDaDoc())) {
                tb.setDaDoc(true);
                thongBaoRepository.save(tb);
            }
        }
    }
}
