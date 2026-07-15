package com.bridgeinspection.service;

import com.bridgeinspection.common.BusinessException;
import com.bridgeinspection.dto.LoginRequest;
import com.bridgeinspection.dto.LoginResponse;
import com.bridgeinspection.dto.RegisterRequest;
import com.bridgeinspection.dto.RegisterResponse;
import com.bridgeinspection.security.AuthenticatedUser;
import com.bridgeinspection.security.JwtService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public AuthService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder, JwtService jwtService,
                       ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    public LoginResponse login(LoginRequest request) {
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
                """
                SELECT u.*, r.role_code, r.permission_set
                FROM tb_user u JOIN tb_role r ON r.role_id = u.role_id
                WHERE u.login_account = ? LIMIT 1
                """, request.account());
        if (users.isEmpty()) {
            throw new BusinessException(401, "账号或密码错误");
        }
        Map<String, Object> userRow = users.get(0);
        if (((Number) userRow.getOrDefault("user_status", 0)).intValue() != 1) {
            throw new BusinessException(403, "账号已停用");
        }
        String encoded = String.valueOf(userRow.get("password"));
        if (!passwordEncoder.matches(request.password(), encoded)) {
            throw new BusinessException(401, "账号或密码错误");
        }

        String userId = String.valueOf(userRow.get("user_id"));
        AuthenticatedUser user = new AuthenticatedUser(
                userId,
                String.valueOf(userRow.get("user_name")),
                String.valueOf(userRow.get("login_account")),
                List.of(String.valueOf(userRow.get("role_code"))),
                permissions(userRow.get("permission_set"))
        );
        jdbcTemplate.update("UPDATE tb_user SET last_login_time = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
        return new LoginResponse(jwtService.createToken(user), user, homePath(user.roles().get(0)));
    }

    public RegisterResponse register(RegisterRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        Integer roleId = jdbcTemplate.queryForObject(
                "SELECT role_id FROM tb_role WHERE role_code = 'viewer' LIMIT 1", Integer.class);
        if (roleId == null) {
            throw new BusinessException("两次输入的密码不一致?");
        }
        try {
            jdbcTemplate.update("""
                    INSERT INTO tb_user
                    (user_name, login_account, password, role_id, department, phone, email, user_status, force_pwd_change)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0)
                    """,
                    request.realName().trim(), request.account().trim(), passwordEncoder.encode(request.password()),
                    roleId, blankToNull(request.department()), blankToNull(request.phone()), blankToNull(request.email()));
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("该登录账号已被注册");
        }
        Integer userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM tb_user WHERE login_account = ?", Integer.class, request.account().trim());
        return new RegisterResponse(userId, request.account().trim(), request.realName().trim(), "viewer", "查询人员");
    }

    public void changePassword(String userId, String oldPassword, String newPassword) {
        Map<String, Object> user = jdbcTemplate.queryForMap("SELECT * FROM tb_user WHERE user_id = ?", userId);
        String encoded = String.valueOf(user.get("password"));
        if (!passwordEncoder.matches(oldPassword, encoded)) {
            throw new BusinessException("原密码不正确");
        }
        if (newPassword == null || newPassword.length() < 8 || !newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BusinessException("新密码不少于8位，且必须包含字母和数字");
        }
        jdbcTemplate.update("UPDATE tb_user SET password = ?, force_pwd_change = 0 WHERE user_id = ?",
                passwordEncoder.encode(newPassword), userId);
    }

    public AuthenticatedUser loadUser(String userId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                SELECT u.*, r.role_code, r.permission_set
                FROM tb_user u JOIN tb_role r ON r.role_id = u.role_id
                WHERE u.user_id = ?
                """, userId);
        return new AuthenticatedUser(
                userId,
                String.valueOf(row.get("user_name")),
                String.valueOf(row.get("login_account")),
                List.of(String.valueOf(row.get("role_code"))),
                permissions(row.get("permission_set"))
        );
    }

    private List<String> permissions(Object value) {
        try {
            return objectMapper.readValue(String.valueOf(value), new TypeReference<>() { });
        } catch (Exception ex) {
            throw new BusinessException("Role permission configuration is invalid");
        }
    }

    private String homePath(String roleCode) {
        return switch (roleCode) {
            case "admin" -> "/dashboard/admin";
            case "engineer" -> "/dashboard/engineer";
            case "inspector" -> "/dashboard/inspector";
            case "reviewer" -> "/dashboard/reviewer";
            default -> "/dashboard/viewer";
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
