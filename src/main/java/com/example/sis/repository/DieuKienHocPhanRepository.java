package com.example.sis.repository;

import com.example.sis.entity.DieuKienHocPhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DieuKienHocPhanRepository extends JpaRepository<DieuKienHocPhan, Long> {
    List<DieuKienHocPhan> findByHocPhanId(Long hocPhanId);
    List<DieuKienHocPhan> findByHocPhanIdAndLoaiDieuKien(Long hocPhanId, DieuKienHocPhan.LoaiDieuKien loaiDieuKien);
}
