package com.example.sis.service;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ClassDatabaseImportService {

    private static final DateTimeFormatter VN_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final HocKyRepository hocKyRepository;
    private final HocPhanRepository hocPhanRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final LichHocRepository lichHocRepository;
    private final LichHocTuanRepository lichHocTuanRepository;
    private final GiangVienRepository giangVienRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final KhoaRepository khoaRepository;
    private final NganhHocRepository nganhHocRepository;
    private final LopDanhNghiaRepository lopDanhNghiaRepository;
    private final CoSoRepository coSoRepository;
    private final PhongHocRepository phongHocRepository;

    public List<Map<String, Object>> listLocalExcelFiles() {
        List<Map<String, Object>> files = new ArrayList<>();
        Path dir = Paths.get("class_database");
        if (!Files.exists(dir) || !Files.isDirectory(dir)) return files;
        File[] list = dir.toFile().listFiles((d, name) -> {
            String low = name.toLowerCase(Locale.ROOT);
            return low.endsWith(".xls") || low.endsWith(".xlsx");
        });
        if (list == null) return files;
        Arrays.sort(list, Comparator.comparing(File::getName));
        for (File f : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", f.getName());
            item.put("sizeKb", Math.max(1, f.length() / 1024));
            files.add(item);
        }
        return files;
    }

    /**
     * Đồng bộ toàn bộ dữ liệu thực tế từ tất cả file .xls/.xlsx trong class_database/
     * vào CSDL chuẩn hóa 3NF (co_so, phong_hoc, lop_danh_nghia, hoc_ky, hoc_phan, giang_vien,
     * lop_hoc_phan, lich_hoc, lich_hoc_tuan).
     */
    @Transactional
    public Map<String, Object> syncAllRealHuitData(String filterPrefix) throws Exception {
        List<Map<String, Object>> files = listLocalExcelFiles();
        int totalHp = 0, totalLhp = 0, totalLich = 0, totalGv = 0, totalCoSo = 0, totalPhong = 0, totalTuan = 0;
        List<String> importedSemesters = new ArrayList<>();

        String effectiveFilter = (filterPrefix == null || filterPrefix.isBlank()) ? "15DHKHDL" : filterPrefix.trim();

        for (Map<String, Object> f : files) {
            String name = String.valueOf(f.get("name"));
            Map<String, Object> res = importFromLocalFile(name, effectiveFilter, 500);
            totalHp += ((Number) res.getOrDefault("createdHocPhan", 0)).intValue();
            totalLhp += ((Number) res.getOrDefault("createdLopHp", 0)).intValue();
            totalLich += ((Number) res.getOrDefault("createdLichHoc", 0)).intValue();
            totalGv += ((Number) res.getOrDefault("createdGiangVien", 0)).intValue();
            totalCoSo += ((Number) res.getOrDefault("createdCoSo", 0)).intValue();
            totalPhong += ((Number) res.getOrDefault("createdPhongHoc", 0)).intValue();
            totalTuan += ((Number) res.getOrDefault("syncedLichHocTuan", 0)).intValue();
            importedSemesters.add(String.valueOf(res.get("maHocKy")));
        }

        // Đặt học kỳ thực tế mới nhất (2026_2) làm học kỳ DANG_MO duy nhất cho ĐKHP
        List<HocKy> allSemesters = hocKyRepository.findAll();
        HocKy newestReal = allSemesters.stream()
                .filter(hk -> hk.getMaHocKy() != null && !hk.getMaHocKy().startsWith("2024_"))
                .max(Comparator.comparing(HocKy::getMaHocKy))
                .orElse(null);

        if (newestReal != null) {
            for (HocKy hk : allSemesters) {
                if (hk.getId().equals(newestReal.getId())) {
                    hk.setTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO);
                    hk.setDonGiaTinChi(570000.0);
                    hocKyRepository.save(hk);
                } else if (hk.getTrangThaiDangKy() == HocKy.TrangThaiDangKy.DANG_MO) {
                    hk.setTrangThaiDangKy(HocKy.TrangThaiDangKy.DA_KHOA);
                    hocKyRepository.save(hk);
                }
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("filesProcessed", files.size());
        summary.put("semesters", importedSemesters);
        summary.put("activeSemester", newestReal != null ? newestReal.getTenHocKy() : "N/A");
        summary.put("createdCoSo", totalCoSo);
        summary.put("createdPhongHoc", totalPhong);
        summary.put("createdHocPhan", totalHp);
        summary.put("createdLopHp", totalLhp);
        summary.put("createdLichHoc", totalLich);
        summary.put("syncedLichHocTuan", totalTuan);
        summary.put("createdGiangVien", totalGv);
        return summary;
    }

    @Transactional
    public Map<String, Object> importFromLocalFile(String fileName, String filterLopPrefix, int maxClasses) throws Exception {
        Path filePath = Paths.get("class_database").resolve(fileName).normalize();
        if (!filePath.startsWith(Paths.get("class_database").normalize()) || !Files.exists(filePath)) {
            throw new IllegalArgumentException("Không tìm thấy file trong thư mục class_database: " + fileName);
        }
        try (InputStream is = new FileInputStream(filePath.toFile())) {
            return importFromStream(is, fileName, filterLopPrefix, maxClasses);
        }
    }

    @Transactional
    public Map<String, Object> importFromMultipart(MultipartFile file, String filterLopPrefix, int maxClasses) throws Exception {
        try (InputStream is = file.getInputStream()) {
            return importFromStream(is, file.getOriginalFilename(), filterLopPrefix, maxClasses);
        }
    }

    private Map<String, Object> importFromStream(InputStream is, String fileName, String filterLopPrefix, int maxClasses) throws Exception {
        DataFormatter formatter = new DataFormatter();
        int createdHocPhan = 0;
        int createdLopHp = 0;
        int createdLichHoc = 0;
        int createdGiangVien = 0;
        int createdCoSo = 0;
        int createdPhongHoc = 0;
        int syncedLichHocTuan = 0;
        int skippedRows = 0;

        String filterNorm = filterLopPrefix != null ? filterLopPrefix.trim().toUpperCase(Locale.ROOT) : "";
        int limit = maxClasses <= 0 ? 500 : maxClasses;

        Khoa defaultKhoa = khoaRepository.findAll().stream().findFirst().orElse(null);
        NganhHoc khdlNganh = nganhHocRepository.findByMaNganh("7480109")
                .orElse(nganhHocRepository.findAll().stream().findFirst().orElse(null));

        try (Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            String title = extractTitle(sheet, formatter);
            HocKy hocKy = resolveOrCreateHocKy(title, fileName);

            int headerRowIdx = -1;
            for (int r = 0; r <= Math.min(15, sheet.getLastRowNum()); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < Math.min(10, row.getLastCellNum()); c++) {
                    String val = formatter.formatCellValue(row.getCell(c)).trim().toLowerCase(Locale.ROOT);
                    if (val.contains("mã lhp") || val.equals("ma lhp")) {
                        headerRowIdx = r;
                        break;
                    }
                }
                if (headerRowIdx != -1) break;
            }
            if (headerRowIdx == -1) {
                throw new IllegalArgumentException("Không nhận diện được dòng tiêu đề 'Mã LHP' trong file Excel.");
            }

            Row mainHeader = sheet.getRow(headerRowIdx);
            Row subHeader = sheet.getRow(headerRowIdx + 1);
            ColumnMap colMap = detectColumns(mainHeader, subHeader, formatter);

            // Memory caches
            Map<String, HocPhan> hpCache = new HashMap<>();
            for (HocPhan hp : hocPhanRepository.findAll()) hpCache.put(hp.getMaHP(), hp);

            Map<String, LopHocPhan> lhpCache = new HashMap<>();
            for (LopHocPhan lhp : lopHocPhanRepository.findByHocKyId(hocKy.getId())) lhpCache.put(lhp.getMaLHP(), lhp);

            Map<String, GiangVien> gvCache = new HashMap<>();
            for (GiangVien gv : giangVienRepository.findAll()) gvCache.put(gv.getMaGV(), gv);

            Map<String, CoSo> coSoCache = new HashMap<>();
            for (CoSo cs : coSoRepository.findAll()) coSoCache.put(cs.getMaCoSo(), cs);

            Map<String, PhongHoc> phongCache = new HashMap<>();
            for (PhongHoc ph : phongHocRepository.findAll()) phongCache.put(ph.getMaPhong(), ph);

            Map<String, LopDanhNghia> ldnCache = new HashMap<>();
            for (LopDanhNghia ldn : lopDanhNghiaRepository.findAll()) ldnCache.put(ldn.getMaLop(), ldn);

            int dataStartRow = headerRowIdx + 2;
            Map<String, LopHocPhan> activeLhpByRawCode = new LinkedHashMap<>();
            Map<String, MergedSchedule> mergedSchedules = new LinkedHashMap<>();

            for (int r = dataStartRow; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String maLhp = getCell(row, colMap.maLhp, formatter);
                String tenMon = getCell(row, colMap.tenMon, formatter);
                if (maLhp.isEmpty() || tenMon.isEmpty()) continue;

                String lopHoc = getCell(row, colMap.lopHoc, formatter);
                if (!filterNorm.isEmpty()
                        && !lopHoc.toUpperCase(Locale.ROOT).contains(filterNorm)
                        && !tenMon.toUpperCase(Locale.ROOT).contains(filterNorm)
                        && !maLhp.toUpperCase(Locale.ROOT).contains(filterNorm)) {
                    skippedRows++;
                    continue;
                }

                if (!activeLhpByRawCode.containsKey(maLhp) && activeLhpByRawCode.size() >= limit) {
                    break;
                }

                int soTc = parseIntSafe(getCell(row, colMap.soTc, formatter), 3);
                int soTietDot = parseIntSafe(getCell(row, colMap.soTietDot, formatter), 30);
                int tongSoTiet = parseIntSafe(getCell(row, colMap.tongSoTiet, formatter), soTietDot);
                String tinhChatHp = getCell(row, colMap.tinhChatHp, formatter);
                if (tinhChatHp.isEmpty()) tinhChatHp = "TRUC_TIEP";
                if (tinhChatHp.length() > 50) tinhChatHp = tinhChatHp.substring(0, 50);

                LocalDate ngayBd = parseDateSafe(getCell(row, colMap.batDau, formatter));
                LocalDate ngayKt = parseDateSafe(getCell(row, colMap.ketThuc, formatter));

                String maHp = maLhp.length() >= 10 ? maLhp.substring(0, 10) : maLhp;
                String loaiLichStr = getCell(row, colMap.loaiLich, formatter).toUpperCase(Locale.ROOT);
                String phongRaw = getCell(row, colMap.phong, formatter);
                boolean isThucHanh = loaiLichStr.contains("TH")
                        || tenMon.toLowerCase(Locale.ROOT).startsWith("thực hành");

                // 0. Chuẩn hóa Cơ sở (co_so) & Phòng học (phong_hoc)
                ParsedRoom parsedRoom = parseRoomAndCampus(phongRaw);
                CoSo coSo = null;
                PhongHoc phongEntity = null;
                if (parsedRoom != null) {
                    coSo = coSoCache.get(parsedRoom.maCoSo);
                    if (coSo == null) {
                        coSo = CoSo.builder()
                                .maCoSo(parsedRoom.maCoSo)
                                .tenCoSo(parsedRoom.tenCoSo)
                                .diaChi(parsedRoom.diaChi)
                                .build();
                        coSo = coSoRepository.save(coSo);
                        coSoCache.put(coSo.getMaCoSo(), coSo);
                        createdCoSo++;
                    }
                    phongEntity = phongCache.get(parsedRoom.maPhong);
                    if (phongEntity == null) {
                        phongEntity = PhongHoc.builder()
                                .maPhong(parsedRoom.maPhong)
                                .tenHienThi(parsedRoom.tenHienThi)
                                .loaiPhong(parsedRoom.loaiPhong)
                                .sucChua(parsedRoom.loaiPhong == PhongHoc.LoaiPhong.ONLINE ? 200 : 60)
                                .coSo(coSo)
                                .build();
                        phongEntity = phongHocRepository.save(phongEntity);
                        phongCache.put(phongEntity.getMaPhong(), phongEntity);
                        createdPhongHoc++;
                    }
                }
                String phongClean = formatRoom(phongRaw);

                // 0.5. Chuẩn hóa Lớp danh nghĩa (lop_danh_nghia) từ cột "Lớp học"
                LopDanhNghia lopDanhNghia = null;
                String lopCode = lopHoc.length() > 30 ? lopHoc.substring(0, 30) : lopHoc;
                if (!lopCode.isEmpty() && khdlNganh != null) {
                    lopDanhNghia = ldnCache.get(lopCode);
                    if (lopDanhNghia == null) {
                        lopDanhNghia = LopDanhNghia.builder()
                                .maLop(lopCode)
                                .tenLop(lopCode)
                                .nienKhoa(lopCode.startsWith("15") ? "2023-2027" : "2024-2028")
                                .nganhHoc(khdlNganh)
                                .build();
                        lopDanhNghia = lopDanhNghiaRepository.save(lopDanhNghia);
                        ldnCache.put(lopCode, lopDanhNghia);
                    }
                }

                // 1. Học phần (hoc_phan)
                HocPhan hp = hpCache.get(maHp);
                if (hp == null) {
                    hp = HocPhan.builder()
                            .maHP(maHp)
                            .tenHP(tenMon.length() > 150 ? tenMon.substring(0, 150) : tenMon)
                            .soTCLyThuyet(isThucHanh ? 0 : soTc)
                            .soTCThucHanh(isThucHanh ? soTc : 0)
                            .tongTinChi(soTc)
                            .tongSoTiet(tongSoTiet)
                            .tinhChatHp(tinhChatHp)
                            .khoa(defaultKhoa)
                            .build();
                    hp = hocPhanRepository.save(hp);
                    hpCache.put(maHp, hp);
                    createdHocPhan++;
                } else if (hp.getTongSoTiet() == null || (!"TRUC_TIEP".equals(tinhChatHp) && "TRUC_TIEP".equals(hp.getTinhChatHp()))) {
                    hp.setTongSoTiet(tongSoTiet);
                    if (!"TRUC_TIEP".equals(tinhChatHp)) hp.setTinhChatHp(tinhChatHp);
                    hp = hocPhanRepository.save(hp);
                }

                // 2. Giảng viên (giang_vien + nguoi_dung)
                GiangVien gv = null;
                String gvRaw = getCell(row, colMap.giangVien, formatter);
                if (!gvRaw.isEmpty() && defaultKhoa != null) {
                    ParsedLecturer parsedGv = parseLecturer(gvRaw);
                    if (parsedGv != null && !parsedGv.maGv.isEmpty()) {
                        gv = gvCache.get(parsedGv.maGv);
                        if (gv == null) {
                            NguoiDung nd = nguoiDungRepository.findByMaDinhDanh(parsedGv.maGv).orElse(null);
                            if (nd == null) {
                                nd = NguoiDung.builder()
                                        .maDinhDanh(parsedGv.maGv)
                                        .hoTen(parsedGv.hoTen)
                                        .email(buildLecturerEmail(parsedGv.hoTen, parsedGv.maGv))
                                        .matKhau("123456")
                                        .vaiTro(VaiTro.GIANG_VIEN)
                                        .build();
                                nd = nguoiDungRepository.save(nd);
                            } else if (nd.getHoTen() == null || nd.getHoTen().equals(parsedGv.maGv)) {
                                nd.setHoTen(parsedGv.hoTen);
                                nd = nguoiDungRepository.save(nd);
                            }
                            gv = giangVienRepository.findByNguoiDungId(nd.getId()).orElse(null);
                            if (gv == null) {
                                gv = GiangVien.builder()
                                        .maGV(parsedGv.maGv)
                                        .hocHamHocVi(parsedGv.hocHamHocVi)
                                        .chuyenMon(tenMon.length() > 100 ? tenMon.substring(0, 100) : tenMon)
                                        .nguoiDung(nd)
                                        .khoa(defaultKhoa)
                                        .build();
                                gv = giangVienRepository.save(gv);
                                createdGiangVien++;
                            }
                            gvCache.put(parsedGv.maGv, gv);
                        } else {
                            boolean changed = false;
                            if (gv.getNguoiDung() != null
                                    && (gv.getNguoiDung().getHoTen() == null || gv.getNguoiDung().getHoTen().equals(gv.getMaGV()))
                                    && !parsedGv.hoTen.equals(parsedGv.maGv)) {
                                gv.getNguoiDung().setHoTen(parsedGv.hoTen);
                                nguoiDungRepository.save(gv.getNguoiDung());
                            }
                            if ("GV.".equals(gv.getHocHamHocVi()) || "ThS.".equals(gv.getHocHamHocVi())) {
                                gv.setHocHamHocVi(parsedGv.hocHamHocVi);
                                changed = true;
                            }
                            if (gv.getChuyenMon() == null || gv.getChuyenMon().isBlank()) {
                                gv.setChuyenMon(tenMon.length() > 100 ? tenMon.substring(0, 100) : tenMon);
                                changed = true;
                            }
                            if (changed) giangVienRepository.save(gv);
                        }
                    }
                }

                // 3. Lớp học phần (lop_hoc_phan)
                int thu = parseIntSafe(getCell(row, colMap.thu, formatter), 2);
                if (thu < 2 || thu > 8) thu = 2;
                int tuTiet = parseIntSafe(getCell(row, colMap.tuTiet, formatter), 1);
                int denTiet = parseIntSafe(getCell(row, colMap.denTiet, formatter), tuTiet + 2);
                int soTietBuoi = Math.max(1, denTiet - tuTiet + 1);

                LopHocPhan lhp = activeLhpByRawCode.get(maLhp);
                if (lhp == null) {
                    String uniqueMaLhp = maLhp.length() > 45 ? maLhp.substring(0, 45) : maLhp;
                    lhp = lhpCache.get(uniqueMaLhp);
                    if (lhp == null) {
                        Optional<LopHocPhan> existingGlobal = lopHocPhanRepository.findByMaLHP(uniqueMaLhp);
                        if (existingGlobal.isPresent() && !existingGlobal.get().getHocKy().getId().equals(hocKy.getId())) {
                            uniqueMaLhp = uniqueMaLhp + "_" + hocKy.getMaHocKy();
                            if (uniqueMaLhp.length() > 50) uniqueMaLhp = uniqueMaLhp.substring(0, 50);
                            lhp = lhpCache.get(uniqueMaLhp);
                        } else if (existingGlobal.isPresent()) {
                            lhp = existingGlobal.get();
                        }
                    }

                    String lopShort = lopHoc.length() > 60 ? lopHoc.substring(0, 60) : lopHoc;
                    if (lhp == null) {
                        String tenLhp = lopShort.isEmpty() ? tenMon : (tenMon + " - " + lopShort);
                        if (tenLhp.length() > 150) tenLhp = tenLhp.substring(0, 150);
                        lhp = LopHocPhan.builder()
                                .maLHP(uniqueMaLhp)
                                .tenLHP(tenLhp)
                                .lopDuKien(lopShort)
                                .lopDanhNghia(lopDanhNghia)
                                .tinhChatHp(tinhChatHp)
                                .tongSoTiet(tongSoTiet)
                                .ngayBatDau(ngayBd)
                                .ngayKetThuc(ngayKt)
                                .hocPhan(hp)
                                .hocKy(hocKy)
                                .giangVien(gv)
                                .siSoToiDa(60)
                                .siSoHienTai(0)
                                .phongMacDinh(phongClean)
                                .thuDuKien(thu)
                                .tietBatDauDuKien(tuTiet)
                                .soTietDuKien(soTietBuoi)
                                .trangThai(LopHocPhan.TrangThaiLHP.DANG_MO)
                                .build();
                        lhp = lopHocPhanRepository.save(lhp);
                        lhpCache.put(uniqueMaLhp, lhp);
                        createdLopHp++;
                    } else {
                        boolean dirty = false;
                        if (gv != null && (lhp.getGiangVien() == null || !lhp.getGiangVien().getId().equals(gv.getId()))) {
                            lhp.setGiangVien(gv);
                            dirty = true;
                        }
                        if (!lopShort.isEmpty() && (lhp.getLopDuKien() == null || lhp.getLopDuKien().isBlank())) {
                            lhp.setLopDuKien(lopShort);
                            dirty = true;
                        }
                        if (lopDanhNghia != null && lhp.getLopDanhNghia() == null) {
                            lhp.setLopDanhNghia(lopDanhNghia);
                            dirty = true;
                        }
                        if (lhp.getTinhChatHp() == null) {
                            lhp.setTinhChatHp(tinhChatHp);
                            dirty = true;
                        }
                        if (lhp.getTongSoTiet() == null) {
                            lhp.setTongSoTiet(tongSoTiet);
                            dirty = true;
                        }
                        if (ngayBd != null && (lhp.getNgayBatDau() == null || ngayBd.isBefore(lhp.getNgayBatDau()))) {
                            lhp.setNgayBatDau(ngayBd);
                            dirty = true;
                        }
                        if (ngayKt != null && (lhp.getNgayKetThuc() == null || ngayKt.isAfter(lhp.getNgayKetThuc()))) {
                            lhp.setNgayKetThuc(ngayKt);
                            dirty = true;
                        }
                        if (dirty) lhp = lopHocPhanRepository.save(lhp);
                    }
                    activeLhpByRawCode.put(maLhp, lhp);
                } else {
                    boolean dirty = false;
                    if (ngayBd != null && (lhp.getNgayBatDau() == null || ngayBd.isBefore(lhp.getNgayBatDau()))) {
                        lhp.setNgayBatDau(ngayBd);
                        dirty = true;
                    }
                    if (ngayKt != null && (lhp.getNgayKetThuc() == null || ngayKt.isAfter(lhp.getNgayKetThuc()))) {
                        lhp.setNgayKetThuc(ngayKt);
                        dirty = true;
                    }
                    if (dirty) lopHocPhanRepository.save(lhp);
                }

                // 4. Gom nhóm lịch học theo (lhpId, thu, tuTiet, soTietBuoi, loaiLich)
                LichHoc.LoaiLich loaiLich = isThucHanh ? LichHoc.LoaiLich.THUC_HANH : LichHoc.LoaiLich.LY_THUYET;
                final LopHocPhan finalLhp = lhp;
                final int finalThu = thu;
                String schedKey = finalLhp.getId() + "|" + finalThu + "|" + tuTiet + "|" + soTietBuoi + "|" + loaiLich.name();
                MergedSchedule ms = mergedSchedules.computeIfAbsent(schedKey,
                        k -> new MergedSchedule(finalLhp, finalThu, tuTiet, soTietBuoi, loaiLich));
                if (!phongClean.isEmpty()) ms.rooms.add(phongClean);
                if (ms.primaryRoomEntity == null && phongEntity != null) ms.primaryRoomEntity = phongEntity;
                ms.soTietDot = Math.max(ms.soTietDot, soTietDot);
                if (ngayBd != null && (ms.ngayBatDau == null || ngayBd.isBefore(ms.ngayBatDau))) ms.ngayBatDau = ngayBd;
                if (ngayKt != null && (ms.ngayKetThuc == null || ngayKt.isAfter(ms.ngayKetThuc))) ms.ngayKetThuc = ngayKt;

                if (!colMap.weekCols.isEmpty()) {
                    for (Map.Entry<Integer, Integer> entry : colMap.weekCols.entrySet()) {
                        String mark = getCell(row, entry.getValue(), formatter);
                        if (!mark.isEmpty()) {
                            ms.weeks.add(entry.getKey());
                        }
                    }
                }
            }

            // 5. Lưu LichHoc + Chuẩn hóa bảng chi tiết LichHocTuan (T1..T30)
            Map<Long, List<LichHoc>> existingLichByLhp = new HashMap<>();
            for (MergedSchedule ms : mergedSchedules.values()) {
                Long lhpId = ms.lhp.getId();
                List<LichHoc> existingList = existingLichByLhp.computeIfAbsent(lhpId, id -> lichHocRepository.findByLopHocPhanId(id));

                int tuTuan = ms.weeks.isEmpty() ? 1 : ms.weeks.first();
                int denTuan = ms.weeks.isEmpty() ? 15 : ms.weeks.last();
                String tuanHocStr = ms.weeks.isEmpty() ? "1-15" : LichHoc.formatWeekSet(ms.weeks);
                String roomStr = String.join(" / ", ms.rooms);
                if (roomStr.length() > 50) roomStr = roomStr.substring(0, 50);

                LichHoc matched = null;
                for (LichHoc ex : existingList) {
                    if (Objects.equals(ex.getThu(), ms.thu)
                            && Objects.equals(ex.getTietBatDau(), ms.tietBatDau)
                            && Objects.equals(ex.getSoTiet(), ms.soTiet)
                            && ex.getLoaiLich() == ms.loaiLich) {
                        matched = ex;
                        break;
                    }
                }

                if (matched == null) {
                    LichHoc lich = LichHoc.builder()
                            .lopHocPhan(ms.lhp)
                            .thu(ms.thu)
                            .tietBatDau(ms.tietBatDau)
                            .soTiet(ms.soTiet)
                            .phongHoc(roomStr.isEmpty() ? "Chưa xếp" : roomStr)
                            .phongHocEntity(ms.primaryRoomEntity)
                            .soTietDot(ms.soTietDot)
                            .ngayBatDau(ms.ngayBatDau)
                            .ngayKetThuc(ms.ngayKetThuc)
                            .tuTuan(tuTuan)
                            .denTuan(denTuan)
                            .tuanHoc(tuanHocStr)
                            .loaiLich(ms.loaiLich)
                            .build();
                    matched = lichHocRepository.save(lich);
                    existingList.add(matched);
                    createdLichHoc++;
                } else {
                    matched.setTuTuan(tuTuan);
                    matched.setDenTuan(denTuan);
                    matched.setTuanHoc(tuanHocStr);
                    if (ms.primaryRoomEntity != null) matched.setPhongHocEntity(ms.primaryRoomEntity);
                    if (ms.soTietDot > 0) matched.setSoTietDot(ms.soTietDot);
                    if (ms.ngayBatDau != null) matched.setNgayBatDau(ms.ngayBatDau);
                    if (ms.ngayKetThuc != null) matched.setNgayKetThuc(ms.ngayKetThuc);
                    if (!roomStr.isEmpty()) matched.setPhongHoc(roomStr);
                    matched = lichHocRepository.save(matched);
                }

                // Đồng bộ bảng lich_hoc_tuan (3NF)
                TreeSet<Integer> targetWeeks = ms.weeks.isEmpty() ? matched.getWeekSet() : ms.weeks;
                List<LichHocTuan> existingWeeks = lichHocTuanRepository.findByLichHocIdOrderByTuanThuAsc(matched.getId());
                Set<Integer> existingWeekNums = new HashSet<>();
                for (LichHocTuan wt : existingWeeks) existingWeekNums.add(wt.getTuanThu());

                if (!existingWeekNums.equals(targetWeeks)) {
                    if (!existingWeeks.isEmpty()) {
                        lichHocTuanRepository.deleteByLichHocId(matched.getId());
                    }
                    List<LichHocTuan> batch = new ArrayList<>();
                    for (int w : targetWeeks) {
                        batch.add(LichHocTuan.builder().lichHoc(matched).tuanThu(w).build());
                    }
                    lichHocTuanRepository.saveAll(batch);
                    syncedLichHocTuan += batch.size();
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("hocKy", hocKy.getTenHocKy());
            result.put("maHocKy", hocKy.getMaHocKy());
            result.put("createdCoSo", createdCoSo);
            result.put("createdPhongHoc", createdPhongHoc);
            result.put("createdHocPhan", createdHocPhan);
            result.put("createdLopHp", createdLopHp);
            result.put("createdLichHoc", createdLichHoc);
            result.put("syncedLichHocTuan", syncedLichHocTuan);
            result.put("createdGiangVien", createdGiangVien);
            result.put("skippedRows", skippedRows);
            return result;
        }
    }

    private ParsedRoom parseRoomAndCampus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim().replaceAll("\\s+", " ");
        String low = cleaned.toLowerCase(Locale.ROOT);

        // Xác định Cơ sở
        String maCoSo, tenCoSo, diaChi;
        if (low.startsWith("zoom") || low.contains("online") || low.contains("elearning")) {
            maCoSo = "CS_ONLINE";
            tenCoSo = "Học trực tuyến (Online / Zoom)";
            diaChi = "Trực tuyến HUITx / Zoom";
        } else if (low.contains("140") || low.contains("lê trọng tấn")) {
            maCoSo = "CS_140LTT";
            tenCoSo = "Cơ sở 140 Lê Trọng Tấn";
            diaChi = "140 Lê Trọng Tấn, P. Tây Thạnh, Q. Tân Phú, TP.HCM";
        } else if (low.contains("93") || low.contains("tân kỳ") || low.contains("tân ky")) {
            maCoSo = "CS_93TKTQ";
            tenCoSo = "Cơ sở 93 Tân Kỳ Tân Quý";
            diaChi = "93 Tân Kỳ Tân Quý, P. Tân Sơn Nhì, Q. Tân Phú, TP.HCM";
        } else if (low.contains("31") || low.contains("chế lan viên")) {
            maCoSo = "CS_31CLV";
            tenCoSo = "Cơ sở 31 Chế Lan Viên";
            diaChi = "31 Chế Lan Viên, P. Tây Thạnh, Q. Tân Phú, TP.HCM";
        } else if (low.contains("nguyễn đỗ cung")) {
            maCoSo = "CS_NDC";
            tenCoSo = "Cơ sở 73/1 Nguyễn Đỗ Cung";
            diaChi = "73/1 Nguyễn Đỗ Cung, P. Tây Thạnh, Q. Tân Phú, TP.HCM";
        } else if (low.contains("dương đức hiền")) {
            maCoSo = "CS_72DDH";
            tenCoSo = "Cơ sở 72 Dương Đức Hiền";
            diaChi = "72 Dương Đức Hiền, P. Tây Thạnh, Q. Tân Phú, TP.HCM";
        } else {
            maCoSo = "CS_KHAC";
            tenCoSo = "Cơ sở khác (HUIT)";
            diaChi = "TP. Hồ Chí Minh";
        }

        // Xác định Mã phòng & Loại phòng
        String[] parts = cleaned.split("\\s*-\\s*");
        String maPhong = parts[0].trim();
        if (maPhong.isEmpty()) maPhong = cleaned;
        if (maPhong.length() > 60) maPhong = maPhong.substring(0, 60);

        PhongHoc.LoaiPhong loaiPhong = PhongHoc.LoaiPhong.LY_THUYET;
        if ("CS_ONLINE".equals(maCoSo)) loaiPhong = PhongHoc.LoaiPhong.ONLINE;
        else if (low.contains("phòng máy")) loaiPhong = PhongHoc.LoaiPhong.PHONG_MAY;
        else if (low.contains("thực hành") || low.contains("lab") || low.contains("xưởng")) loaiPhong = PhongHoc.LoaiPhong.THUC_HANH;

        String tenHienThi = cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
        return new ParsedRoom(maPhong, tenHienThi, loaiPhong, maCoSo, tenCoSo, diaChi);
    }

    private String formatRoom(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String cleaned = raw.trim().replaceAll("\\s+", " ");
        String[] parts = cleaned.split("\\s*-\\s*");
        if (parts.length == 1) {
            return cleaned.length() > 50 ? cleaned.substring(0, 50) : cleaned;
        }
        String roomCode = parts[0].trim();
        String campus = parts[parts.length - 1].trim();
        String campusShort = campus
                .replace("140 Lê Trọng Tấn", "140 LTT")
                .replace("93 Tân kỳ Tân quý", "93 TKTQ")
                .replace("93 Tân Kỳ Tân Quý", "93 TKTQ");
        String out = campusShort.equalsIgnoreCase(roomCode) ? roomCode : (roomCode + " · " + campusShort);
        return out.length() > 50 ? out.substring(0, 50) : out;
    }

    private String extractTitle(Sheet sheet, DataFormatter formatter) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r <= Math.min(6, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String v = formatter.formatCellValue(row.getCell(0)).trim();
            if (!v.isEmpty()) sb.append(v).append(" ");
        }
        return sb.toString().trim();
    }

    private HocKy resolveOrCreateHocKy(String title, String fileName) {
        String src = (title + " " + (fileName != null ? fileName : "")).toUpperCase(Locale.ROOT);
        String parsedNamHoc = "2025-2026";
        if (src.contains("2026") && src.contains("2027")) parsedNamHoc = "2026-2027";
        else if (src.contains("2025") && src.contains("2026")) parsedNamHoc = "2025-2026";
        else if (src.contains("2026")) parsedNamHoc = "2026-2027";
        final String namHoc = parsedNamHoc;

        boolean isHk2 = src.contains("HK2") || src.contains("HKII") || src.contains("HỌC KỲ 2") || src.contains("KY-2");
        String maHk = namHoc.substring(0, 4) + "_" + (isHk2 ? "2" : "1");
        String tenHk = "Học kỳ " + (isHk2 ? "2" : "1") + " - Năm học " + namHoc;

        return hocKyRepository.findByMaHocKy(maHk).orElseGet(() -> {
            HocKy hk = HocKy.builder()
                    .maHocKy(maHk)
                    .tenHocKy(tenHk)
                    .namHoc(namHoc)
                    .loaiHocKy(isHk2 ? HocKy.LoaiHocKy.CHINH_2 : HocKy.LoaiHocKy.CHINH_1)
                    .trangThaiDangKy(HocKy.TrangThaiDangKy.DA_KHOA)
                    .ngayBatDauDkhp(LocalDate.now().minusDays(7))
                    .ngayKetThucDkhp(LocalDate.now().plusDays(30))
                    .donGiaTinChi(570000.0)
                    .build();
            return hocKyRepository.save(hk);
        });
    }

    private ColumnMap detectColumns(Row mainHeader, Row subHeader, DataFormatter formatter) {
        ColumnMap map = new ColumnMap();
        int maxCol = Math.max(mainHeader != null ? mainHeader.getLastCellNum() : 0,
                subHeader != null ? subHeader.getLastCellNum() : 0);

        for (int c = 0; c < maxCol; c++) {
            String h1 = mainHeader != null ? formatter.formatCellValue(mainHeader.getCell(c)).trim().toLowerCase(Locale.ROOT) : "";
            String h2 = subHeader != null ? formatter.formatCellValue(subHeader.getCell(c)).trim().toLowerCase(Locale.ROOT) : "";
            String h = (h1 + " " + h2).trim();

            if (h.contains("mã lhp")) map.maLhp = c;
            else if (h.contains("tên môn")) map.tenMon = c;
            else if (h.contains("tính chất")) map.tinhChatHp = c;
            else if (h.contains("số tc")) map.soTc = c;
            else if (h.contains("lớp học")) map.lopHoc = c;
            else if (h2.equals("thứ") || h1.equals("thứ")) map.thu = c;
            else if (h2.contains("từ tiết")) map.tuTiet = c;
            else if (h2.contains("đến tiết")) map.denTiet = c;
            else if (h2.contains("loại lịch")) map.loaiLich = c;
            else if (h2.equals("số tiết")) map.soTietDot = c;
            else if (h2.contains("bắt đầu")) map.batDau = c;
            else if (h2.contains("kết thúc")) map.ketThuc = c;
            else if (h1.contains("tổng số tiết")) map.tongSoTiet = c;
            else if (h.contains("phòng")) map.phong = c;
            else if (h.contains("giảng viên")) map.giangVien = c;
            else if (h2.matches("t\\d{1,2}")) {
                int w = Integer.parseInt(h2.substring(1));
                map.weekCols.put(w, c);
            }
        }
        return map;
    }

    private String getCell(Row row, int colIdx, DataFormatter formatter) {
        if (colIdx < 0 || row == null) return "";
        Cell cell = row.getCell(colIdx);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private int parseIntSafe(String s, int def) {
        try {
            if (s == null || s.isBlank()) return def;
            return (int) Math.round(Double.parseDouble(s.replace(",", ".")));
        } catch (Exception e) {
            return def;
        }
    }

    private LocalDate parseDateSafe(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim(), VN_DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private ParsedLecturer parseLecturer(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim().replaceAll("\\s+", " ");
        String maGv;
        String tenRaw;
        int dashIdx = cleaned.indexOf('-');
        if (dashIdx > 0 && cleaned.substring(0, dashIdx).trim().matches("[A-Za-z0-9_.]{3,20}")) {
            maGv = cleaned.substring(0, dashIdx).trim();
            tenRaw = cleaned.substring(dashIdx + 1).trim();
        } else {
            tenRaw = cleaned;
            String slug = toAsciiSlug(tenRaw);
            maGv = "GV" + (slug.length() > 14 ? slug.substring(0, 14) : slug).toUpperCase(Locale.ROOT);
        }
        if (maGv.length() > 20) maGv = maGv.substring(0, 20);

        String hocVi = "";
        String hoTen = tenRaw.isEmpty() ? maGv : tenRaw;
        String[] prefixes = {"PGS.TS.", "PGS.TS", "GS.TS.", "GS.TS", "TS.", "ThS.", "Th.S", "GV."};
        for (String p : prefixes) {
            if (hoTen.toLowerCase(Locale.ROOT).startsWith(p.toLowerCase(Locale.ROOT) + " ")) {
                hocVi = p.endsWith(".") ? p : p + ".";
                hoTen = hoTen.substring(p.length()).trim();
                break;
            }
        }
        return new ParsedLecturer(maGv, hoTen, hocVi);
    }

    private String buildLecturerEmail(String hoTen, String maGv) {
        String slug = toAsciiSlug(hoTen);
        String maClean = maGv.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (slug.isEmpty()) return maClean + "@huit.edu.vn";
        return slug + "." + maClean + "@huit.edu.vn";
    }

    private String toAsciiSlug(String input) {
        if (input == null) return "";
        String norm = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        return norm.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private static class ParsedRoom {
        final String maPhong;
        final String tenHienThi;
        final PhongHoc.LoaiPhong loaiPhong;
        final String maCoSo;
        final String tenCoSo;
        final String diaChi;
        ParsedRoom(String maPhong, String tenHienThi, PhongHoc.LoaiPhong loaiPhong, String maCoSo, String tenCoSo, String diaChi) {
            this.maPhong = maPhong;
            this.tenHienThi = tenHienThi;
            this.loaiPhong = loaiPhong;
            this.maCoSo = maCoSo;
            this.tenCoSo = tenCoSo;
            this.diaChi = diaChi;
        }
    }

    private static class MergedSchedule {
        final LopHocPhan lhp;
        final int thu;
        final int tietBatDau;
        final int soTiet;
        final LichHoc.LoaiLich loaiLich;
        final TreeSet<Integer> weeks = new TreeSet<>();
        final LinkedHashSet<String> rooms = new LinkedHashSet<>();
        PhongHoc primaryRoomEntity;
        int soTietDot = 0;
        LocalDate ngayBatDau;
        LocalDate ngayKetThuc;

        MergedSchedule(LopHocPhan lhp, int thu, int tietBatDau, int soTiet, LichHoc.LoaiLich loaiLich) {
            this.lhp = lhp;
            this.thu = thu;
            this.tietBatDau = tietBatDau;
            this.soTiet = soTiet;
            this.loaiLich = loaiLich;
        }
    }

    private static class ParsedLecturer {
        final String maGv;
        final String hoTen;
        final String hocHamHocVi;
        ParsedLecturer(String maGv, String hoTen, String hocHamHocVi) {
            this.maGv = maGv;
            this.hoTen = hoTen;
            this.hocHamHocVi = hocHamHocVi;
        }
    }

    private static class ColumnMap {
        int maLhp = 1;
        int tenMon = 2;
        int tinhChatHp = -1;
        int soTc = 3;
        int lopHoc = 4;
        int thu = 5;
        int tuTiet = 6;
        int denTiet = 7;
        int loaiLich = -1;
        int soTietDot = -1;
        int batDau = -1;
        int ketThuc = -1;
        int tongSoTiet = -1;
        int phong = -1;
        int giangVien = -1;
        Map<Integer, Integer> weekCols = new LinkedHashMap<>();
    }
}
