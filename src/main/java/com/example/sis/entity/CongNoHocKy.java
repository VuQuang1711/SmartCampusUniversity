package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cong_no_hoc_ky", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"sinh_vien_id", "hoc_ky_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CongNoHocKy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sinh_vien_id", nullable = false)
    private SinhVien sinhVien;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_ky_id", nullable = false)
    private HocKy hocKy;

    @Column(name = "tong_so_tc", nullable = false)
    @Builder.Default
    private Integer tongSoTC = 0;

    @Column(name = "tong_hoc_phi", nullable = false)
    @Builder.Default
    private Double tongHocPhi = 0.0;

    @Column(name = "mien_giam")
    @Builder.Default
    private Double mienGiam = 0.0;

    @Column(name = "da_dong", nullable = false)
    @Builder.Default
    private Double daDong = 0.0;

    @Column(name = "con_no", nullable = false)
    @Builder.Default
    private Double conNo = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TrangThaiCongNo trangThai = TrangThaiCongNo.CHUA_DONG;

    @OneToMany(mappedBy = "congNoHocKy", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PhieuThuHocPhi> danhSachPhieuThu = new ArrayList<>();

    public void updateBalance() {
        double phaiDong = (tongHocPhi != null ? tongHocPhi : 0.0) - (mienGiam != null ? mienGiam : 0.0);
        if (phaiDong < 0) phaiDong = 0.0;
        double paid = daDong != null ? daDong : 0.0;
        this.conNo = Math.max(0.0, phaiDong - paid);
        if (this.conNo <= 0 && paid > 0) {
            this.trangThai = TrangThaiCongNo.DA_HOAN_THANH;
        } else if (paid > 0) {
            this.trangThai = TrangThaiCongNo.DONG_MOT_PHAN;
        } else {
            this.trangThai = TrangThaiCongNo.CHUA_DONG;
        }
    }

    public enum TrangThaiCongNo {
        CHUA_DONG, DONG_MOT_PHAN, DA_HOAN_THANH
    }
}
