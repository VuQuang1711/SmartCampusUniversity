package com.example.sis.repository;

import com.example.sis.entity.LichHoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LichHocRepository extends JpaRepository<LichHoc, Long> {
    List<LichHoc> findByLopHocPhanId(Long lopHocPhanId);
    List<LichHoc> findByLopHocPhanHocKyId(Long hocKyId);

    @Query("SELECT lh FROM LichHoc lh JOIN DangKyHocPhan dk ON dk.lopHocPhan = lh.lopHocPhan " +
           "WHERE dk.sinhVien.id = :sinhVienId AND lh.lopHocPhan.hocKy.id = :hocKyId AND dk.trangThai = 'THANH_CONG'")
    List<LichHoc> findLichHocBySinhVienAndHocKy(Long sinhVienId, Long hocKyId);

    @Query("SELECT lh FROM LichHoc lh WHERE lh.lopHocPhan.giangVien.id = :giangVienId AND lh.lopHocPhan.hocKy.id = :hocKyId")
    List<LichHoc> findLichHocByGiangVienAndHocKy(Long giangVienId, Long hocKyId);
}
