package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "phong_hoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhongHoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_phong", nullable = false, unique = true, length = 60)
    private String maPhong; // VD: B203, A105, Zoom001

    @Column(name = "ten_hien_thi", length = 120)
    private String tenHienThi; // VD: A105 - Phòng máy tính

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_phong", nullable = false, length = 20)
    @Builder.Default
    private LoaiPhong loaiPhong = LoaiPhong.LY_THUYET;

    @Column(name = "suc_chua")
    @Builder.Default
    private Integer sucChua = 60;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_so_id")
    private CoSo coSo;

    public enum LoaiPhong {
        LY_THUYET, PHONG_MAY, THUC_HANH, ONLINE
    }
}
