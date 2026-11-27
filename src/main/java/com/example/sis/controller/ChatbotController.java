package com.example.sis.controller;

import lombok.RequiredArgsConstructor;
import com.example.sis.entity.NguoiDung;
import com.example.sis.service.StudentChatbotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final StudentChatbotService studentChatbotService;

    @SuppressWarnings("unchecked")
    @PostMapping("/ask")
    public ResponseEntity<Map<String, Object>> ask(@RequestBody Map<String, Object> body, HttpSession session) {
        NguoiDung currentUser = (NguoiDung) session.getAttribute("currentUser");
        String message = body.get("message") != null ? String.valueOf(body.get("message")) : "";
        List<Map<String, String>> history = null;
        if (body.get("history") instanceof List<?>) {
            history = (List<Map<String, String>>) body.get("history");
        }
        Map<String, Object> result = studentChatbotService.ask(currentUser, message, history);
        return ResponseEntity.ok(result);
    }
}
