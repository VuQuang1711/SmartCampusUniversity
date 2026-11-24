package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "yeu_cau_doi_lich")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeuCauDoiLich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private LopHocPhan lopHocPhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lich_hoc_id")
    private LichHoc lichHoc; // Null nếu là đề xuất dạy bù thêm buổi mới

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "giang_vien_id", nullable = false)
    private GiangVien giangVien;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_yeu_cau", nullable = false, length = 30)
    @Builder.Default
    private LoaiYeuCau loaiYeuCau = LoaiYeuCau.DOI_CA;

    @Column(name = "thu_moi", nullable = false)
    private Integer thuMoi;

    @Column(name = "tiet_bat_dau_moi", nullable = false)
    private Integer tietBatDauMoi;

    @Column(name = "so_tiet_moi", nullable = false)
    private Integer soTietMoi;

    @Column(name = "phong_hoc_moi", nullable = false, length = 50)
    private String phongHocMoi;

    @Column(name = "tu_tuan_moi", nullable = false)
    @Builder.Default
    private Integer tuTuanMoi = 1;

    @Column(name = "den_tuan_moi", nullable = false)
    @Builder.Default
    private Integer denTuanMoi = 15;

    @Column(length = 500)
    private String lyDo;

    @Column(name = "ket_qua_kiem_tra", length = 500)
    private String ketQuaKiemTra;

    @Column(name = "so_sv_trung", nullable = false)
    @Builder.Default
    private Integer soSvTrung = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private TrangThaiDuyet trangThai = TrangThaiDuyet.CHO_DUYET;

    @Column(name = "phan_hoi_admin", length = 500)
    private String phanHoiAdmin;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    @Column(name = "ngay_xu_ly")
    private LocalDateTime ngayXuLy;

    @PrePersist
    public void prePersist() {
        if (ngayTao == null) ngayTao = LocalDateTime.now();
    }

    public int getTietKetThucMoi() {
        return tietBatDauMoi + soTietMoi - 1;
    }

    public enum LoaiYeuCau {
        DOI_CA("Đổi ca học"),
        DOI_PHONG("Đổi phòng học"),
        DAY_BU("Dạy bù");

        private final String label;
        LoaiYeuCau(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum TrangThaiDuyet {
        CHO_DUYET, DA_DUYET, TU_CHOI
    }
}
