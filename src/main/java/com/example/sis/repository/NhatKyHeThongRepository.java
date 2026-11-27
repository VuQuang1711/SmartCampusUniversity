package com.example.sis.repository;

import com.example.sis.entity.NhatKyHeThong;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NhatKyHeThongRepository extends JpaRepository<NhatKyHeThong, Long> {
    List<NhatKyHeThong> findTop100ByOrderByThoiGianDesc();
    List<NhatKyHeThong> findByHanhDongOrderByThoiGianDesc(String hanhDong);
}
