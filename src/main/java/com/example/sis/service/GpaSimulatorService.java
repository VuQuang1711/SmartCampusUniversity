package com.example.sis.service;

import com.example.sis.entity.*;
import com.example.sis.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GpaSimulatorService {

    @Autowired private SinhVienRepository sinhVienRepository;
    @Autowired private DiemHocPhanRepository diemHocPhanRepository;

    public static class SimulationResult {
        public double currentCpa;
        public int currentCredits;
        public int totalCreditsRequired;
        public int remainingCredits;
        public double targetCpa;
        public double requiredFutureGpa;
        public boolean achievable;
        public String adviceMessage;
        public List<SubjectImprovementSuggestion> suggestedImprovements = new ArrayList<>();
    }

    public static class SubjectImprovementSuggestion {
        public String maHp;
        public String tenHp;
        public int credits;
        public String currentGrade;
        public double currentScale4;
        public double expectedCpaIncreaseIfA; // Nếu cải thiện lên A thì CPA tăng bao nhiêu

        public SubjectImprovementSuggestion(String maHp, String tenHp, int credits, String currentGrade, double currentScale4, double expectedCpaIncreaseIfA) {
            this.maHp = maHp;
            this.tenHp = tenHp;
            this.credits = credits;
            this.currentGrade = currentGrade;
            this.currentScale4 = currentScale4;
            this.expectedCpaIncreaseIfA = expectedCpaIncreaseIfA;
        }
    }

    /**
     * MÔ PHỎNG GPA MỤC TIÊU & GỢI Ý MÔN CẢI THIỆN TỐI ƯU NHẤT
     */
    public SimulationResult simulateTargetCpa(Long sinhVienId, double targetCpa) {
        SinhVien sv = sinhVienRepository.findById(sinhVienId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sinh viên"));

        SimulationResult res = new SimulationResult();
        res.currentCpa = sv.getCpaTichLuy() != null ? sv.getCpaTichLuy() : 0.0;
        res.currentCredits = sv.getTongTcTichLuy() != null ? sv.getTongTcTichLuy() : 0;
        res.totalCreditsRequired = sv.getNganhHoc().getTongTinChiYeuCau();
        res.remainingCredits = Math.max(0, res.totalCreditsRequired - res.currentCredits);
        res.targetCpa = targetCpa;

        if (res.remainingCredits == 0) {
            res.achievable = false;
            res.adviceMessage = "Bạn đã hoàn thành đủ số tín chỉ toàn khóa. Để tăng GPA tích lũy, bạn cần đăng ký học cải thiện các môn điểm thấp!";
        } else {
            // Required Future GPA = (Target * Total - Current * Earned) / Remaining
            double requiredPoints = (targetCpa * res.totalCreditsRequired) - (res.currentCpa * res.currentCredits);
            res.requiredFutureGpa = Math.round((requiredPoints / res.remainingCredits) * 100.0) / 100.0;

            if (res.requiredFutureGpa > 4.0) {
                res.achievable = false;
                res.adviceMessage = "Mục tiêu GPA " + targetCpa + " không thể đạt được chỉ bằng các môn còn lại (cần GPA tương lai " + res.requiredFutureGpa + " > 4.0). Bạn NÊN kết hợp học cải thiện các môn D, C dưới đây!";
            } else if (res.requiredFutureGpa < 0) {
                res.achievable = true;
                res.requiredFutureGpa = 0.0;
                res.adviceMessage = "GPA tích lũy hiện tại của bạn đã vượt mức mục tiêu! Chỉ cần duy trì phong độ hiện tại để tốt nghiệp loại xuất sắc.";
            } else {
                res.achievable = true;
                res.adviceMessage = "Khả thi! Bạn cần duy trì điểm trung bình GPA tối thiểu " + res.requiredFutureGpa + " ở " + res.remainingCredits + " tín chỉ còn lại.";
            }
        }

        // Tìm các môn điểm D, D+, C để gợi ý học cải thiện
        List<DiemHocPhan> allGrades = diemHocPhanRepository.findAllBySinhVienId(sinhVienId);
        for (DiemHocPhan d : allGrades) {
            String letter = d.getDiemChu();
            if (letter != null && (letter.equals("D") || letter.equals("D+") || letter.equals("C"))) {
                HocPhan hp = d.getDangKyHocPhan().getLopHocPhan().getHocPhan();
                double scale4 = d.getDiemHe4() != null ? d.getDiemHe4() : 1.0;
                // Nếu nâng lên A (4.0), số điểm tích lũy tăng thêm = (4.0 - scale4) * credits
                double pointGain = (4.0 - scale4) * hp.getTongTinChi();
                double cpaBoost = Math.round((pointGain / Math.max(1, res.totalCreditsRequired)) * 100.0) / 100.0;

                res.suggestedImprovements.add(new SubjectImprovementSuggestion(
                        hp.getMaHP(), hp.getTenHP(), hp.getTongTinChi(), letter, scale4, cpaBoost
                ));
            }
        }

        // Sắp xếp môn gợi ý theo mức độ kéo CPA cao nhất
        res.suggestedImprovements.sort((a, b) -> Double.compare(b.expectedCpaIncreaseIfA, a.expectedCpaIncreaseIfA));

        return res;
    }
}
