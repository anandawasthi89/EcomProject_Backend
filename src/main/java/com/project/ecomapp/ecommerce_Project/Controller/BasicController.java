package com.project.ecomapp.ecommerce_Project.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BasicController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BasicController.class);

    @GetMapping(path = "/")
    public Map<String, String> getHome() {
        LOGGER.debug("Health endpoint called");
        return Map.of("service", "ecommerce-backend", "status", "ok");
    }

}
