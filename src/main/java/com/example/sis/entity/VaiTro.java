package com.example.sis.entity;

public enum VaiTro {
    ADMIN("Phòng Đào Tạo"),
    PHONG_DAO_TAO("Phòng Đào Tạo"),
    KE_TOAN("Phòng Kế Toán"),
    GIANG_VIEN("Giảng Viên"),
    CO_VAN_HOC_TAP("Cố Vấn Học Tập"),
    SINH_VIEN("Sinh Viên");

    private final String tenHienThi;

    VaiTro(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public String getDisplayName() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }
}
