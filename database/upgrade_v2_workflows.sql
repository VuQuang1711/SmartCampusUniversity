-- =========================================================================
-- UPGRADE V2: LIÊN THÔNG 3 VAI TRÒ (ADMIN - GIẢNG VIÊN - SINH VIÊN)
-- Chạy an toàn trên CSDL university_sis hiện có (idempotent)
-- =========================================================================

USE university_sis;

-- 1. Nâng cấp bảng nguoi_dung: thêm trạng thái khóa tài khoản
SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='nguoi_dung' AND COLUMN_NAME='trang_thai');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE nguoi_dung ADD COLUMN trang_thai VARCHAR(20) NOT NULL DEFAULT ''HOAT_DONG''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Nâng cấp bảng nganh_hoc: đơn giá tín chỉ riêng theo ngành (tùy chọn)
SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='nganh_hoc' AND COLUMN_NAME='don_gia_tin_chi_rieng');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE nganh_hoc ADD COLUMN don_gia_tin_chi_rieng DOUBLE NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Nâng cấp bảng hoc_ky: mở/đóng cổng nhập điểm, hệ số học lại/cải thiện
SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='hoc_ky' AND COLUMN_NAME='cho_phep_nhap_diem');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE hoc_ky ADD COLUMN cho_phep_nhap_diem BOOLEAN NOT NULL DEFAULT TRUE', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='hoc_ky' AND COLUMN_NAME='he_so_hoc_lai');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE hoc_ky ADD COLUMN he_so_hoc_lai DOUBLE NOT NULL DEFAULT 1.5', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='hoc_ky' AND COLUMN_NAME='he_so_cai_thien');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE hoc_ky ADD COLUMN he_so_cai_thien DOUBLE NOT NULL DEFAULT 1.2', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Nâng cấp bảng lop_hoc_phan: hỗ trợ luồng Giảng viên đề xuất -> Admin duyệt
SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='lop_hoc_phan' AND COLUMN_NAME='ly_do_tu_choi');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE lop_hoc_phan ADD COLUMN ly_do_tu_choi VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='lop_hoc_phan' AND COLUMN_NAME='ngay_de_xuat');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE lop_hoc_phan ADD COLUMN ngay_de_xuat DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='lop_hoc_phan' AND COLUMN_NAME='thu_du_kien');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE lop_hoc_phan ADD COLUMN thu_du_kien INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='lop_hoc_phan' AND COLUMN_NAME='tiet_bat_dau_du_kien');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE lop_hoc_phan ADD COLUMN tiet_bat_dau_du_kien INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='lop_hoc_phan' AND COLUMN_NAME='so_tiet_du_kien');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE lop_hoc_phan ADD COLUMN so_tiet_du_kien INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. Nâng cấp bảng chung_chi_chuan_dau_ra: upload PDF, điểm số, lý do từ chối, thời gian nộp/duyệt
SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='chung_chi_chuan_dau_ra' AND COLUMN_NAME='file_pdf_url');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE chung_chi_chuan_dau_ra ADD COLUMN file_pdf_url VARCHAR(255) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='chung_chi_chuan_dau_ra' AND COLUMN_NAME='ten_file_goc');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE chung_chi_chuan_dau_ra ADD COLUMN ten_file_goc VARCHAR(255) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='chung_chi_chuan_dau_ra' AND COLUMN_NAME='diem_so');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE chung_chi_chuan_dau_ra ADD COLUMN diem_so VARCHAR(50) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='chung_chi_chuan_dau_ra' AND COLUMN_NAME='ly_do_tu_choi');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE chung_chi_chuan_dau_ra ADD COLUMN ly_do_tu_choi VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='chung_chi_chuan_dau_ra' AND COLUMN_NAME='ngay_nop');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE chung_chi_chuan_dau_ra ADD COLUMN ngay_nop DATETIME DEFAULT CURRENT_TIMESTAMP', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='chung_chi_chuan_dau_ra' AND COLUMN_NAME='ngay_duyet');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE chung_chi_chuan_dau_ra ADD COLUMN ngay_duyet DATETIME NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. Nâng cấp bảng diem_ren_luyen: trạng thái duyệt cấp lớp -> khoa/trường
SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='diem_ren_luyen' AND COLUMN_NAME='trang_thai');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE diem_ren_luyen ADD COLUMN trang_thai VARCHAR(30) NOT NULL DEFAULT ''CHO_CVHT_DUYET''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='university_sis' AND TABLE_NAME='diem_ren_luyen' AND COLUMN_NAME='ghi_chu');
SET @sql := IF(@col_exists = 0, 'ALTER TABLE diem_ren_luyen ADD COLUMN ghi_chu VARCHAR(255) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. Bảng YÊU CẦU ĐỔI LỊCH (yeu_cau_doi_lich)
CREATE TABLE IF NOT EXISTS yeu_cau_doi_lich (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lop_hoc_phan_id BIGINT NOT NULL,
    lich_hoc_id BIGINT NULL,
    giang_vien_id BIGINT NOT NULL,
    loai_yeu_cau VARCHAR(30) NOT NULL DEFAULT 'DOI_CA',
    thu_moi INT NOT NULL,
    tiet_bat_dau_moi INT NOT NULL,
    so_tiet_moi INT NOT NULL,
    phong_hoc_moi VARCHAR(50) NOT NULL,
    tu_tuan_moi INT NOT NULL DEFAULT 1,
    den_tuan_moi INT NOT NULL DEFAULT 15,
    ly_do VARCHAR(500),
    ket_qua_kiem_tra VARCHAR(500),
    so_sv_trung INT NOT NULL DEFAULT 0,
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'CHO_DUYET',
    phan_hoi_admin VARCHAR(500),
    ngay_tao DATETIME DEFAULT CURRENT_TIMESTAMP,
    ngay_xu_ly DATETIME NULL,
    CONSTRAINT fk_ycdl_lhp FOREIGN KEY (lop_hoc_phan_id) REFERENCES lop_hoc_phan(id),
    CONSTRAINT fk_ycdl_lh FOREIGN KEY (lich_hoc_id) REFERENCES lich_hoc(id) ON DELETE SET NULL,
    CONSTRAINT fk_ycdl_gv FOREIGN KEY (giang_vien_id) REFERENCES giang_vien(id)
);

-- 8. Bảng THÔNG BÁO / HỘP THƯ LIÊN THÔNG (thong_bao)
CREATE TABLE IF NOT EXISTS thong_bao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nguoi_gui_id BIGINT NULL,
    nguoi_nhan_id BIGINT NOT NULL,
    tieu_de VARCHAR(200) NOT NULL,
    noi_dung TEXT NOT NULL,
    loai_thong_bao VARCHAR(30) NOT NULL DEFAULT 'HE_THONG',
    da_doc BOOLEAN NOT NULL DEFAULT FALSE,
    lien_ket VARCHAR(255) NULL,
    ngay_gui DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tb_gui FOREIGN KEY (nguoi_gui_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL,
    CONSTRAINT fk_tb_nhan FOREIGN KEY (nguoi_nhan_id) REFERENCES nguoi_dung(id) ON DELETE CASCADE
);

-- 9. Bảng NHẬT KÝ HỆ THỐNG (nhat_ky_he_thong - Audit Log)
CREATE TABLE IF NOT EXISTS nhat_ky_he_thong (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nguoi_thuc_hien_id BIGINT NULL,
    ten_nguoi_thuc_hien VARCHAR(100),
    vai_tro VARCHAR(30),
    hanh_dong VARCHAR(50) NOT NULL,
    doi_tuong VARCHAR(100),
    chi_tiet VARCHAR(1000),
    thoi_gian DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (nguoi_thuc_hien_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL
);

-- =========================================================================
-- DỮ LIỆU MẪU V2 CHO CÁC LUỒNG NGHIỆP VỤ MỚI
-- =========================================================================

-- Đảm bảo 3 sinh viên có đăng ký lớp HK2 để test conflict check sinh động
INSERT IGNORE INTO dang_ky_hoc_phan (sinh_vien_id, lop_hoc_phan_id, loai_dang_ky, trang_thai) VALUES
(1, 16, 'HOC_MOI', 'THANH_CONG'),
(1, 17, 'HOC_MOI', 'THANH_CONG'),
(1, 18, 'HOC_MOI', 'THANH_CONG'),
(2, 16, 'HOC_MOI', 'THANH_CONG'),
(2, 17, 'HOC_MOI', 'THANH_CONG'),
(3, 16, 'HOC_MOI', 'THANH_CONG');

-- Khởi tạo điểm học phần cho các đăng ký HK2 chưa có
INSERT INTO diem_hoc_phan (dang_ky_hoc_phan_id, cam_thi, khoa_diem)
SELECT dk.id, FALSE, FALSE
FROM dang_ky_hoc_phan dk
LEFT JOIN diem_hoc_phan d ON d.dang_ky_hoc_phan_id = dk.id
WHERE d.id IS NULL;

-- Thêm 1 lớp đề xuất CHỜ DUYỆT từ Giảng viên GV001 để Admin test Trung tâm Phê duyệt ngay
INSERT INTO lop_hoc_phan (id, ma_lhp, ten_lhp, hoc_phan_id, hoc_ky_id, giang_vien_id, si_so_toi_da, si_so_hien_tai, phong_mac_dinh, trang_thai, thu_du_kien, tiet_bat_dau_du_kien, so_tiet_du_kien, ngay_de_xuat)
VALUES (21, 'DS102_2024_2_01', 'Học máy thống kê - Lớp Đề Xuất 01', 32, 2, 1, 50, 0, 'A401', 'CHO_DUYET', 4, 1, 3, NOW())
ON DUPLICATE KEY UPDATE trang_thai = VALUES(trang_thai);

-- Đảm bảo các lớp HK2 (16, 17, 18) có lịch học chuẩn để test Thời khóa biểu & Conflict Check
INSERT INTO lich_hoc (id, lop_hoc_phan_id, thu, tiet_bat_dau, so_tiet, phong_hoc, tu_tuan, den_tuan, loai_lich) VALUES
(10, 16, 2, 1, 3, 'A302', 1, 15, 'LY_THUYET'),
(11, 16, 4, 7, 3, 'LAB01', 2, 15, 'THUC_HANH'),
(12, 17, 3, 1, 3, 'B204', 1, 15, 'LY_THUYET'),
(13, 18, 5, 1, 4, 'D103', 1, 15, 'LY_THUYET'),
(16, 16, 6, 1, 2, 'Hội trường A', 16, 16, 'LICH_THI')
ON DUPLICATE KEY UPDATE phong_hoc = VALUES(phong_hoc), loai_lich = VALUES(loai_lich);

-- Thêm 1 yêu cầu đổi lịch mẫu (đã pass 0 SV trùng) từ GV001 để Admin duyệt
INSERT INTO yeu_cau_doi_lich (id, lop_hoc_phan_id, lich_hoc_id, giang_vien_id, loai_yeu_cau, thu_moi, tiet_bat_dau_moi, so_tiet_moi, phong_hoc_moi, tu_tuan_moi, den_tuan_moi, ly_do, ket_qua_kiem_tra, so_sv_trung, trang_thai, ngay_tao)
VALUES (1, 16, 10, 1, 'DOI_CA', 6, 4, 3, 'A305', 1, 15, 'Chuyển lịch do trùng hội nghị khoa học cấp trường sáng thứ 2', 'Đạt chuẩn: 0 sinh viên trùng lịch, phòng A305 trống, lịch GV trống', 0, 'CHO_DUYET', NOW())
ON DUPLICATE KEY UPDATE trang_thai = VALUES(trang_thai), ly_do = VALUES(ly_do), ket_qua_kiem_tra = VALUES(ket_qua_kiem_tra);

-- Thêm chứng chỉ CHỜ DUYỆT mẫu
INSERT INTO chung_chi_chuan_dau_ra (id, sinh_vien_id, loai_chung_chi, so_hieu, diem_so, ngay_cap, don_vi_cap, trang_thai, ngay_nop)
VALUES (3, 1, 'TIN_HOC_MOS', 'MOS-2025-9981', '880/1000', '2025-02-10', 'IIG Vietnam - HUIT', 'CHO_DUYET', NOW())
ON DUPLICATE KEY UPDATE trang_thai = VALUES(trang_thai);

-- Điểm rèn luyện mẫu các học kỳ
INSERT INTO diem_ren_luyen (id, sinh_vien_id, hoc_ky_id, diem_tu_cham, diem_lop_cham, diem_khoa_duyet, xep_loai, trang_thai, ghi_chu) VALUES
(1, 1, 1, 92, 90, 90, 'XUAT_SAC', 'DA_CHOT', 'Tham gia tích cực nghiên cứu khoa học sinh viên'),
(2, 1, 2, 86, 85, NULL, 'TOT', 'CVHT_DA_DUYET', 'Hoàn thành tốt các phong trào Đoàn khoa'),
(3, 2, 1, 78, 76, 76, 'KHA', 'DA_CHOT', 'Tham gia đầy đủ sinh hoạt lớp'),
(4, 3, 2, 65, NULL, NULL, 'TRUNG_BINH', 'CHO_CVHT_DUYET', 'Cần cải thiện chuyên cần học tập')
ON DUPLICATE KEY UPDATE trang_thai = VALUES(trang_thai);

-- Thông báo hộp thư mẫu cho SV, GV
INSERT INTO thong_bao (id, nguoi_gui_id, nguoi_nhan_id, tieu_de, noi_dung, loai_thong_bao, da_doc, lien_ket, ngay_gui) VALUES
(1, 1, 7, 'Thông báo mở cổng Đăng ký Học phần HK2 (2024-2025)', 'Phòng Đào Tạo thông báo mở cổng ĐKHP cho sinh viên toàn trường. Sinh viên vui lòng kiểm tra chương trình khung và môn tiên quyết trước khi đăng ký.', 'TOAN_TRUONG', FALSE, '/Student/Registration', NOW()),
(2, 3, 7, 'Nhắc nhở đối soát học phí & chứng chỉ chuẩn đầu ra', 'Vui lòng kiểm tra mục Công nợ học phí và nộp bổ sung chứng chỉ Tin học MOS dạng PDF để hoàn tất chuẩn đầu ra.', 'HOC_PHI', FALSE, '/Student/Certificates', NOW()),
(3, 1, 4, 'Tiếp nhận đề xuất mở lớp học phần DS102_2024_2_01', 'Đề xuất mở lớp Học máy thống kê của thầy/cô đã được ghi nhận và đang chờ Phòng Đào Tạo phê duyệt.', 'MO_LOP', FALSE, '/Faculty/Proposals', NOW()),
(4, 1, 4, 'Hướng dẫn kiểm tra xung đột lịch trước khi đề xuất đổi lịch', 'Khi đề xuất đổi ca/đổi phòng/dạy bù, hệ thống tự động kiểm tra trùng lịch trên toàn bộ sinh viên lớp học phần và phòng học.', 'HE_THONG', TRUE, '/Faculty/Schedule', NOW())
ON DUPLICATE KEY UPDATE tieu_de = VALUES(tieu_de);

-- Nhật ký hệ thống mẫu
INSERT INTO nhat_ky_he_thong (id, nguoi_thuc_hien_id, ten_nguoi_thuc_hien, vai_tro, hanh_dong, doi_tuong, chi_tiet, thoi_gian) VALUES
(1, 1, 'Phòng Đào Tạo', 'ADMIN', 'MO_CONG_DKHP', 'HocKy#2024_2', 'Mở cổng đăng ký học phần Học kỳ 2 (2024-2025) - Đơn giá 570.000 đ/TC', NOW()),
(2, 3, 'Phòng Tài Chính Kế Toán', 'KE_TOAN', 'XAC_NHAN_THANH_TOAN', 'CongNo#2', 'Xác nhận thanh toán VietQR 6.270.000 đ cho SV 2180601234', NOW()),
(3, 4, 'PGS.TS Nguyễn Văn An', 'GIANG_VIEN', 'DE_XUAT_MO_LOP', 'LopHocPhan#DS102_2024_2_01', 'Đề xuất mở lớp Học máy thống kê (50 SV, Thứ 4 Tiết 1-3, P.A401)', NOW())
ON DUPLICATE KEY UPDATE hanh_dong = VALUES(hanh_dong);
