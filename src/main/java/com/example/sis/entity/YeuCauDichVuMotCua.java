package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "yeu_cau_dich_vu_mot_cua")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeuCauDichVuMotCua {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private SinhVien sinhVien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LoaiDichVu loaiDichVu;

    @Column(length = 500)
    private String lyDo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TrangThaiYeuCau trangThai = TrangThaiYeuCau.CHO_DUYET;

    @Column(name = "ma_xac_thuc_qr", length = 100)
    private String maXacThucQr;

    @Column(name = "file_ket_qua_url", length = 255)
    private String fileKetQuaUrl;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_xu_ly")
    private LocalDateTime ngayXuLy;

    @Column(name = "phan_hoi_admin", length = 500)
    private String phanHoiAdmin;

    @PrePersist
    public void prePersist() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
    }

    public enum LoaiDichVu {
        GIAY_XAC_NHAN_SV,
        BANG_DIEM_TAM_THOI,
        DON_XIN_BAO_LUU,
        DON_PHUC_KHAO,
        XAC_NHAN_VAY_VON
    }

    public enum TrangThaiYeuCau {
        CHO_DUYET, DA_DUYET, TU_CHOI
    }
}
