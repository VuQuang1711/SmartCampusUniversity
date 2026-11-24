package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lop_danh_nghia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LopDanhNghia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String maLop; // Ví dụ: 15DHKHDL03

    @Column(nullable = false, length = 100)
    private String tenLop;

    @Column(length = 20)
    private String nienKhoa; // Ví dụ: 2023-2027 (K15)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nganh_hoc_id", nullable = false)
    private NganhHoc nganhHoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "co_van_hoc_tap_id")
    private GiangVien coVanHocTap;
}
