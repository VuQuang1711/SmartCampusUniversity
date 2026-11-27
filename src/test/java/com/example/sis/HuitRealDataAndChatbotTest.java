package com.example.sis;

import com.example.sis.entity.HocKy;
import com.example.sis.entity.LopHocPhan;
import com.example.sis.repository.CoSoRepository;
import com.example.sis.repository.GiangVienRepository;
import com.example.sis.repository.HocKyRepository;
import com.example.sis.repository.LichHocTuanRepository;
import com.example.sis.repository.LopHocPhanRepository;
import com.example.sis.repository.PhongHocRepository;
import com.example.sis.service.CourseRegistrationService;
import com.example.sis.service.RegulationKnowledgeService;
import com.example.sis.service.StudentChatbotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "app.cloudflare.tunnel.enabled=false",
        "app.gemini.api-key="
})
class HuitRealDataAndChatbotTest {

    @Autowired
    private RegulationKnowledgeService regulationKnowledgeService;
    @Autowired
    private StudentChatbotService studentChatbotService;
    @Autowired
    private HocKyRepository hocKyRepository;
    @Autowired
    private LopHocPhanRepository lopHocPhanRepository;
    @Autowired
    private GiangVienRepository giangVienRepository;
    @Autowired
    private CoSoRepository coSoRepository;
    @Autowired
    private PhongHocRepository phongHocRepository;
    @Autowired
    private LichHocTuanRepository lichHocTuanRepository;
    @Autowired
    private CourseRegistrationService courseRegistrationService;

    @Test
    @Transactional
    void verifyReRegisterAfterDropDoesNotThrowNpe() {
        // Re-registering a previously dropped class (diemChu=null) must never throw
        // NullPointerException
        CourseRegistrationService.RegistrationResult res = assertDoesNotThrow(
                () -> courseRegistrationService.registerCourse(1L, 78L),
                "Registering or re-registering a class must never throw NullPointerException");
        assertNotNull(res);
        assertNotNull(res.getMessage());
        assertFalse(res.getMessage().contains("NullPointerException"));
    }

    @Test
    void verifyRegulationRagAndRealHuitDatabase() {
        // 1. Verify Regulation PDF loaded
        assertTrue(regulationKnowledgeService.getTotalChunks() > 20, "Regulation 3344 PDF must be indexed");

        // Test RAG queries
        String[] queries = {
                "Quy định cảnh báo học vụ và buộc thôi học như thế nào?",
                "Sinh viên được đăng ký tối đa bao nhiêu tín chỉ trong học kỳ?",
                "Điều kiện xét tốt nghiệp đại học tại HUIT là gì?"
        };
        for (String q : queries) {
            List<RegulationKnowledgeService.RetrievedChunk> hits = regulationKnowledgeService.retrieveRelevantChunks(q,
                    3);
            assertFalse(hits.isEmpty(), "Should find regulation chunks for: " + q);
        }

        // Test chatbot response on regulation question
        Map<String, Object> botRes = studentChatbotService.ask(null, "Điều kiện cảnh báo học vụ theo quy chế 3344?",
                null);
        assertNotNull(botRes.get("reply"));

        // 2. Verify active ĐKHP semester is REAL HUIT semester (2026_2) and NOT mock
        // 2024_2
        HocKy active = hocKyRepository.findFirstByTrangThaiDangKy(HocKy.TrangThaiDangKy.DANG_MO).orElse(null);
        assertNotNull(active, "Active registration semester must exist");
        assertEquals("2026_2", active.getMaHocKy(), "Active semester must be real HUIT 2026_2");

        List<LopHocPhan> openClasses = lopHocPhanRepository.findByHocKyIdAndTrangThai(active.getId(),
                LopHocPhan.TrangThaiLHP.DANG_MO);
        assertTrue(openClasses.size() >= 40, "Should have real HUIT 15DHKHDL classes in 2026_2");

        // Verify no mock GV00* codes remain
        long mockGvCount = giangVienRepository.findAll().stream()
                .filter(g -> g.getMaGV() != null && g.getMaGV().startsWith("GV00"))
                .count();
        assertEquals(0, mockGvCount, "No mock GV00* lecturer codes should remain");

        // 3. Verify normalized 3NF HUIT tables populated from class_database Excel
        // files
        assertTrue(coSoRepository.count() > 0, "co_so table must be populated");
        assertTrue(phongHocRepository.count() > 0, "phong_hoc table must be populated");
        assertTrue(lichHocTuanRepository.count() > 100, "lich_hoc_tuan (T1..T30) must be populated");
    }
}
