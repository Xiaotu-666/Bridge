package com.bridgeinspection.dto;

import com.bridgeinspection.security.AuthenticatedUser;

public record LoginResponse(String token, AuthenticatedUser user, String homePath) {
}
