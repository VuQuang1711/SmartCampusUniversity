package com.example.sis.repository;

import com.example.sis.entity.GiangVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface GiangVienRepository extends JpaRepository<GiangVien, Long> {
    Optional<GiangVien> findByMaGV(String maGV);
    Optional<GiangVien> findByNguoiDungId(Long nguoiDungId);
}
