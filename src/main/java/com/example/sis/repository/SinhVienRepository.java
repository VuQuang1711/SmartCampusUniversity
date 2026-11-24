package com.example.sis.repository;

import com.example.sis.entity.SinhVien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SinhVienRepository extends JpaRepository<SinhVien, Long> {
    Optional<SinhVien> findByMssv(String mssv);
    Optional<SinhVien> findByNguoiDungId(Long nguoiDungId);
    List<SinhVien> findByLopDanhNghiaId(Long lopDanhNghiaId);
    List<SinhVien> findByNganhHocId(Long nganhHocId);
    List<SinhVien> findByTrangThaiHocVu(String trangThaiHocVu);

    @Query("SELECT s FROM SinhVien s WHERE LOWER(s.mssv) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(s.nguoiDung.hoTen) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<SinhVien> searchSinhVien(String query);
}
