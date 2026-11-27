package com.example.sis.repository;

import com.example.sis.entity.DieuKienHocPhan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DieuKienHocPhanRepository extends JpaRepository<DieuKienHocPhan, Long> {
    List<DieuKienHocPhan> findByHocPhanId(Long hocPhanId);
    List<DieuKienHocPhan> findByHocPhanIdAndLoaiDieuKien(Long hocPhanId, DieuKienHocPhan.LoaiDieuKien loaiDieuKien);
}
