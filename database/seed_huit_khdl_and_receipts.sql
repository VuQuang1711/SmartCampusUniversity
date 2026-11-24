-- =========================================================================
-- HỆ THỐNG QUẢN LÝ ĐÀO TẠO & SINH VIÊN - TRƯỜNG ĐẠI HỌC CÔNG THƯƠNG TP.HCM (HUIT)
-- Dữ liệu Chương trình Khung Đào tạo Ngành Khoa Học Dữ Liệu (123 Tín Chỉ)
-- Và Lịch sử Biên Lai Học Phí Điện Tử
-- =========================================================================

USE university_sis;

-- 1. THÊM HOẶC CẬP NHẬT NGÀNH KHOA HỌC DỮ LIỆU (HUIT)
INSERT INTO nganh_hoc (id, ma_nganh, ten_nganh, khoa_id, tong_tin_chi_yeu_cau) VALUES
(4, '7480109', 'Khoa Học Dữ Liệu', 1, 123)
ON DUPLICATE KEY UPDATE 
    ma_nganh = '7480109',
    ten_nganh = 'Khoa Học Dữ Liệu',
    tong_tin_chi_yeu_cau = 123;

-- Cập nhật Lớp Danh Nghĩa 15DHKHDL03 theo ngành Khoa Học Dữ Liệu
UPDATE lop_danh_nghia 
SET nganh_hoc_id = 4, ten_lop = '15DHKHDL03' 
WHERE id = 2 OR ma_lop = '15DHKHDL03';

-- Cập nhật Sinh viên Nguyễn Minh Khoa sang Lớp 15DHKHDL03 và Ngành Khoa Học Dữ Liệu
UPDATE sinh_vien 
SET lop_danh_nghia_id = 2, nganh_hoc_id = 4, chuyen_nganh_id = 3, tong_tc_tich_luy = 40 
WHERE id = 1 OR mssv = '2180601234';

