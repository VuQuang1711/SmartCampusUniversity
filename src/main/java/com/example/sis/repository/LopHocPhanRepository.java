package com.example.sis.repository;

import com.example.sis.entity.LopHocPhan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface LopHocPhanRepository extends JpaRepository<LopHocPhan, Long> {
    Optional<LopHocPhan> findByMaLHP(String maLHP);
    List<LopHocPhan> findByHocKyId(Long hocKyId);
    List<LopHocPhan> findByHocPhanId(Long hocPhanId);
    List<LopHocPhan> findByGiangVienId(Long giangVienId);
    List<LopHocPhan> findByTrangThaiOrderByNgayDeXuatDesc(LopHocPhan.TrangThaiLHP trangThai);
    List<LopHocPhan> findByHocKyIdAndTrangThai(Long hocKyId, LopHocPhan.TrangThaiLHP trangThai);
    long countByTrangThai(LopHocPhan.TrangThaiLHP trangThai);

    // Pessimistic Write Lock khi đăng ký để tuyệt đối chống race condition vượt quá sĩ số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT lhp FROM LopHocPhan lhp WHERE lhp.id = :id")
    Optional<LopHocPhan> findByIdWithLock(Long id);
}
