package com.tasos.demo.controller;

import com.tasos.demo.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private MessageService messageService;

    @GetMapping("/")
    public String home(Model model) {
        logger.debug("Start HomeController.home");
        model.addAttribute("message", messageService.getMessage());
        logger.debug("End HomeController.home");
        return "home";
    }

    @GetMapping("/marina")
    public String marina(Model model) {
        logger.debug("Start HomeController.marina");
        model.addAttribute("message", messageService.getMessageForMarina());
        logger.debug("End HomeController.marina");
        return "marina";
    }
}
