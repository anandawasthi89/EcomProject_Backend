package com.project.ecomapp.ecommerce_Project.user.repository;

import com.project.ecomapp.ecommerce_Project.Bean.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepDAO extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    java.util.List<User> findAllByFlaggedTrueOrderByIdAsc();
}
