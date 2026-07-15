package com.bridgeinspection.security;

import java.util.List;

public record AuthenticatedUser(
        String userId,
        String userName,
        String loginAccount,
        List<String> roles,
        List<String> permissions
) {
}
