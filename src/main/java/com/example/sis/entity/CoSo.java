package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "co_so")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoSo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_co_so", nullable = false, unique = true, length = 30)
    private String maCoSo; // VD: CS_140LTT, CS_93TKTQ, CS_31CLV, CS_ONLINE

    @Column(name = "ten_co_so", nullable = false, length = 150)
    private String tenCoSo; // VD: Cơ sở 140 Lê Trọng Tấn

    @Column(name = "dia_chi", length = 255)
    private String diaChi;
}
