package com.example.sis.repository;

import com.example.sis.entity.DiemDanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface DiemDanhRepository extends JpaRepository<DiemDanh, Long> {
    List<DiemDanh> findByLichHocId(Long lichHocId);
    List<DiemDanh> findByLichHocIdAndNgayHoc(Long lichHocId, LocalDate ngayHoc);
    List<DiemDanh> findBySinhVienId(Long sinhVienId);

    @Query("SELECT dd FROM DiemDanh dd WHERE dd.sinhVien.id = :sinhVienId AND dd.lichHoc.lopHocPhan.id = :lopHocPhanId")
    List<DiemDanh> findBySinhVienAndLopHocPhan(Long sinhVienId, Long lopHocPhanId);

    @Query("SELECT COUNT(dd) FROM DiemDanh dd WHERE dd.sinhVien.id = :sinhVienId AND dd.lichHoc.lopHocPhan.id = :lopHocPhanId AND (dd.trangThai = 'VANG' OR dd.trangThai = 'VANG_CO_PHEP')")
    long countVangBySinhVienAndLopHocPhan(Long sinhVienId, Long lopHocPhanId);

    @Query("SELECT COUNT(dd) FROM DiemDanh dd WHERE dd.sinhVien.id = :sinhVienId AND dd.lichHoc.lopHocPhan.id = :lopHocPhanId")
    long countTongBuoiBySinhVienAndLopHocPhan(Long sinhVienId, Long lopHocPhanId);
}
