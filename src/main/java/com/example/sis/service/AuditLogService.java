package com.example.sis.service;

import com.example.sis.entity.NguoiDung;
import com.example.sis.entity.NhatKyHeThong;
import com.example.sis.repository.NhatKyHeThongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    @Autowired
    private NhatKyHeThongRepository nhatKyHeThongRepository;

    public void log(NguoiDung actor, String hanhDong, String doiTuong, String chiTiet) {
        NhatKyHeThong entry = NhatKyHeThong.builder()
                .nguoiThucHien(actor)
                .tenNguoiThucHien(actor != null ? actor.getHoTen() : "Hệ thống")
                .vaiTro(actor != null && actor.getVaiTro() != null ? actor.getVaiTro().name() : "SYSTEM")
                .hanhDong(hanhDong)
                .doiTuong(doiTuong)
                .chiTiet(chiTiet)
                .thoiGian(LocalDateTime.now())
                .build();
        nhatKyHeThongRepository.save(entry);
    }
}
