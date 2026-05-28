package com.project.ecomapp.ecommerce_Project.service;

import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Bean.UserRole;
import com.project.ecomapp.ecommerce_Project.user.bootstrap.DevUserBootstrap;
import com.project.ecomapp.ecommerce_Project.user.repository.UserRepDAO;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevUserBootstrapUnitTest {

    @Test
    void runCreatesAdminAndManagerWhenMissing() throws Exception {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        DevUserBootstrap bootstrap = new DevUserBootstrap(
                userRepDAO,
                passwordEncoder,
                true,
                "admin@example.com",
                "admin12345",
                "Dev Admin",
                "manager@example.com",
                "manager12345",
                "Dev Manager"
        );

        when(userRepDAO.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(userRepDAO.findByEmail("manager@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin12345")).thenReturn("encoded-admin");
        when(passwordEncoder.encode("manager12345")).thenReturn("encoded-manager");
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        bootstrap.run();

        verify(userRepDAO, times(2)).save(any(User.class));
    }

    @Test
    void runUpdatesExistingPrivilegedAccountsToExpectedRoleAndPassword() throws Exception {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        DevUserBootstrap bootstrap = new DevUserBootstrap(
                userRepDAO,
                passwordEncoder,
                true,
                "admin@example.com",
                "admin12345",
                "Dev Admin",
                "manager@example.com",
                "manager12345",
                "Dev Manager"
        );
        User existingAdmin = new User("Wrong", "admin@example.com", "old", UserRole.CUSTOMER);
        existingAdmin.setFlagged(true);
        existingAdmin.setActive(false);
        User existingManager = new User("Wrong", "manager@example.com", "old", UserRole.CUSTOMER);
        existingManager.setFlagged(true);
        existingManager.setActive(false);

        when(userRepDAO.findByEmail("admin@example.com")).thenReturn(Optional.of(existingAdmin));
        when(userRepDAO.findByEmail("manager@example.com")).thenReturn(Optional.of(existingManager));
        when(passwordEncoder.encode("admin12345")).thenReturn("encoded-admin");
        when(passwordEncoder.encode("manager12345")).thenReturn("encoded-manager");
        doAnswer(invocation -> invocation.getArgument(0)).when(userRepDAO).save(any(User.class));

        bootstrap.run();

        assertEquals(UserRole.ADMIN, existingAdmin.getRole());
        assertEquals("encoded-admin", existingAdmin.getPassword());
        assertEquals("Dev Admin", existingAdmin.getName());
        assertFalse(existingAdmin.isFlagged());
        assertEquals(true, existingAdmin.isActive());

        assertEquals(UserRole.MANAGER, existingManager.getRole());
        assertEquals("encoded-manager", existingManager.getPassword());
        assertEquals("Dev Manager", existingManager.getName());
        assertFalse(existingManager.isFlagged());
        assertEquals(true, existingManager.isActive());
    }

    @Test
    void runDoesNothingWhenDisabled() throws Exception {
        UserRepDAO userRepDAO = mock(UserRepDAO.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        DevUserBootstrap bootstrap = new DevUserBootstrap(
                userRepDAO,
                passwordEncoder,
                false,
                "admin@example.com",
                "admin12345",
                "Dev Admin",
                "manager@example.com",
                "manager12345",
                "Dev Manager"
        );

        bootstrap.run();

        verify(userRepDAO, never()).findByEmail(any());
        verify(userRepDAO, never()).save(any());
    }
}
