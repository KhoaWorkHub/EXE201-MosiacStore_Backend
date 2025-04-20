package com.mosiacstore.mosiac.infrastructure.security;

import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.domain.user.UserRole;
import com.mosiacstore.mosiac.domain.user.UserStatus;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        // Parse OAuth2 authentication token
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        // Extract user info
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        String providerId = (String) attributes.get("sub");  // Google uses "sub" for user ID
        String name = (String) attributes.get("name");

        // Check if user exists
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            // User exists - update OAuth info if needed
            user = userOptional.get();
            if (user.getProvider() == null || !user.getProvider().equals(provider) ||
                    user.getProviderId() == null || !user.getProviderId().equals(providerId)) {

                user.setProvider(provider.toUpperCase());
                user.setProviderId(providerId);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        } else {
            // Create new user
            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .provider(provider.toUpperCase())
                    .providerId(providerId)
                    .passwordHash(UUID.randomUUID().toString()) // Random password
                    .role(UserRole.CUSTOMER)
                    .status(UserStatus.ACTIVE)
                    .build();

            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        // Generate JWT token
        CustomUserDetail userDetail = new CustomUserDetail(user);
        String token = jwtService.generateToken(userDetail);
        String refreshToken = jwtService.generateRefreshToken(userDetail);

        // Redirect with token (frontend will capture this)
        // For development, you might redirect to frontend with token as parameter
        response.sendRedirect("http://localhost:3000/oauth2/redirect?token=" + token + "&refreshToken=" + refreshToken);
    }
}