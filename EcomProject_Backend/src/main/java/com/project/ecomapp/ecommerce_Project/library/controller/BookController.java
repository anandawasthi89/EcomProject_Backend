package com.project.ecomapp.ecommerce_Project.library.controller;

import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistoryResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookCatalogResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservationResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookUpsertRequest;
import com.project.ecomapp.ecommerce_Project.library.service.LibraryService;
import com.project.ecomapp.ecommerce_Project.publisher.BookUploadPayload;
import com.project.ecomapp.ecommerce_Project.publisher.BookUploadPublisherHelper;
import com.project.ecomapp.ecommerce_Project.publisher.RedisListPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/books")
@Validated
public class BookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookController.class);

    private final LibraryService libraryService;
    private final RedisListPublisher redisListPublisher;

    public BookController(LibraryService libraryService, RedisListPublisher redisListPublisher) {
        this.libraryService = libraryService;
        this.redisListPublisher = redisListPublisher;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<BookCatalogResponse> getBooks(Authentication authentication) {
        LOGGER.debug("Catalog list requested by {}", authentication.getName());
        return libraryService.getBooks(authentication.getName());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public BookCatalogResponse getBook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.debug("Catalog item requested id={} actor={}", id, authentication.getName());
        return libraryService.getBook(id, authentication.getName());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<BookCatalogResponse> createBook(@Valid @RequestBody BookUpsertRequest request, Authentication authentication) {
        LOGGER.info("Catalog create requested title={} actor={}", request.title(), authentication.getName());
        BookCatalogResponse bookCatalogResponse = libraryService.createBook(request);
        if (bookCatalogResponse!=null) redisListPublisher.publish(BookUploadPublisherHelper.getUploadPayload(bookCatalogResponse));
        return ResponseEntity.status(HttpStatus.CREATED).body(bookCatalogResponse);
    }

    @GetMapping("/dummy")
    @PreAuthorize("isAuthenticated()")
    public BookUploadPayload dummyBookUploadPayloadConsumer(Authentication authentication) {
        BookUploadPayload bookUploadPayload = redisListPublisher.dummyConsume();
        LOGGER.debug("Dummy consumer {}", authentication.getName());
        return bookUploadPayload;
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public BookCatalogResponse updateBook(@PathVariable Integer id, @Valid @RequestBody BookUpsertRequest request, Authentication authentication) {
        LOGGER.info("Catalog update requested id={} title={} actor={}", id, request.title(), authentication.getName());
        return libraryService.updateBook(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void deleteBook(
            @PathVariable Integer id,
            @RequestParam(name = "forceArchiveWithActiveLoans", defaultValue = "false") boolean forceArchiveWithActiveLoans,
            Authentication authentication
    ) {
        LOGGER.warn(
                "Catalog archive requested id={} actor={} forceArchiveWithActiveLoans={}",
                id,
                authentication.getName(),
                forceArchiveWithActiveLoans
        );
        libraryService.deleteBook(id, forceArchiveWithActiveLoans);
    }

    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public BookCatalogResponse likeBook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Book like requested id={} actor={}", id, authentication.getName());
        return libraryService.likeBook(id, authentication.getName());
    }

    @DeleteMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public BookCatalogResponse unlikeBook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Book unlike requested id={} actor={}", id, authentication.getName());
        return libraryService.unlikeBook(id, authentication.getName());
    }

    @PostMapping("/{id}/favorite")
    @PreAuthorize("isAuthenticated()")
    public BookCatalogResponse favoriteBook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Book favorite requested id={} actor={}", id, authentication.getName());
        return libraryService.favoriteBook(id, authentication.getName());
    }

    @DeleteMapping("/{id}/favorite")
    @PreAuthorize("isAuthenticated()")
    public BookCatalogResponse unfavoriteBook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Book unfavorite requested id={} actor={}", id, authentication.getName());
        return libraryService.unfavoriteBook(id, authentication.getName());
    }

    @PostMapping("/{id}/access/ebook")
    @PreAuthorize("hasAnyRole('CUSTOMER','CUSTOMER_WITH_READ_ALLOWED')")
    public BookAccessHistoryResponse accessEbook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Ebook access requested bookId={} actor={}", id, authentication.getName());
        return libraryService.accessEbook(id, authentication.getName());
    }

    @PostMapping("/{id}/access/in-store")
    @PreAuthorize("hasAnyRole('CUSTOMER','CUSTOMER_WITH_READ_ALLOWED')")
    public BookAccessHistoryResponse accessInStoreRead(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("In-store read requested bookId={} actor={}", id, authentication.getName());
        return libraryService.accessInStoreRead(id, authentication.getName());
    }

    @PostMapping("/{id}/reserve")
    @PreAuthorize("hasAnyRole('CUSTOMER','CUSTOMER_WITH_READ_ALLOWED')")
    public ResponseEntity<BookReservationResponse> reservePhysicalBook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Book reservation requested bookId={} actor={}", id, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryService.reservePhysicalBook(id, authentication.getName()));
    }

    @PostMapping("/{id}/reservation/return")
    @PreAuthorize("hasAnyRole('CUSTOMER','CUSTOMER_WITH_READ_ALLOWED')")
    public BookReservationResponse returnReservedBook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Reservation return requested bookId={} actor={}", id, authentication.getName());
        return libraryService.returnReservedBook(id, authentication.getName());
    }

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasRole('CUSTOMER_WITH_READ_ALLOWED')")
    public BookAccessHistoryResponse checkoutPhysicalBook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Physical checkout requested bookId={} actor={}", id, authentication.getName());
        return libraryService.checkoutPhysicalBook(id, authentication.getName());
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('CUSTOMER_WITH_READ_ALLOWED')")
    public BookAccessHistoryResponse returnPhysicalBook(@PathVariable Integer id, Authentication authentication) {
        LOGGER.info("Physical return requested bookId={} actor={}", id, authentication.getName());
        return libraryService.returnPhysicalBook(id, authentication.getName());
    }

    @GetMapping("/history/me")
    @PreAuthorize("isAuthenticated()")
    public List<BookAccessHistoryResponse> getCurrentUserHistory(Authentication authentication) {
        LOGGER.debug("Current user history requested actor={}", authentication.getName());
        return libraryService.getCurrentUserHistory(authentication.getName());
    }

    @GetMapping("/reservations/me")
    @PreAuthorize("isAuthenticated()")
    public List<BookReservationResponse> getCurrentUserReservations(Authentication authentication) {
        LOGGER.debug("Current user reservations requested actor={}", authentication.getName());
        return libraryService.getCurrentUserReservations(authentication.getName());
    }

    @GetMapping("/history/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<BookAccessHistoryResponse> getUserHistory(@PathVariable Integer userId, Authentication authentication) {
        LOGGER.debug("Managed user history requested targetId={} actor={}", userId, authentication.getName());
        return libraryService.getUserHistory(userId, authentication.getAuthorities());
    }

    @GetMapping("/reservations/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<BookReservationResponse> getUserReservations(@PathVariable Integer userId, Authentication authentication) {
        LOGGER.debug("Managed user reservations requested targetId={} actor={}", userId, authentication.getName());
        return libraryService.getUserReservations(userId, authentication.getAuthorities());
    }
}
