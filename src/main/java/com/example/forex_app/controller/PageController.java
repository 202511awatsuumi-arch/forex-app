package com.example.forex_app.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/convert")
    public String convert() {
        return "convert";
    }

    @GetMapping("/history")
    public String history() {
        return "history";
    }

    @GetMapping("/alerts")
    public String alerts() {
        return "alerts";
    }
}