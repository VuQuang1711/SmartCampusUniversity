package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "nguoi_dung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String maDinhDanh; // MSSV hoặc MSGV hoặc Mã nhân viên

    @Column(nullable = false, length = 100)
    private String hoTen;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String matKhau;

    @Column(length = 20)
    private String soDienThoai;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VaiTro vaiTro;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private TrangThaiTaiKhoan trangThai = TrangThaiTaiKhoan.HOAT_DONG;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @PrePersist
    public void prePersist() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
        if (trangThai == null) trangThai = TrangThaiTaiKhoan.HOAT_DONG;
    }

    public enum TrangThaiTaiKhoan {
        HOAT_DONG, BI_KHOA
    }
}
