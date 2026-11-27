package com.example.sis.repository;

import com.example.sis.entity.KhungChuongTrinh;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KhungChuongTrinhRepository extends JpaRepository<KhungChuongTrinh, Long> {
    List<KhungChuongTrinh> findByNganhHocId(Long nganhHocId);
    List<KhungChuongTrinh> findByNganhHocIdAndHocKyDuKien(Long nganhHocId, Integer hocKyDuKien);
    List<KhungChuongTrinh> findByNganhHocIdOrderByHocKyDuKienAsc(Long nganhHocId);
}

