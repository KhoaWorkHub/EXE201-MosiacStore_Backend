package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.request.AuthenticationRequest;
import com.mosiacstore.mosiac.application.dto.response.AuthenticationResponse;
import com.mosiacstore.mosiac.application.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.DecoderException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @Operation(
            summary = "Đăng nhập người dùng",
            description = "Xác thực người dùng bằng email và mật khẩu, trả về JWT token"
    )
    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) throws DecoderException {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
}