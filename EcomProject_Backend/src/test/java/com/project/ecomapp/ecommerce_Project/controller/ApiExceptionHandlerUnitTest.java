package com.project.ecomapp.ecommerce_Project.controller;

import com.project.ecomapp.ecommerce_Project.Controller.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

class ApiExceptionHandlerUnitTest {

    @Test
    void handleValidationReturnsFieldErrors() throws Exception {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "email", "Email must be valid"));
        bindingResult.addError(new FieldError("request", "password", "Password too short"));

        Method method = SampleController.class.getDeclaredMethod("handle", String.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidation(exception);

        assertEquals(BAD_REQUEST, response.getStatusCode());
        assertEquals("Validation failed", response.getBody().get("message"));
        assertTrue(response.getBody().containsKey("errors"));
    }

    @Test
    void handleResponseStatusReturnsReason() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatus(
                new ResponseStatusException(CONFLICT, "Email already registered")
        );

        assertEquals(CONFLICT, response.getStatusCode());
        assertEquals("Email already registered", response.getBody().get("message"));
    }

    @Test
    void handleResponseStatusFallsBackWhenReasonMissing() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleResponseStatus(
                new ResponseStatusException(CONFLICT)
        );

        assertEquals(CONFLICT, response.getStatusCode());
        assertEquals("Request failed", response.getBody().get("message"));
    }

    @Test
    void handleGenericReturnsInternalServerError() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleGeneric(new RuntimeException("boom"));

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Internal server error", response.getBody().get("message"));
    }

    static class SampleController {
        void handle(String value) {
        }
    }
}