-- 2. THÊM TẤT CẢ CÁC HỌC PHẦN THEO ĐỀ ÁN HUIT KHOA HỌC DỮ LIỆU
INSERT INTO hoc_phan (id, ma_hp, ten_hp, so_tc_ly_thuyet, so_tc_thuc_hanh, tong_tin_chi, he_so_hoc_phi, ty_le_chuyen_can, ty_le_qua_trinh, ty_le_thuc_hanh, ty_le_cuoi_ky, khoa_id) VALUES
-- Học kỳ 1
(10, 'IT001', 'Nhập môn Lập trình', 3, 1, 4, 1.0, 0.10, 0.20, 0.20, 0.50, 1),
(11, 'MA006', 'Giải tích', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(12, 'MA003', 'Đại số tuyến tính', 3, 0, 3, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(13, 'IT010', 'Tổ chức và cấu trúc máy tính', 2, 0, 2, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(14, 'IT009', 'Giới thiệu ngành KHDL', 2, 0, 2, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(15, 'EN004', 'Anh văn 1', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(16, 'PE001', 'Giáo dục thể chất 1', 0, 0, 0, 1.0, 0.20, 0.40, 0.00, 0.40, 1),
(17, 'ME001', 'Giáo dục quốc phòng', 0, 0, 0, 1.0, 0.20, 0.40, 0.00, 0.40, 1),

-- Học kỳ 2
(18, 'SS004', 'Kỹ năng nghề nghiệp', 0, 2, 2, 1.0, 0.10, 0.20, 0.20, 0.50, 1),
(19, 'IT003', 'Cấu trúc dữ liệu và giải thuật', 3, 1, 4, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(20, 'MA004', 'Cấu trúc rời rạc', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(21, 'MA005', 'Xác suất thống kê', 3, 0, 3, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(22, 'EN005', 'Anh văn 2', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(23, 'PE002', 'Giáo dục thể chất 2', 0, 0, 0, 1.0, 0.20, 0.40, 0.00, 0.40, 1),

-- Học kỳ 3
(24, 'IT004', 'Cơ sở dữ liệu', 3, 1, 4, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(25, 'IT005', 'Nhập môn mạng máy tính', 3, 1, 4, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(26, 'IT002', 'Lập trình hướng đối tượng', 3, 1, 4, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(27, 'EN006', 'Anh văn 3', 4, 0, 4, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(28, 'DS101', 'Thống kê và xác suất chuyên sâu', 2, 1, 3, 1.1, 0.10, 0.20, 0.20, 0.50, 1),

-- Học kỳ 4
(29, 'SS001', 'Những nguyên lý cơ bản của CN Mác-Lênin', 5, 0, 5, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(30, 'DS103', 'Thu thập và tiền xử lý dữ liệu', 2, 1, 3, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(31, 'DS106', 'Tối ưu hoá và ứng dụng', 2, 1, 3, 1.1, 0.10, 0.20, 0.20, 0.50, 1),
(32, 'DS102', 'Học máy thống kê', 3, 1, 4, 1.2, 0.10, 0.20, 0.20, 0.50, 1),

-- Học kỳ 5
(33, 'SS002', 'Đường lối cách mạng của ĐCSVN', 3, 0, 3, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(34, 'DS104', 'Tính toán song song & phân tán', 3, 1, 4, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(35, 'DS105', 'Phân tích và trực quan dữ liệu', 2, 1, 3, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(36, 'DS301', 'Các giải thuật khai phá dữ liệu lớn (Tự chọn 1)', 2, 1, 3, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(37, 'IS217', 'Kho dữ liệu và OLAP (Tự do 1)', 3, 0, 3, 1.1, 0.10, 0.30, 0.00, 0.60, 1),

-- Học kỳ 6
(38, 'SS003', 'Tư tưởng Hồ Chí Minh', 2, 0, 2, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(39, 'IS211', 'Cơ sở dữ liệu phân tán', 3, 1, 4, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(40, 'DS200', 'Phân tích dữ liệu lớn', 3, 1, 4, 1.2, 0.10, 0.20, 0.20, 0.50, 1),
(41, 'DS202', 'Đồ án khoa học dữ liệu và ứng dụng 1', 0, 2, 2, 1.3, 0.00, 0.30, 0.30, 0.40, 1),
(42, 'DS300', 'Hệ khuyến nghị (Tự chọn 2)', 2, 1, 3, 1.2, 0.10, 0.20, 0.20, 0.50, 1),

-- Học kỳ 7
(43, 'SS006', 'Pháp luật đại cương', 2, 0, 2, 1.0, 0.10, 0.30, 0.00, 0.60, 1),
(44, 'DS201', 'Deep Learning trong khoa học dữ liệu', 3, 1, 4, 1.3, 0.10, 0.20, 0.20, 0.50, 1),
(45, 'INT701', 'Thực tập doanh nghiệp', 2, 0, 2, 1.0, 0.00, 0.30, 0.00, 0.70, 1),
(46, 'DS203', 'Đồ án khoa học dữ liệu và ứng dụng 2', 0, 2, 2, 1.3, 0.00, 0.30, 0.30, 0.40, 1),
(47, 'CS221', 'Xử lý ngôn ngữ tự nhiên (Tự chọn 3)', 2, 0, 2, 1.2, 0.10, 0.30, 0.00, 0.60, 1),

-- Học kỳ 8
(48, 'KLTN01', 'Khóa luận tốt nghiệp (KHDL HUIT)', 0, 10, 10, 1.5, 0.00, 0.00, 0.00, 1.00, 1)
ON DUPLICATE KEY UPDATE 
    ten_hp = VALUES(ten_hp),
    so_tc_ly_thuyet = VALUES(so_tc_ly_thuyet),
    so_tc_thuc_hanh = VALUES(so_tc_thuc_hanh),
    tong_tin_chi = VALUES(tong_tin_chi);

-- 3. THIẾT LẬP ĐIỀU KIỆN TIÊN QUYẾT CHO CÁC MÔN HỌC
DELETE FROM dieu_kien_hoc_phan WHERE id >= 10;
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

-- 4. THIẾT LẬP CHƯƠNG TRÌNH KHUNG THEO 8 HỌC KỲ (NGÀNH KHOA HỌC DỮ LIỆU - HUIT)
DELETE FROM khung_chuong_trinh WHERE nganh_hoc_id = 4;
INSERT INTO khung_chuong_trinh (nganh_hoc_id, hoc_phan_id, hoc_ky_du_kien, bat_buoc) VALUES
-- Học kỳ 1 (19 TC)
(4, 10, 1, TRUE),
(4, 11, 1, TRUE),
(4, 12, 1, TRUE),
(4, 13, 1, TRUE),
(4, 14, 1, TRUE),
(4, 15, 1, TRUE),
(4, 16, 1, TRUE),
(4, 17, 1, TRUE),

-- Học kỳ 2 (17 TC)
(4, 18, 2, TRUE),
(4, 19, 2, TRUE),
(4, 20, 2, TRUE),
(4, 21, 2, TRUE),
(4, 22, 2, TRUE),
(4, 23, 2, TRUE),

-- Học kỳ 3 (19 TC)
(4, 24, 3, TRUE),
(4, 25, 3, TRUE),
(4, 26, 3, TRUE),
(4, 27, 3, TRUE),
(4, 28, 3, TRUE),

-- Học kỳ 4 (15 TC)
(4, 29, 4, TRUE),
(4, 30, 4, TRUE),
(4, 31, 4, TRUE),
(4, 32, 4, TRUE),

-- Học kỳ 5 (16 TC)
(4, 33, 5, TRUE),
(4, 34, 5, TRUE),
(4, 35, 5, TRUE),
(4, 36, 5, FALSE), -- Tự chọn
(4, 37, 5, FALSE), -- Tự chọn tự do

-- Học kỳ 6 (15 TC)
(4, 38, 6, TRUE),
(4, 39, 6, TRUE),
(4, 40, 6, TRUE),
(4, 41, 6, TRUE),
(4, 42, 6, FALSE), -- Tự chọn

-- Học kỳ 7 (12 TC)
(4, 43, 7, TRUE),
(4, 44, 7, TRUE),
(4, 45, 7, TRUE),
(4, 46, 7, TRUE),
(4, 47, 7, FALSE), -- Tự chọn

-- Học kỳ 8 (10 TC)
(4, 48, 8, TRUE);

-- 5. CẬP NHẬT ĐIỂM HỌC TẬP MẪU THEO CÁC MÔN CỦA HUIT CHO NGUYỄN MINH KHOA
-- Tạo lớp học phần cho kỳ 1 và đăng ký
INSERT INTO lop_hoc_phan (id, ma_lhp, ten_lhp, hoc_phan_id, hoc_ky_id, giang_vien_id, si_so_toi_da, si_so_hien_tai, phong_mac_dinh, trang_thai) VALUES
(10, 'IT001_01', 'Nhập môn Lập trình - 15DHKHDL03', 10, 1, 1, 60, 45, 'A301', 'DA_KET_THUC'),
(11, 'MA006_01', 'Giải tích - 15DHKHDL03', 11, 1, 2, 60, 45, 'B202', 'DA_KET_THUC'),
(12, 'MA003_01', 'Đại số tuyến tính - 15DHKHDL03', 12, 1, 2, 60, 45, 'B203', 'DA_KET_THUC'),
(13, 'IT010_01', 'Tổ chức và cấu trúc máy tính - 15DHKHDL03', 13, 1, 3, 60, 45, 'C101', 'DA_KET_THUC'),
(14, 'IT009_01', 'Giới thiệu ngành KHDL - 15DHKHDL03', 14, 1, 1, 60, 45, 'A201', 'DA_KET_THUC'),
(15, 'EN004_01', 'Anh văn 1 - 15DHKHDL03', 15, 1, 2, 60, 45, 'D102', 'DA_KET_THUC'),
-- Kỳ 2 đang học
(16, 'IT003_01', 'Cấu trúc dữ liệu và giải thuật - 15DHKHDL03', 19, 2, 1, 60, 42, 'A302', 'DANG_MO'),
(17, 'MA005_01', 'Xác suất thống kê - 15DHKHDL03', 21, 2, 2, 60, 42, 'B204', 'DANG_MO'),
(18, 'EN005_01', 'Anh văn 2 - 15DHKHDL03', 22, 2, 3, 60, 42, 'D103', 'DANG_MO')
ON DUPLICATE KEY UPDATE ten_lhp = VALUES(ten_lhp);

-- Đăng ký học phần kỳ 1 cho SV 1
INSERT INTO dang_ky_hoc_phan (id, sinh_vien_id, lop_hoc_phan_id, loai_dang_ky, trang_thai) VALUES
(10, 1, 10, 'HOC_MOI', 'THANH_CONG'),
(11, 1, 11, 'HOC_MOI', 'THANH_CONG'),
(12, 1, 12, 'HOC_MOI', 'THANH_CONG'),
(13, 1, 13, 'HOC_MOI', 'THANH_CONG'),
(14, 1, 14, 'HOC_MOI', 'THANH_CONG'),
(15, 1, 15, 'HOC_MOI', 'THANH_CONG'),
-- Đăng ký kỳ 2 đang học
(16, 1, 16, 'HOC_MOI', 'THANH_CONG'),
(17, 1, 17, 'HOC_MOI', 'THANH_CONG'),
(18, 1, 18, 'HOC_MOI', 'THANH_CONG')
ON DUPLICATE KEY UPDATE trang_thai = VALUES(trang_thai);

-- Điểm học phần kỳ 1
INSERT INTO diem_hoc_phan (id, dang_ky_hoc_phan_id, diem_chuyen_can, diem_qua_trinh, diem_thuc_hanh, diem_cuoi_ky, diem_tong_ket_he10, diem_he4, diem_chu, ket_qua_qua_mon, cam_thi, khoa_diem) VALUES
(10, 10, 10.0, 9.0, 9.5, 8.5, 9.0, 4.0, 'A', TRUE, FALSE, TRUE),
(11, 11, 9.0, 8.5, 0.0, 8.0, 8.3, 3.5, 'B+', TRUE, FALSE, TRUE),
(12, 12, 9.5, 8.0, 0.0, 8.5, 8.6, 4.0, 'A', TRUE, FALSE, TRUE),
(13, 13, 10.0, 8.5, 0.0, 8.0, 8.5, 4.0, 'A', TRUE, FALSE, TRUE),
(14, 14, 9.0, 9.0, 0.0, 9.0, 9.0, 4.0, 'A', TRUE, FALSE, TRUE),
(15, 15, 8.5, 8.0, 0.0, 7.5, 7.8, 3.0, 'B', TRUE, FALSE, TRUE)
ON DUPLICATE KEY UPDATE diem_tong_ket_he10 = VALUES(diem_tong_ket_he10);

-- 6. DỮ LIỆU CÔNG NỢ & CÁC BIÊN LAI HỌC PHÍ ĐIỆN TỬ QUA CÁC HỌC KỲ (HUIT)
-- Công nợ Kỳ 1 (2024-2025)
INSERT INTO cong_no_hoc_ky (id, sinh_vien_id, hoc_ky_id, tong_so_tc, tong_hoc_phi, mien_giam, da_dong, con_no, trang_thai) VALUES
(10, 1, 1, 19, 10450000.0, 1000000.0, 9450000.0, 0.0, 'DA_HOAN_THANH')
ON DUPLICATE KEY UPDATE da_dong = VALUES(da_dong), tong_hoc_phi = VALUES(tong_hoc_phi);

-- Biên lai thu học phí chi tiết cho Sinh viên 1 (Nguyễn Minh Khoa)
INSERT INTO phieu_thu_hoc_phi (id, cong_no_hoc_ky_id, ma_phieu_thu, so_tien, phuong_thuc, ma_giao_dich, ngay_thu, nguoi_thu, ghi_chu) VALUES
(10, 10, 'PT-HUIT-20241-001', 5000000.0, 'CHUYEN_KHOAN', 'MB9921827', '2024-08-20 09:30:15', 'Phòng Kế Toán HUIT', 'Đóng học phí đợt 1 - Học kỳ 1 (2024-2025)'),
(11, 10, 'PT-HUIT-20241-002', 4450000.0, 'VIETQR', 'VQR-MB-778129', '2024-09-05 14:15:20', 'Cổng thanh toán tự động VietQR Napas 247', 'Đóng phần học phí còn lại - Học kỳ 1 (2024-2025)'),
(12, 1, 'PT-HUIT-20242-001', 3630000.0, 'VIETQR', 'TXN99281749', '2025-01-15 10:22:00', 'Cổng thanh toán tự động VietQR Napas 247', 'Thanh toán học phí tín chỉ Học kỳ 2 (2024-2025)')
ON DUPLICATE KEY UPDATE so_tien = VALUES(so_tien), ma_phieu_thu = VALUES(ma_phieu_thu);
