package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hoc_phan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HocPhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_hp", nullable = false, unique = true, length = 20)
    private String maHP; // Ví dụ: INT1306, MAT1093

    @Column(name = "ten_hp", nullable = false, length = 150)
    private String tenHP;

    @Column(name = "so_tc_ly_thuyet", nullable = false)
    @Builder.Default
    private Integer soTCLyThuyet = 3;

    @Column(name = "so_tc_thuc_hanh", nullable = false)
    @Builder.Default
    private Integer soTCThucHanh = 0;

    @Column(name = "tong_tin_chi", nullable = false)
    private Integer tongTinChi;

    @Column(name = "tong_so_tiet")
    private Integer tongSoTiet; // VD: 30, 45, 60 (từ cột "Tổng số tiết" trong TKB HUIT)

    @Column(name = "tinh_chat_hp", length = 50)
    @Builder.Default
    private String tinhChatHp = "TRUC_TIEP"; // TRUC_TIEP, E_learning(HUITx)

    @Column(name = "he_so_hoc_phi")
    @Builder.Default
    private Double heSoHocPhi = 1.0; // Hệ số nhân học phí (ví dụ môn chuyên ngành lab hệ số 1.2)

    // Cấu hình tỷ lệ điểm (Tổng = 1.0)
    @Column(name = "ty_le_chuyen_can")
    @Builder.Default
    private Double tyLeChuyenCan = 0.10; // 10%

    @Column(name = "ty_le_qua_trinh")
    @Builder.Default
    private Double tyLeQuaTrinh = 0.20; // 20%

    @Column(name = "ty_le_thuc_hanh")
    @Builder.Default
    private Double tyLeThucHanh = 0.20; // 20%

    @Column(name = "ty_le_cuoi_ky")
    @Builder.Default
    private Double tyLeCuoiKy = 0.50; // 50%

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khoa_id")
    private Khoa khoa;

    @PrePersist
    @PreUpdate
    public void calculateTotalCredits() {
        int lt = soTCLyThuyet != null ? soTCLyThuyet : 0;
        int th = soTCThucHanh != null ? soTCThucHanh : 0;
        this.tongTinChi = lt + th;
    }
}
