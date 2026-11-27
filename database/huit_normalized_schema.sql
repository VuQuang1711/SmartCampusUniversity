-- =========================================================================
-- THIẾT KẾ CSDL CHUẨN HÓA 3NF DỰA TRÊN DỮ LIỆU THỜI KHÓA BIỂU THỰC TẾ HUIT
-- Nguồn phân tích: 4 file Excel trong thư mục class_database/
--   1. thoi-khoa-bieu-du-kien-hk1-nam-hoc-2025-2026-he-dhcq...xls (10.212 dòng)
--   2. thoi-khoa-bieu-hoc-ky-2-nam-hoc-2025-2026-he-dh (1).xls   (10.810 dòng)
--   3. Thời khóa biểu học kỳ 1 năm học 2026-2027 hệ ĐHCQ.xls      (9.977 dòng)
--   4. TKB_HKII_2026.xls (HK2 2026-2027)                         (11.069 dòng)
-- =========================================================================

USE university_sis;

-- 1. BẢNG CƠ SỞ ĐÀO TẠO (Tách từ cột "Phòng": 140 Lê Trọng Tấn, 93 Tân Kỳ Tân Quý, 31 Chế Lan Viên, Online/Zoom...)
CREATE TABLE IF NOT EXISTS co_so (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_co_so VARCHAR(30) NOT NULL UNIQUE,
    ten_co_so VARCHAR(150) NOT NULL,
    dia_chi VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. BẢNG PHÒNG HỌC (Tách từ cột "Phòng": Mã phòng + Loại phòng + Cơ sở)
CREATE TABLE IF NOT EXISTS phong_hoc (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_phong VARCHAR(60) NOT NULL UNIQUE,
    ten_hien_thi VARCHAR(120),
    loai_phong VARCHAR(20) NOT NULL DEFAULT 'LY_THUYET', -- LY_THUYET, PHONG_MAY, THUC_HANH, ONLINE
    suc_chua INT DEFAULT 60,
    co_so_id BIGINT,
    CONSTRAINT fk_phong_coso FOREIGN KEY (co_so_id) REFERENCES co_so(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. NÂNG CẤP BẢNG HỌC PHẦN (Bổ sung Tính chất học phần & Tổng số tiết)
ALTER TABLE hoc_phan
    ADD COLUMN IF NOT EXISTS tong_so_tiet INT NULL COMMENT 'Tổng số tiết chuẩn từ cột Tổng số tiết',
    ADD COLUMN IF NOT EXISTS tinh_chat_hp VARCHAR(50) DEFAULT 'TRUC_TIEP' COMMENT 'TRUC_TIEP hoặc E_learning(HUITx)';

-- 4. NÂNG CẤP BẢNG LỚP HỌC PHẦN (Liên kết Lớp danh nghĩa, Tính chất, Ngày bắt đầu/kết thúc, Tổng số tiết)
ALTER TABLE lop_hoc_phan
    ADD COLUMN IF NOT EXISTS lop_du_kien VARCHAR(60) NULL COMMENT 'Mã lớp học trong TKB (VD: 15DHKHDL03)',
    ADD COLUMN IF NOT EXISTS lop_danh_nghia_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS tinh_chat_hp VARCHAR(50) NULL,
    ADD COLUMN IF NOT EXISTS tong_so_tiet INT NULL,
    ADD COLUMN IF NOT EXISTS ngay_bat_dau DATE NULL,
    ADD COLUMN IF NOT EXISTS ngay_ket_thuc DATE NULL;

-- 5. NÂNG CẤP BẢNG LỊCH HỌC (Liên kết Phòng học chuẩn hóa, Ngày bắt đầu/kết thúc, Số tiết đợt, Chuỗi tuần gọn)
ALTER TABLE lich_hoc
    ADD COLUMN IF NOT EXISTS phong_hoc_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS so_tiet_dot INT NULL,
    ADD COLUMN IF NOT EXISTS ngay_bat_dau DATE NULL,
    ADD COLUMN IF NOT EXISTS ngay_ket_thuc DATE NULL,
    ADD COLUMN IF NOT EXISTS tuan_hoc VARCHAR(120) NULL;

-- 6. BẢNG CHI TIẾT TUẦN HỌC (Chuẩn hóa 1NF/3NF cho 30 cột T1..T30 trong Excel HUIT)
CREATE TABLE IF NOT EXISTS lich_hoc_tuan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lich_hoc_id BIGINT NOT NULL,
    tuan_thu INT NOT NULL COMMENT 'Số thứ tự tuần học 1..30 (T1..T30)',
    UNIQUE KEY uk_lich_tuan (lich_hoc_id, tuan_thu),
    KEY idx_lht_tuan (tuan_thu),
    KEY idx_lht_lich (lich_hoc_id),
    CONSTRAINT fk_lht_lich FOREIGN KEY (lich_hoc_id) REFERENCES lich_hoc(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
