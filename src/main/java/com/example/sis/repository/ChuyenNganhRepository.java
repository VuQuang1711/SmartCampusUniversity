package com.example.sis.repository;

import com.example.sis.entity.ChuyenNganh;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ChuyenNganhRepository extends JpaRepository<ChuyenNganh, Long> {
    Optional<ChuyenNganh> findByMaChuyenNganh(String maChuyenNganh);
    List<ChuyenNganh> findByNganhHocId(Long nganhHocId);
}
