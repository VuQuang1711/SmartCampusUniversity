package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lich_hoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichHoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private LopHocPhan lopHocPhan;

    @Column(nullable = false)
    private Integer thu; // 2 -> Thứ Hai, 3 -> Thứ Ba, ..., 8 -> Chủ Nhật

    @Column(name = "tiet_bat_dau", nullable = false)
    private Integer tietBatDau; // 1 đến 15

    @Column(name = "so_tiet", nullable = false)
    private Integer soTiet; // Ví dụ: 3 tiết (tiết 1 đến 3)

    @Column(name = "phong_hoc", length = 50)
    private String phongHoc;

    @Column(name = "tu_tuan", nullable = false)
    @Builder.Default
    private Integer tuTuan = 1;

    @Column(name = "den_tuan", nullable = false)
    @Builder.Default
    private Integer denTuan = 15;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LoaiLich loaiLich = LoaiLich.LY_THUYET;

    public int getTietKetThuc() {
        return tietBatDau + soTiet - 1;
    }

    public enum LoaiLich {
        LY_THUYET, THUC_HANH, LICH_THI
    }
}
