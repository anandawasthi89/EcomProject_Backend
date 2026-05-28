package com.project.ecomapp.ecommerce_Project.Bean;

import javax.validation.constraints.NotNull;

public record RoleAssignmentRequest(
        @NotNull(message = "Role is required")
        UserRole role
) {
}
