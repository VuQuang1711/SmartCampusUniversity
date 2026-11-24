-- =========================================================================
-- HỆ THỐNG QUẢN LÝ ĐÀO TẠO & SINH VIÊN ĐẠI HỌC THEO HỌC CHẾ TÍN CHỈ (SIS / ERP)
-- Script Khởi Tạo Cơ Sở Dữ Liệu & Dữ Liệu Mẫu Chuẩn Tín Chỉ
-- =========================================================================

CREATE DATABASE IF NOT EXISTS university_sis CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE university_sis;

-- 1. BẢNG NGƯỜI DÙNG (nguoi_dung)
CREATE TABLE IF NOT EXISTS nguoi_dung (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_dinh_danh VARCHAR(50) NOT NULL UNIQUE,
    ho_ten VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    mat_khau VARCHAR(100) NOT NULL,
    so_dien_thoai VARCHAR(20),
    vai_tro VARCHAR(30) NOT NULL,
    avatar_url VARCHAR(255),
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 2. BẢNG KHOA (khoa)
CREATE TABLE IF NOT EXISTS khoa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_khoa VARCHAR(20) NOT NULL UNIQUE,
    ten_khoa VARCHAR(100) NOT NULL,
    van_phong VARCHAR(100),
    so_dien_thoai VARCHAR(20)
);

-- 3. BẢNG NGÀNH HỌC (nganh_hoc)
CREATE TABLE IF NOT EXISTS nganh_hoc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_nganh VARCHAR(20) NOT NULL UNIQUE,
    ten_nganh VARCHAR(100) NOT NULL,
    khoa_id BIGINT NOT NULL,
    tong_tin_chi_yeu_cau INT NOT NULL DEFAULT 145,
    CONSTRAINT fk_nganh_khoa FOREIGN KEY (khoa_id) REFERENCES khoa(id)
);

-- 4. BẢNG CHUYÊN NGÀNH (chuyen_nganh)
CREATE TABLE IF NOT EXISTS chuyen_nganh (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_chuyen_nganh VARCHAR(20) NOT NULL UNIQUE,
    ten_chuyen_nganh VARCHAR(100) NOT NULL,
    nganh_hoc_id BIGINT NOT NULL,
    CONSTRAINT fk_chuyen_nganh_nganh FOREIGN KEY (nganh_hoc_id) REFERENCES nganh_hoc(id)
);

-- 5. BẢNG GIẢNG VIÊN (giang_vien)
CREATE TABLE IF NOT EXISTS giang_vien (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_gv VARCHAR(20) NOT NULL UNIQUE,
    hoc_ham_hoc_vi VARCHAR(50),
    chuyen_mon VARCHAR(100),
    nguoi_dung_id BIGINT NOT NULL UNIQUE,
    khoa_id BIGINT NOT NULL,
    CONSTRAINT fk_gv_user FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id),
    CONSTRAINT fk_gv_khoa FOREIGN KEY (khoa_id) REFERENCES khoa(id)
);

-- 6. BẢNG LỚP DANH NGHĨA (lop_danh_nghia)
CREATE TABLE IF NOT EXISTS lop_danh_nghia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_lop VARCHAR(30) NOT NULL UNIQUE,
    ten_lop VARCHAR(100) NOT NULL,
    nien_khoa VARCHAR(20),
    nganh_hoc_id BIGINT NOT NULL,
    co_van_hoc_tap_id BIGINT,
    CONSTRAINT fk_lop_nganh FOREIGN KEY (nganh_hoc_id) REFERENCES nganh_hoc(id),
    CONSTRAINT fk_lop_covan FOREIGN KEY (co_van_hoc_tap_id) REFERENCES giang_vien(id)
);

-- 7. BẢNG SINH VIÊN (sinh_vien)
CREATE TABLE IF NOT EXISTS sinh_vien (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mssv VARCHAR(20) NOT NULL UNIQUE,
    lop_danh_nghia_id BIGINT,
    nganh_hoc_id BIGINT NOT NULL,
    chuyen_nganh_id BIGINT,
    khoa_tuyen_sinh INT,
    trang_thai_hoc_vu VARCHAR(30) DEFAULT 'BINH_THUONG',
    cpa_tich_luy DOUBLE DEFAULT 0.0,
    tong_tc_tich_luy INT DEFAULT 0,
    ngay_sinh DATE,
    gioi_tinh VARCHAR(10),
    noi_sinh VARCHAR(100),
    dia_chi VARCHAR(255),
    nguoi_dung_id BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_sv_lop FOREIGN KEY (lop_danh_nghia_id) REFERENCES lop_danh_nghia(id),
    CONSTRAINT fk_sv_nganh FOREIGN KEY (nganh_hoc_id) REFERENCES nganh_hoc(id),
    CONSTRAINT fk_sv_chuyen_nganh FOREIGN KEY (chuyen_nganh_id) REFERENCES chuyen_nganh(id),
    CONSTRAINT fk_sv_user FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id)
);

-- 8. BẢNG HỌC KỲ (hoc_ky)
CREATE TABLE IF NOT EXISTS hoc_ky (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_hoc_ky VARCHAR(30) NOT NULL UNIQUE,
    ten_hoc_ky VARCHAR(100) NOT NULL,
    nam_hoc VARCHAR(20) NOT NULL,
    loai_hoc_ky VARCHAR(20) NOT NULL,
    trang_thai_dang_ky VARCHAR(20) DEFAULT 'CHUA_MO',
    ngay_bat_dau_dkhp DATE,
    ngay_ket_thuc_dkhp DATE,
    don_gia_tin_chi DOUBLE DEFAULT 550000.0
);

