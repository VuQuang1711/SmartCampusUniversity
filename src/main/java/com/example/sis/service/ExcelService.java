package com.example.sis.service;

import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {

    @Autowired private SinhVienRepository sinhVienRepository;
    @Autowired private NguoiDungRepository nguoiDungRepository;
    @Autowired private LopDanhNghiaRepository lopDanhNghiaRepository;
    @Autowired private NganhHocRepository nganhHocRepository;
    @Autowired private DiemHocPhanRepository diemHocPhanRepository;
    @Autowired private LopHocPhanRepository lopHocPhanRepository;
    @Autowired private DangKyHocPhanRepository dangKyHocPhanRepository;
    @Autowired private CongNoHocKyRepository congNoHocKyRepository;
    @Autowired private GradingService gradingService;

    /**
     * IMPORT TÂN SINH VIÊN TỪ EXCEL VÀ TỰ ĐỘNG PHÂN BỔ VÀO LỚP DANH NGHĨA
     * Định dạng cột Excel: MSSV | Họ Tên | Email | Số ĐT | Ngày Sinh (yyyy-MM-dd) | Giới Tính | Mã Ngành | Mã Lớp
     */
    @Transactional
    public List<SinhVien> importStudents(MultipartFile file) throws IOException {
        List<SinhVien> importedList = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String mssv = formatter.formatCellValue(row.getCell(0)).trim();
                String hoTen = formatter.formatCellValue(row.getCell(1)).trim();
                String email = formatter.formatCellValue(row.getCell(2)).trim();
                String phone = formatter.formatCellValue(row.getCell(3)).trim();
                String dobStr = formatter.formatCellValue(row.getCell(4)).trim();
                String gender = formatter.formatCellValue(row.getCell(5)).trim();
                String maNganh = formatter.formatCellValue(row.getCell(6)).trim();
                String maLop = formatter.formatCellValue(row.getCell(7)).trim();

                if (mssv.isEmpty() || hoTen.isEmpty()) continue;

                NganhHoc nganh = nganhHocRepository.findByMaNganh(maNganh)
                        .orElse(nganhHocRepository.findAll().stream().findFirst().orElse(null));
                if (nganh == null) continue;

                LopDanhNghia lop = lopDanhNghiaRepository.findByMaLop(maLop).orElse(null);
                if (lop == null) {
                    String finalMaLop = maLop.isEmpty() ? "15DHKHDL01" : maLop;
                    lop = LopDanhNghia.builder()
                            .maLop(finalMaLop)
                            .tenLop(finalMaLop)
                            .nganhHoc(nganh)
                            .nienKhoa("2024-2028")
                            .build();
                    lop = lopDanhNghiaRepository.save(lop);
                }

                // Tạo hoặc cập nhật Người dùng
                NguoiDung user = nguoiDungRepository.findByMaDinhDanh(mssv).orElse(null);
                if (user == null) {
                    user = NguoiDung.builder()
                            .maDinhDanh(mssv)
                            .hoTen(hoTen)
                            .email(email.isEmpty() ? mssv + "@sinhvien.sis.edu.vn" : email)
                            .matKhau("123456")
                            .soDienThoai(phone)
                            .vaiTro(VaiTro.SINH_VIEN)
                            .build();
                    user = nguoiDungRepository.save(user);
                }

                // Tạo Sinh viên
                SinhVien sv = sinhVienRepository.findByMssv(mssv).orElse(null);
                LocalDate dob = LocalDate.of(2004, 1, 1);
                try {
                    if (!dobStr.isEmpty()) dob = LocalDate.parse(dobStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } catch (Exception ignored) {}

                if (sv == null) {
                    sv = SinhVien.builder()
                            .mssv(mssv)
                            .lopDanhNghia(lop)
                            .nganhHoc(nganh)
                            .khoaTuyenSinh(2024)
                            .ngaySinh(dob)
                            .gioiTinh(gender.isEmpty() ? "Nam" : gender)
                            .trangThaiHocVu("BINH_THUONG")
                            .cpaTichLuy(0.0)
                            .tongTcTichLuy(0)
                            .nguoiDung(user)
                            .build();
                    sv = sinhVienRepository.save(sv);
                    importedList.add(sv);
                }
            }
        }
        return importedList;
    }

    /**
     * XUẤT BẢNG ĐIỂM LỚP HỌC PHẦN RA FILE EXCEL
     */
    public byte[] exportClassGrades(Long lopHocPhanId) throws IOException {
        LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học phần"));

        List<DiemHocPhan> gradeList = diemHocPhanRepository.findByLopHocPhanId(lopHocPhanId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BangDiem_" + lhp.getMaLHP());

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Row Header
            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "MSSV", "Họ và Tên", "Lớp Danh Nghĩa", "Chuyên Cần (10%)", "Quá Trình (20%)", "Thực Hành (20%)", "Cuối Kỳ (50%)", "Tổng Kết Hệ 10", "Thang 4", "Điểm Chữ", "Cấm Thi", "Kết Quả"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (DiemHocPhan d : gradeList) {
                Row row = sheet.createRow(rowIdx++);
                SinhVien sv = d.getDangKyHocPhan().getSinhVien();

                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(sv.getMssv());
                row.createCell(2).setCellValue(sv.getNguoiDung().getHoTen());
                row.createCell(3).setCellValue(sv.getLopDanhNghia() != null ? sv.getLopDanhNghia().getMaLop() : "");
                row.createCell(4).setCellValue(d.getDiemChuyenCan() != null ? d.getDiemChuyenCan() : 0.0);
                row.createCell(5).setCellValue(d.getDiemQuaTrinh() != null ? d.getDiemQuaTrinh() : 0.0);
                row.createCell(6).setCellValue(d.getDiemThucHanh() != null ? d.getDiemThucHanh() : 0.0);
                row.createCell(7).setCellValue(d.getDiemCuoiKy() != null ? d.getDiemCuoiKy() : 0.0);
                row.createCell(8).setCellValue(d.getDiemTongKetHe10() != null ? d.getDiemTongKetHe10() : 0.0);
                row.createCell(9).setCellValue(d.getDiemHe4() != null ? d.getDiemHe4() : 0.0);
                row.createCell(10).setCellValue(d.getDiemChu() != null ? d.getDiemChu() : "");
                row.createCell(11).setCellValue(Boolean.TRUE.equals(d.getCamThi()) ? "CẤM THI" : "Bình thường");
                row.createCell(12).setCellValue(Boolean.TRUE.equals(d.getKetQuaQuaMon()) ? "ĐẠT" : "RỚT");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * XUẤT DANH SÁCH SINH VIÊN & PHIẾU ĐIỂM DANH LỚP HỌC PHẦN (.xlsx)
     */
    public byte[] exportClassAttendanceRoster(Long lopHocPhanId) throws IOException {
        LopHocPhan lhp = lopHocPhanRepository.findById(lopHocPhanId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học phần"));
        List<DangKyHocPhan> enrollments = dangKyHocPhanRepository.findByLopHocPhanId(lopHocPhanId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("DiemDanh_" + lhp.getMaLHP());

            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "MSSV", "Họ và Tên", "Lớp Danh Nghĩa", "Email", "Số Điện Thoại", "Loại ĐK", "Buổi 1", "Buổi 2", "Buổi 3", "Buổi 4", "Buổi 5", "Ghi Chú"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (DangKyHocPhan dk : enrollments) {
                if (dk.getTrangThai() != DangKyHocPhan.TrangThaiDangKy.THANH_CONG) continue;
                SinhVien sv = dk.getSinhVien();
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(sv.getMssv());
                row.createCell(2).setCellValue(sv.getNguoiDung().getHoTen());
                row.createCell(3).setCellValue(sv.getLopDanhNghia() != null ? sv.getLopDanhNghia().getMaLop() : "");
                row.createCell(4).setCellValue(sv.getNguoiDung().getEmail());
                row.createCell(5).setCellValue(sv.getNguoiDung().getSoDienThoai() != null ? sv.getNguoiDung().getSoDienThoai() : "");
                row.createCell(6).setCellValue(dk.getLoaiDangKy().name());
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * IMPORT ĐIỂM TỪ FILE EXCEL (.xlsx) VÀO LỚP HỌC PHẦN
     * Cột: MSSV (0 hoặc 1 nếu có STT) -> tự nhận diện theo cột MSSV, CC, QT, TH, CK
     */
    @Transactional
    public int importClassGrades(Long lopHocPhanId, MultipartFile file) throws IOException {
        List<DiemHocPhan> gradeList = diemHocPhanRepository.findByLopHocPhanId(lopHocPhanId);
        java.util.Map<String, DiemHocPhan> byMssv = new java.util.HashMap<>();
        for (DiemHocPhan d : gradeList) {
            if (!Boolean.TRUE.equals(d.getKhoaDiem())) {
                byMssv.put(d.getDangKyHocPhan().getSinhVien().getMssv().trim(), d);
            }
        }

        DataFormatter formatter = new DataFormatter();
        int updatedCount = 0;

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Hỗ trợ cả định dạng xuất từ hệ thống (cột 1 = MSSV, 4=CC, 5=QT, 6=TH, 7=CK)
                // và định dạng rút gọn (cột 0 = MSSV, 1=CC, 2=QT, 3=TH, 4=CK)
                String c0 = formatter.formatCellValue(row.getCell(0)).trim();
                String c1 = formatter.formatCellValue(row.getCell(1)).trim();
                DiemHocPhan target = byMssv.get(c0);
                int offset = 1;
                if (target == null) {
                    target = byMssv.get(c1);
                    offset = 4;
                }
                if (target == null) continue;

                Double cc = parseScore(formatter.formatCellValue(row.getCell(offset)));
                Double qt = parseScore(formatter.formatCellValue(row.getCell(offset + 1)));
                Double th = parseScore(formatter.formatCellValue(row.getCell(offset + 2)));
                Double ck = parseScore(formatter.formatCellValue(row.getCell(offset + 3)));

                if (cc != null) target.setDiemChuyenCan(cc);
                if (qt != null) target.setDiemQuaTrinh(qt);
                if (th != null) target.setDiemThucHanh(th);
                if (ck != null && !Boolean.TRUE.equals(target.getCamThi())) target.setDiemCuoiKy(ck);

                gradingService.calculateComponentGrades(target);
                updatedCount++;
            }
        }
        return updatedCount;
    }

    private Double parseScore(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            double v = Double.parseDouble(raw.trim().replace(",", "."));
            return Math.max(0.0, Math.min(10.0, v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * XUẤT BÁO CÁO DOANH THU HỌC PHÍ THEO HỌC KỲ (.xlsx)
     */
    public byte[] exportTuitionRevenueReport(Long hocKyId) throws IOException {
        List<CongNoHocKy> debts = hocKyId != null ? congNoHocKyRepository.findByHocKyId(hocKyId) : congNoHocKyRepository.findAll();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("BaoCaoDoanhThuHocPhi");
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"STT", "MSSV", "Họ và Tên", "Lớp", "Ngành", "Học Kỳ", "Số TC", "Tổng Học Phí", "Miễn Giảm", "Đã Thu", "Còn Nợ", "Trạng Thái"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            double sumTotal = 0, sumPaid = 0, sumDebt = 0;
            for (CongNoHocKy cn : debts) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(cn.getSinhVien().getMssv());
                row.createCell(2).setCellValue(cn.getSinhVien().getNguoiDung().getHoTen());
                row.createCell(3).setCellValue(cn.getSinhVien().getLopDanhNghia() != null ? cn.getSinhVien().getLopDanhNghia().getMaLop() : "");
                row.createCell(4).setCellValue(cn.getSinhVien().getNganhHoc().getTenNganh());
                row.createCell(5).setCellValue(cn.getHocKy().getMaHocKy());
                row.createCell(6).setCellValue(cn.getTongSoTC() != null ? cn.getTongSoTC() : 0);
                double t = cn.getTongHocPhi() != null ? cn.getTongHocPhi() : 0;
                double m = cn.getMienGiam() != null ? cn.getMienGiam() : 0;
                double p = cn.getDaDong() != null ? cn.getDaDong() : 0;
                double d = cn.getConNo() != null ? cn.getConNo() : 0;
                row.createCell(7).setCellValue(t);
                row.createCell(8).setCellValue(m);
                row.createCell(9).setCellValue(p);
                row.createCell(10).setCellValue(d);
                row.createCell(11).setCellValue(cn.getTrangThai().name());
                sumTotal += t; sumPaid += p; sumDebt += d;
            }
            Row totalRow = sheet.createRow(rowIdx + 1);
            totalRow.createCell(2).setCellValue("TỔNG CỘNG");
            totalRow.createCell(7).setCellValue(sumTotal);
            totalRow.createCell(9).setCellValue(sumPaid);
            totalRow.createCell(10).setCellValue(sumDebt);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(out);
            return out.toByteArray();
        }
    }
}
