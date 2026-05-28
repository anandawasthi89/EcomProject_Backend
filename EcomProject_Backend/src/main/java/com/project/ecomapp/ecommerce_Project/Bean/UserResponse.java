package com.project.ecomapp.ecommerce_Project.Bean;

public record UserResponse(
        Integer id,
        String name,
        String email,
        UserRole role,
        boolean flagged,
        boolean active
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isFlagged(),
                user.isActive()
        );
    }
}
