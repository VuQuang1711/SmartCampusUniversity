package com.example.sis.repository;

import com.example.sis.entity.KhungChuongTrinh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KhungChuongTrinhRepository extends JpaRepository<KhungChuongTrinh, Long> {
    List<KhungChuongTrinh> findByNganhHocId(Long nganhHocId);
    List<KhungChuongTrinh> findByNganhHocIdAndHocKyDuKien(Long nganhHocId, Integer hocKyDuKien);
    List<KhungChuongTrinh> findByNganhHocIdOrderByHocKyDuKienAsc(Long nganhHocId);
}

