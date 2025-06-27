package com.tasos.demo.controller.restaurant;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GrillRestaurantController {

    @GetMapping("/grill-restaurant")
    public String grillRestaurant(Model model) {
        return "restaurant/grill-restaurant";
    }

    @GetMapping("/grill-menu")
    public String grillMenu(Model model) {
        return "restaurant/grill-menu";
    }

}
