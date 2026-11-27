package com.example.sis.repository;

import com.example.sis.entity.CoSo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoSoRepository extends JpaRepository<CoSo, Long> {
    Optional<CoSo> findByMaCoSo(String maCoSo);
}
