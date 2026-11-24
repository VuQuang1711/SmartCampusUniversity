package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "giang_vien")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GiangVien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_gv", nullable = false, unique = true, length = 20)
    private String maGV;

    @Column(name = "hoc_ham_hoc_vi", length = 50)
    private String hocHamHocVi; // TS., ThS., PGS.TS

    @Column(name = "chuyen_mon", length = 100)
    private String chuyenMon;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id", nullable = false, unique = true)
    private NguoiDung nguoiDung;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "khoa_id", nullable = false)
    private Khoa khoa;

    /**
     * Trả về học hàm/học vị + họ tên giảng viên, tự động chống lặp học vị (VD: tránh "PGS.TS PGS.TS Nguyễn Văn An")
     */
    public String getTenDayDu() {
        if (nguoiDung == null || nguoiDung.getHoTen() == null || nguoiDung.getHoTen().isBlank()) {
            return maGV != null ? maGV : "Chưa phân công";
        }
        String ten = nguoiDung.getHoTen().trim();
        if (hocHamHocVi == null || hocHamHocVi.isBlank()) {
            return ten;
        }
        String hv = hocHamHocVi.trim();
        String normTen = ten.toLowerCase().replace(".", "").trim();
        String normHv = hv.toLowerCase().replace(".", "").trim();
        if (normTen.startsWith(normHv + " ")
                || normTen.startsWith("pgsts ")
                || normTen.startsWith("pgs ")
                || normTen.startsWith("gs ")
                || normTen.startsWith("ts ")
                || normTen.startsWith("ths ")) {
            return ten;
        }
        return hv + " " + ten;
    }
}
