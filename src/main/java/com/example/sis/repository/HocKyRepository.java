package com.example.sis.repository;

import com.example.sis.entity.HocKy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HocKyRepository extends JpaRepository<HocKy, Long> {
    Optional<HocKy> findByMaHocKy(String maHocKy);
    List<HocKy> findByTrangThaiDangKy(HocKy.TrangThaiDangKy trangThai);
    Optional<HocKy> findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy trangThai);
}
