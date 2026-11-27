package com.example.sis.repository;

import com.example.sis.entity.Khoa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface KhoaRepository extends JpaRepository<Khoa, Long> {
    Optional<Khoa> findByMaKhoa(String maKhoa);
}
