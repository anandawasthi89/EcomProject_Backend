package com.project.ecomapp.ecommerce_Project.user.service;

import com.project.ecomapp.ecommerce_Project.Bean.BatchManagedUserCreateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.IdentityResponse;
import com.project.ecomapp.ecommerce_Project.Bean.ManagedUserCreateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.RoleOptionResponse;
import com.project.ecomapp.ecommerce_Project.Bean.SelfUserUpdateRequest;
import com.project.ecomapp.ecommerce_Project.Bean.CustomUserDetails;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Bean.UserRole;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserUpsertRequest;
import com.project.ecomapp.ecommerce_Project.user.repository.UserRepDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepDAO userRepDAO;
    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(UserRepDAO userRepDAO, PasswordEncoder passwordEncoder) {
        this.userRepDAO = userRepDAO;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepDAO.findByEmail(normalizeEmail(email))
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public List<UserResponse> getAllUsers() {
        return userRepDAO.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    public List<UserResponse> getFlaggedUsers(Collection<? extends GrantedAuthority> actorAuthorities) {
        return userRepDAO.findAllByFlaggedTrueOrderByIdAsc()
                .stream()
                .filter(user -> canManageTarget(user, actorAuthorities))
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse getUserByEmail(String email) {
        return UserResponse.from(findUserByEmail(email));
    }

    public UserResponse getUserById(Integer id, Collection<? extends GrantedAuthority> actorAuthorities) {
        User user = findUserById(id);
        assertCanManageTarget(user, actorAuthorities);
        return UserResponse.from(user);
    }

    public UserResponse getUserFlagStatus(Integer id, Collection<? extends GrantedAuthority> actorAuthorities) {
        User user = findUserById(id);
        assertCanManageTarget(user, actorAuthorities);
        return UserResponse.from(user);
    }

    public IdentityResponse getIdentityByEmail(String email, Collection<? extends GrantedAuthority> authorities) {
        return IdentityResponse.from(findUserByEmail(email), authorities);
    }

    public List<RoleOptionResponse> getRoleOptions() {
        return Arrays.stream(UserRole.values())
                .map(RoleOptionResponse::from)
                .toList();
    }

    public Optional<User> findUser(Integer id) {
        return userRepDAO.findById(id);
    }

    @Transactional
    public UserResponse addNewUser(String email, String rawPassword, String name) {
        UserResponse createdUser = createUser(email, rawPassword, name, UserRole.CUSTOMER);
        LOGGER.info("Registered public customer account email={}", createdUser.email());
        return createdUser;
    }

    @Transactional
    public UserResponse createManagedUser(ManagedUserCreateRequest request) {
        UserResponse createdUser = createUser(request.email(), request.password(), request.name(), UserRole.CUSTOMER);
        LOGGER.info("Created managed customer account email={}", createdUser.email());
        return createdUser;
    }

    @Transactional
    public List<UserResponse> createManagedUsers(BatchManagedUserCreateRequest request) {
        List<UserResponse> createdUsers = request.users().stream()
                .map(this::createManagedUser)
                .toList();
        LOGGER.info("Created managed customer batch size={}", createdUsers.size());
        return createdUsers;
    }

    @Transactional
    public UserResponse updateCurrentUser(String currentEmail, SelfUserUpdateRequest request) {
        User user = findUserByEmail(currentEmail);
        user.setName(request.name().trim());
        user.setPassword(passwordEncoder.encode(request.password()));
        UserResponse updatedUser = UserResponse.from(userRepDAO.save(user));
        LOGGER.info("Updated self-service profile email={}", updatedUser.email());
        return updatedUser;
    }

    @Transactional
    public UserResponse upgradeCustomerReadAccess(Integer id, Collection<? extends GrantedAuthority> actorAuthorities) {
        User user = findUserById(id);
        assertCanManageTarget(user, actorAuthorities);
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only CUSTOMER users can be upgraded to CUSTOMER_WITH_READ_ALLOWED");
        }
        user.setRole(UserRole.CUSTOMER_WITH_READ_ALLOWED);
        UserResponse updatedUser = UserResponse.from(userRepDAO.save(user));
        LOGGER.info("Granted read-allowed role to user id={} email={}", updatedUser.id(), updatedUser.email());
        return updatedUser;
    }

    @Transactional
    public UserResponse createUser(String email, String rawPassword, String name, UserRole role) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepDAO.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setName(name != null && !name.isBlank() ? name.trim() : normalizedEmail);
        user.setRole(role);
        user.setActive(true);
        user.setFlagged(false);
        return UserResponse.from(userRepDAO.save(user));
    }

    @Transactional
    public UserResponse updateExistingUser(Integer id, UserUpsertRequest request, Collection<? extends GrantedAuthority> actorAuthorities) {
        Integer resolvedId = id != null ? id : request.id();
        if (resolvedId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }

        User user = findUserById(resolvedId);
        assertCanManageTarget(user, actorAuthorities);

        String normalizedEmail = normalizeEmail(request.email());

        userRepDAO.findByEmail(normalizedEmail)
                .filter(existingUser -> !existingUser.getId().equals(resolvedId))
                .ifPresent(existingUser -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
                });

        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        UserResponse updatedUser = UserResponse.from(userRepDAO.save(user));
        LOGGER.info("Updated managed user id={} email={}", updatedUser.id(), updatedUser.email());
        return updatedUser;
    }

    @Transactional
    public void deleteExistingUser(Integer id, Collection<? extends GrantedAuthority> actorAuthorities) {
        User user = findUserById(id);
        assertCanManageTarget(user, actorAuthorities);
        userRepDAO.delete(user);
        LOGGER.warn("Deleted user id={} email={}", user.getId(), user.getEmail());
    }

    @Transactional
    public UserResponse deactivateUser(Integer id, Collection<? extends GrantedAuthority> actorAuthorities) {
        User user = findUserById(id);
        assertCanManageTarget(user, actorAuthorities);
        user.setActive(false);
        user.setFlagged(false);
        UserResponse updatedUser = UserResponse.from(userRepDAO.save(user));
        LOGGER.warn("Deactivated user id={} email={}", updatedUser.id(), updatedUser.email());
        return updatedUser;
    }

    private User findUserByEmail(String email) {
        return userRepDAO.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User findUserById(Integer id) {
        return userRepDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> actorAuthorities, UserRole role) {
        String roleAuthority = "ROLE_" + role.name();
        return actorAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roleAuthority::equals);
    }

    private boolean canManageTarget(User targetUser, Collection<? extends GrantedAuthority> actorAuthorities) {
        if (hasRole(actorAuthorities, UserRole.ADMIN)) {
            return true;
        }
        return hasRole(actorAuthorities, UserRole.MANAGER)
                && (targetUser.getRole() == UserRole.CUSTOMER
                || targetUser.getRole() == UserRole.CUSTOMER_WITH_READ_ALLOWED);
    }

    private void assertCanManageTarget(User targetUser, Collection<? extends GrantedAuthority> actorAuthorities) {
        if (!canManageTarget(targetUser, actorAuthorities)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to manage this user");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
