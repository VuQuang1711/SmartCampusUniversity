package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "thong_bao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_gui_id")
    private NguoiDung nguoiGui;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_nhan_id", nullable = false)
    private NguoiDung nguoiNhan;

    @Column(name = "tieu_de", nullable = false, length = 200)
    private String tieuDe;

    @Column(name = "noi_dung", nullable = false, columnDefinition = "TEXT")
    private String noiDung;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thong_bao", nullable = false, length = 30)
    @Builder.Default
    private LoaiThongBao loaiThongBao = LoaiThongBao.HE_THONG;

    @Column(name = "da_doc", nullable = false)
    @Builder.Default
    private Boolean daDoc = false;

    public Boolean getDaDoc() {
        return Boolean.TRUE.equals(this.daDoc);
    }

    @Column(name = "lien_ket", length = 255)
    private String lienKet;

    @Column(name = "ngay_gui")
    private LocalDateTime ngayGui;

    @PrePersist
    public void prePersist() {
        if (ngayGui == null) ngayGui = LocalDateTime.now();
    }

    public enum LoaiThongBao {
        HE_THONG("Hệ thống"),
        DOI_LICH("Đổi lịch học"),
        CHUNG_CHI("Chứng chỉ"),
        HOC_PHI("Học phí"),
        MO_LOP("Mở lớp học phần"),
        LOP_HOC_PHAN("Thông báo lớp"),
        PHAN_HOI("Phản hồi / Đơn từ"),
        TOAN_TRUONG("Thông báo toàn trường");

        private final String label;
        LoaiThongBao(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
}
