package com.project.ecomapp.ecommerce_Project.Bean;

public class UserJWTResponse {

    private String token;

    public UserJWTResponse(){}

    public UserJWTResponse(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "UserJWTResponse{" +
                "token='" + token + '\'' +
                '}';
    }
}
