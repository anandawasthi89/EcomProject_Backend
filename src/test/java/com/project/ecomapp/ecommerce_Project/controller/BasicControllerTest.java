package com.project.ecomapp.ecommerce_Project.controller;

import com.project.ecomapp.ecommerce_Project.Config.JWTUtils;
import com.project.ecomapp.ecommerce_Project.Controller.BasicController;
import com.project.ecomapp.ecommerce_Project.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BasicController.class)
@AutoConfigureMockMvc(addFilters = false)
class BasicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JWTUtils jwtUtils;

    @Test
    void getHomeReturnsServiceStatus() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ecommerce-backend"))
                .andExpect(jsonPath("$.status").value("ok"));
    }
}
