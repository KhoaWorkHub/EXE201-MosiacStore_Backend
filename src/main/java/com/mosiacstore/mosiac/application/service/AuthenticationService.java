package com.mosiacstore.mosiac.application.service;


import com.mosiacstore.mosiac.application.dto.request.AuthenticationRequest;
import com.mosiacstore.mosiac.application.dto.response.AuthenticationResponse;
import com.mosiacstore.mosiac.application.exception.AuthenticationException;
import com.mosiacstore.mosiac.application.mapper.AuthMapper;
import com.mosiacstore.mosiac.domain.user.User;

import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import com.mosiacstore.mosiac.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthMapper authMapper;

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (Exception e) {
            throw new AuthenticationException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        CustomUserDetail userDetails = new CustomUserDetail(user);
        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(authMapper.toUserDto(user))
                .build();
    }
}