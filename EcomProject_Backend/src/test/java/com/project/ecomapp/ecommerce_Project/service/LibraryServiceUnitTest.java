package com.project.ecomapp.ecommerce_Project.service;

import com.project.ecomapp.ecommerce_Project.Bean.Book;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistory;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistoryResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessStatus;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessType;
import com.project.ecomapp.ecommerce_Project.Bean.BookCatalogResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservation;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservationResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservationStatus;
import com.project.ecomapp.ecommerce_Project.Bean.BookUpsertRequest;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import com.project.ecomapp.ecommerce_Project.Bean.UserRole;
import com.project.ecomapp.ecommerce_Project.library.repository.BookAccessHistoryRepDAO;
import com.project.ecomapp.ecommerce_Project.library.repository.BookFavoriteRepDAO;
import com.project.ecomapp.ecommerce_Project.library.repository.BookLikeRepDAO;
import com.project.ecomapp.ecommerce_Project.library.repository.BookRepDAO;
import com.project.ecomapp.ecommerce_Project.library.repository.BookReservationRepDAO;
import com.project.ecomapp.ecommerce_Project.library.service.LibraryService;
import com.project.ecomapp.ecommerce_Project.user.repository.UserRepDAO;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;

class LibraryServiceUnitTest {

    @Test
    void getBooksMapsCatalogResponseForCurrentUser() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Clean Architecture", 2);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER);

        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.bookRepDAO.findAllByArchivedFalseOrderByTitleAsc()).thenReturn(List.of(book));
        when(fixture.bookLikeRepDAO.countByBook(book)).thenReturn(1L);
        when(fixture.bookFavoriteRepDAO.countByBook(book)).thenReturn(2L);
        when(fixture.bookLikeRepDAO.existsByUserAndBook(user, book)).thenReturn(true);
        when(fixture.bookFavoriteRepDAO.existsByUserAndBook(user, book)).thenReturn(false);

        List<BookCatalogResponse> response = fixture.service.getBooks("reader@example.com");

        assertEquals(1, response.size());
        assertEquals("Clean Architecture", response.get(0).title());
        assertTrue(response.get(0).likedByCurrentUser());
    }

    @Test
    void createAndUpdateBookApplyRequestFields() {
        LibraryFixture fixture = new LibraryFixture();
        Book saved = new Book();
        doAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            if (book.getId() == null) {
                book.setId(11);
            }
            return book;
        }).when(fixture.bookRepDAO).save(any(Book.class));

        BookCatalogResponse created = fixture.service.createBook(bookRequest(" New Title ", 4));
        when(fixture.bookRepDAO.findByIdAndArchivedFalse(11)).thenReturn(Optional.of(savedBookFromResponse(created)));

        BookCatalogResponse updated = fixture.service.updateBook(11, bookRequest(" Updated Title ", 6));

        assertEquals("New Title", created.title());
        assertEquals("Updated Title", updated.title());
        assertEquals(6, updated.physicalStockQuantity());
    }

    @Test
    void likeUnlikeFavoriteAndUnfavoriteToggleResponses() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Clean Architecture", 2);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER);

        when(fixture.bookRepDAO.findByIdAndArchivedFalse(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.bookLikeRepDAO.existsByUserAndBook(user, book)).thenReturn(false, true, false);
        when(fixture.bookFavoriteRepDAO.existsByUserAndBook(user, book)).thenReturn(false, false, true, false);
        when(fixture.bookLikeRepDAO.countByBook(book)).thenReturn(1L);
        when(fixture.bookFavoriteRepDAO.countByBook(book)).thenReturn(1L);
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.bookLikeRepDAO).save(any());
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.bookFavoriteRepDAO).save(any());

        assertTrue(fixture.service.likeBook(7, "reader@example.com").likedByCurrentUser());
        assertTrue(fixture.service.favoriteBook(7, "reader@example.com").favoritedByCurrentUser());
        assertTrue(!fixture.service.unlikeBook(7, "reader@example.com").likedByCurrentUser());
        assertTrue(!fixture.service.unfavoriteBook(7, "reader@example.com").favoritedByCurrentUser());
    }

    @Test
    void accessModesCreateCompletedHistory() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Clean Architecture", 2);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER);

        when(fixture.bookRepDAO.findByIdAndArchivedFalse(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        doAnswer(invocation -> {
            BookAccessHistory history = invocation.getArgument(0);
            history.setId(22);
            return history;
        }).when(fixture.historyRepDAO).save(any(BookAccessHistory.class));

        BookAccessHistoryResponse ebook = fixture.service.accessEbook(7, "reader@example.com");
        BookAccessHistoryResponse inStore = fixture.service.accessInStoreRead(7, "reader@example.com");

        assertEquals(BookAccessStatus.COMPLETED, ebook.status());
        assertEquals(BookAccessType.EBOOK, ebook.accessType());
        assertEquals(BookAccessType.IN_STORE_READ, inStore.accessType());
    }

    @Test
    void getManagedHistoryAndReservationsRespectManagerScope() {
        LibraryFixture fixture = new LibraryFixture();
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED);
        Book book = createBook(7, "Clean Architecture", 2);
        BookAccessHistory history = createActiveCheckoutHistory(88, user, book, LocalDateTime.now().plusDays(25));
        BookReservation reservation = createReservation(12, user, book, true);

        when(fixture.userRepDAO.findById(3)).thenReturn(Optional.of(user));
        when(fixture.historyRepDAO.findAllByUserOrderByStartedAtDesc(user)).thenReturn(List.of(history));
        when(fixture.reservationRepDAO.findAllByUserOrderByReservedAtDesc(user)).thenReturn(List.of(reservation));

        assertEquals(1, fixture.service.getUserHistory(3, UserRole.MANAGER.asAuthorities()).size());
        assertEquals(1, fixture.service.getUserReservations(3, UserRole.MANAGER.asAuthorities()).size());
    }

    @Test
    void reservePhysicalBookCreatesReservationWhenCopyAvailable() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Clean Architecture", 2);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER);

        when(fixture.bookRepDAO.findByIdAndArchivedFalse(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.reservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(fixture.reservationRepDAO.countByBookAndStatus(book, BookReservationStatus.ACTIVE)).thenReturn(1L);
        doAnswer(invocation -> {
            BookReservation reservation = invocation.getArgument(0);
            reservation.setId(41);
            return reservation;
        }).when(fixture.reservationRepDAO).save(any(BookReservation.class));

        BookReservationResponse response = fixture.service.reservePhysicalBook(7, "reader@example.com");

        assertEquals(BookReservationStatus.ACTIVE, response.status());
        assertEquals(41, response.id());
        assertEquals("reader@example.com", response.userEmail());
        assertNotNull(response.reservedAt());
    }

    @Test
    void reservePhysicalBookRejectsDuplicateReservation() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Clean Architecture", 2);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER);
        BookReservation existingReservation = createReservation(50, user, book, false);

        when(fixture.bookRepDAO.findByIdAndArchivedFalse(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.reservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE))
                .thenReturn(Optional.of(existingReservation));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.service.reservePhysicalBook(7, "reader@example.com")
        );

        assertEquals(CONFLICT, exception.getStatus());
        verify(fixture.reservationRepDAO, never()).save(any(BookReservation.class));
    }

    @Test
    void checkoutPhysicalBookUsesExistingReservationAndCreatesLoan() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Domain-Driven Design", 1);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED);
        BookReservation reservation = createReservation(20, user, book, false);

        when(fixture.bookRepDAO.findByIdAndArchivedFalse(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.reservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));
        when(fixture.reservationRepDAO.countByUserAndStatusAndCheckedOutAtIsNotNull(user, BookReservationStatus.ACTIVE)).thenReturn(0L);
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.reservationRepDAO).save(any(BookReservation.class));
        doAnswer(invocation -> {
            BookAccessHistory history = invocation.getArgument(0);
            history.setId(99);
            return history;
        }).when(fixture.historyRepDAO).save(any(BookAccessHistory.class));

        BookAccessHistoryResponse response = fixture.service.checkoutPhysicalBook(7, "reader@example.com");

        assertEquals(BookAccessType.PHYSICAL_TAKE_HOME, response.accessType());
        assertEquals(BookAccessStatus.ACTIVE, response.status());
        assertNotNull(reservation.getCheckedOutAt());
        assertEquals(1, book.getPhysicalStockQuantity());
    }

    @Test
    void checkoutPhysicalBookRejectsWhenAllCopiesAllocatedToOthers() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Refactoring", 1);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED);

        when(fixture.bookRepDAO.findByIdAndArchivedFalse(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.reservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(fixture.reservationRepDAO.countByUserAndStatusAndCheckedOutAtIsNotNull(user, BookReservationStatus.ACTIVE)).thenReturn(0L);
        when(fixture.reservationRepDAO.countByBookAndStatus(book, BookReservationStatus.ACTIVE)).thenReturn(1L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.service.checkoutPhysicalBook(7, "reader@example.com")
        );

        assertEquals(CONFLICT, exception.getStatus());
    }

    @Test
    void checkoutPhysicalBookRejectsPlainCustomer() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Clean Code", 2);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER);

        when(fixture.bookRepDAO.findByIdAndArchivedFalse(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.service.checkoutPhysicalBook(7, "reader@example.com")
        );

        assertEquals(FORBIDDEN, exception.getStatus());
    }

    @Test
    void checkoutPhysicalBookRejectsWhenThreeDistinctBooksAlreadyCheckedOut() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Clean Code", 2);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED);

        when(fixture.bookRepDAO.findByIdAndArchivedFalse(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.reservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(fixture.reservationRepDAO.countByUserAndStatusAndCheckedOutAtIsNotNull(user, BookReservationStatus.ACTIVE)).thenReturn(3L);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.service.checkoutPhysicalBook(7, "reader@example.com")
        );

        assertEquals(BAD_REQUEST, exception.getStatus());
    }

    @Test
    void returnReservedBookCancelsReservation() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Pragmatic Programmer", 1);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER);
        BookReservation reservation = createReservation(12, user, book, false);

        when(fixture.bookRepDAO.findById(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.reservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.reservationRepDAO).save(any(BookReservation.class));

        BookReservationResponse response = fixture.service.returnReservedBook(7, "reader@example.com");

        assertEquals(BookReservationStatus.CANCELLED, response.status());
        assertNotNull(response.completedAt());
    }

    @Test
    void returnReservedBookRejectsCheckedOutReservation() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Pragmatic Programmer", 1);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER);
        BookReservation reservation = createReservation(12, user, book, true);

        when(fixture.bookRepDAO.findById(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.reservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.service.returnReservedBook(7, "reader@example.com")
        );

        assertEquals(BAD_REQUEST, exception.getStatus());
    }

    @Test
    void returnPhysicalBookClosesCheckoutAndReservation() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Pragmatic Programmer", 1);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED);
        BookReservation reservation = createReservation(12, user, book, true);
        BookAccessHistory history = createActiveCheckoutHistory(88, user, book, LocalDateTime.now().plusDays(25));

        when(fixture.bookRepDAO.findById(7)).thenReturn(Optional.of(book));
        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.reservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));
        when(fixture.historyRepDAO.findFirstByUserAndBookAndAccessTypeAndStatusOrderByStartedAtDesc(
                user, book, BookAccessType.PHYSICAL_TAKE_HOME, BookAccessStatus.ACTIVE
        )).thenReturn(Optional.of(history));
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.reservationRepDAO).save(any(BookReservation.class));
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.historyRepDAO).save(any(BookAccessHistory.class));

        BookAccessHistoryResponse response = fixture.service.returnPhysicalBook(7, "reader@example.com");

        assertEquals(BookAccessStatus.RETURNED, response.status());
        assertEquals(BookReservationStatus.FULFILLED, reservation.getStatus());
        assertNotNull(response.completedAt());
    }

    @Test
    void deleteBookRejectsActiveLoansUnlessForced() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Designing Data-Intensive Applications", 3);

        when(fixture.bookRepDAO.findByIdAndArchivedFalse(7)).thenReturn(Optional.of(book));
        when(fixture.reservationRepDAO.existsByBookAndStatus(book, BookReservationStatus.ACTIVE)).thenReturn(true);
        when(fixture.historyRepDAO.existsByBookAndAccessTypeAndStatus(book, BookAccessType.PHYSICAL_TAKE_HOME, BookAccessStatus.ACTIVE))
                .thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.service.deleteBook(7, false)
        );

        assertEquals(CONFLICT, exception.getStatus());

        fixture.service.deleteBook(7, true);
        assertTrue(book.isArchived());
    }

    @Test
    void scanAndFlagUsersWithOverduePhysicalLoansFlagsDistinctUsers() {
        LibraryFixture fixture = new LibraryFixture();
        User overdueUser = createUser(3, "reader@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED);
        overdueUser.setFlagged(false);
        Book book = createBook(7, "Refactoring", 2);
        BookAccessHistory firstOverdue = createActiveCheckoutHistory(1, overdueUser, book, LocalDateTime.now().minusDays(2));
        BookAccessHistory secondOverdue = createActiveCheckoutHistory(2, overdueUser, book, LocalDateTime.now().minusDays(1));

        when(fixture.historyRepDAO.findAllByAccessTypeAndStatusAndDueAtBeforeOrderByDueAtAsc(
                eq(BookAccessType.PHYSICAL_TAKE_HOME),
                eq(BookAccessStatus.ACTIVE),
                any(LocalDateTime.class)
        )).thenReturn(List.of(firstOverdue, secondOverdue));
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.userRepDAO).save(any(User.class));

        List<User> flaggedUsers = fixture.service.scanAndFlagUsersWithOverduePhysicalLoans();

        assertEquals(1, flaggedUsers.size());
        assertTrue(flaggedUsers.get(0).isFlagged());
        verify(fixture.userRepDAO).save(overdueUser);
    }

    @Test
    void forceReturnPhysicalBookClearsFlagWhenNoOverdueLoansRemain() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Refactoring", 2);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED);
        user.setFlagged(true);
        BookReservation reservation = createReservation(12, user, book, true);
        BookAccessHistory history = createActiveCheckoutHistory(88, user, book, LocalDateTime.now().minusDays(2));

        when(fixture.userRepDAO.findById(3)).thenReturn(Optional.of(user));
        when(fixture.bookRepDAO.findById(7)).thenReturn(Optional.of(book));
        when(fixture.historyRepDAO.findFirstByUserAndBookAndAccessTypeAndStatusOrderByStartedAtDesc(
                user, book, BookAccessType.PHYSICAL_TAKE_HOME, BookAccessStatus.ACTIVE
        )).thenReturn(Optional.of(history));
        when(fixture.reservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE))
                .thenReturn(Optional.of(reservation));
        when(fixture.historyRepDAO.existsByUserAndAccessTypeAndStatusAndDueAtBefore(
                eq(user), eq(BookAccessType.PHYSICAL_TAKE_HOME), eq(BookAccessStatus.ACTIVE), any(LocalDateTime.class)
        )).thenReturn(false);
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.reservationRepDAO).save(any(BookReservation.class));
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.historyRepDAO).save(any(BookAccessHistory.class));
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.userRepDAO).save(any(User.class));

        BookAccessHistoryResponse response = fixture.service.forceReturnPhysicalBook(3, 7, UserRole.MANAGER.asAuthorities());

        assertEquals(BookAccessStatus.RETURNED, response.status());
        assertEquals(BookReservationStatus.FULFILLED, reservation.getStatus());
        assertTrue(!user.isFlagged());
    }

    @Test
    void extendPhysicalCheckoutDurationRejectsMoreThanTwentyDays() {
        LibraryFixture fixture = new LibraryFixture();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> fixture.service.extendPhysicalCheckoutDuration(3, 7, 21, UserRole.ADMIN.asAuthorities())
        );

        assertEquals(BAD_REQUEST, exception.getStatus());
    }

    @Test
    void extendPhysicalCheckoutDurationUpdatesDueDateAndClearsFlag() {
        LibraryFixture fixture = new LibraryFixture();
        Book book = createBook(7, "Refactoring", 2);
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED);
        user.setFlagged(true);
        BookAccessHistory history = createActiveCheckoutHistory(88, user, book, LocalDateTime.now().minusDays(2));

        when(fixture.userRepDAO.findById(3)).thenReturn(Optional.of(user));
        when(fixture.bookRepDAO.findById(7)).thenReturn(Optional.of(book));
        when(fixture.historyRepDAO.findFirstByUserAndBookAndAccessTypeAndStatusOrderByStartedAtDesc(
                user, book, BookAccessType.PHYSICAL_TAKE_HOME, BookAccessStatus.ACTIVE
        )).thenReturn(Optional.of(history));
        when(fixture.historyRepDAO.existsByUserAndAccessTypeAndStatusAndDueAtBefore(
                eq(user), eq(BookAccessType.PHYSICAL_TAKE_HOME), eq(BookAccessStatus.ACTIVE), any(LocalDateTime.class)
        )).thenReturn(false);
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.historyRepDAO).save(any(BookAccessHistory.class));
        doAnswer(invocation -> invocation.getArgument(0)).when(fixture.userRepDAO).save(any(User.class));

        BookAccessHistoryResponse response = fixture.service.extendPhysicalCheckoutDuration(3, 7, 20, UserRole.ADMIN.asAuthorities());

        assertTrue(response.dueAt().isAfter(LocalDateTime.now().plusDays(15)));
        assertTrue(!user.isFlagged());
    }

    @Test
    void getCurrentUserViewsReturnOwnHistoryAndReservations() {
        LibraryFixture fixture = new LibraryFixture();
        User user = createUser(3, "reader@example.com", UserRole.CUSTOMER_WITH_READ_ALLOWED);
        Book book = createBook(7, "Clean Architecture", 2);
        BookAccessHistory history = createActiveCheckoutHistory(88, user, book, LocalDateTime.now().plusDays(25));
        BookReservation reservation = createReservation(12, user, book, true);

        when(fixture.userRepDAO.findByEmail("reader@example.com")).thenReturn(Optional.of(user));
        when(fixture.historyRepDAO.findAllByUserOrderByStartedAtDesc(user)).thenReturn(List.of(history));
        when(fixture.reservationRepDAO.findAllByUserOrderByReservedAtDesc(user)).thenReturn(List.of(reservation));

        assertEquals(1, fixture.service.getCurrentUserHistory("reader@example.com").size());
        assertEquals(1, fixture.service.getCurrentUserReservations("reader@example.com").size());
    }

    private static Book createBook(int id, String title, int stock) {
        Book book = new Book();
        book.setId(id);
        book.setTitle(title);
        book.setAuthor("Author");
        book.setGenre("Tech");
        book.setPublisher("Publisher");
        book.setPhysicalStockQuantity(stock);
        book.setEbookAvailable(true);
        book.setInStoreReadAvailable(true);
        return book;
    }

    private static BookUpsertRequest bookRequest(String title, int stock) {
        return new BookUpsertRequest(
                title,
                "Author",
                "Tech",
                "Publisher",
                LocalDate.of(2024, 1, 1),
                "isbn-" + stock,
                "description",
                stock,
                true,
                true,
                "English",
                250
        );
    }

    private static Book savedBookFromResponse(BookCatalogResponse response) {
        Book book = new Book();
        book.setId(response.id());
        book.setTitle(response.title());
        book.setAuthor(response.author());
        book.setGenre(response.genre());
        book.setPublisher(response.publisher());
        book.setPublishedDate(response.publishedDate());
        book.setIsbn(response.isbn());
        book.setDescription(response.description());
        book.setPhysicalStockQuantity(response.physicalStockQuantity());
        book.setEbookAvailable(response.ebookAvailable());
        book.setInStoreReadAvailable(response.inStoreReadAvailable());
        book.setLanguage(response.language());
        book.setPageCount(response.pageCount());
        return book;
    }

    private static User createUser(int id, String email, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setName(email);
        user.setEmail(email);
        user.setPassword("encoded");
        user.setRole(role);
        user.setActive(true);
        return user;
    }

    private static BookReservation createReservation(int id, User user, Book book, boolean checkedOut) {
        BookReservation reservation = new BookReservation();
        reservation.setId(id);
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setStatus(BookReservationStatus.ACTIVE);
        reservation.setReservedAt(LocalDateTime.now().minusDays(1));
        if (checkedOut) {
            reservation.setCheckedOutAt(LocalDateTime.now().minusHours(1));
        }
        return reservation;
    }

    private static BookAccessHistory createActiveCheckoutHistory(int id, User user, Book book, LocalDateTime dueAt) {
        BookAccessHistory history = new BookAccessHistory();
        history.setId(id);
        history.setUser(user);
        history.setBook(book);
        history.setAccessType(BookAccessType.PHYSICAL_TAKE_HOME);
        history.setStatus(BookAccessStatus.ACTIVE);
        history.setStartedAt(LocalDateTime.now().minusDays(5));
        history.setDueAt(dueAt);
        return history;
    }

    private static final class LibraryFixture {
        private final BookRepDAO bookRepDAO = mock(BookRepDAO.class);
        private final BookLikeRepDAO bookLikeRepDAO = mock(BookLikeRepDAO.class);
        private final BookFavoriteRepDAO bookFavoriteRepDAO = mock(BookFavoriteRepDAO.class);
        private final BookAccessHistoryRepDAO historyRepDAO = mock(BookAccessHistoryRepDAO.class);
        private final BookReservationRepDAO reservationRepDAO = mock(BookReservationRepDAO.class);
        private final UserRepDAO userRepDAO = mock(UserRepDAO.class);
        private final LibraryService service = new LibraryService(
                bookRepDAO,
                bookLikeRepDAO,
                bookFavoriteRepDAO,
                historyRepDAO,
                reservationRepDAO,
                userRepDAO
        );
    }
}
