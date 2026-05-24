package com.project.ecomapp.ecommerce_Project.Bean;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public enum UserRole {
    ADMIN(
            "Platform administrator with full IAM and user-management control",
            List.of("iam:read", "iam:write", "users:read", "users:write", "users:delete", "catalog:read")
    ),
    MANAGER(
            "Business manager who can operate on customer accounts and review IAM metadata",
            List.of("iam:read", "users:read", "users:write", "catalog:read")
    ),
    CUSTOMER(
            "Standard storefront customer with self-service account access",
            List.of("profile:read", "profile:write")
    ),
    CUSTOMER_WITH_READ_ALLOWED(
            "Customer with explicit read access to shared storefront resources",
            List.of("profile:read", "profile:write", "catalog:read")
    );

    private final String description;
    private final List<String> permissions;

    UserRole(String description, List<String> permissions) {
        this.description = description;
        this.permissions = permissions;
    }

    public String description() {
        return description;
    }

    public List<String> permissions() {
        return permissions;
    }

    public Collection<SimpleGrantedAuthority> asAuthorities() {
        return Stream.concat(
                        Stream.of(new SimpleGrantedAuthority("ROLE_" + name())),
                        permissions.stream().map(SimpleGrantedAuthority::new)
                )
                .toList();
    }
}
