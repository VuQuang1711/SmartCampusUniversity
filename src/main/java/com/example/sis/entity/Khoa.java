package com.example.sis.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "khoa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Khoa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String maKhoa;

    @Column(nullable = false, length = 100)
    private String tenKhoa;

    @Column(length = 100)
    private String vanPhong;

    @Column(length = 20)
    private String soDienThoai;
}
