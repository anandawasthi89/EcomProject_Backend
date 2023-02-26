package com.project.ecomapp.ecommerce_Project.Repository;

import com.project.ecomapp.ecommerce_Project.Bean.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepDAO extends JpaRepository<User,Integer> {

    public User findByEmail(String email);

}
