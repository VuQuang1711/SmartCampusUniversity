package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

@Entity
@Table(name = "lich_hoc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichHoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lop_hoc_phan_id", nullable = false)
    private LopHocPhan lopHocPhan;

    @Column(nullable = false)
    private Integer thu; // 2 -> Thứ Hai, 3 -> Thứ Ba, ..., 8 -> Chủ Nhật

    @Column(name = "tiet_bat_dau", nullable = false)
    private Integer tietBatDau; // 1 đến 15

    @Column(name = "so_tiet", nullable = false)
    private Integer soTiet; // Ví dụ: 3 tiết (tiết 1 đến 3)

    @Column(name = "phong_hoc", length = 50)
    private String phongHoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phong_hoc_id")
    private PhongHoc phongHocEntity; // FK chuẩn hóa tới bảng phong_hoc -> co_so

    @Column(name = "so_tiet_dot")
    private Integer soTietDot; // Cột "Số tiết" của đợt lịch trong Excel (VD: 30, 45)

    @Column(name = "ngay_bat_dau")
    private LocalDate ngayBatDau; // Cột "Bắt đầu" (VD: 11/01/2027)

    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc; // Cột "Kết thúc" (VD: 12/04/2027)

    @Column(name = "tu_tuan", nullable = false)
    @Builder.Default
    private Integer tuTuan = 1;

    @Column(name = "den_tuan", nullable = false)
    @Builder.Default
    private Integer denTuan = 15;

    @Column(name = "tuan_hoc", length = 120)
    private String tuanHoc; // Ví dụ: "1-2, 7-13, 15"

    @OneToMany(mappedBy = "lichHoc", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LichHocTuan> danhSachTuan = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LoaiLich loaiLich = LoaiLich.LY_THUYET;

    public int getTietKetThuc() {
        return tietBatDau + soTiet - 1;
    }

    /**
     * Phân tích tập hợp các tuần học thực tế từ chuỗi tuanHoc (VD: "1-2, 7-13, 15").
     * Nếu tuanHoc trống thì lấy theo khoảng [tuTuan..denTuan].
     */
    public TreeSet<Integer> getWeekSet() {
        TreeSet<Integer> weeks = new TreeSet<>();
        if (tuanHoc != null && !tuanHoc.isBlank()) {
            for (String part : tuanHoc.split("[,;\\s]+")) {
                String p = part.trim();
                if (p.isEmpty()) continue;
                try {
                    if (p.contains("-")) {
                        String[] bounds = p.split("-", 2);
                        int s = Integer.parseInt(bounds[0].trim());
                        int e = Integer.parseInt(bounds[1].trim());
                        for (int w = Math.min(s, e); w <= Math.max(s, e); w++) weeks.add(w);
                    } else {
                        weeks.add(Integer.parseInt(p));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        if (weeks.isEmpty()) {
            int s = tuTuan != null ? tuTuan : 1;
            int e = denTuan != null ? denTuan : 15;
            for (int w = Math.min(s, e); w <= Math.max(s, e); w++) weeks.add(w);
        }
        return weeks;
    }

    /**
     * Trả về tập các tuần trùng nhau giữa 2 lịch học (rỗng nếu không trùng tuần nào).
     */
    public TreeSet<Integer> getOverlapWeeks(LichHoc other) {
        TreeSet<Integer> intersection = new TreeSet<>(this.getWeekSet());
        if (other == null) {
            intersection.clear();
            return intersection;
        }
        intersection.retainAll(other.getWeekSet());
        return intersection;
    }

    public static String formatWeekSet(Collection<Integer> weeks) {
        if (weeks == null || weeks.isEmpty()) return "";
        TreeSet<Integer> sorted = new TreeSet<>(weeks);
        List<String> ranges = new ArrayList<>();
        Integer start = null, prev = null;
        for (int w : sorted) {
            if (start == null) {
                start = prev = w;
            } else if (w == prev + 1) {
                prev = w;
            } else {
                ranges.add(start.equals(prev) ? String.valueOf(start) : (start + "-" + prev));
                start = prev = w;
            }
        }
        if (start != null) {
            ranges.add(start.equals(prev) ? String.valueOf(start) : (start + "-" + prev));
        }
        return String.join(", ", ranges);
    }

    public enum LoaiLich {
        LY_THUYET, THUC_HANH, LICH_THI
    }
}
