package com.example.sis.repository;

import com.example.sis.entity.PhieuThuHocPhi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PhieuThuHocPhiRepository extends JpaRepository<PhieuThuHocPhi, Long> {
    List<PhieuThuHocPhi> findByCongNoHocKyId(Long congNoHocKyId);
    Optional<PhieuThuHocPhi> findByMaPhieuThu(String maPhieuThu);

    @Query("SELECT p FROM PhieuThuHocPhi p WHERE p.congNoHocKy.sinhVien.id = :sinhVienId ORDER BY p.ngayThu DESC")
    List<PhieuThuHocPhi> findBySinhVienId(@Param("sinhVienId") Long sinhVienId);
}