-- 9. BẢNG HỌC PHẦN (hoc_phan)
CREATE TABLE IF NOT EXISTS hoc_phan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_hp VARCHAR(20) NOT NULL UNIQUE,
    ten_hp VARCHAR(150) NOT NULL,
    so_tc_ly_thuyet INT NOT NULL DEFAULT 3,
    so_tc_thuc_hanh INT NOT NULL DEFAULT 0,
    tong_tin_chi INT NOT NULL,
    he_so_hoc_phi DOUBLE DEFAULT 1.0,
    ty_le_chuyen_can DOUBLE DEFAULT 0.10,
    ty_le_qua_trinh DOUBLE DEFAULT 0.20,
    ty_le_thuc_hanh DOUBLE DEFAULT 0.20,
    ty_le_cuoi_ky DOUBLE DEFAULT 0.50,
    khoa_id BIGINT,
    CONSTRAINT fk_hp_khoa FOREIGN KEY (khoa_id) REFERENCES khoa(id)
);

-- 10. BẢNG ĐIỀU KIỆN HỌC PHẦN (dieu_kien_hoc_phan)
CREATE TABLE IF NOT EXISTS dieu_kien_hoc_phan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hoc_phan_id BIGINT NOT NULL,
    hoc_phan_dieu_kien_id BIGINT NOT NULL,
    loai_dieu_kien VARCHAR(20) NOT NULL,
    CONSTRAINT fk_dk_hp FOREIGN KEY (hoc_phan_id) REFERENCES hoc_phan(id),
    CONSTRAINT fk_dk_hp_target FOREIGN KEY (hoc_phan_dieu_kien_id) REFERENCES hoc_phan(id)
);

-- 11. BẢNG KHUNG CHƯƠNG TRÌNH (khung_chuong_trinh)
CREATE TABLE IF NOT EXISTS khung_chuong_trinh (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nganh_hoc_id BIGINT NOT NULL,
    hoc_phan_id BIGINT NOT NULL,
    hoc_ky_du_kien INT NOT NULL,
    bat_buoc BOOLEAN DEFAULT TRUE,
    CONSTRAINT fk_kct_nganh FOREIGN KEY (nganh_hoc_id) REFERENCES nganh_hoc(id),
    CONSTRAINT fk_kct_hp FOREIGN KEY (hoc_phan_id) REFERENCES hoc_phan(id)
);

-- 12. BẢNG LỚP HỌC PHẦN (lop_hoc_phan)
CREATE TABLE IF NOT EXISTS lop_hoc_phan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_lhp VARCHAR(50) NOT NULL UNIQUE,
    ten_lhp VARCHAR(150) NOT NULL,
    hoc_phan_id BIGINT NOT NULL,
    hoc_ky_id BIGINT NOT NULL,
    giang_vien_id BIGINT,
    si_so_toi_da INT NOT NULL DEFAULT 50,
    si_so_hien_tai INT NOT NULL DEFAULT 0,
    phong_mac_dinh VARCHAR(50),
    trang_thai VARCHAR(20) DEFAULT 'DANG_MO',
    version BIGINT DEFAULT 0,
    CONSTRAINT fk_lhp_hp FOREIGN KEY (hoc_phan_id) REFERENCES hoc_phan(id),
    CONSTRAINT fk_lhp_hk FOREIGN KEY (hoc_ky_id) REFERENCES hoc_ky(id),
    CONSTRAINT fk_lhp_gv FOREIGN KEY (giang_vien_id) REFERENCES giang_vien(id)
);

-- 13. BẢNG LỊCH HỌC (lich_hoc)
CREATE TABLE IF NOT EXISTS lich_hoc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lop_hoc_phan_id BIGINT NOT NULL,
    thu INT NOT NULL,
    tiet_bat_dau INT NOT NULL,
    so_tiet INT NOT NULL,
    phong_hoc VARCHAR(50),
    tu_tuan INT NOT NULL DEFAULT 1,
    den_tuan INT NOT NULL DEFAULT 15,
    loai_lich VARCHAR(20) DEFAULT 'LY_THUYET',
    CONSTRAINT fk_lich_lhp FOREIGN KEY (lop_hoc_phan_id) REFERENCES lop_hoc_phan(id)
);

-- 14. BẢNG ĐĂNG KÝ HỌC PHẦN (dang_ky_hoc_phan)
CREATE TABLE IF NOT EXISTS dang_ky_hoc_phan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sinh_vien_id BIGINT NOT NULL,
    lop_hoc_phan_id BIGINT NOT NULL,
    loai_dang_ky VARCHAR(20) NOT NULL DEFAULT 'HOC_MOI',
    ngay_dang_ky DATETIME DEFAULT CURRENT_TIMESTAMP,
    trang_thai VARCHAR(20) DEFAULT 'THANH_CONG',
    UNIQUE KEY uk_sv_lhp (sinh_vien_id, lop_hoc_phan_id),
    CONSTRAINT fk_dkhp_sv FOREIGN KEY (sinh_vien_id) REFERENCES sinh_vien(id),
    CONSTRAINT fk_dkhp_lhp FOREIGN KEY (lop_hoc_phan_id) REFERENCES lop_hoc_phan(id)
);

