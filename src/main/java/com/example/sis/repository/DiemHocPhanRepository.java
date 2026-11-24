package com.example.sis.repository;

import com.example.sis.entity.DiemHocPhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiemHocPhanRepository extends JpaRepository<DiemHocPhan, Long> {
    Optional<DiemHocPhan> findByDangKyHocPhanId(Long dangKyHocPhanId);

    @Query("SELECT d FROM DiemHocPhan d WHERE d.dangKyHocPhan.sinhVien.id = :sinhVienId")
    List<DiemHocPhan> findAllBySinhVienId(Long sinhVienId);

    @Query("SELECT d FROM DiemHocPhan d WHERE d.dangKyHocPhan.sinhVien.id = :sinhVienId AND d.dangKyHocPhan.lopHocPhan.hocKy.id = :hocKyId")
    List<DiemHocPhan> findBySinhVienIdAndHocKyId(Long sinhVienId, Long hocKyId);

    @Query("SELECT d FROM DiemHocPhan d WHERE d.dangKyHocPhan.lopHocPhan.id = :lopHocPhanId")
    List<DiemHocPhan> findByLopHocPhanId(Long lopHocPhanId);
}
