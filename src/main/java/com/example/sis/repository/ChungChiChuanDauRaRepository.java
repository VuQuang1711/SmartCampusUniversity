package com.example.sis.repository;

import com.example.sis.entity.ChungChiChuanDauRa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChungChiChuanDauRaRepository extends JpaRepository<ChungChiChuanDauRa, Long> {
    List<ChungChiChuanDauRa> findBySinhVienId(Long sinhVienId);
    List<ChungChiChuanDauRa> findBySinhVienIdOrderByNgayNopDesc(Long sinhVienId);
    List<ChungChiChuanDauRa> findBySinhVienIdAndTrangThai(Long sinhVienId, ChungChiChuanDauRa.TrangThaiChungChi trangThai);
    List<ChungChiChuanDauRa> findByTrangThaiOrderByNgayNopDesc(ChungChiChuanDauRa.TrangThaiChungChi trangThai);
    List<ChungChiChuanDauRa> findAllByOrderByNgayNopDesc();
    long countByTrangThai(ChungChiChuanDauRa.TrangThaiChungChi trangThai);
}
