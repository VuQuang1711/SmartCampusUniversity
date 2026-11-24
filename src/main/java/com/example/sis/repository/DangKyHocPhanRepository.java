package com.example.sis.repository;

import com.example.sis.entity.DangKyHocPhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DangKyHocPhanRepository extends JpaRepository<DangKyHocPhan, Long> {
    List<DangKyHocPhan> findBySinhVienId(Long sinhVienId);
    List<DangKyHocPhan> findByLopHocPhanId(Long lopHocPhanId);
    Optional<DangKyHocPhan> findBySinhVienIdAndLopHocPhanId(Long sinhVienId, Long lopHocPhanId);

    @Query("SELECT dk FROM DangKyHocPhan dk WHERE dk.sinhVien.id = :sinhVienId AND dk.lopHocPhan.hocKy.id = :hocKyId AND dk.trangThai = 'THANH_CONG'")
    List<DangKyHocPhan> findActiveRegistrations(Long sinhVienId, Long hocKyId);

    @Query("SELECT dk FROM DangKyHocPhan dk WHERE dk.sinhVien.id = :sinhVienId AND dk.lopHocPhan.hocPhan.id = :hocPhanId")
    List<DangKyHocPhan> findHistoryByStudentAndSubject(Long sinhVienId, Long hocPhanId);
}