-- 15. BẢNG ĐIỂM HỌC PHẦN (diem_hoc_phan)
CREATE TABLE IF NOT EXISTS diem_hoc_phan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dang_ky_hoc_phan_id BIGINT NOT NULL UNIQUE,
    diem_chuyen_can DOUBLE,
    diem_qua_trinh DOUBLE,
    diem_thuc_hanh DOUBLE,
    diem_cuoi_ky DOUBLE,
    diem_tong_ket_he10 DOUBLE,
    diem_he4 DOUBLE,
    diem_chu VARCHAR(5),
    ket_qua_qua_mon BOOLEAN,
    cam_thi BOOLEAN DEFAULT FALSE,
    khoa_diem BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_diem_dkhp FOREIGN KEY (dang_ky_hoc_phan_id) REFERENCES dang_ky_hoc_phan(id)
);

-- 16. BẢNG ĐIỂM DANH (diem_danh)
CREATE TABLE IF NOT EXISTS diem_danh (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lich_hoc_id BIGINT NOT NULL,
    sinh_vien_id BIGINT NOT NULL,
    ngay_hoc DATE NOT NULL,
    trang_thai VARCHAR(20) DEFAULT 'CO_MAT',
    ghi_chu VARCHAR(255),
    CONSTRAINT fk_dd_lich FOREIGN KEY (lich_hoc_id) REFERENCES lich_hoc(id),
    CONSTRAINT fk_dd_sv FOREIGN KEY (sinh_vien_id) REFERENCES sinh_vien(id)
);

-- 17. BẢNG ĐIỂM RÈN LUYỆN (diem_ren_luyen)
CREATE TABLE IF NOT EXISTS diem_ren_luyen (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sinh_vien_id BIGINT NOT NULL,
    hoc_ky_id BIGINT NOT NULL,
    diem_tu_cham INT,
    diem_lop_cham INT,
    diem_khoa_duyet INT,
    xep_loai VARCHAR(30),
    minh_chung_url VARCHAR(255),
    CONSTRAINT fk_drl_sv FOREIGN KEY (sinh_vien_id) REFERENCES sinh_vien(id),
    CONSTRAINT fk_drl_hk FOREIGN KEY (hoc_ky_id) REFERENCES hoc_ky(id)
);

-- 18. BẢNG CÔNG NỢ HỌC KỲ (cong_no_hoc_ky)
CREATE TABLE IF NOT EXISTS cong_no_hoc_ky (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sinh_vien_id BIGINT NOT NULL,
    hoc_ky_id BIGINT NOT NULL,
    tong_so_tc INT NOT NULL DEFAULT 0,
    tong_hoc_phi DOUBLE NOT NULL DEFAULT 0.0,
    mien_giam DOUBLE DEFAULT 0.0,
    da_dong DOUBLE NOT NULL DEFAULT 0.0,
    con_no DOUBLE NOT NULL DEFAULT 0.0,
    trang_thai VARCHAR(30) DEFAULT 'CHUA_DONG',
    UNIQUE KEY uk_cn_sv_hk (sinh_vien_id, hoc_ky_id),
    CONSTRAINT fk_cn_sv FOREIGN KEY (sinh_vien_id) REFERENCES sinh_vien(id),
    CONSTRAINT fk_cn_hk FOREIGN KEY (hoc_ky_id) REFERENCES hoc_ky(id)
);

-- 19. BẢNG PHIẾU THU HỌC PHÍ (phieu_thu_hoc_phi)
CREATE TABLE IF NOT EXISTS phieu_thu_hoc_phi (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cong_no_hoc_ky_id BIGINT NOT NULL,
    ma_phieu_thu VARCHAR(50) NOT NULL UNIQUE,
    so_tien DOUBLE NOT NULL,
    phuong_thuc VARCHAR(30) DEFAULT 'VIETQR',
    ma_giao_dich VARCHAR(100),
    ngay_thu DATETIME DEFAULT CURRENT_TIMESTAMP,
    nguoi_thu VARCHAR(100),
    ghi_chu VARCHAR(255),
    CONSTRAINT fk_pt_cn FOREIGN KEY (cong_no_hoc_ky_id) REFERENCES cong_no_hoc_ky(id)
);

-- 20. BẢNG DỊCH VỤ MỘT CỬA (yeu_cau_dich_vu_mot_cua)
CREATE TABLE IF NOT EXISTS yeu_cau_dich_vu_mot_cua (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sinh_vien_id BIGINT NOT NULL,
    loai_dich_vu VARCHAR(50) NOT NULL,
    ly_do VARCHAR(500),
    trang_thai VARCHAR(30) DEFAULT 'CHO_DUYET',
    ma_xac_thuc_qr VARCHAR(100),
    file_ket_qua_url VARCHAR(255),
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_xu_ly DATETIME,
    phan_hoi_admin VARCHAR(500),
    CONSTRAINT fk_dvmc_sv FOREIGN KEY (sinh_vien_id) REFERENCES sinh_vien(id)
);

-- 21. BẢNG CHUẨN ĐẦU RA (chung_chi_chuan_dau_ra)
CREATE TABLE IF NOT EXISTS chung_chi_chuan_dau_ra (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sinh_vien_id BIGINT NOT NULL,
    loai_chung_chi VARCHAR(50) NOT NULL,
    so_hieu VARCHAR(100),
    ngay_cap DATE,
    don_vi_cap VARCHAR(150),
    trang_thai VARCHAR(20) DEFAULT 'CHO_DUYET',
    CONSTRAINT fk_cc_sv FOREIGN KEY (sinh_vien_id) REFERENCES sinh_vien(id)
);

-- =========================================================================
-- DỮ LIỆU SEED MẪU TOÀN DIỆN (ACCOUNTS, CURRICULUM, ROSTERS, GRADES, TUITION)
-- =========================================================================

