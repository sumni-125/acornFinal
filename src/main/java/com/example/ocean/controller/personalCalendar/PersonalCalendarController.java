package com.example.ocean.controller.personalCalendar;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PersonalCalendarController {
    @GetMapping("/personalCalendar")
    public String main(){
        return "calendar";
    }
}
