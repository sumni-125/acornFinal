package com.example.ocean.service;

import com.example.ocean.repository.CalendarEventRepository;
import com.example.ocean.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;

@Service
@RequiredArgsConstructor
public class EventAlarmMailService {

    private final CalendarEventRepository calendarEventRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private JavaMailSender  emailSender;

    // 초(0–59) 분(0–59) 시(0–23) 일(1–31) 월(1–12) 요일(0–7)(0과 7은 일요일)
    @Scheduled(cron = "0 0 8 * * *")
    public void alarm8am(){

    }

    @Scheduled(cron = "0 0 20 * * *")
    public void alarm8pm(){

    }
}
