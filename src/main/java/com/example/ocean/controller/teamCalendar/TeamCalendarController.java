package com.example.ocean.controller.teamCalendar;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TeamCalendarController {

    @GetMapping("/teamCalendar")
    public String hihi(){
        return "calendar";
    }

}
