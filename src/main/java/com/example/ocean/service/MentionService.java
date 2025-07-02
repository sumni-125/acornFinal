package com.example.ocean.service;

import com.example.ocean.dto.request.MentionNotification;
import com.example.ocean.dto.request.ReadNotiRequest;
import com.example.ocean.repository.MentionNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class MentionService {
    private final MentionNotificationRepository mentionNotificationRepository;
    public List<MentionNotification> selectUserNoti(String userId){
        return mentionNotificationRepository.selectUserNoti(userId);
    }

    public void updateAllUserNoti(String userId){
        mentionNotificationRepository.updateAllNoti(userId);
    }

    public void updateUserNoti(List<ReadNotiRequest> request){
        if(request != null){
            for(ReadNotiRequest noti : request){
                mentionNotificationRepository.updateNoti(noti);
            }
        }

    }
}
