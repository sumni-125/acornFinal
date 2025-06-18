package com.example.ocean.service;

import com.example.ocean.dto.request.UserOnboardingRequest;
import com.example.ocean.dto.request.UserProfileUpdateRequest;
import com.example.ocean.dto.response.UserProfileResponse;
import com.example.ocean.entity.User;
import com.example.ocean.entity.WorkspaceMember;
import com.example.ocean.exception.ResourceNotFoundException;
import com.example.ocean.repository.UserRepository;
import com.example.ocean.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    // User + WorkspaceMember 정보를 합쳐서 UserProfileResponse로 변환
    private UserProfileResponse mapToUserProfileResponse(User user, WorkspaceMember workspaceMember) {
        UserProfileResponse.UserProfileResponseBuilder builder = UserProfileResponse.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userProfileImg(user.getUserImg())
                .provider(user.getProvider().name())
                .isActive(user.getActiveState().equals("Y"))
                .createdAt(user.getCreatedDate());

        // WorkspaceMember 정보가 있으면 추가
        if (workspaceMember != null) {
            builder.nickName(workspaceMember.getUserNickname())
                    .email(workspaceMember.getEmail())
                    .phoneNumber(workspaceMember.getPhoneNum())
                    .department(workspaceMember.getDeptCd())
                    .position(workspaceMember.getPosition())
                    .workspaceCd(workspaceMember.getWorkspace().getWorkspaceCd())
                    .workspaceName(workspaceMember.getWorkspace().getWorkspaceNm());
        }

        return builder.build();
    }

    // 사용자 프로필 조회
    public UserProfileResponse getUserProfile(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        // 사용자의 첫 번째 활성 워크스페이스 정보 가져오기
        List<WorkspaceMember> workspaceMembers = workspaceMemberRepository.findFirstWorkspaceInfo(userId);
        WorkspaceMember primaryWorkspace = workspaceMembers.isEmpty() ? null : workspaceMembers.get(0);

        return mapToUserProfileResponse(user, primaryWorkspace);
    }

    // 프로필 수정 (워크스페이스별로 수정)
    @Transactional
    public UserProfileResponse updateUserProfile(String userId, String workspaceCd, UserProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        WorkspaceMember workspaceMember = workspaceMemberRepository
                .findByUserIdAndWorkspaceCd(userId, workspaceCd)
                .orElseThrow(() -> new ResourceNotFoundException("워크스페이스 멤버 정보를 찾을 수 없습니다."));

        // WorkspaceMember 정보 업데이트
        if (request.getNickName() != null) {
            workspaceMember.setUserNickname(request.getNickName());
        }
        if (request.getPhoneNumber() != null) {
            workspaceMember.setPhoneNum(request.getPhoneNumber());
        }
        if (request.getDepartment() != null) {
            workspaceMember.setDeptCd(request.getDepartment());
        }
        if (request.getPosition() != null) {
            workspaceMember.setPosition(request.getPosition());
        }
        if (request.getEmail() != null) {
            workspaceMember.setEmail(request.getEmail());
        }

        WorkspaceMember savedMember = workspaceMemberRepository.save(workspaceMember);

        log.info("사용자 프로필 수정 완료 - userId: {}, workspaceCd: {}", userId, workspaceCd);
        return mapToUserProfileResponse(user, savedMember);
    }

    // 온보딩 완료 (워크스페이스에 추가 정보 입력)
    @Transactional
    public UserProfileResponse completeOnboarding(String userId, String workspaceCd, UserOnboardingRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        WorkspaceMember workspaceMember = workspaceMemberRepository
                .findByUserIdAndWorkspaceCd(userId, workspaceCd)
                .orElseThrow(() -> new ResourceNotFoundException("워크스페이스 멤버 정보를 찾을 수 없습니다."));

        // 온보딩 정보 설정
        workspaceMember.setUserNickname(request.getNickname());
        workspaceMember.setPhoneNum(request.getPhoneNumber());
        workspaceMember.setDeptCd(request.getDepartment());
        workspaceMember.setPosition(request.getPosition());
        workspaceMember.setEmail(request.getEmail());

        WorkspaceMember savedMember = workspaceMemberRepository.save(workspaceMember);

        log.info("사용자 온보딩 완료 - userId: {}, workspaceCd: {}", userId, workspaceCd);
        return mapToUserProfileResponse(user, savedMember);
    }

    // 사용자의 모든 워크스페이스 조회
    public List<WorkspaceMember> getUserWorkspaces(String userId) {
        return workspaceMemberRepository.findActiveWorkspacesByUserId(userId);
    }
}