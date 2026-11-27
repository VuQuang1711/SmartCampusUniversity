package com.example.sis.repository;

import com.example.sis.entity.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {
    List<ThongBao> findByNguoiNhanIdOrderByNgayGuiDesc(Long nguoiNhanId);
    List<ThongBao> findByNguoiGuiIdOrderByNgayGuiDesc(Long nguoiGuiId);
    List<ThongBao> findByLoaiThongBaoOrderByNgayGuiDesc(ThongBao.LoaiThongBao loai);
    long countByNguoiNhanIdAndDaDocFalse(Long nguoiNhanId);
}
