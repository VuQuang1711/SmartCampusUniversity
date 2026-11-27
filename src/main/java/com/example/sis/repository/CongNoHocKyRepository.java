package com.example.sis.repository;

import com.example.sis.entity.CongNoHocKy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CongNoHocKyRepository extends JpaRepository<CongNoHocKy, Long> {
    List<CongNoHocKy> findBySinhVienId(Long sinhVienId);
    Optional<CongNoHocKy> findBySinhVienIdAndHocKyId(Long sinhVienId, Long hocKyId);
    List<CongNoHocKy> findByHocKyId(Long hocKyId);
}
