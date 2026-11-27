package com.example.sis.repository;

import com.example.sis.entity.PhongHoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhongHocRepository extends JpaRepository<PhongHoc, Long> {
    Optional<PhongHoc> findByMaPhong(String maPhong);
}
