package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dang_ky_hoc_phan", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sinh_vien_id", "lop_hoc_phan_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DangKyHocPhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private SinhVien sinhVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private LopHocPhan lopHocPhan;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_dang_ky", nullable = false, length = 20)
    @Builder.Default
    private LoaiDangKy loaiDangKy = LoaiDangKy.HOC_MOI;

    @Column(name = "ngay_dang_ky")
    private LocalDateTime ngayDangKy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TrangThaiDangKy trangThai = TrangThaiDangKy.THANH_CONG;

    @PrePersist
    public void prePersist() {
        if (ngayDangKy == null) ngayDangKy = LocalDateTime.now();
    }

    public enum LoaiDangKy {
        HOC_MOI, HOC_LAI, CAI_THIEN
    }

    public enum TrangThaiDangKy {
        THANH_CONG, DA_HUY
    }
}
