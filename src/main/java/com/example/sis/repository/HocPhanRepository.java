package com.example.sis.repository;

import com.example.sis.entity.HocPhan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface HocPhanRepository extends JpaRepository<HocPhan, Long> {
    Optional<HocPhan> findByMaHP(String maHP);
    List<HocPhan> findByKhoaId(Long khoaId);

    @Query("SELECT hp FROM HocPhan hp WHERE LOWER(hp.maHP) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(hp.tenHP) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<HocPhan> searchHocPhan(String query);
}
