package com.portfolio.jobtracker.auth.dto;

import com.portfolio.jobtracker.user.User;

public record UserResponse(Long id, String name, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail());
    }
}
