package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chuyen_nganh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChuyenNganh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String maChuyenNganh;

    @Column(nullable = false, length = 100)
    private String tenChuyenNganh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nganh_hoc_id", nullable = false)
    private NganhHoc nganhHoc;
}
