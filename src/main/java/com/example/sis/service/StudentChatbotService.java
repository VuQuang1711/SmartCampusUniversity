package com.example.sis.service;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.*;
import com.example.sis.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.text.NumberFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class StudentChatbotService {

    @Value("${app.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${app.gemini.model:gemini-3.8-flash}")
    private String geminiModel;

    private final SinhVienRepository sinhVienRepository;
    private final HocKyRepository hocKyRepository;
    private final DangKyHocPhanRepository dangKyHocPhanRepository;
    private final LichHocRepository lichHocRepository;
    private final DiemHocPhanRepository diemHocPhanRepository;
    private final CongNoHocKyRepository congNoHocKyRepository;
    private final GradingService gradingService;
    private final RegulationKnowledgeService regulationKnowledgeService;
    private final ObjectMapper objectMapper;

    private final RestClient restClient = RestClient.create();

    public Map<String, Object> ask(NguoiDung currentUser, String userMessage, List<Map<String, String>> history) {
        Map<String, Object> response = new LinkedHashMap<>();
        if (userMessage == null || userMessage.isBlank()) {
            response.put("reply",
                    "Chào bạn! Bạn cần Gấu HUIT hỗ trợ tra cứu lịch học, điểm số, học phí hay quy chế đào tạo nào nhỉ?");
            response.put("mode", "system");
            return response;
        }

        SinhVien sv = null;
        if (currentUser != null && currentUser.getVaiTro() == VaiTro.SINH_VIEN) {
            sv = sinhVienRepository.findByNguoiDungId(currentUser.getId()).orElse(null);
            if (sv != null) {
                try {
                    gradingService.updateStudentCpa(sv.getId());
                    sv = sinhVienRepository.findById(sv.getId()).orElse(sv);
                } catch (Exception ignored) {
                }
            }
        }

        StudentContext ctx = buildStudentContext(sv);
        List<RegulationKnowledgeService.RetrievedChunk> regChunks = regulationKnowledgeService
                .retrieveRelevantChunks(userMessage, 5);
        String regContext = formatRegulationContext(regChunks);
        String effectiveKey = resolveApiKey();

        if (effectiveKey != null && !effectiveKey.isBlank()) {
            try {
                String aiReply = callGeminiApi(effectiveKey, ctx.contextText, regContext, userMessage, history);
                if (aiReply != null && !aiReply.isBlank()) {
                    response.put("reply", aiReply);
                    response.put("mode", "gemini");
                    response.put("sources", regChunks.stream()
                            .map(r -> r.chunk.article + " (Trang " + r.chunk.page + ")").distinct().toList());
                    return response;
                }
            } catch (Exception e) {
                // Fallback to smart local DB + Regulation RAG responder if Gemini fails
                String fallback = buildSmartLocalReply(ctx, regChunks, userMessage);
                response.put("reply", fallback
                        + "\n\n_(Lưu ý: Kết nối Gemini AI tạm thời gián đoạn, Gấu HUIT đang trả lời trực tiếp từ dữ liệu hệ thống & Quy chế 3344/QĐ-DCT)_");
                response.put("mode", "fallback");
                return response;
            }
        }

        // No API key configured -> smart local DB + Regulation RAG assistant
        response.put("reply", buildSmartLocalReply(ctx, regChunks, userMessage));
        response.put("mode", "local");
        return response;
    }

    private String formatRegulationContext(List<RegulationKnowledgeService.RetrievedChunk> regChunks) {
        if (regChunks == null || regChunks.isEmpty()) {
            return "(Không tìm thấy điều khoản quy chế khớp trực tiếp trong tài liệu.)";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < regChunks.size(); i++) {
            RegulationKnowledgeService.DocChunk ch = regChunks.get(i).chunk;
            sb.append("--- TRÍCH ĐOẠN ").append(i + 1)
                    .append(" [Nguồn: ").append(ch.sourceFile)
                    .append(" | ").append(ch.article)
                    .append(" | Trang ").append(ch.page).append("] ---\n")
                    .append(ch.content).append("\n\n");
        }
        return sb.toString();
    }

    private String resolveApiKey() {
        if (geminiApiKey != null && !geminiApiKey.isBlank())
            return geminiApiKey.trim();
        String envGoogle = System.getenv("GOOGLE_API_KEY");
        if (envGoogle != null && !envGoogle.isBlank())
            return envGoogle.trim();
        String envGemini = System.getenv("GEMINI_API_KEY");
        if (envGemini != null && !envGemini.isBlank())
            return envGemini.trim();
        return "";
    }

    private String callGeminiApi(String apiKey, String studentContext, String regContext, String userMessage,
            List<Map<String, String>> history) throws Exception {
        String systemPrompt = """
                Bạn là **Gấu HUIT** — Trợ lý AI cá nhân thông minh của Trường Đại học Công Thương TP.HCM (HUIT).
                NHIỆM VỤ:
                - Hỗ trợ sinh viên tra cứu chính xác: Lịch học, Lịch thi, Điểm số/GPA/CPA, Học phí/Công nợ, Tín chỉ tích lũy, và **Quy chế đào tạo đại học theo hệ thống tín chỉ (QĐ số 3344/QĐ-DCT ngày 05/09/2025 của HUIT)**.
                - Trả lời ngắn gọn, rõ ràng bằng tiếng Việt, dùng Markdown (in đậm, gạch đầu dòng) cho dễ đọc.
                - CHỈ sử dụng dữ liệu sinh viên và trích đoạn Quy chế được cung cấp dưới đây. KHÔNG tự bịa ra quy định, điểm, phòng học, hay số tiền không có trong dữ liệu.
                - Khi trả lời câu hỏi về quy định / quy chế đào tạo, BẮT BUỘC trích dẫn rõ **Điều, Khoản, Trang** từ Quy chế 3344/QĐ-DCT nếu có trong trích đoạn.

                TRÍCH ĐOẠN QUY CHẾ ĐÀO TẠO HUIT (QĐ 3344/QĐ-DCT NGÀY 05/09/2025) LIÊN QUAN:
                """
                + regContext + """

                        DỮ LIỆU THỰC TẾ CỦA SINH VIÊN ĐANG ĐĂNG NHẬP:
                        """ + studentContext;

        List<Map<String, Object>> contents = new ArrayList<>();
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 6);
            for (int i = start; i < history.size(); i++) {
                Map<String, String> turn = history.get(i);
                String role = "assistant".equals(turn.get("role")) ? "model" : "user";
                String text = turn.get("text");
                if (text != null && !text.isBlank()) {
                    contents.add(Map.of(
                            "role", role,
                            "parts", List.of(Map.of("text", text))));
                }
            }
        }
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userMessage))));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
        payload.put("contents", contents);
        payload.put("generationConfig", Map.of(
                "temperature", 0.35,
                "maxOutputTokens", 800));

        List<String> urls = new ArrayList<>();
        if (apiKey.startsWith("AQ.")) {
            // Vertex AI Express API key format (AQ....)
            urls.add("https://aiplatform.googleapis.com/v1/publishers/google/models/" + geminiModel
                    + ":generateContent?key=" + apiKey);
            urls.add(
                    "https://aiplatform.googleapis.com/v1/publishers/google/models/gemini-2.5-flash-lite:generateContent?key="
                            + apiKey);
            urls.add("https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key="
                    + apiKey);
        } else {
            urls.add("https://generativelanguage.googleapis.com/v1beta/models/" + geminiModel + ":generateContent?key="
                    + apiKey);
            urls.add("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key="
                    + apiKey);
        }

        Exception lastErr = null;
        for (String url : urls) {
            try {
                String rawResp = restClient.post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                JsonNode root = objectMapper.readTree(rawResp);
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    if (text != null && !text.isBlank())
                        return text;
                }
            } catch (Exception ex) {
                lastErr = ex;
            }
        }
        if (lastErr != null)
            throw lastErr;
        return null;
    }

    private StudentContext buildStudentContext(SinhVien sv) {
        StudentContext ctx = new StudentContext();
        if (sv == null) {
            ctx.contextText = "Người dùng hiện tại chưa đăng nhập tài khoản Sinh viên (có thể là Admin/Giảng viên).";
            return ctx;
        }

        ctx.student = sv;
        NumberFormat vnd = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        StringBuilder sb = new StringBuilder();

        String hoTen = sv.getNguoiDung() != null && sv.getNguoiDung().getHoTen() != null ? sv.getNguoiDung().getHoTen()
                : sv.getMssv();
        sb.append("- Họ tên: ").append(hoTen).append("\n");
        sb.append("- MSSV: ").append(sv.getMssv()).append("\n");
        if (sv.getNganhHoc() != null) {
            sb.append("- Ngành: ").append(sv.getNganhHoc().getTenNganh())
                    .append(" (Yêu cầu tốt nghiệp: ").append(sv.getNganhHoc().getTongTinChiYeuCau()).append(" TC)\n");
        }
        if (sv.getLopDanhNghia() != null) {
            sb.append("- Lớp danh nghĩa: ").append(sv.getLopDanhNghia().getTenLop()).append("\n");
        }
        sb.append("- CPA tích lũy (hệ 4): ").append(String.format("%.2f", sv.getCpaTichLuy())).append("\n");
        sb.append("- Tổng tín chỉ tích lũy: ").append(sv.getTongTcTichLuy()).append(" TC\n");

        // Active semester
        HocKy activeSemester = hocKyRepository.findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO)
                .orElse(hocKyRepository.findAll().stream().findFirst().orElse(null));
        ctx.activeSemester = activeSemester;

        if (activeSemester != null) {
            sb.append("\n[HỌC KỲ HIỆN TẠI: ").append(activeSemester.getTenHocKy()).append("]\n");
            List<DangKyHocPhan> regs = dangKyHocPhanRepository.findActiveRegistrations(sv.getId(),
                    activeSemester.getId());
            int totalCredits = regs.stream().mapToInt(r -> r.getLopHocPhan().getHocPhan().getTongTinChi()).sum();
            ctx.currentCredits = totalCredits;
            sb.append("- Số lớp đã đăng ký: ").append(regs.size()).append(" lớp (").append(totalCredits)
                    .append(" tín chỉ)\n");
            for (DangKyHocPhan r : regs) {
                LopHocPhan lhp = r.getLopHocPhan();
                sb.append("  * ").append(lhp.getHocPhan().getTenHP())
                        .append(" (Mã LHP: ").append(lhp.getMaLHP())
                        .append(", ").append(lhp.getHocPhan().getTongTinChi()).append(" TC)\n");
            }

            // Schedules
            List<LichHoc> scheds = lichHocRepository.findLichHocBySinhVienAndHocKy(sv.getId(), activeSemester.getId());
            ctx.schedules = scheds;
            sb.append("\n[THỜI KHÓA BIỂU & LỊCH THI KỲ NÀY]\n");
            if (scheds.isEmpty()) {
                sb.append("- Chưa có lịch học nào.\n");
            } else {
                for (LichHoc lh : scheds) {
                    String dayName = lh.getThu() != null ? (lh.getThu() == 8 ? "Chủ nhật" : "Thứ " + lh.getThu()) : "";
                    String type = lh.getLoaiLich() != null ? lh.getLoaiLich().name() : "LY_THUYET";
                    sb.append("  * ").append(lh.getLopHocPhan().getHocPhan().getTenHP())
                            .append(" [").append(type).append("] - ").append(dayName)
                            .append(", Tiết ").append(lh.getTietBatDau()).append("-").append(lh.getTietKetThuc())
                            .append(", Phòng: ").append(lh.getPhongHoc() != null ? lh.getPhongHoc() : "Chưa rõ")
                            .append("\n");
                }
            }

            // Tuition
            CongNoHocKy debt = congNoHocKyRepository.findBySinhVienIdAndHocKyId(sv.getId(), activeSemester.getId())
                    .orElse(null);
            ctx.currentDebt = debt;
            sb.append("\n[HỌC PHÍ & CÔNG NỢ KỲ NÀY]\n");
            if (debt != null) {
                long tong = debt.getTongHocPhi() != null ? debt.getTongHocPhi().longValue() : 0L;
                long daDong = debt.getDaDong() != null ? debt.getDaDong().longValue() : 0L;
                long conNo = Math.max(0L, tong - daDong);
                sb.append("- Tổng học phí: ").append(vnd.format(tong)).append(" đ\n");
                sb.append("- Đã đóng: ").append(vnd.format(daDong)).append(" đ\n");
                sb.append("- Còn nợ: ").append(vnd.format(conNo)).append(" đ\n");
                sb.append("- Trạng thái: ").append(debt.getTrangThai() != null ? debt.getTrangThai().name() : "")
                        .append("\n");
            } else {
                long est = totalCredits * 570_000L;
                sb.append("- Chưa chốt công nợ chính thức. Tạm tính theo ").append(totalCredits)
                        .append(" TC: ").append(vnd.format(est)).append(" đ.\n");
            }
        }

        // Grades
        List<DiemHocPhan> allGrades = diemHocPhanRepository.findAllBySinhVienId(sv.getId());
        ctx.grades = allGrades;
        sb.append("\n[BẢNG ĐIỂM CÁC MÔN ĐÃ CÓ ĐIỂM]\n");
        if (allGrades.isEmpty()) {
            sb.append("- Chưa có điểm học phần nào.\n");
        } else {
            for (DiemHocPhan d : allGrades) {
                String tenMon = d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getTenHP();
                sb.append("  * ").append(tenMon)
                        .append(": Tổng kết ")
                        .append(d.getDiemTongKetHe10() != null ? String.format("%.1f", d.getDiemTongKetHe10()) : "N/A")
                        .append(" (Điểm chữ: ").append(d.getDiemChu() != null ? d.getDiemChu() : "N/A")
                        .append(", Hệ 4: ")
                        .append(d.getDiemHe4() != null ? String.format("%.1f", d.getDiemHe4()) : "N/A")
                        .append(")\n");
            }
        }

        ctx.contextText = sb.toString();
        return ctx;
    }

    private String buildSmartLocalReply(StudentContext ctx, List<RegulationKnowledgeService.RetrievedChunk> regChunks,
            String msg) {
        String q = msg.toLowerCase(Locale.ROOT);
        NumberFormat vnd = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

        boolean asksRegulation = q.contains("quy chế") || q.contains("quy định") || q.contains("điều ")
                || q.contains("cảnh báo") || q.contains("thôi học") || q.contains("tốt nghiệp")
                || q.contains("bảo lưu") || q.contains("tạm dừng") || q.contains("phúc khảo")
                || q.contains("rút bớt") || q.contains("hủy học phần") || q.contains("tối đa")
                || q.contains("tối thiểu") || q.contains("học lại") || q.contains("cải thiện")
                || q.contains("cấm thi") || q.contains("chuyển ngành") || q.contains("song ngành")
                || q.contains("hai chương trình") || q.contains("miễn giảm") || q.contains("xếp loại")
                || q.contains("thang điểm") || q.contains("khóa luận");

        // Ưu tiên trả lời từ file Quy chế 3344/QĐ-DCT nếu sinh viên hỏi về quy định
        if (asksRegulation && regChunks != null && !regChunks.isEmpty()) {
            StringBuilder sb = new StringBuilder(
                    "📘 **Theo Quy chế đào tạo HUIT (QĐ 3344/QĐ-DCT ngày 05/09/2025):**\n\n");
            int limit = Math.min(2, regChunks.size());
            for (int i = 0; i < limit; i++) {
                RegulationKnowledgeService.DocChunk ch = regChunks.get(i).chunk;
                String excerpt = ch.content.length() > 620 ? ch.content.substring(0, 620).trim() + "..." : ch.content;
                sb.append("• **").append(ch.article).append("** _(Trang ").append(ch.page).append(")_:\n")
                        .append(excerpt).append("\n\n");
            }
            return sb.toString().trim();
        }

        if (ctx.student == null) {
            if (regChunks != null && !regChunks.isEmpty()) {
                RegulationKnowledgeService.DocChunk ch = regChunks.get(0).chunk;
                String excerpt = ch.content.length() > 650 ? ch.content.substring(0, 650).trim() + "..." : ch.content;
                return "📘 **Theo Quy chế đào tạo HUIT (QĐ 3344/QĐ-DCT):**\n• **" + ch.article
                        + "** _(Trang " + ch.page + ")_:\n" + excerpt;
            }
            return "Chào bạn! Mình là **Gấu HUIT**. Hãy đăng nhập bằng tài khoản **Sinh viên** để tra cứu lịch học, điểm số, học phí cá nhân, hoặc hỏi mình về **Quy chế đào tạo HUIT (QĐ 3344/QĐ-DCT)** nhé!";
        }

        String hoTen = ctx.student.getNguoiDung() != null && ctx.student.getNguoiDung().getHoTen() != null
                ? ctx.student.getNguoiDung().getHoTen()
                : ctx.student.getMssv();

        // Lịch học / Thời khóa biểu
        if (q.contains("lịch") || q.contains("thời khóa biểu") || q.contains("tkb") || q.contains("phòng")
                || q.contains("thứ") || q.contains("học gì")) {
            if (ctx.schedules.isEmpty()) {
                return "Hiện tại bạn chưa có lịch học nào trong học kỳ **"
                        + (ctx.activeSemester != null ? ctx.activeSemester.getTenHocKy() : "hiện tại") + "**.";
            }
            StringBuilder sb = new StringBuilder("📅 **Lịch học kỳ này của " + hoTen + ":**\n");
            for (LichHoc lh : ctx.schedules) {
                String dayName = lh.getThu() != null ? (lh.getThu() == 8 ? "Chủ nhật" : "Thứ " + lh.getThu()) : "";
                sb.append("• **").append(lh.getLopHocPhan().getHocPhan().getTenHP()).append("** — ")
                        .append(dayName).append(", Tiết ").append(lh.getTietBatDau()).append("-")
                        .append(lh.getTietKetThuc())
                        .append(" (Phòng `").append(lh.getPhongHoc() != null ? lh.getPhongHoc() : "N/A").append("`)\n");
            }
            return sb.toString();
        }

        // Học phí / Công nợ
        if (q.contains("học phí") || q.contains("tiền") || q.contains("nợ") || q.contains("đóng")) {
            if (ctx.currentDebt != null) {
                long tong = ctx.currentDebt.getTongHocPhi() != null ? ctx.currentDebt.getTongHocPhi().longValue() : 0L;
                long daDong = ctx.currentDebt.getDaDong() != null ? ctx.currentDebt.getDaDong().longValue() : 0L;
                long conNo = Math.max(0L, tong - daDong);
                return "💰 **Thông tin học phí kỳ này:**\n"
                        + "• Tổng học phí: **" + vnd.format(tong) + " đ**\n"
                        + "• Đã đóng: **" + vnd.format(daDong) + " đ**\n"
                        + "• Còn nợ: **" + vnd.format(conNo) + " đ**\n"
                        + "• Số tín chỉ đã đăng ký: **" + ctx.currentCredits + " TC**";
            }
            long est = ctx.currentCredits * 570_000L;
            return "💰 Kỳ này bạn đã đăng ký **" + ctx.currentCredits + " tín chỉ**.\n"
                    + "• Học phí tạm tính (570.000 đ/TC): **" + vnd.format(est) + " đ**.";
        }

        // Điểm số / GPA / CPA
        if (q.contains("điểm") || q.contains("gpa") || q.contains("cpa") || q.contains("tín chỉ")
                || q.contains("kết quả")) {
            StringBuilder sb = new StringBuilder("🎓 **Kết quả học tập của " + hoTen + ":**\n");
            sb.append("• CPA tích lũy (hệ 4): **")
                    .append(String.format("%.2f", ctx.student.getCpaTichLuy())).append("**\n");
            sb.append("• Tín chỉ tích lũy: **")
                    .append(ctx.student.getTongTcTichLuy()).append(" TC**\n");
            if (!ctx.grades.isEmpty()) {
                sb.append("\n**Một số môn đã có điểm:**\n");
                int limit = Math.min(6, ctx.grades.size());
                for (int i = 0; i < limit; i++) {
                    DiemHocPhan d = ctx.grades.get(i);
                    sb.append("• ").append(d.getDangKyHocPhan().getLopHocPhan().getHocPhan().getTenHP())
                            .append(": **")
                            .append(d.getDiemTongKetHe10() != null ? String.format("%.1f", d.getDiemTongKetHe10())
                                    : "N/A")
                            .append("** (").append(d.getDiemChu() != null ? d.getDiemChu() : "-").append(")\n");
                }
            }
            return sb.toString();
        }

        // Nếu có trích đoạn Quy chế điểm số cao thì trả lời theo Quy chế 3344
        if (regChunks != null && !regChunks.isEmpty() && regChunks.get(0).score >= 4.0) {
            RegulationKnowledgeService.DocChunk ch = regChunks.get(0).chunk;
            String excerpt = ch.content.length() > 650 ? ch.content.substring(0, 650).trim() + "..." : ch.content;
            return "📘 **Theo Quy chế đào tạo HUIT (QĐ 3344/QĐ-DCT):**\n• **" + ch.article
                    + "** _(Trang " + ch.page + ")_:\n" + excerpt;
        }

        return "Chào **" + hoTen + "**! 🐻 Mình là **Gấu HUIT** — trợ lý cá nhân của bạn.\n\n"
                + "Bạn có thể hỏi mình về:\n"
                + "• 📅 **Lịch học kỳ này** (thứ, tiết, phòng học)\n"
                + "• 🎓 **Điểm số, CPA & tín chỉ tích lũy**\n"
                + "• 💰 **Học phí & công nợ hiện tại**\n"
                + "• 📘 **Quy chế đào tạo HUIT (QĐ 3344/QĐ-DCT)**: cảnh báo học vụ, đăng ký/rút học phần, xét tốt nghiệp, phúc khảo, bảo lưu...";
    }

    private static class StudentContext {
        SinhVien student;
        HocKy activeSemester;
        List<LichHoc> schedules = new ArrayList<>();
        List<DiemHocPhan> grades = new ArrayList<>();
        CongNoHocKy currentDebt;
        int currentCredits = 0;
        String contextText = "";
    }
}
