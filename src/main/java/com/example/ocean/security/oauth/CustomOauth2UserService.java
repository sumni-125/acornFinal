package com.example.ocean.security.oauth;

import com.example.ocean.entity.User;
import com.example.ocean.repository.UserRepository;
import com.example.ocean.security.oauth.provider.OAuth2UserInfo;
import com.example.ocean.security.oauth.provider.OAuth2UserInfoFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOauth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
            OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
                registrationId,oAuth2User.getAttributes()
        );

        User user = userRepository.findbyEmail(oAuth2UserInfo.getEmail())
                .map

    }
}
