package com.project.ecomapp.ecommerce_Project.Bean;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class UserJWTRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    public UserJWTRequest() {}

    public UserJWTRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "UserJWTRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}
