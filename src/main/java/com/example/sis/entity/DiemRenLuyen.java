package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diem_ren_luyen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiemRenLuyen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private SinhVien sinhVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_ky_id", nullable = false)
    private HocKy hocKy;

    @Column(name = "diem_tu_cham")
    private Integer diemTuCham;

    @Column(name = "diem_lop_cham")
    private Integer diemLopCham;

    @Column(name = "diem_khoa_duyet")
    private Integer diemKhoaDuyet;

    @Column(name = "xep_loai", length = 30)
    private String xepLoai; // XUAT_SAC, TOT, KHA, TRUNG_BINH, YEU, KEM

    @Column(name = "minh_chung_url", length = 255)
    private String minhChungUrl;

    @Column(name = "trang_thai", length = 30)
    @Builder.Default
    private String trangThai = "CHO_CVHT_DUYET"; // CHO_CVHT_DUYET, CVHT_DA_DUYET, DA_CHOT

    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;

    public static String classify(int score) {
        if (score >= 90) return "XUAT_SAC";
        if (score >= 80) return "TOT";
        if (score >= 65) return "KHA";
        if (score >= 50) return "TRUNG_BINH";
        if (score >= 35) return "YEU";
        return "KEM";
    }
}
