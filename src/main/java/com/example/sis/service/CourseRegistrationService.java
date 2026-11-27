package com.example.sis.service;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class CourseRegistrationService {

    private final LopHocPhanRepository lopHocPhanRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final DieuKienHocPhanRepository dieuKienHocPhanRepository;
    private final DiemHocPhanRepository diemHocPhanRepository;
    private final SinhVienRepository sinhVienRepository;
    private final LichHocRepository lichHocRepository;
    private final TuitionService tuitionService;

    public static class RegistrationResult {
        private final boolean success;
        private final String message;

        public RegistrationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    /**
     * ENGINE ĐĂNG KÝ HỌC PHẦN 5 BƯỚC KIỂM TRA CHUẨN ĐẠI HỌC
     */
    @Transactional
    public RegistrationResult registerCourse(Long sinhVienId, Long lopHocPhanId) {
        SinhVien sv = sinhVienRepository.findById(sinhVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên ID: " + sinhVienId));

        // Dùng Pessimistic Lock để chống race condition khi nhiều SV bấm cùng lúc
        LopHocPhan lhp = lopHocPhanRepository.findByIdWithLock(lopHocPhanId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp học phần ID: " + lopHocPhanId));

        HocKy hocKy = lhp.getHocKy();
        HocPhan hocPhan = lhp.getHocPhan();

        // 1. Kiểm tra trạng thái cổng ĐKHP và trạng thái lớp đã được Admin duyệt
        if (hocKy.getTrangThaiDangKy() != HocKy.TrangThaiDangKy.DANG_MO) {
            return new RegistrationResult(false, "Cổng đăng ký học phần của " + hocKy.getTenHocKy() + " hiện đang đóng!");
        }
        if (lhp.getTrangThai() != LopHocPhan.TrangThaiLHP.DANG_MO) {
            return new RegistrationResult(false, "Lớp học phần " + lhp.getMaLHP() + " chưa được Admin phê duyệt mở đăng ký!");
        }

        // Kiểm tra xem sinh viên đã đăng ký chính lớp này hoặc môn này trong kỳ chưa
        List<DangKyHocPhan> currentSemesterRegs = dangKyHocPhanRepository.findActiveRegistrations(sv.getId(), hocKy.getId());
        for (DangKyHocPhan dk : currentSemesterRegs) {
            if (dk.getLopHocPhan().getHocPhan().getId().equals(hocPhan.getId())) {
                return new RegistrationResult(false, "Bạn đã đăng ký học phần [" + hocPhan.getTenHP() + "] ở lớp " + dk.getLopHocPhan().getMaLHP() + " trong học kỳ này!");
            }
        }

        // 2. Kiểm tra sĩ số lớp (chống vượt sĩ số)
        int curSiSo = lhp.getSiSoHienTai() != null ? lhp.getSiSoHienTai() : 0;
        int maxSiSo = lhp.getSiSoToiDa() != null ? lhp.getSiSoToiDa() : 60;
        if (curSiSo >= maxSiSo) {
            return new RegistrationResult(false, "Lớp học phần " + lhp.getMaLHP() + " đã hết chỗ (Sĩ số: " + curSiSo + "/" + maxSiSo + ")!");
        }

        // 3. Kiểm tra môn tiên quyết
        List<DieuKienHocPhan> dieuKienList = dieuKienHocPhanRepository.findByHocPhanIdAndLoaiDieuKien(
                hocPhan.getId(), DieuKienHocPhan.LoaiDieuKien.TIEN_QUYET);
        List<DiemHocPhan> allPastGrades = diemHocPhanRepository.findAllBySinhVienId(sv.getId());

        for (DieuKienHocPhan dk : dieuKienList) {
            HocPhan prerequisite = dk.getHocPhanDieuKien();
            boolean passedPrerequisite = false;

            for (DiemHocPhan d : allPastGrades) {
                if (d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getId().equals(prerequisite.getId())) {
                    if (Boolean.TRUE.equals(d.getKetQuaQuaMon()) || (d.getDiemChu() != null && !"F".equalsIgnoreCase(d.getDiemChu()))) {
                        passedPrerequisite = true;
                        break;
                    }
                }
            }

            if (!passedPrerequisite) {
                return new RegistrationResult(false, "Chưa thỏa mãn điều kiện tiên quyết: Môn [" + hocPhan.getTenHP() + "] yêu cầu phải đạt môn [" + prerequisite.getTenHP() + " (" + prerequisite.getMaHP() + ")]!");
            }
        }

        // 4. Kiểm tra giới hạn tín chỉ (<= 25 TC thông thường, <= 18 TC nếu bị Cảnh báo học vụ)
        int currentRegisteredCredits = currentSemesterRegs.stream()
                .mapToInt(dk -> dk.getLopHocPhan().getHocPhan().getTongTinChi())
                .sum();
        int newCreditCount = currentRegisteredCredits + hocPhan.getTongTinChi();

        boolean isWarning = sv.getTrangThaiHocVu() != null && sv.getTrangThaiHocVu().startsWith("CANH_BAO");
        int maxCreditsAllowed = isWarning ? 18 : 25;

        if (newCreditCount > maxCreditsAllowed) {
            return new RegistrationResult(false, "Vượt quá giới hạn tín chỉ quy định! Bạn đang đăng ký " + newCreditCount + " TC (Tối đa " + maxCreditsAllowed + " TC" + (isWarning ? " do đang bị cảnh báo học vụ" : "") + ")!");
        }

        // 5. Kiểm tra trùng thời khóa biểu (Thứ, Tiết bắt đầu -> Tiết kết thúc, Tuần học giao thoa)
        List<LichHoc> newClassSchedules = lhp.getDanhSachLichHoc();
        if (newClassSchedules.isEmpty()) {
            newClassSchedules = lichHocRepository.findByLopHocPhanId(lhp.getId());
        }

        for (DangKyHocPhan existingDk : currentSemesterRegs) {
            List<LichHoc> existingSchedules = existingDk.getLopHocPhan().getDanhSachLichHoc();
            if (existingSchedules.isEmpty()) {
                existingSchedules = lichHocRepository.findByLopHocPhanId(existingDk.getLopHocPhan().getId());
            }

            for (LichHoc newLh : newClassSchedules) {
                if (newLh.getLoaiLich() == LichHoc.LoaiLich.LICH_THI || newLh.getThu() == null) continue;
                for (LichHoc exLh : existingSchedules) {
                    if (exLh.getLoaiLich() == LichHoc.LoaiLich.LICH_THI || exLh.getThu() == null) continue;
                    if (newLh.getThu().equals(exLh.getThu())) {
                        // Kiểm tra giao thoa tiết
                        boolean periodOverlap = !(newLh.getTietKetThuc() < exLh.getTietBatDau() || newLh.getTietBatDau() > exLh.getTietKetThuc());
                        if (!periodOverlap) continue;
                        // Kiểm tra giao thoa TỪNG TUẦN THỰC TẾ (tuanHoc: VD "1-2, 7-13, 15")
                        TreeSet<Integer> overlapWeeks = newLh.getOverlapWeeks(exLh);
                        if (!overlapWeeks.isEmpty()) {
                            String dayLabel = newLh.getThu() == 8 ? "Chủ nhật" : ("Thứ " + newLh.getThu());
                            return new RegistrationResult(false, "Trùng thời khóa biểu! Lớp " + lhp.getMaLHP()
                                    + " trùng lịch (" + dayLabel + ", tiết " + newLh.getTietBatDau() + "-" + newLh.getTietKetThuc()
                                    + ", trùng tuần: " + LichHoc.formatWeekSet(overlapWeeks)
                                    + ") với lớp đã đăng ký [" + existingDk.getLopHocPhan().getMaLHP() + "]!");
                        }
                    }
                }
            }
        }

        // Xác định loại đăng ký: HOC_MOI, HOC_LAI, CAI_THIEN (bỏ qua các lớp đã hủy hoặc chưa có điểm chữ để tránh NPE)
        DangKyHocPhan.LoaiDangKy loaiDangKy = DangKyHocPhan.LoaiDangKy.HOC_MOI;
        for (DiemHocPhan d : allPastGrades) {
            if (d.getDangKyHocPhan() == null
                    || d.getDangKyHocPhan().getTrangThai() != DangKyHocPhan.TrangThaiDangKy.THANH_CONG) {
                continue;
            }
            if (d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getId().equals(hocPhan.getId())) {
                String letter = d.getDiemChu();
                if (letter == null || letter.isBlank()) continue;
                String normLetter = letter.trim().toUpperCase();
                if ("F".equals(normLetter)) {
                    loaiDangKy = DangKyHocPhan.LoaiDangKy.HOC_LAI;
                } else if ("D".equals(normLetter) || "D+".equals(normLetter) || "C".equals(normLetter)) {
                    loaiDangKy = DangKyHocPhan.LoaiDangKy.CAI_THIEN;
                }
            }
        }

        // Lưu đăng ký
        Optional<DangKyHocPhan> existingOpt = dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(sv.getId(), lhp.getId());
        DangKyHocPhan dk;
        if (existingOpt.isPresent()) {
            dk = existingOpt.get();
            dk.setTrangThai(DangKyHocPhan.TrangThaiDangKy.THANH_CONG);
            dk.setLoaiDangKy(loaiDangKy);
            dk.setNgayDangKy(LocalDateTime.now());
        } else {
            dk = DangKyHocPhan.builder()
                    .sinhVien(sv)
                    .lopHocPhan(lhp)
                    .loaiDangKy(loaiDangKy)
                    .trangThai(DangKyHocPhan.TrangThaiDangKy.THANH_CONG)
                    .ngayDangKy(LocalDateTime.now())
                    .build();
        }
        dk = dangKyHocPhanRepository.save(dk);

        // Tăng sĩ số hiện tại
        lhp.setSiSoHienTai(curSiSo + 1);
        lopHocPhanRepository.save(lhp);

        // Khởi tạo bản ghi điểm học phần
        if (diemHocPhanRepository.findByDangKyHocPhanId(dk.getId()).isEmpty()) {
            DiemHocPhan dhp = DiemHocPhan.builder()
                    .dangKyHocPhan(dk)
                    .camThi(false)
                    .khoaDiem(false)
                    .build();
            diemHocPhanRepository.save(dhp);
        }

        // Đồng bộ công nợ học phí tức thì
        tuitionService.syncTuition(sv.getId(), hocKy.getId());

        return new RegistrationResult(true, "Đăng ký thành công lớp học phần " + lhp.getMaLHP() + " (" + hocPhan.getTenHP() + ") - Phân loại: " + loaiDangKy);
    }

    /**
     * HỦY ĐĂNG KÝ HỌC PHẦN
     */
    @Transactional
    public RegistrationResult dropCourse(Long sinhVienId, Long lopHocPhanId) {
        SinhVien sv = sinhVienRepository.findById(sinhVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));
        LopHocPhan lhp = lopHocPhanRepository.findByIdWithLock(lopHocPhanId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp"));

        if (lhp.getHocKy().getTrangThaiDangKy() != HocKy.TrangThaiDangKy.DANG_MO) {
            return new RegistrationResult(false, "Không thể hủy! Cổng đăng ký học phần đã đóng!");
        }

        Optional<DangKyHocPhan> dkOpt = dangKyHocPhanRepository.findBySinhVienIdAndLopHocPhanId(sv.getId(), lhp.getId());
        if (dkOpt.isEmpty() || dkOpt.get().getTrangThai() != DangKyHocPhan.TrangThaiDangKy.THANH_CONG) {
            return new RegistrationResult(false, "Bạn chưa đăng ký lớp học phần này!");
        }

        DangKyHocPhan dk = dkOpt.get();
        dk.setTrangThai(DangKyHocPhan.TrangThaiDangKy.DA_HUY);
        dangKyHocPhanRepository.save(dk);

        // Giảm sĩ số lớp
        if (lhp.getSiSoHienTai() > 0) {
            lhp.setSiSoHienTai(lhp.getSiSoHienTai() - 1);
            lopHocPhanRepository.save(lhp);
        }

        // Cập nhật lại công nợ học phí
        tuitionService.syncTuition(sv.getId(), lhp.getHocKy().getId());

        return new RegistrationResult(true, "Hủy thành công lớp học phần " + lhp.getMaLHP());
    }
}
