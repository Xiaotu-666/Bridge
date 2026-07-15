package com.bridgeinspection.controller;

import com.bridgeinspection.common.ApiResponse;
import com.bridgeinspection.dto.LoginRequest;
import com.bridgeinspection.dto.LoginResponse;
import com.bridgeinspection.dto.RegisterRequest;
import com.bridgeinspection.dto.RegisterResponse;
import com.bridgeinspection.security.SecurityUtils;
import com.bridgeinspection.service.AuthService;
import com.bridgeinspection.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final OperationLogService logService;

    public AuthController(AuthService authService, OperationLogService logService) {
        this.authService = authService;
        this.logService = logService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request);
        logService.log(httpRequest, "system", "登录", "tb_user", response.user().userId(), "用户登录", true);
        return ApiResponse.ok(response);
    }

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @GetMapping("/me")
    public ApiResponse<?> me() {
        return ApiResponse.ok(SecurityUtils.currentUserOrNull());
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        authService.changePassword(SecurityUtils.currentUserId(), body.get("oldPassword"), body.get("newPassword"));
        logService.log(request, "system", "修改", "tb_user", SecurityUtils.currentUserId(), "修改登录密码", true);
        return ApiResponse.ok(null);
    }
}
