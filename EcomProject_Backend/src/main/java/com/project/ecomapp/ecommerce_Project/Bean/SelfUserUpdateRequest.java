package com.project.ecomapp.ecommerce_Project.Bean;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public record SelfUserUpdateRequest(
        @NotBlank(message = "Name is required")
        String name,
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password
) {
}
