package com.example.ocean.controller.personalCalendar;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PersonalCalendarController {
    @GetMapping("/calendar/personal")
    public String personalCalendar(Model model) {
        model.addAttribute("currentUserId", "102049037194510417221");
        return "personalCalendar";
    }
}
