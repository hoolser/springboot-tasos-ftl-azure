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
        logger.info("Start HomeController.home");
        model.addAttribute("message", messageService.getMessage());
        logger.info("End HomeController.home");
        return "home";
    }

    @GetMapping("/marina")
    public String marina(Model model) {
        logger.info("Start HomeController.marina");
        model.addAttribute("message", messageService.getMessageForMarina());
        logger.info("End HomeController.marina");
        return "marina";
    }

    @GetMapping("/tasos")
    public String tasos(Model model) {
        logger.info("Start HomeController.tasos");
        model.addAttribute("message", messageService.getMessageForTasos());
        logger.info("End HomeController.tasos");
        return "home";
    }

    @GetMapping("/storage-blob-page")
    public String storageBlob(Model model) {
        logger.info("Start HomeController.storage-blob");
        model.addAttribute("message", messageService.getMessageForBlob());
        logger.info("End HomeController.storage-blob");
        return "storage-blob";
    }

    @GetMapping("/share-blob-page")
    public String shareBlob(Model model) {
        logger.info("Start HomeController.shareBlob");
        model.addAttribute("message", messageService.getMessageForShareBlob());
        logger.info("End HomeController.shareBlob");
        return "share-file-blob";
    }

}
