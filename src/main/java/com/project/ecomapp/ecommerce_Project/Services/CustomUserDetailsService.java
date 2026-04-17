package com.project.ecomapp.ecommerce_Project.Services;

import com.project.ecomapp.ecommerce_Project.Bean.CustomUserDetails;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Bean.UserResponse;
import com.project.ecomapp.ecommerce_Project.Bean.UserUpsertRequest;
import com.project.ecomapp.ecommerce_Project.Repository.UserRepDAO;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

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

    public UserResponse getUserByEmail(String email) {
        return userRepDAO.findByEmail(normalizeEmail(email))
                .map(UserResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public Optional<User> findUser(Integer id) {
        return userRepDAO.findById(id);
    }

    @Transactional
    public UserResponse addNewUser(String email, String rawPassword, String name) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepDAO.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setName(name != null && !name.isBlank() ? name.trim() : normalizedEmail);
        return UserResponse.from(userRepDAO.save(user));
    }

    @Transactional
    public UserResponse updateExistingUser(Integer id, UserUpsertRequest request) {
        Integer resolvedId = id != null ? id : request.id();
        if (resolvedId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User id is required");
        }

        User user = userRepDAO.findById(resolvedId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String normalizedEmail = normalizeEmail(request.email());

        userRepDAO.findByEmail(normalizedEmail)
                .filter(existingUser -> !existingUser.getId().equals(resolvedId))
                .ifPresent(existingUser -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
                });

        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepDAO.save(user));
    }

    @Transactional
    public void deleteExistingUser(Integer id) {
        User user = userRepDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        userRepDAO.delete(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
