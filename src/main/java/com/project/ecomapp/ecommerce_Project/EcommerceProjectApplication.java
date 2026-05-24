package com.project.ecomapp.ecommerce_Project;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class EcommerceProjectApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(EcommerceProjectApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(EcommerceProjectApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {
		LOGGER.info("Application startup completed");
	}

	@Bean
	public WebMvcConfigurer corsConfigurer(@Value("${app.cors.allowed-origins:http://localhost:4200}") String allowedOrigins) {
		LOGGER.info("Configuring CORS allowed origins={}", allowedOrigins);
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/**")
						.allowedOrigins(allowedOrigins.split(","))
						.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
			}
		};
	}
}
