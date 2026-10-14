package com.portfolio.jobtracker.auth.dto;

public record AuthResponse(String token, UserResponse user) {
}
