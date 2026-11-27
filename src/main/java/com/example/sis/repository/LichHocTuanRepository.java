package com.example.sis.repository;

import com.example.sis.entity.LichHocTuan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LichHocTuanRepository extends JpaRepository<LichHocTuan, Long> {
    List<LichHocTuan> findByLichHocIdOrderByTuanThuAsc(Long lichHocId);
    void deleteByLichHocId(Long lichHocId);
}
