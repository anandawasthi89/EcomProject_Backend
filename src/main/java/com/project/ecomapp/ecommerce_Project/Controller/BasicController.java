package com.project.ecomapp.ecommerce_Project.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BasicController {

    @GetMapping(path = "/")
    public Map<String, String> getHome() {
        return Map.of("service", "ecommerce-backend", "status", "ok");
    }

}