-- Người dùng (Tất cả mật khẩu mặc định: 123456)
INSERT INTO nguoi_dung (id, ma_dinh_danh, ho_ten, email, mat_khau, so_dien_thoai, vai_tro) VALUES
(1, 'ADMIN01', 'Phòng Đào Tạo', 'admin@sis.edu.vn', '123456', '0901111222', 'ADMIN'),
(2, 'DAOTAO01', 'Phòng Đào Tạo HUIT', 'daotao@sis.edu.vn', '123456', '0902222333', 'PHONG_DAO_TAO'),
(3, 'KETOAN01', 'Phòng Tài Chính Kế Toán', 'ketoan@sis.edu.vn', '123456', '0903333444', 'KE_TOAN'),
(4, 'GV001', 'PGS.TS Nguyễn Văn An', 'gv.an@sis.edu.vn', '123456', '0904444555', 'GIANG_VIEN'),
(5, 'GV002', 'TS. Trần Thị Bình', 'gv.binh@sis.edu.vn', '123456', '0905555666', 'CO_VAN_HOC_TAP'),
(6, 'GV003', 'ThS. Lê Văn Cường', 'gv.cuong@sis.edu.vn', '123456', '0906666777', 'GIANG_VIEN'),
(7, '2180601234', 'Nguyễn Minh Khoa', '2180601234@sinhvien.sis.edu.vn', '123456', '0907777888', 'SINH_VIEN'),
(8, '2180605678', 'Lê Anh Tuấn', '2180605678@sinhvien.sis.edu.vn', '123456', '0908888999', 'SINH_VIEN'),
(9, '2180609999', 'Trần Thị Hoa', '2180609999@sinhvien.sis.edu.vn', '123456', '0909999000', 'SINH_VIEN')
ON DUPLICATE KEY UPDATE ho_ten = VALUES(ho_ten);

-- Khoa & Ngành
-- Khoa & Ngành
INSERT INTO khoa (id, ma_khoa, ten_khoa, van_phong, so_dien_thoai) VALUES
(1, 'CNTT', 'Khoa Công Nghệ Thông Tin (HUIT)', 'Tòa B - Phòng B302', '02838123456'),
(2, 'KINHTE', 'Khoa Quản Trị Kinh Doanh (HUIT)', 'Tòa C - Phòng C201', '02838123457')
ON DUPLICATE KEY UPDATE ten_khoa = VALUES(ten_khoa);

INSERT INTO nganh_hoc (id, ma_nganh, ten_nganh, khoa_id, tong_tin_chi_yeu_cau) VALUES
(1, 'KTPM', 'Kỹ Thuật Phần Mềm', 1, 145),
(2, 'KHMT', 'Khoa Học Máy Tính', 1, 145),
(3, 'QTKD', 'Quản Trị Kinh Doanh', 2, 135),
(4, '7480109', 'Khoa Học Dữ Liệu', 1, 123)
ON DUPLICATE KEY UPDATE ten_nganh = VALUES(ten_nganh), tong_tin_chi_yeu_cau = VALUES(tong_tin_chi_yeu_cau);

INSERT INTO chuyen_nganh (id, ma_chuyen_nganh, ten_chuyen_nganh, nganh_hoc_id) VALUES
(1, 'CNPM', 'Công Nghệ Phần Mềm Doanh Nghiệp', 1),
(2, 'AI', 'Trí Tuệ Nhân Tạo & Dữ Liệu Lớn', 2),
(3, 'KHDL_DS', 'Phân Tích Dữ Liệu', 4)
ON DUPLICATE KEY UPDATE ten_chuyen_nganh = VALUES(ten_chuyen_nganh);

-- Giảng viên
INSERT INTO giang_vien (id, ma_gv, hoc_ham_hoc_vi, chuyen_mon, nguoi_dung_id, khoa_id) VALUES
(1, 'GV001', 'PGS.TS', 'Khoa học máy tính & Trí tuệ nhân tạo (HUIT)', 4, 1),
(2, 'GV002', 'TS.', 'Khoa học dữ liệu & Xác suất thống kê (HUIT)', 5, 1),
(3, 'GV003', 'ThS.', 'Dữ liệu lớn & Khai phá dữ liệu (HUIT)', 6, 1)
ON DUPLICATE KEY UPDATE chuyen_mon = VALUES(chuyen_mon);

-- Lớp danh nghĩa
INSERT INTO lop_danh_nghia (id, ma_lop, ten_lop, nien_khoa, nganh_hoc_id, co_van_hoc_tap_id) VALUES
(1, '15DHKTPM01', '15DHKTPM01', '2023-2027', 1, 2),
(2, '15DHKHDL03', '15DHKHDL03', '2023-2027', 4, 1)
ON DUPLICATE KEY UPDATE ten_lop = VALUES(ten_lop), nganh_hoc_id = VALUES(nganh_hoc_id);

