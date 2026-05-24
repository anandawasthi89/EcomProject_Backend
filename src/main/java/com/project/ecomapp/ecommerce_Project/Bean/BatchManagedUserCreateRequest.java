package com.project.ecomapp.ecommerce_Project.Bean;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

public record BatchManagedUserCreateRequest(
        @NotEmpty(message = "At least one user is required")
        List<@Valid ManagedUserCreateRequest> users
) {
}
