package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dieu_kien_hoc_phan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DieuKienHocPhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_phan_id", nullable = false)
    private HocPhan hocPhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_phan_dieu_kien_id", nullable = false)
    private HocPhan hocPhanDieuKien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoaiDieuKien loaiDieuKien;

    public enum LoaiDieuKien {
        TIEN_QUYET,  // Bắt buộc phải ĐẠT (Điểm chữ != F) trước khi đăng ký
        HOC_TRUOC,   // Từng đăng ký học môn này (có điểm, kể cả F)
        SONG_HANH    // Có thể đăng ký cùng kỳ hoặc đã học trước
    }
}
