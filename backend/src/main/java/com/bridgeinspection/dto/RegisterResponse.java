package com.bridgeinspection.dto;

public record RegisterResponse(
        Integer userId,
        String account,
        String realName,
        String roleCode,
        String roleName
) {
}
