package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chung_chi_chuan_dau_ra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChungChiChuanDauRa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private SinhVien sinhVien;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LoaiChungChi loaiChungChi;

    @Column(length = 100)
    private String soHieu;

    @Column(name = "diem_so", length = 50)
    private String diemSo;

    @Column(name = "ngay_cap")
    private LocalDate ngayCap;

    @Column(name = "don_vi_cap", length = 150)
    private String donViCap;

    @Column(name = "file_pdf_url", length = 255)
    private String filePdfUrl;

    @Column(name = "ten_file_goc", length = 255)
    private String tenFileGoc;

    @Column(name = "ly_do_tu_choi", length = 500)
    private String lyDoTuChoi;

    @Column(name = "ngay_nop")
    private LocalDateTime ngayNop;

    @Column(name = "ngay_duyet")
    private LocalDateTime ngayDuyet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TrangThaiChungChi trangThai = TrangThaiChungChi.CHO_DUYET;

    @PrePersist
    public void prePersist() {
        if (ngayNop == null) ngayNop = LocalDateTime.now();
    }

    public enum LoaiChungChi {
        TIENG_ANH_B1("Tiếng Anh VSTEP B1"),
        TIENG_ANH_IELTS("Tiếng Anh IELTS"),
        TIENG_ANH_TOEIC("Tiếng Anh TOEIC"),
        TIN_HOC_MOS("Tin học quốc tế MOS / IC3"),
        GIAO_DUC_QUOC_PHONG("Giáo dục Quốc phòng & An ninh"),
        GIAO_DUC_THE_CHAT("Giáo dục Thể chất");

        private final String label;
        LoaiChungChi(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public enum TrangThaiChungChi {
        CHO_DUYET, DA_DAT, KHONG_DAT
    }
}
