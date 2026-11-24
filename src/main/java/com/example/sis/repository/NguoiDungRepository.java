package com.example.sis.repository;

import com.example.sis.entity.NguoiDung;
import com.example.sis.entity.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByEmail(String email);
    Optional<NguoiDung> findByMaDinhDanh(String maDinhDanh);
    List<NguoiDung> findByVaiTro(VaiTro vaiTro);
    List<NguoiDung> findByVaiTroIn(List<VaiTro> vaiTros);
}
