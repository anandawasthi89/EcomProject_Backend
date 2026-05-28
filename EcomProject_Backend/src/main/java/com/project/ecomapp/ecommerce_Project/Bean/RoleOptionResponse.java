package com.project.ecomapp.ecommerce_Project.Bean;

import java.util.List;

public record RoleOptionResponse(
        UserRole role,
        String description,
        List<String> authorities
) {
    public static RoleOptionResponse from(UserRole role) {
        return new RoleOptionResponse(role, role.description(), role.permissions());
    }
}
