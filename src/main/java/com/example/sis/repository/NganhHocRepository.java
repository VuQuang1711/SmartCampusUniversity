package com.example.sis.repository;

import com.example.sis.entity.NganhHoc;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface NganhHocRepository extends JpaRepository<NganhHoc, Long> {
    Optional<NganhHoc> findByMaNganh(String maNganh);
    List<NganhHoc> findByKhoaId(Long khoaId);
}