-- Sinh viên (Khoa 15 - HUIT)
INSERT INTO sinh_vien (id, mssv, lop_danh_nghia_id, nganh_hoc_id, chuyen_nganh_id, khoa_tuyen_sinh, trang_thai_hoc_vu, cpa_tich_luy, tong_tc_tich_luy, ngay_sinh, gioi_tinh, noi_sinh, dia_chi, nguoi_dung_id) VALUES
(1, '2180601234', 2, 4, 3, 2023, 'BINH_THUONG', 3.55, 40, '2004-05-15', 'Nam', 'TP. Hồ Chí Minh', '140 Lê Trọng Tấn, Tân Phú', 7),
(2, '2180605678', 2, 4, 3, 2023, 'BINH_THUONG', 2.85, 36, '2004-08-20', 'Nam', 'Đồng Nai', '45 CMT8, Biên Hòa', 8),
(3, '2180609999', 2, 4, 3, 2023, 'CANH_BAO_1', 1.75, 22, '2004-12-10', 'Nữ', 'Bình Dương', '78 Đại Lộ Bình Dương', 9)
ON DUPLICATE KEY UPDATE lop_danh_nghia_id = VALUES(lop_danh_nghia_id), nganh_hoc_id = VALUES(nganh_hoc_id), cpa_tich_luy = VALUES(cpa_tich_luy), tong_tc_tich_luy = VALUES(tong_tc_tich_luy);

-- Học kỳ
INSERT INTO hoc_ky (id, ma_hoc_ky, ten_hoc_ky, nam_hoc, loai_hoc_ky, trang_thai_dang_ky, ngay_bat_dau_dkhp, ngay_ket_thuc_dkhp, don_gia_tin_chi) VALUES
(1, '2024_1', 'Học kỳ 1 (2024-2025)', '2024-2025', 'CHINH_1', 'DA_KHOA', '2024-08-01', '2024-08-25', 550000.0),
(2, '2024_2', 'Học kỳ 2 (2024-2025)', '2024-2025', 'CHINH_2', 'DANG_MO', '2026-10-01', '2026-11-30', 570000.0),
(3, '2024_3', 'Học kỳ Hè (2024-2025)', '2024-2025', 'PHU_HE', 'CHUA_MO', '2027-05-01', '2027-05-20', 600000.0)
ON DUPLICATE KEY UPDATE trang_thai_dang_ky = VALUES(trang_thai_dang_ky);

