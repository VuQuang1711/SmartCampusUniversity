package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "khung_chuong_trinh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhungChuongTrinh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nganh_hoc_id", nullable = false)
    private NganhHoc nganhHoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_phan_id", nullable = false)
    private HocPhan hocPhan;

    @Column(name = "hoc_ky_du_kien", nullable = false)
    private Integer hocKyDuKien; // Học kỳ 1 đến 8

    @Column(nullable = false)
    @Builder.Default
    private Boolean batBuoc = true; // true: Bắt buộc, false: Tự chọn

    public Boolean getBatBuoc() {
        return !Boolean.FALSE.equals(this.batBuoc);
    }
}
