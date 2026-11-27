package com.example.sis.repository;

import com.example.sis.entity.LopDanhNghia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LopDanhNghiaRepository extends JpaRepository<LopDanhNghia, Long> {
    Optional<LopDanhNghia> findByMaLop(String maLop);
    List<LopDanhNghia> findByNganhHocId(Long nganhHocId);
    List<LopDanhNghia> findByCoVanHocTapId(Long coVanHocTapId);
}
