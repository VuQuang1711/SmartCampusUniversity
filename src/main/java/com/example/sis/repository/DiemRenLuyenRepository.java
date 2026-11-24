package com.example.sis.repository;

import com.example.sis.entity.DiemRenLuyen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiemRenLuyenRepository extends JpaRepository<DiemRenLuyen, Long> {
    List<DiemRenLuyen> findBySinhVienId(Long sinhVienId);
    List<DiemRenLuyen> findByHocKyId(Long hocKyId);
    List<DiemRenLuyen> findBySinhVienLopDanhNghiaId(Long lopDanhNghiaId);
    Optional<DiemRenLuyen> findBySinhVienIdAndHocKyId(Long sinhVienId, Long hocKyId);
}
