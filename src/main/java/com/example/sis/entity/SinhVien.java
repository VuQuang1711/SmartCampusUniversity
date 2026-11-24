package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "sinh_vien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SinhVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String mssv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_danh_nghia_id")
    private LopDanhNghia lopDanhNghia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nganh_hoc_id", nullable = false)
    private NganhHoc nganhHoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chuyen_nganh_id")
    private ChuyenNganh chuyenNganh;

    @Column(name = "khoa_tuyen_sinh")
    private Integer khoaTuyenSinh; // Ví dụ: 2023

    @Column(name = "trang_thai_hoc_vu", length = 30)
    @Builder.Default
    private String trangThaiHocVu = "BINH_THUONG"; // BINH_THUONG, CANH_BAO_1, CANH_BAO_2, BUOC_THOI_HOC, DA_TOT_NGHIEP

    @Column(name = "cpa_tich_luy")
    @Builder.Default
    private Double cpaTichLuy = 0.0;

    @Column(name = "tong_tc_tich_luy")
    @Builder.Default
    private Integer tongTcTichLuy = 0;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    @Column(length = 10)
    private String gioiTinh;

    @Column(length = 100)
    private String noiSinh;

    @Column(length = 255)
    private String diaChi;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false, unique = true)
    private NguoiDung nguoiDung;

    public Double getCpaTichLuy() {
        return cpaTichLuy != null ? cpaTichLuy : 0.0;
    }

    public Integer getTongTcTichLuy() {
        return tongTcTichLuy != null ? tongTcTichLuy : 0;
    }
}
