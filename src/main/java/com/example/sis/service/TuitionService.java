package com.example.sis.service;

import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TuitionService {

    @Autowired private CongNoHocKyRepository congNoHocKyRepository;
    @Autowired private PhieuThuHocPhiRepository phieuThuHocPhiRepository;
    @Autowired private DangKyHocPhanRepository dangKyHocPhanRepository;
    @Autowired private HocKyRepository hocKyRepository;
    @Autowired private SinhVienRepository sinhVienRepository;

    /**
     * TỰ ĐỘNG KẾT CHUYỂN DỮ LIỆU TỪ ĐĂNG KÝ HỌC PHẦN SANG CÔNG NỢ HỌC KỲ
     * Công thức: (Số TC LT * Đơn giá) + (Số TC TH * Đơn giá * Hệ số học phí)
     */
    @Transactional
    public CongNoHocKy syncTuition(Long sinhVienId, Long hocKyId) {
        SinhVien sv = sinhVienRepository.findById(sinhVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));
        HocKy hk = hocKyRepository.findById(hocKyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy học kỳ"));

        List<DangKyHocPhan> activeRegs = dangKyHocPhanRepository.findActiveRegistrations(sinhVienId, hocKyId);

        double basePricePerCredit = (sv.getNganhHoc() != null && sv.getNganhHoc().getDonGiaTinChiRieng() != null && sv.getNganhHoc().getDonGiaTinChiRieng() > 0)
                ? sv.getNganhHoc().getDonGiaTinChiRieng()
                : (hk.getDonGiaTinChi() != null ? hk.getDonGiaTinChi() : 550000.0);
        double heSoHocLai = hk.getHeSoHocLai() != null ? hk.getHeSoHocLai() : 1.5;
        double heSoCaiThien = hk.getHeSoCaiThien() != null ? hk.getHeSoCaiThien() : 1.2;

        int totalCredits = 0;
        double totalTuition = 0.0;

        for (DangKyHocPhan dk : activeRegs) {
            HocPhan hp = dk.getLopHocPhan().getHocPhan();
            int lt = hp.getSoTCLyThuyet() != null ? hp.getSoTCLyThuyet() : 0;
            int th = hp.getSoTCThucHanh() != null ? hp.getSoTCThucHanh() : 0;
            double coeff = hp.getHeSoHocPhi() != null ? hp.getHeSoHocPhi() : 1.0;

            double typeMultiplier = 1.0;
            if (dk.getLoaiDangKy() == DangKyHocPhan.LoaiDangKy.HOC_LAI) {
                typeMultiplier = heSoHocLai;
            } else if (dk.getLoaiDangKy() == DangKyHocPhan.LoaiDangKy.CAI_THIEN) {
                typeMultiplier = heSoCaiThien;
            }

            totalCredits += (lt + th);
            totalTuition += ((lt * basePricePerCredit) + (th * basePricePerCredit * coeff)) * typeMultiplier;
        }

        Optional<CongNoHocKy> cnOpt = congNoHocKyRepository.findBySinhVienIdAndHocKyId(sinhVienId, hocKyId);
        CongNoHocKy cn;
        if (cnOpt.isPresent()) {
            cn = cnOpt.get();
        } else {
            cn = CongNoHocKy.builder()
                    .sinhVien(sv)
                    .hocKy(hk)
                    .mienGiam(0.0)
                    .daDong(0.0)
                    .build();
        }

        cn.setTongSoTC(totalCredits);
        cn.setTongHocPhi(totalTuition);
        cn.updateBalance();

        return congNoHocKyRepository.save(cn);
    }

    /**
     * TẠO PHIẾU THU HỌC PHÍ (THANH TOÁN NHIỀU ĐỢT)
     */
    @Transactional
    public PhieuThuHocPhi createPayment(Long congNoHocKyId, Double soTien, PhieuThuHocPhi.PhuongThucThanhToan phuongThuc, String nguoiThu, String ghiChu) {
        CongNoHocKy cn = congNoHocKyRepository.findById(congNoHocKyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công nợ"));

        if (soTien <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");
        }

        String maPhieuThu = "PT" + cn.getHocKy().getNamHoc().replace("-", "") + "_" + (System.currentTimeMillis() % 1000000);
        String maGiaoDich = "TXN" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        PhieuThuHocPhi pt = PhieuThuHocPhi.builder()
                .congNoHocKy(cn)
                .maPhieuThu(maPhieuThu)
                .soTien(soTien)
                .phuongThuc(phuongThuc != null ? phuongThuc : PhieuThuHocPhi.PhuongThucThanhToan.VIETQR)
                .maGiaoDich(maGiaoDich)
                .ngayThu(LocalDateTime.now())
                .nguoiThu(nguoiThu != null ? nguoiThu : "Hệ thống tự động")
                .ghiChu(ghiChu)
                .build();

        phieuThuHocPhiRepository.save(pt);

        // Cập nhật số tiền đã đóng và số dư nợ
        cn.setDaDong((cn.getDaDong() != null ? cn.getDaDong() : 0.0) + soTien);
        cn.updateBalance();
        congNoHocKyRepository.save(cn);

        return pt;
    }

    /**
     * TẠO ĐƯỜNG DẪN MÃ VIETQR ĐỘNG CHUẨN NAPAS247
     */
    public String generateVietQrUrl(CongNoHocKy cn) {
        if (cn == null || cn.getConNo() <= 0) return null;
        String bankId = "MB"; // Ngân hàng Quân Đội MBBank
        String accountNo = "0901111222";
        String template = "compact2";
        long amount = Math.round(cn.getConNo());
        String description = "HP " + cn.getSinhVien().getMssv() + " " + cn.getHocKy().getMaHocKy();
        return String.format("https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=TRUONG+DH+CONG+THUONG+TPHCM",
                bankId, accountNo, template, amount, description.replace(" ", "%20"));
    }

    /**
     * ĐỒNG BỘ LẠI TOÀN BỘ CÔNG NỢ HỌC KỲ SAU KHI ADMIN THAY ĐỔI ĐƠN GIÁ / HỆ SỐ
     */
    @Transactional
    public int syncAllTuitionForSemester(Long hocKyId) {
        List<SinhVien> allSv = sinhVienRepository.findAll();
        int updated = 0;
        for (SinhVien sv : allSv) {
            List<DangKyHocPhan> regs = dangKyHocPhanRepository.findActiveRegistrations(sv.getId(), hocKyId);
            if (!regs.isEmpty()) {
                syncTuition(sv.getId(), hocKyId);
                updated++;
            }
        }
        return updated;
    }
}
