package com.example.sis.repository;

import com.example.sis.entity.ThongBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ThongBaoRepository extends JpaRepository<ThongBao, Long> {
    List<ThongBao> findByNguoiNhanIdOrderByNgayGuiDesc(Long nguoiNhanId);
    List<ThongBao> findByNguoiGuiIdOrderByNgayGuiDesc(Long nguoiGuiId);
    List<ThongBao> findByLoaiThongBaoOrderByNgayGuiDesc(ThongBao.LoaiThongBao loai);
    long countByNguoiNhanIdAndDaDocFalse(Long nguoiNhanId);
}
