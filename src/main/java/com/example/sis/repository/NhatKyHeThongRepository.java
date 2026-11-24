package com.example.sis.repository;

import com.example.sis.entity.NhatKyHeThong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NhatKyHeThongRepository extends JpaRepository<NhatKyHeThong, Long> {
    List<NhatKyHeThong> findTop100ByOrderByThoiGianDesc();
    List<NhatKyHeThong> findByHanhDongOrderByThoiGianDesc(String hanhDong);
}
