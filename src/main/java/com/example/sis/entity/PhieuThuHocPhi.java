package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "phieu_thu_hoc_phi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuThuHocPhi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cong_no_hoc_ky_id", nullable = false)
    private CongNoHocKy congNoHocKy;

    @Column(nullable = false, unique = true, length = 50)
    private String maPhieuThu;

    @Column(nullable = false)
    private Double soTien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PhuongThucThanhToan phuongThuc = PhuongThucThanhToan.VIETQR;

    @Column(name = "ma_giao_dich", length = 100)
    private String maGiaoDich;

    @Column(name = "ngay_thu")
    private LocalDateTime ngayThu;

    @Column(name = "nguoi_thu", length = 100)
    private String nguoiThu;

    @Column(length = 255)
    private String ghiChu;

    @PrePersist
    public void prePersist() {
        if (ngayThu == null) ngayThu = LocalDateTime.now();
    }

    public enum PhuongThucThanhToan {
        VIETQR, CHUYEN_KHOAN, TIEN_MAT, VNPAY
    }
}
