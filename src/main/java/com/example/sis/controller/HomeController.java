package com.example.sis.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/Account/Login";
        }
        return "redirect:/Account/Login";
    }
}
