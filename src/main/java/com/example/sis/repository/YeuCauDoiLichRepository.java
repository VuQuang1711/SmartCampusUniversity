package com.example.sis.repository;

import com.example.sis.entity.YeuCauDoiLich;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface YeuCauDoiLichRepository extends JpaRepository<YeuCauDoiLich, Long> {
    List<YeuCauDoiLich> findByGiangVienIdOrderByNgayTaoDesc(Long giangVienId);
    List<YeuCauDoiLich> findByTrangThaiOrderByNgayTaoDesc(YeuCauDoiLich.TrangThaiDuyet trangThai);
    List<YeuCauDoiLich> findAllByOrderByNgayTaoDesc();
    long countByTrangThai(YeuCauDoiLich.TrangThaiDuyet trangThai);
}
