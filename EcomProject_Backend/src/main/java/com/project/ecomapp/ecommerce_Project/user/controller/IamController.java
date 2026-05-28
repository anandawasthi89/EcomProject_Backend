package com.project.ecomapp.ecommerce_Project.user.controller;

import com.project.ecomapp.ecommerce_Project.Bean.BatchManagedUserCreateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistoryResponse;
import com.project.ecomapp.ecommerce_Project.Bean.IdentityResponse;
import com.project.ecomapp.ecommerce_Project.Bean.ManagedUserCreateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.RoleOptionResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.library.service.LibraryService;
import com.project.ecomapp.ecommerce_Project.user.service.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/iam")
@Validated
public class IamController {

    private static final Logger LOGGER = LoggerFactory.getLogger(IamController.class);

    private final CustomUserDetailsService userService;
    private final LibraryService libraryService;

    public IamController(CustomUserDetailsService userService, LibraryService libraryService) {
        this.userService = userService;
        this.libraryService = libraryService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public IdentityResponse getCurrentIdentity(Authentication authentication) {
        LOGGER.debug("IAM identity requested email={}", authentication.getName());
        return userService.getIdentityByEmail(authentication.getName(), authentication.getAuthorities());
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<RoleOptionResponse> getSupportedRoles() {
        LOGGER.debug("IAM role catalog requested");
        return userService.getRoleOptions();
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<UserResponse> createManagedUser(@Valid @RequestBody ManagedUserCreateRequest request, Authentication authentication) {
        LOGGER.info("Managed user create requested email={} actor={}", request.email(), authentication.getName());
        UserResponse createdUser = userService.createManagedUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    @PostMapping("/users/batch")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<UserResponse>> createManagedUsers(@Valid @RequestBody BatchManagedUserCreateRequest request, Authentication authentication) {
        LOGGER.info("Managed user batch create requested count={} actor={}", request.users().size(), authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createManagedUsers(request));
    }

    @PatchMapping("/users/{id}/grant-read-access")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public UserResponse grantReadAccess(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Read-access upgrade requested targetId={} actor={}", id, authentication.getName());
        return userService.upgradeCustomerReadAccess(id, authentication.getAuthorities());
    }

    @GetMapping("/users/flagged")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<UserResponse> getFlaggedUsers(Authentication authentication) {
        LOGGER.debug("Flagged user list requested actor={}", authentication.getName());
        return userService.getFlaggedUsers(authentication.getAuthorities());
    }

    @GetMapping("/users/{id}/flag-status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public UserResponse getUserFlagStatus(@PathVariable Integer id, Authentication authentication) {
        LOGGER.debug("Flag status requested targetId={} actor={}", id, authentication.getName());
        return userService.getUserFlagStatus(id, authentication.getAuthorities());
    }

    @PostMapping("/users/flags/scan-overdue")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<UserResponse> scanOverduePhysicalLoans(Authentication authentication) {
        LOGGER.warn("Overdue scan requested actor={}", authentication.getName());
        return libraryService.scanAndFlagUsersWithOverduePhysicalLoans()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @PostMapping("/users/{id}/flags/resolve/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public UserResponse deactivateFlaggedUser(@PathVariable Integer id, Authentication authentication) {
        LOGGER.warn("Deactivate flagged user requested targetId={} actor={}", id, authentication.getName());
        return userService.deactivateUser(id, authentication.getAuthorities());
    }

    @PostMapping("/users/{id}/flags/resolve/force-return")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BookAccessHistoryResponse forceReturnPhysicalBook(
            @PathVariable Integer id,
            @RequestParam Integer bookId,
            Authentication authentication
    ) {
        LOGGER.warn("Force return requested targetId={} bookId={} actor={}", id, bookId, authentication.getName());
        return libraryService.forceReturnPhysicalBook(id, bookId, authentication.getAuthorities());
    }

    @PostMapping("/users/{id}/flags/resolve/extend-duration")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BookAccessHistoryResponse extendPhysicalCheckoutDuration(
            @PathVariable Integer id,
            @RequestParam Integer bookId,
            @RequestParam Integer extraDays,
            Authentication authentication
    ) {
        LOGGER.info(
                "Extend checkout duration requested targetId={} bookId={} extraDays={} actor={}",
                id,
                bookId,
                extraDays,
                authentication.getName()
        );
        return libraryService.extendPhysicalCheckoutDuration(id, bookId, extraDays, authentication.getAuthorities());
    }

    @GetMapping("/access/admin-console")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, String> adminConsole() {
        LOGGER.debug("Admin console access granted");
        return Map.of("scope", "admin-console", "status", "granted");
    }

    @GetMapping("/access/manager-console")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Map<String, String> managerConsole() {
        LOGGER.debug("Manager console access granted");
        return Map.of("scope", "manager-console", "status", "granted");
    }

    @GetMapping("/access/customer-read")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','CUSTOMER_WITH_READ_ALLOWED')")
    public Map<String, String> customerReadAccess() {
        LOGGER.debug("Customer-read access granted");
        return Map.of("scope", "customer-read", "status", "granted");
    }
}
