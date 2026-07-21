package com.bridgeinspection.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static AuthenticatedUser currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return null;
        }
        return user;
    }

    public static String currentUserId() {
        AuthenticatedUser user = currentUserOrNull();
        return user == null ? null : user.userId();
    }

    public static Long currentUserIdLong() {
        String id = currentUserId();
        if (id == null) return null;
        try { return Long.valueOf(id); } catch (NumberFormatException e) { return null; }
    }

    public static String currentUserName() {
        AuthenticatedUser user = currentUserOrNull();
        return user == null ? null : user.userName();
    }
}
