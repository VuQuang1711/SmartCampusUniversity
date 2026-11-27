package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lop_hoc_phan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LopHocPhan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_lhp", nullable = false, unique = true, length = 50)
    private String maLHP; // Ví dụ: INT1306_2024_1_01

    @Column(name = "ten_lhp", nullable = false, length = 150)
    private String tenLHP;

    @Column(name = "lop_du_kien", length = 60)
    private String lopDuKien; // Ví dụ: 15DHKHDL01, 15DHKHDL03

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_danh_nghia_id")
    private LopDanhNghia lopDanhNghia; // FK chuẩn hóa từ cột "Lớp học" trong Excel HUIT

    @Column(name = "tinh_chat_hp", length = 50)
    private String tinhChatHp; // VD: E_learning(HUITx) hoặc TRUC_TIEP

    @Column(name = "tong_so_tiet")
    private Integer tongSoTiet; // Tổng số tiết thực dạy (cột "Tổng số tiết")

    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau; // Ngày bắt đầu đợt học (cột "Bắt đầu")

    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc; // Ngày kết thúc đợt học (cột "Kết thúc")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_phan_id", nullable = false)
    private HocPhan hocPhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hoc_ky_id", nullable = false)
    private HocKy hocKy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "giang_vien_id")
    private GiangVien giangVien;

    @Column(name = "si_so_toi_da", nullable = false)
    @Builder.Default
    private Integer siSoToiDa = 50;

    @Column(name = "si_so_hien_tai", nullable = false)
    @Builder.Default
    private Integer siSoHienTai = 0;

    @Column(length = 50)
    private String phongMacDinh;

    @Column(name = "thu_du_kien")
    private Integer thuDuKien;

    @Column(name = "tiet_bat_dau_du_kien")
    private Integer tietBatDauDuKien;

    @Column(name = "so_tiet_du_kien")
    private Integer soTietDuKien;

    @Column(name = "ly_do_tu_choi", length = 500)
    private String lyDoTuChoi;

    @Column(name = "ngay_de_xuat")
    private LocalDateTime ngayDeXuat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TrangThaiLHP trangThai = TrangThaiLHP.DANG_MO;

    @Version
    private Long version; // Optimistic locking để chống race condition khi nhiều SV đăng ký cùng lúc

    @OneToMany(mappedBy = "lopHocPhan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LichHoc> danhSachLichHoc = new ArrayList<>();

    public enum TrangThaiLHP {
        CHO_DUYET, DANG_MO, TU_CHOI, DA_KHOA, DA_KET_THUC, HUY_LOP
    }
}
