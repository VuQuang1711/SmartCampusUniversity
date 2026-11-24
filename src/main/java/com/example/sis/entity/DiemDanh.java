package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "diem_danh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiemDanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lich_hoc_id", nullable = false)
    private LichHoc lichHoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private SinhVien sinhVien;

    @Column(name = "ngay_hoc", nullable = false)
    private LocalDate ngayHoc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TrangThaiDiemDanh trangThai = TrangThaiDiemDanh.CO_MAT;

    @Column(length = 255)
    private String ghiChu;

    public enum TrangThaiDiemDanh {
        CO_MAT, VANG, VANG_CO_PHEP, DI_TRE
    }
}
