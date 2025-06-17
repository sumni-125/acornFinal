package com.example.ocean.security.oauth;

import com.example.ocean.entity.User;
import com.example.ocean.repository.UserRepository;
import com.example.ocean.security.oauth.UserPrincipal;
import com.example.ocean.security.oauth.provider.OAuth2UserInfo;
import com.example.ocean.security.oauth.provider.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest,oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,oAuth2User.getAttributes()
        );

        // 소셜 ID와 제공자로 사용자 조회
        User user = userRepository.findByUserIdAndProvider(oAuth2UserInfo.getId(), registrationId.toUpperCase())
                .map(existingUser -> updateUser(existingUser, oAuth2UserInfo))
                .orElse(createUser(oAuth2UserInfo, registrationId));

        return UserPrincipal.create(user,oAuth2User.getAttributes());
    }

    private User createUser(OAuth2UserInfo oAuth2UserInfo,String provider) {
        // 이메일이 없는 경우 대체 이메일 생성
        String email = oAuth2UserInfo.getEmail();
        if (email == null || email.isEmpty()) {
            email = oAuth2UserInfo.getId() + "@" + provider.toLowerCase() + ".oauth";
        }
        
        User user = User.builder()
                .userCode(UUID.randomUUID().toString())
                .userId(oAuth2UserInfo.getId())
                .userName(oAuth2UserInfo.getName())
                .email(email)
                .userProfileImg(oAuth2UserInfo.getImageUrl())
                .provider(provider.toUpperCase())
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    private User updateUser(User user, OAuth2UserInfo oAuth2UserInfo) {
        user.setUserName(oAuth2UserInfo.getName());
        user.setUserProfileImg(oAuth2UserInfo.getImageUrl());

        return userRepository.save(user);
    }
}
