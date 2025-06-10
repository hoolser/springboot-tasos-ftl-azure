package com.tasos.demo.controller;

import com.tasos.demo.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("message", messageService.getMessage());
        return "home";
    }
}
