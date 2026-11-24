package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "hoc_ky")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocKy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String maHocKy; // Ví dụ: "2024_1", "2024_2", "2024_3"

    @Column(nullable = false, length = 100)
    private String tenHocKy; // Ví dụ: "Học kỳ 1 - Năm học 2024-2025"

    @Column(nullable = false, length = 20)
    private String namHoc; // Ví dụ: "2024-2025"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoaiHocKy loaiHocKy; // CHINH_1, CHINH_2, PHU_HE

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TrangThaiDangKy trangThaiDangKy = TrangThaiDangKy.CHUA_MO;

    @Column(name = "ngay_bat_dau_dkhp")
    private LocalDate ngayBatDauDkhp;

    @Column(name = "ngay_ket_thuc_dkhp")
    private LocalDate ngayKetThucDkhp;

    @Column(name = "don_gia_tin_chi")
    @Builder.Default
    private Double donGiaTinChi = 550000.0; // 550.000 VNĐ / tín chỉ

    @Column(name = "cho_phep_nhap_diem")
    @Builder.Default
    private Boolean choPhepNhapDiem = true;

    public Boolean getChoPhepNhapDiem() {
        return !Boolean.FALSE.equals(this.choPhepNhapDiem);
    }

    @Column(name = "he_so_hoc_lai")
    @Builder.Default
    private Double heSoHocLai = 1.5;

    @Column(name = "he_so_cai_thien")
    @Builder.Default
    private Double heSoCaiThien = 1.2;

    public enum LoaiHocKy {
        CHINH_1, CHINH_2, PHU_HE
    }

    public enum TrangThaiDangKy {
        CHUA_MO, DANG_MO, DA_KHOA
    }
}