-- Học phần (Toàn bộ 39 môn theo Đề án Mở Ngành Khoa Học Dữ Liệu HUIT - 123 Tín Chỉ)
INSERT INTO hoc_phan (id, ma_hp, ten_hp, so_tc_ly_thuyet, so_tc_thuc_hanh, tong_tin_chi, he_so_hoc_phi, ty_le_chuyen_can, ty_le_qua_trinh, ty_le_thuc_hanh, ty_le_cuoi_ky, khoa_id) VALUES
-- Học kỳ 1 (19 TC)
(10, 'IT001', 'Nhập môn Lập trình', 3, 1, 4, 1.0, 0.10, 0.20, 0.20, 0.50, 1),
(11, 'MA006', 'Giải tích', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(12, 'MA003', 'Đại số tuyến tính', 3, 0, 3, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(13, 'IT010', 'Tổ chức và cấu trúc máy tính', 2, 0, 2, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(14, 'IT009', 'Giới thiệu ngành KHDL', 2, 0, 2, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(15, 'EN004', 'Anh văn 1', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(16, 'PE001', 'Giáo dục thể chất 1', 0, 0, 0, 1.0, 0.20, 0.40, 0.00, 0.40, 1),
(17, 'ME001', 'Giáo dục quốc phòng', 0, 0, 0, 1.0, 0.20, 0.40, 0.00, 0.40, 1),

-- Học kỳ 2 (17 TC)
(18, 'SS004', 'Kỹ năng nghề nghiệp', 0, 2, 2, 1.0, 0.10, 0.20, 0.20, 0.50, 1),
(19, 'IT003', 'Cấu trúc dữ liệu và giải thuật', 3, 1, 4, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(20, 'MA004', 'Cấu trúc rời rạc', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(21, 'MA005', 'Xác suất thống kê', 3, 0, 3, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(22, 'EN005', 'Anh văn 2', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(23, 'PE002', 'Giáo dục thể chất 2', 0, 0, 0, 1.0, 0.20, 0.40, 0.00, 0.40, 1),

-- Học kỳ 3 (19 TC)
(24, 'IT004', 'Cơ sở dữ liệu', 3, 1, 4, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(25, 'IT005', 'Nhập môn mạng máy tính', 3, 1, 4, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(26, 'IT002', 'Lập trình hướng đối tượng', 3, 1, 4, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(27, 'EN006', 'Anh văn 3', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(28, 'DS101', 'Thống kê và xác suất chuyên sâu', 2, 1, 3, 1.1, 0.10, 0.20, 0.20, 0.50, 1),

-- Học kỳ 4 (15 TC)
(29, 'SS001', 'Những nguyên lý cơ bản của CN Mác-Lênin', 5, 0, 5, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(30, 'DS103', 'Thu thập và tiền xử lý dữ liệu', 2, 1, 3, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(31, 'DS106', 'Tối ưu hoá và ứng dụng', 2, 1, 3, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(32, 'DS102', 'Học máy thống kê', 3, 1, 4, 1.2, 0.10, 0.20, 0.20, 0.50, 1),

-- Học kỳ 5 (16 TC)
(33, 'SS002', 'Đường lối cách mạng của ĐCSVN', 3, 0, 3, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(34, 'DS104', 'Tính toán song song & phân tán', 3, 1, 4, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(35, 'DS105', 'Phân tích và trực quan dữ liệu', 2, 1, 3, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(36, 'DS301', 'Các giải thuật khai phá dữ liệu lớn (Tự chọn 1)', 2, 1, 3, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(37, 'IS217', 'Kho dữ liệu và OLAP (Tự do 1)', 3, 0, 3, 1.1, 0.10, 0.30, 0.00, 0.60, 1),

-- Học kỳ 6 (15 TC)
(38, 'SS003', 'Tư tưởng Hồ Chí Minh', 2, 0, 2, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(39, 'IS211', 'Cơ sở dữ liệu phân tán', 3, 1, 4, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(40, 'DS200', 'Phân tích dữ liệu lớn', 3, 1, 4, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(41, 'DS202', 'Đồ án khoa học dữ liệu và ứng dụng 1', 0, 2, 2, 1.3, 0.00, 0.30, 0.30, 0.40, 1),
(42, 'DS300', 'Hệ khuyến nghị (Tự chọn 2)', 2, 1, 3, 1.2, 0.10, 0.20, 0.20, 0.50, 1),

-- Học kỳ 7 (12 TC)
(43, 'SS006', 'Pháp luật đại cương', 2, 0, 2, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(44, 'DS201', 'Deep Learning trong khoa học dữ liệu', 3, 1, 4, 1.3, 0.10, 0.20, 0.20, 0.50, 1),
(45, 'INT701', 'Thực tập doanh nghiệp', 2, 0, 2, 1.0, 0.00, 0.30, 0.00, 0.70, 1),
(46, 'DS203', 'Đồ án khoa học dữ liệu và ứng dụng 2', 0, 2, 2, 1.3, 0.00, 0.30, 0.30, 0.40, 1),
(47, 'CS221', 'Xử lý ngôn ngữ tự nhiên (Tự chọn 3)', 2, 0, 2, 1.2, 0.10, 0.30, 0.00, 0.60, 1),

-- Học kỳ 8 (10 TC)
(48, 'KLTN01', 'Khóa luận tốt nghiệp (KHDL HUIT)', 0, 10, 10, 1.5, 0.00, 0.00, 0.00, 1.00, 1)
ON DUPLICATE KEY UPDATE 
    ten_hp = VALUES(ten_hp),
    so_tc_ly_thuyet = VALUES(so_tc_ly_thuyet),
    so_tc_thuc_hanh = VALUES(so_tc_thuc_hanh),
    tong_tin_chi = VALUES(tong_tin_chi);

-- Điều kiện học phần
INSERT INTO dieu_kien_hoc_phan (id, hoc_phan_id, hoc_phan_dieu_kien_id, loai_dieu_kien) VALUES
(10, 19, 10, 'TIEN_QUYET'), -- IT003 (CTDL&GT) yêu cầu IT001
(11, 22, 15, 'TIEN_QUYET'), -- EN005 (Anh văn 2) yêu cầu EN004
(12, 24, 10, 'TIEN_QUYET'), -- IT004 (CSDL) yêu cầu IT001
(13, 26, 10, 'TIEN_QUYET'), -- IT002 (OOP) yêu cầu IT001
(14, 27, 22, 'TIEN_QUYET'), -- EN006 (Anh văn 3) yêu cầu EN005
(15, 28, 21, 'TIEN_QUYET'), -- DS101 yêu cầu MA005
(16, 30, 24, 'TIEN_QUYET'), -- DS103 (Tiền xử lý) yêu cầu IT004
(17, 31, 12, 'TIEN_QUYET'), -- DS106 (Tối ưu hóa) yêu cầu MA003
(18, 32, 28, 'TIEN_QUYET'), -- DS102 (Học máy TK) yêu cầu DS101
(19, 34, 13, 'TIEN_QUYET'), -- DS104 yêu cầu IT010
(20, 35, 30, 'TIEN_QUYET'), -- DS105 yêu cầu DS103
(21, 39, 24, 'TIEN_QUYET'), -- IS211 (CSDL phân tán) yêu cầu IT004
(22, 40, 34, 'TIEN_QUYET'), -- DS200 (Big Data) yêu cầu DS104
(23, 44, 32, 'TIEN_QUYET'), -- DS201 (Deep Learning) yêu cầu DS102
(24, 46, 41, 'TIEN_QUYET')  -- DS203 (Đồ án 2) yêu cầu DS202
ON DUPLICATE KEY UPDATE loai_dieu_kien = VALUES(loai_dieu_kien);

-- Khung chương trình chuẩn 8 Học kỳ ngành Khoa Học Dữ Liệu HUIT (123 Tín Chỉ)
INSERT INTO khung_chuong_trinh (id, nganh_hoc_id, hoc_phan_id, hoc_ky_du_kien, bat_buoc) VALUES
-- Học kỳ 1 (19 TC)
(10, 4, 10, 1, TRUE),
(11, 4, 11, 1, TRUE),
(12, 4, 12, 1, TRUE),
(13, 4, 13, 1, TRUE),
(14, 4, 14, 1, TRUE),
(15, 4, 15, 1, TRUE),
(16, 4, 16, 1, TRUE),
(17, 4, 17, 1, TRUE),
-- Học kỳ 2 (17 TC)
(18, 4, 18, 2, TRUE),
(19, 4, 19, 2, TRUE),
(20, 4, 20, 2, TRUE),
(21, 4, 21, 2, TRUE),
(22, 4, 22, 2, TRUE),
(23, 4, 23, 2, TRUE),
-- Học kỳ 3 (19 TC)
(24, 4, 24, 3, TRUE),
(25, 4, 25, 3, TRUE),
(26, 4, 26, 3, TRUE),
(27, 4, 27, 3, TRUE),
(28, 4, 28, 3, TRUE),
-- Học kỳ 4 (15 TC)
(29, 4, 29, 4, TRUE),
(30, 4, 30, 4, TRUE),
(31, 4, 31, 4, TRUE),
(32, 4, 32, 4, TRUE),
-- Học kỳ 5 (16 TC)
(33, 4, 33, 5, TRUE),
(34, 4, 34, 5, TRUE),
(35, 4, 35, 5, TRUE),
(36, 4, 36, 5, FALSE),
(37, 4, 37, 5, FALSE),
-- Học kỳ 6 (15 TC)
(38, 4, 38, 6, TRUE),
(39, 4, 39, 6, TRUE),
(40, 4, 40, 6, TRUE),
(41, 4, 41, 6, TRUE),
(42, 4, 42, 6, FALSE),
-- Học kỳ 7 (12 TC)
(43, 4, 43, 7, TRUE),
(44, 4, 44, 7, TRUE),
(45, 4, 45, 7, TRUE),
(46, 4, 46, 7, TRUE),
(47, 4, 47, 7, FALSE),
-- Học kỳ 8 (10 TC)
(48, 4, 48, 8, TRUE)
ON DUPLICATE KEY UPDATE bat_buoc = VALUES(bat_buoc);

-- Lớp học phần đang mở trong Học kỳ 2 (2024_2)
INSERT INTO lop_hoc_phan (id, ma_lhp, ten_lhp, hoc_phan_id, hoc_ky_id, giang_vien_id, si_so_toi_da, si_so_hien_tai, phong_mac_dinh, trang_thai) VALUES
(10, 'IT001_01', 'Nhập môn Lập trình - 15DHKHDL03', 10, 1, 1, 60, 45, 'A301', 'DA_KET_THUC'),
(11, 'MA006_01', 'Giải tích - 15DHKHDL03', 11, 1, 2, 60, 45, 'B202', 'DA_KET_THUC'),
(12, 'MA003_01', 'Đại số tuyến tính - 15DHKHDL03', 12, 1, 2, 60, 45, 'B203', 'DA_KET_THUC'),
(13, 'IT010_01', 'Tổ chức và cấu trúc máy tính - 15DHKHDL03', 13, 1, 3, 60, 45, 'C101', 'DA_KET_THUC'),
(14, 'IT009_01', 'Giới thiệu ngành KHDL - 15DHKHDL03', 14, 1, 1, 60, 45, 'A201', 'DA_KET_THUC'),
(15, 'EN004_01', 'Anh văn 1 - 15DHKHDL03', 15, 1, 2, 60, 45, 'D102', 'DA_KET_THUC'),
(16, 'IT003_01', 'Cấu trúc dữ liệu và giải thuật - 15DHKHDL03', 19, 2, 1, 60, 42, 'A302', 'DANG_MO'),
(17, 'MA005_01', 'Xác suất thống kê - 15DHKHDL03', 21, 2, 2, 60, 42, 'B204', 'DANG_MO'),
(18, 'EN005_01', 'Anh văn 2 - 15DHKHDL03', 22, 2, 3, 60, 42, 'D103', 'DANG_MO'),
(19, 'IT004_01', 'Cơ sở dữ liệu - 15DHKHDL03', 24, 2, 2, 60, 40, 'B102', 'DANG_MO'),
(20, 'IT002_01', 'Lập trình hướng đối tượng (Java) - 15DHKHDL03', 26, 2, 1, 60, 38, 'A205', 'DANG_MO')
ON DUPLICATE KEY UPDATE ten_lhp = VALUES(ten_lhp);

-- Lịch học
INSERT INTO lich_hoc (id, lop_hoc_phan_id, thu, tiet_bat_dau, so_tiet, phong_hoc, tu_tuan, den_tuan, loai_lich) VALUES
(10, 16, 2, 1, 3, 'A302', 1, 15, 'LY_THUYET'),  -- IT003_01: Thứ 2, tiết 1-3
(11, 16, 4, 7, 3, 'LAB01', 2, 15, 'THUC_HANH'), -- IT003_01: Thứ 4, tiết 7-9 (TH Lab HUIT)
(12, 17, 3, 1, 3, 'B204', 1, 15, 'LY_THUYET'),  -- MA005_01: Thứ 3, tiết 1-3
(13, 18, 5, 1, 4, 'D103', 1, 15, 'LY_THUYET'),  -- EN005_01: Thứ 5, tiết 1-4
(14, 19, 6, 7, 3, 'B102', 1, 15, 'LY_THUYET'),  -- IT004_01: Thứ 6, tiết 7-9
(15, 20, 7, 1, 4, 'A205', 1, 15, 'LY_THUYET')   -- IT002_01: Thứ 7, tiết 1-4
ON DUPLICATE KEY UPDATE phong_hoc = VALUES(phong_hoc);

-- Đăng ký học phần
INSERT INTO dang_ky_hoc_phan (id, sinh_vien_id, lop_hoc_phan_id, loai_dang_ky, trang_thai) VALUES
-- Kỳ 1 (Đã hoàn thành)
(10, 1, 10, 'HOC_MOI', 'THANH_CONG'),
(11, 1, 11, 'HOC_MOI', 'THANH_CONG'),
(12, 1, 12, 'HOC_MOI', 'THANH_CONG'),
(13, 1, 13, 'HOC_MOI', 'THANH_CONG'),
(14, 1, 14, 'HOC_MOI', 'THANH_CONG'),
(15, 1, 15, 'HOC_MOI', 'THANH_CONG'),
-- Kỳ 2 (Đang học kỳ này)
(16, 1, 16, 'HOC_MOI', 'THANH_CONG'),
(17, 1, 17, 'HOC_MOI', 'THANH_CONG'),
(18, 1, 18, 'HOC_MOI', 'THANH_CONG')
ON DUPLICATE KEY UPDATE trang_thai = VALUES(trang_thai);

-- Điểm học phần kỳ 1 của Sinh viên Nguyễn Minh Khoa
INSERT INTO diem_hoc_phan (id, dang_ky_hoc_phan_id, diem_chuyen_can, diem_qua_trinh, diem_thuc_hanh, diem_cuoi_ky, diem_tong_ket_he10, diem_he4, diem_chu, ket_qua_qua_mon, cam_thi, khoa_diem) VALUES
(10, 10, 10.0, 9.0, 9.5, 8.5, 9.0, 4.0, 'A', TRUE, FALSE, TRUE),
(11, 11, 9.0, 8.5, 0.0, 8.0, 8.3, 3.5, 'B+', TRUE, FALSE, TRUE),
(12, 12, 9.5, 8.0, 0.0, 8.5, 8.6, 4.0, 'A', TRUE, FALSE, TRUE),
(13, 13, 10.0, 8.5, 0.0, 8.0, 8.5, 4.0, 'A', TRUE, FALSE, TRUE),
(14, 14, 9.0, 9.0, 0.0, 9.0, 9.0, 4.0, 'A', TRUE, FALSE, TRUE),
(15, 15, 8.5, 8.0, 0.0, 7.5, 7.8, 3.0, 'B', TRUE, FALSE, TRUE)
ON DUPLICATE KEY UPDATE diem_tong_ket_he10 = VALUES(diem_tong_ket_he10);

-- Công nợ học phí các học kỳ (HUIT)
INSERT INTO cong_no_hoc_ky (id, sinh_vien_id, hoc_ky_id, tong_so_tc, tong_hoc_phi, mien_giam, da_dong, con_no, trang_thai) VALUES
(1, 1, 1, 19, 10450000.0, 1000000.0, 9450000.0, 0.0, 'DA_HOAN_THANH'),
(2, 1, 2, 11, 6270000.0, 0.0, 6270000.0, 0.0, 'DA_HOAN_THANH'),
(3, 2, 2, 11, 6270000.0, 0.0, 3000000.0, 3270000.0, 'DONG_MOT_PHAN'),
(4, 3, 2, 6, 3420000.0, 0.0, 0.0, 3420000.0, 'CHUA_DONG')
ON DUPLICATE KEY UPDATE da_dong = VALUES(da_dong), tong_hoc_phi = VALUES(tong_hoc_phi), con_no = VALUES(con_no);

-- Lịch sử biên lai điện tử qua các kỳ của HUIT
INSERT INTO phieu_thu_hoc_phi (id, cong_no_hoc_ky_id, ma_phieu_thu, so_tien, phuong_thuc, ma_giao_dich, ngay_thu, nguoi_thu, ghi_chu) VALUES
(1, 1, 'PT-HUIT-20241-001', 5000000.0, 'CHUYEN_KHOAN', 'MB9921827', '2024-08-20 09:30:15', 'Phòng Kế Toán HUIT', 'Đóng học phí đợt 1 - Học kỳ 1 (2024-2025)'),
(2, 1, 'PT-HUIT-20241-002', 4450000.0, 'VIETQR', 'VQR-MB-778129', '2024-09-05 14:15:20', 'Cổng thanh toán tự động VietQR Napas 247', 'Đóng phần học phí còn lại - Học kỳ 1 (2024-2025)'),
(3, 2, 'PT-HUIT-20242-001', 6270000.0, 'VIETQR', 'TXN99281749', '2025-01-15 10:22:00', 'Cổng thanh toán tự động VietQR Napas 247', 'Thanh toán học phí tín chỉ Học kỳ 2 (2024-2025)'),
(4, 3, 'PT-HUIT-20242-002', 3000000.0, 'CHUYEN_KHOAN', 'MB8827110', '2025-01-16 11:05:00', 'Phòng Kế Toán HUIT', 'Đóng học phí đợt 1')
ON DUPLICATE KEY UPDATE so_tien = VALUES(so_tien), ma_phieu_thu = VALUES(ma_phieu_thu);

-- Yêu cầu dịch vụ một cửa mẫu
INSERT INTO yeu_cau_dich_vu_mot_cua (id, sinh_vien_id, loai_dich_vu, ly_do, trang_thai, ma_xac_thuc_qr, ngay_tao) VALUES
(1, 1, 'GIAY_XAC_NHAN_SV', 'Bổ sung hồ sơ tạm hoãn nghĩa vụ quân sự địa phương', 'DA_DUYET', 'VERIFY-QR-2180601234-NAVY', NOW()),
(2, 2, 'BANG_DIEM_TAM_THOI', 'Nộp hồ sơ ứng tuyển thực tập doanh nghiệp', 'CHO_DUYET', 'VERIFY-QR-2180605678-TRANS', NOW())
ON DUPLICATE KEY UPDATE trang_thai = VALUES(trang_thai);

-- Chuẩn đầu ra
INSERT INTO chung_chi_chuan_dau_ra (id, sinh_vien_id, loai_chung_chi, so_hieu, ngay_cap, don_vi_cap, trang_thai) VALUES
(1, 1, 'TIENG_ANH_TOEIC', 'TOEIC-750-2024', '2024-03-10', 'IIG Vietnam', 'DA_DAT'),
(2, 1, 'GIAO_DUC_QUOC_PHONG', 'GDQP-2023-8891', '2023-08-15', 'Trung tâm GDQP&AN ĐHQG', 'DA_DAT')
ON DUPLICATE KEY UPDATE trang_thai = VALUES(trang_thai);

