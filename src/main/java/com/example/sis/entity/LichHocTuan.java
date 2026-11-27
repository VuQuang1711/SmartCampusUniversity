package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Chuẩn hóa 1NF/3NF cho 30 cột Tuần học (T1..T30) từ file TKB Excel HUIT.
 * Mỗi bản ghi đại diện cho 1 tuần thực tế có lịch học của một LichHoc.
 */
@Entity
@Table(
        name = "lich_hoc_tuan",
        uniqueConstraints = @UniqueConstraint(name = "uk_lich_tuan", columnNames = {"lich_hoc_id", "tuan_thu"}),
        indexes = {
                @Index(name = "idx_lht_tuan", columnList = "tuan_thu"),
                @Index(name = "idx_lht_lich", columnList = "lich_hoc_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichHocTuan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lich_hoc_id", nullable = false)
    private LichHoc lichHoc;

    @Column(name = "tuan_thu", nullable = false)
    private Integer tuanThu; // 1..30 tương ứng T1..T30 trong file Excel HUIT
}
