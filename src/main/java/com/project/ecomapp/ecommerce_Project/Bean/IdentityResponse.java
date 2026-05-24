package com.project.ecomapp.ecommerce_Project.Bean;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public record IdentityResponse(
        Integer id,
        String name,
        String email,
        UserRole role,
        boolean flagged,
        boolean active,
        List<String> authorities
) {
    public static IdentityResponse from(User user, Collection<? extends GrantedAuthority> authorities) {
        return new IdentityResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isFlagged(),
                user.isActive(),
                authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .sorted(Comparator.naturalOrder())
                        .toList()
        );
    }
}
