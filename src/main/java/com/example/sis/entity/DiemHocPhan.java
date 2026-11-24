package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diem_hoc_phan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiemHocPhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dang_ky_hoc_phan_id", nullable = false, unique = true)
    private DangKyHocPhan dangKyHocPhan;

    @Column(name = "diem_chuyen_can")
    private Double diemChuyenCan;

    @Column(name = "diem_qua_trinh")
    private Double diemQuaTrinh;

    @Column(name = "diem_thuc_hanh")
    private Double diemThucHanh;

    @Column(name = "diem_cuoi_ky")
    private Double diemCuoiKy;

    @Column(name = "diem_tong_ket_he10")
    private Double diemTongKetHe10;

    @Column(name = "diem_he4")
    private Double diemHe4;

    @Column(name = "diem_chu", length = 5)
    private String diemChu; // A, B+, B, C+, C, D+, D, F

    @Column(name = "ket_qua_qua_mon")
    private Boolean ketQuaQuaMon; // true nếu điểm chữ != F

    @Column(name = "cam_thi")
    @Builder.Default
    private Boolean camThi = false; // Bị cấm thi nếu vắng > 20% số tiết

    @Column(name = "khoa_diem")
    @Builder.Default
    private Boolean khoaDiem = false; // Đã chốt điểm hay chưa

    public Boolean getCamThi() {
        return Boolean.TRUE.equals(this.camThi);
    }

    public Boolean getKhoaDiem() {
        return Boolean.TRUE.equals(this.khoaDiem);
    }
}
