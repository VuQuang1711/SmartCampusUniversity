package com.example.sis.repository;

import com.example.sis.entity.YeuCauDichVuMotCua;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface YeuCauDichVuMotCuaRepository extends JpaRepository<YeuCauDichVuMotCua, Long> {
    List<YeuCauDichVuMotCua> findBySinhVienId(Long sinhVienId);
    List<YeuCauDichVuMotCua> findByTrangThai(YeuCauDichVuMotCua.TrangThaiYeuCau trangThai);
    Optional<YeuCauDichVuMotCua> findByMaXacThucQr(String maXacThucQr);
}
