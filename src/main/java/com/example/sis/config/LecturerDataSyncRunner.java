package com.example.sis.config;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.GiangVien;
import com.example.sis.entity.HocKy;
import com.example.sis.entity.NguoiDung;
import com.example.sis.repository.CoSoRepository;
import com.example.sis.repository.GiangVienRepository;
import com.example.sis.repository.HocKyRepository;
import com.example.sis.repository.LichHocTuanRepository;
import com.example.sis.repository.LopHocPhanRepository;
import com.example.sis.repository.NguoiDungRepository;
import com.example.sis.service.ClassDatabaseImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Tự động đồng bộ dữ liệu THỰC TẾ từ thư mục class_database/ ngay khi ứng dụng khởi động:
 * 1. Nạp đầy đủ 4 học kỳ TKB HUIT (2025_1, 2025_2, 2026_1, 2026_2) cùng toàn bộ Học phần,
 *    Lớp học phần, Lịch học (tuần T1-T30, phòng) và Giảng viên thật (đúng Mã GV + Tên GV).
 * 2. Khóa học kỳ giả lập cũ (2024_2) và mở học kỳ thực tế mới nhất (2026_2) cho trang ĐKHP
 *    để ĐKHP chỉ dùng 100% dữ liệu thật từ class_database/.
 * 3. Làm sạch mã giảng viên giả lập GV001/GV002/GV003 cũ thành mã giảng viên thật HUIT.
 */
@Component
@RequiredArgsConstructor
public class LecturerDataSyncRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LecturerDataSyncRunner.class);

    private final ClassDatabaseImportService classDatabaseImportService;
    private final HocKyRepository hocKyRepository;
    private final LopHocPhanRepository lopHocPhanRepository;
    private final GiangVienRepository giangVienRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final CoSoRepository coSoRepository;
    private final LichHocTuanRepository lichHocTuanRepository;

    @Override
    @Transactional
    public void run(String... args) {
        try {
            // Kiểm tra xem đã nạp đầy đủ CSDL chuẩn hóa 3NF (co_so, phong_hoc, lich_hoc_tuan, học kỳ 2026_2) chưa
            Optional<HocKy> hk20262 = hocKyRepository.findByMaHocKy("2026_2");
            boolean alreadySynced = hk20262.isPresent()
                    && hk20262.get().getTrangThaiDangKy() == HocKy.TrangThaiDangKy.DANG_MO
                    && lopHocPhanRepository.findByHocKyId(hk20262.get().getId()).size() >= 30
                    && coSoRepository.count() > 0
                    && lichHocTuanRepository.count() > 0;

            if (!alreadySynced) {
                Map<String, Object> summary = classDatabaseImportService.syncAllRealHuitData("15DHKHDL");
                log.info("[HuitRealDataSync] Đã đồng bộ dữ liệu thực tế từ class_database/: {}", summary);
            }

            // Đảm bảo học kỳ giả lập 2024_2 không còn mở đăng ký
            hocKyRepository.findByMaHocKy("2024_2").ifPresent(hk -> {
                if (hk.getTrangThaiDangKy() == HocKy.TrangThaiDangKy.DANG_MO) {
                    hk.setTrangThaiDangKy(HocKy.TrangThaiDangKy.DA_KHOA);
                    hocKyRepository.save(hk);
                }
            });

            // Xóa học vị giả "ThS." gán nhầm trước đó trên các GV import từ Excel và đổi mã GV001/GV002/GV003 nếu còn
            cleanupLegacyMockLecturers();
        } catch (Exception e) {
            log.warn("[HuitRealDataSync] Lỗi khi đồng bộ dữ liệu thực tế: {}", e.getMessage());
        }
    }

    private void cleanupLegacyMockLecturers() {
        List<GiangVien> allGvs = giangVienRepository.findAll();
        // Bản đồ thay thế 3 mã GV001/GV002/GV003 giả lập bằng 3 mã GV HUIT thật chưa trùng
        String[][] legacyReplacements = {
                {"01007065", "Đồng Quang Chung"},
                {"01015009", "Nguyễn Thị Hoàng Liên"},
                {"01015014", "Nguyễn Thị Thanh Trúc"}
        };
        int repIdx = 0;

        for (GiangVien gv : allGvs) {
            String ma = gv.getMaGV() != null ? gv.getMaGV().trim() : "";
            if (ma.startsWith("GV00") && repIdx < legacyReplacements.length) {
                String newMa = legacyReplacements[repIdx][0];
                String newTen = legacyReplacements[repIdx][1];
                repIdx++;

                boolean codeTaken = giangVienRepository.findByMaGV(newMa).isPresent()
                        || nguoiDungRepository.findByMaDinhDanh(newMa).isPresent();
                if (!codeTaken) {
                    gv.setMaGV(newMa);
                    gv.setHocHamHocVi("");
                    NguoiDung nd = gv.getNguoiDung();
                    if (nd != null) {
                        nd.setMaDinhDanh(newMa);
                        if (nd.getHoTen() == null || nd.getHoTen().contains("Nguyễn Văn A")
                                || nd.getHoTen().contains("Trần Thị Bình") || nd.getHoTen().contains("Lê Văn Cường")) {
                            nd.setHoTen(newTen);
                        }
                        nguoiDungRepository.save(nd);
                    }
                    giangVienRepository.save(gv);
                }
            }
        }
    }
}
