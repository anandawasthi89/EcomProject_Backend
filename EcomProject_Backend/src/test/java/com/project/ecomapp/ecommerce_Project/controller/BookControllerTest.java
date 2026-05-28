package com.project.ecomapp.ecommerce_Project.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistoryResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessStatus;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessType;
import com.project.ecomapp.ecommerce_Project.Bean.BookCatalogResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservationResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservationStatus;
import com.project.ecomapp.ecommerce_Project.Config.JWTAuthenticationEntryPoint;
import com.project.ecomapp.ecommerce_Project.Config.JWTAuthenticationFilter;
import com.project.ecomapp.ecommerce_Project.Config.JWTUtils;
import com.project.ecomapp.ecommerce_Project.Config.PasswordConfiguration;
import com.project.ecomapp.ecommerce_Project.Config.SecurityConfiguration;
import com.project.ecomapp.ecommerce_Project.Controller.ApiExceptionHandler;
import com.project.ecomapp.ecommerce_Project.library.controller.BookController;
import com.project.ecomapp.ecommerce_Project.library.service.LibraryService;
import com.project.ecomapp.ecommerce_Project.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookController.class)
@Import({
        ApiExceptionHandler.class,
        SecurityConfiguration.class,
        PasswordConfiguration.class,
        JWTAuthenticationFilter.class,
        JWTAuthenticationEntryPoint.class
})
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LibraryService libraryService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private JWTUtils jwtUtils;

    @Test
    void getBooksReturnsCatalogForAuthenticatedUser() throws Exception {
        when(libraryService.getBooks("customer@example.com")).thenReturn(List.of(
                createBookResponse(1, "Clean Architecture", false, true)
        ));

        mockMvc.perform(get("/api/books").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Clean Architecture"));
    }

    @Test
    void getBookReturnsCatalogItemForAuthenticatedUser() throws Exception {
        when(libraryService.getBook(7, "customer@example.com")).thenReturn(createBookResponse(7, "Clean Architecture", false, true));

        mockMvc.perform(get("/api/books/7").with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.title").value("Clean Architecture"));
    }

    @Test
    void createBookAllowsManager() throws Exception {
        when(libraryService.createBook(any())).thenReturn(createBookResponse(2, "DDD", false, false));

        mockMvc.perform(post("/api/books")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookPayload(
                                "DDD", "Evans", "Tech", "Addison-Wesley", LocalDate.of(2003, 8, 30),
                                "isbn-1", "description", 4, true, true, "English", 560
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("DDD"));
    }

    @Test
    void updateBookAllowsManager() throws Exception {
        when(libraryService.updateBook(eq(7), any())).thenReturn(createBookResponse(7, "Updated DDD", false, false));

        mockMvc.perform(put("/api/books/7")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookPayload(
                                "Updated DDD", "Evans", "Tech", "Addison-Wesley", LocalDate.of(2003, 8, 30),
                                "isbn-1", "description", 4, true, true, "English", 560
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated DDD"));
    }

    @Test
    void deleteBookPassesForceArchiveFlag() throws Exception {
        mockMvc.perform(delete("/api/books/7")
                        .param("forceArchiveWithActiveLoans", "true")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isNoContent());

        verify(libraryService).deleteBook(7, true);
    }

    @Test
    void reservePhysicalBookAllowsCustomer() throws Exception {
        when(libraryService.reservePhysicalBook(7, "customer@example.com"))
                .thenReturn(createReservationResponse(3, 7, "customer@example.com", BookReservationStatus.ACTIVE));

        mockMvc.perform(post("/api/books/7/reserve")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.bookId").value(7));
    }

    @Test
    void likeAndUnlikeBookAllowCustomer() throws Exception {
        when(libraryService.likeBook(7, "customer@example.com"))
                .thenReturn(createBookResponse(7, "Clean Architecture", true, false));
        when(libraryService.unlikeBook(7, "customer@example.com"))
                .thenReturn(createBookResponse(7, "Clean Architecture", false, false));

        mockMvc.perform(post("/api/books/7/like")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByCurrentUser").value(true));

        mockMvc.perform(delete("/api/books/7/like")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByCurrentUser").value(false));
    }

    @Test
    void favoriteAndUnfavoriteBookAllowCustomer() throws Exception {
        when(libraryService.favoriteBook(7, "customer@example.com"))
                .thenReturn(createBookResponse(7, "Clean Architecture", false, true));
        when(libraryService.unfavoriteBook(7, "customer@example.com"))
                .thenReturn(createBookResponse(7, "Clean Architecture", false, false));

        mockMvc.perform(post("/api/books/7/favorite")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritedByCurrentUser").value(true));

        mockMvc.perform(delete("/api/books/7/favorite")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoritedByCurrentUser").value(false));
    }

    @Test
    void returnReservedBookAllowsCustomer() throws Exception {
        when(libraryService.returnReservedBook(7, "customer@example.com"))
                .thenReturn(createReservationResponse(3, 7, "customer@example.com", BookReservationStatus.CANCELLED));

        mockMvc.perform(post("/api/books/7/reservation/return")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void checkoutAllowsReadAllowedCustomer() throws Exception {
        when(libraryService.checkoutPhysicalBook(7, "reader@example.com"))
                .thenReturn(createHistoryResponse(7, "reader@example.com", BookAccessType.PHYSICAL_TAKE_HOME, BookAccessStatus.ACTIVE));

        mockMvc.perform(post("/api/books/7/checkout")
                        .with(user("reader@example.com").roles("CUSTOMER_WITH_READ_ALLOWED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessType").value("PHYSICAL_TAKE_HOME"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void checkoutRejectsPlainCustomer() throws Exception {
        mockMvc.perform(post("/api/books/7/checkout")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void ebookAndInStoreAccessAllowCustomer() throws Exception {
        when(libraryService.accessEbook(7, "customer@example.com"))
                .thenReturn(createHistoryResponse(7, "customer@example.com", BookAccessType.EBOOK, BookAccessStatus.COMPLETED));
        when(libraryService.accessInStoreRead(7, "customer@example.com"))
                .thenReturn(createHistoryResponse(7, "customer@example.com", BookAccessType.IN_STORE_READ, BookAccessStatus.COMPLETED));

        mockMvc.perform(post("/api/books/7/access/ebook")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessType").value("EBOOK"));

        mockMvc.perform(post("/api/books/7/access/in-store")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessType").value("IN_STORE_READ"));
    }

    @Test
    void returnAllowsReadAllowedCustomer() throws Exception {
        when(libraryService.returnPhysicalBook(7, "reader@example.com"))
                .thenReturn(createHistoryResponse(7, "reader@example.com", BookAccessType.PHYSICAL_TAKE_HOME, BookAccessStatus.RETURNED));

        mockMvc.perform(post("/api/books/7/return")
                        .with(user("reader@example.com").roles("CUSTOMER_WITH_READ_ALLOWED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    void getCurrentUserReservationsReturnsReservationList() throws Exception {
        when(libraryService.getCurrentUserReservations("customer@example.com")).thenReturn(List.of(
                createReservationResponse(3, 7, "customer@example.com", BookReservationStatus.ACTIVE)
        ));

        mockMvc.perform(get("/api/books/reservations/me")
                        .with(user("customer@example.com").roles("CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value("customer@example.com"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getCurrentUserHistoryReturnsHistoryList() throws Exception {
        when(libraryService.getCurrentUserHistory("reader@example.com")).thenReturn(List.of(
                createHistoryResponse(7, "reader@example.com", BookAccessType.PHYSICAL_TAKE_HOME, BookAccessStatus.RETURNED)
        ));

        mockMvc.perform(get("/api/books/history/me")
                        .with(user("reader@example.com").roles("CUSTOMER_WITH_READ_ALLOWED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("RETURNED"));
    }

    @Test
    void getUserHistoryAllowsManager() throws Exception {
        when(libraryService.getUserHistory(eq(9), any())).thenReturn(List.of(
                createHistoryResponse(4, "customer@example.com", BookAccessType.IN_STORE_READ, BookAccessStatus.COMPLETED)
        ));

        mockMvc.perform(get("/api/books/history/users/9")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value("customer@example.com"));

        verify(libraryService).getUserHistory(eq(9), any());
    }

    @Test
    void getUserReservationsAllowsManager() throws Exception {
        when(libraryService.getUserReservations(eq(9), any())).thenReturn(List.of(
                createReservationResponse(3, 7, "customer@example.com", BookReservationStatus.ACTIVE)
        ));

        mockMvc.perform(get("/api/books/reservations/users/9")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookId").value(7))
                .andExpect(jsonPath("$[0].userEmail").value("customer@example.com"));
    }

    private static BookCatalogResponse createBookResponse(int id, String title, boolean liked, boolean favorited) {
        return new BookCatalogResponse(
                id,
                title,
                "Author",
                "Tech",
                "Publisher",
                LocalDate.of(2020, 1, 1),
                "isbn",
                "description",
                5,
                true,
                true,
                "English",
                400,
                3,
                2,
                liked,
                favorited
        );
    }

    private static BookAccessHistoryResponse createHistoryResponse(int bookId, String userEmail, BookAccessType accessType, BookAccessStatus status) {
        return new BookAccessHistoryResponse(
                15,
                bookId,
                "Clean Architecture",
                9,
                userEmail,
                accessType,
                status,
                LocalDateTime.of(2026, 4, 19, 12, 0),
                LocalDateTime.of(2026, 5, 19, 12, 0),
                status == BookAccessStatus.ACTIVE ? null : LocalDateTime.of(2026, 4, 20, 12, 0)
        );
    }

    private static BookReservationResponse createReservationResponse(
            int reservationId,
            int bookId,
            String userEmail,
            BookReservationStatus status
    ) {
        return new BookReservationResponse(
                reservationId,
                bookId,
                "Clean Architecture",
                9,
                userEmail,
                status,
                LocalDateTime.of(2026, 4, 19, 12, 0),
                status == BookReservationStatus.ACTIVE ? null : LocalDateTime.of(2026, 4, 20, 12, 0),
                status == BookReservationStatus.ACTIVE ? null : LocalDateTime.of(2026, 4, 21, 12, 0)
        );
    }

    private record BookPayload(
            String title,
            String author,
            String genre,
            String publisher,
            LocalDate publishedDate,
            String isbn,
            String description,
            int physicalStockQuantity,
            boolean ebookAvailable,
            boolean inStoreReadAvailable,
            String language,
            Integer pageCount
    ) {
    }
}
