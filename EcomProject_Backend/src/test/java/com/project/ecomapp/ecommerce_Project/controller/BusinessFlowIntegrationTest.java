package com.project.ecomapp.ecommerce_Project.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistory;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservation;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Bean.UserRole;
import com.project.ecomapp.ecommerce_Project.library.repository.BookAccessHistoryRepDAO;
import com.project.ecomapp.ecommerce_Project.library.repository.BookFavoriteRepDAO;
import com.project.ecomapp.ecommerce_Project.library.repository.BookLikeRepDAO;
import com.project.ecomapp.ecommerce_Project.library.repository.BookRepDAO;
import com.project.ecomapp.ecommerce_Project.library.repository.BookReservationRepDAO;
import com.project.ecomapp.ecommerce_Project.user.repository.UserRepDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BusinessFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepDAO userRepDAO;

    @Autowired
    private BookRepDAO bookRepDAO;

    @Autowired
    private BookReservationRepDAO bookReservationRepDAO;

    @Autowired
    private BookAccessHistoryRepDAO bookAccessHistoryRepDAO;

    @Autowired
    private BookLikeRepDAO bookLikeRepDAO;

    @Autowired
    private BookFavoriteRepDAO bookFavoriteRepDAO;

    @BeforeEach
    void setUp() {
        bookFavoriteRepDAO.deleteAll();
        bookLikeRepDAO.deleteAll();
        bookAccessHistoryRepDAO.deleteAll();
        bookReservationRepDAO.deleteAll();
        bookRepDAO.deleteAll();
        userRepDAO.deleteAll();

        createPrivilegedUser("admin@example.com", "admin12345", "Admin User", UserRole.ADMIN);
        createPrivilegedUser("manager@example.com", "manager12345", "Manager User", UserRole.MANAGER);
    }

    @Test
    void reservationCheckoutAndReturnFlowWorksThroughEndpoints() throws Exception {
        String managerToken = login("manager@example.com", "manager12345");

        int customerId = registerPublicCustomer("customer@example.com", "password123", "Primary Customer");
        String customerToken = login("customer@example.com", "password123");

        int managedCustomerId = createManagedCustomer(managerToken, "reader@example.com", "password123", "Reader Customer");
        grantReadAccess(managerToken, managedCustomerId);
        String readAllowedToken = login("reader@example.com", "password123");

        int reservableBookId = createBook(managerToken, "Reservation Flow Book", "isbn-res-1", 2);

        mockMvc.perform(post("/api/books/{id}/reserve", reservableBookId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.checkedOutAt").doesNotExist());

        mockMvc.perform(get("/api/books/reservations/me")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(customerId))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        mockMvc.perform(post("/api/books/{id}/reservation/return", reservableBookId)
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(get("/api/books/reservations/me")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CANCELLED"));

        int checkoutBookId = createBook(managerToken, "Checkout Flow Book", "isbn-checkout-1", 1);

        mockMvc.perform(post("/api/books/{id}/checkout", checkoutBookId)
                        .header("Authorization", bearer(readAllowedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessType").value("PHYSICAL_TAKE_HOME"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/books/reservations/me")
                        .header("Authorization", bearer(readAllowedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(checkoutBookId))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].checkedOutAt").isNotEmpty());

        mockMvc.perform(post("/api/books/{id}/return", checkoutBookId)
                        .header("Authorization", bearer(readAllowedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        mockMvc.perform(get("/api/books/history/me")
                        .header("Authorization", bearer(readAllowedToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(checkoutBookId))
                .andExpect(jsonPath("$[0].status").value("RETURNED"));

        mockMvc.perform(delete("/api/books/{id}", checkoutBookId)
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void overdueScanAndResolutionFlowWorksThroughEndpoints() throws Exception {
        String managerToken = login("manager@example.com", "manager12345");
        int managedCustomerId = createManagedCustomer(managerToken, "overdue.reader@example.com", "password123", "Overdue Reader");
        grantReadAccess(managerToken, managedCustomerId);
        String readAllowedToken = login("overdue.reader@example.com", "password123");
        int bookId = createBook(managerToken, "Overdue Flow Book", "isbn-overdue-1", 1);

        mockMvc.perform(post("/api/books/{id}/checkout", bookId)
                        .header("Authorization", bearer(readAllowedToken)))
                .andExpect(status().isOk());

        User managedUser = userRepDAO.findById(managedCustomerId).orElseThrow();
        BookAccessHistory activeHistory = bookAccessHistoryRepDAO.findAllByUserOrderByStartedAtDesc(managedUser).get(0);
        activeHistory.setDueAt(LocalDateTime.now().minusDays(2));
        bookAccessHistoryRepDAO.save(activeHistory);

        mockMvc.perform(post("/api/iam/users/flags/scan-overdue")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(managedCustomerId))
                .andExpect(jsonPath("$[0].flagged").value(true));

        mockMvc.perform(get("/api/iam/users/flagged")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(managedCustomerId));

        mockMvc.perform(get("/api/iam/users/{id}/flag-status", managedCustomerId)
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(true));

        mockMvc.perform(post("/api/iam/users/{id}/flags/resolve/extend-duration", managedCustomerId)
                        .param("bookId", String.valueOf(bookId))
                        .param("extraDays", "20")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/iam/users/{id}/flag-status", managedCustomerId)
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(false));

        activeHistory = bookAccessHistoryRepDAO.findAllByUserOrderByStartedAtDesc(managedUser).get(0);
        activeHistory.setDueAt(LocalDateTime.now().minusDays(1));
        bookAccessHistoryRepDAO.save(activeHistory);

        mockMvc.perform(post("/api/iam/users/flags/scan-overdue")
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flagged").value(true));

        mockMvc.perform(post("/api/iam/users/{id}/flags/resolve/force-return", managedCustomerId)
                        .param("bookId", String.valueOf(bookId))
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));

        mockMvc.perform(get("/api/iam/users/{id}/flag-status", managedCustomerId)
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flagged").value(false));

        BookReservation reservation = bookReservationRepDAO.findAllByUserOrderByReservedAtDesc(managedUser).get(0);
        assertEquals("FULFILLED", reservation.getStatus().name());
    }

    @Test
    void deactivatedUserCannotLogInOrUseExistingToken() throws Exception {
        String managerToken = login("manager@example.com", "manager12345");
        int managedCustomerId = createManagedCustomer(managerToken, "inactive.reader@example.com", "password123", "Inactive Reader");
        grantReadAccess(managerToken, managedCustomerId);
        String readAllowedToken = login("inactive.reader@example.com", "password123");

        mockMvc.perform(post("/api/iam/users/{id}/flags/resolve/deactivate", managedCustomerId)
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.flagged").value(false));

        mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "inactive.reader@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", bearer(readAllowedToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized"));

        User user = userRepDAO.findById(managedCustomerId).orElseThrow();
        assertFalse(user.isActive());
    }

    private void createPrivilegedUser(String email, String password, String name, UserRole role) {
        User user = new User(name, email, passwordEncoder.encode(password), role);
        user.setFlagged(false);
        user.setActive(true);
        userRepDAO.save(user);
    }

    private int registerPublicCustomer(String email, String password, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(name, email, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asInt();
    }

    private int createManagedCustomer(String managerToken, String email, String password, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/iam/users")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(name, email, password)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asInt();
    }

    private void grantReadAccess(String managerToken, int managedCustomerId) throws Exception {
        mockMvc.perform(patch("/api/iam/users/{id}/grant-read-access", managedCustomerId)
                        .header("Authorization", bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER_WITH_READ_ALLOWED"));
    }

    private int createBook(String managerToken, String title, String isbn, int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/books")
                        .header("Authorization", bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "author": "Author",
                                  "genre": "Technology",
                                  "publisher": "Publisher",
                                  "publishedDate": "2024-01-01",
                                  "isbn": "%s",
                                  "description": "Business flow book",
                                  "physicalStockQuantity": %d,
                                  "ebookAvailable": true,
                                  "inStoreReadAvailable": true,
                                  "language": "English",
                                  "pageCount": 200
                                }
                                """.formatted(title, isbn, stock)))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asInt();
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = readJson(result);
        assertNotNull(json.get("token"));
        return json.get("token").asText();
    }

    private JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
