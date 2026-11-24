package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nganh_hoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NganhHoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String maNganh;

    @Column(nullable = false, length = 100)
    private String tenNganh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khoa_id", nullable = false)
    private Khoa khoa;

    @Column(nullable = false)
    private Integer tongTinChiYeuCau; // Ví dụ: 145 tín chỉ

    @Column(name = "don_gia_tin_chi_rieng")
    private Double donGiaTinChiRieng; // Đơn giá riêng theo ngành (null = dùng đơn giá học kỳ)
}
