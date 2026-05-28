package com.project.ecomapp.ecommerce_Project.library.service;

import com.project.ecomapp.ecommerce_Project.Bean.Book;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistory;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistoryResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessStatus;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessType;
import com.project.ecomapp.ecommerce_Project.Bean.BookCatalogResponse;
import com.project.ecomapp.ecommerce_Project.Bean.BookFavorite;
import com.project.ecomapp.ecommerce_Project.Bean.BookLike;
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
import com.project.ecomapp.ecommerce_Project.user.repository.UserRepDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class LibraryService {

    private static final int MAX_PHYSICAL_BOOKS_PER_USER = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(LibraryService.class);

    private final BookRepDAO bookRepDAO;
    private final BookLikeRepDAO bookLikeRepDAO;
    private final BookFavoriteRepDAO bookFavoriteRepDAO;
    private final BookAccessHistoryRepDAO bookAccessHistoryRepDAO;
    private final BookReservationRepDAO bookReservationRepDAO;
    private final UserRepDAO userRepDAO;

    public LibraryService(
            BookRepDAO bookRepDAO,
            BookLikeRepDAO bookLikeRepDAO,
            BookFavoriteRepDAO bookFavoriteRepDAO,
            BookAccessHistoryRepDAO bookAccessHistoryRepDAO,
            BookReservationRepDAO bookReservationRepDAO,
            UserRepDAO userRepDAO
    ) {
        this.bookRepDAO = bookRepDAO;
        this.bookLikeRepDAO = bookLikeRepDAO;
        this.bookFavoriteRepDAO = bookFavoriteRepDAO;
        this.bookAccessHistoryRepDAO = bookAccessHistoryRepDAO;
        this.bookReservationRepDAO = bookReservationRepDAO;
        this.userRepDAO = userRepDAO;
    }

    public List<BookCatalogResponse> getBooks(String currentEmail) {
        User currentUser = findUserByEmail(currentEmail);
        return bookRepDAO.findAllByArchivedFalseOrderByTitleAsc()
                .stream()
                .map(book -> toCatalogResponse(book, currentUser))
                .toList();
    }

    public BookCatalogResponse getBook(Integer id, String currentEmail) {
        return toCatalogResponse(findBookById(id), findUserByEmail(currentEmail));
    }

    @Transactional
    public BookCatalogResponse createBook(BookUpsertRequest request) {
        Book book = new Book();
        applyBookRequest(book, request);
        Book savedBook = bookRepDAO.save(book);
        LOGGER.info("Created catalog title id={} title={}", savedBook.getId(), savedBook.getTitle());
        return toCatalogResponse(savedBook, null);
    }

    @Transactional
    public BookCatalogResponse updateBook(Integer id, BookUpsertRequest request) {
        Book book = findBookById(id);
        applyBookRequest(book, request);
        Book savedBook = bookRepDAO.save(book);
        LOGGER.info("Updated catalog title id={} title={}", savedBook.getId(), savedBook.getTitle());
        return toCatalogResponse(savedBook, null);
    }

    @Transactional
    public void deleteBook(Integer id, boolean forceArchiveWithActiveLoans) {
        Book book = findBookById(id);
        boolean hasActiveReservations = bookReservationRepDAO.existsByBookAndStatus(book, BookReservationStatus.ACTIVE);
        boolean hasActivePhysicalCheckouts = bookAccessHistoryRepDAO.existsByBookAndAccessTypeAndStatus(
                book,
                BookAccessType.PHYSICAL_TAKE_HOME,
                BookAccessStatus.ACTIVE
        );
        if (!forceArchiveWithActiveLoans && (hasActiveReservations || hasActivePhysicalCheckouts)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Book has active reservations or physical checkouts. Use force archive to archive it anyway"
            );
        }
        book.setArchived(true);
        bookRepDAO.save(book);
        LOGGER.info(
                "Archived catalog title id={} title={} forceArchiveWithActiveLoans={}",
                book.getId(),
                book.getTitle(),
                forceArchiveWithActiveLoans
        );
    }

    @Transactional
    public BookCatalogResponse likeBook(Integer id, String currentEmail) {
        Book book = findBookById(id);
        User user = findUserByEmail(currentEmail);
        if (!bookLikeRepDAO.existsByUserAndBook(user, book)) {
            BookLike bookLike = new BookLike();
            bookLike.setBook(book);
            bookLike.setUser(user);
            bookLike.setCreatedAt(LocalDateTime.now());
            bookLikeRepDAO.save(bookLike);
            LOGGER.info("User {} liked book id={}", user.getEmail(), book.getId());
        }
        return toCatalogResponse(book, user);
    }

    @Transactional
    public BookCatalogResponse unlikeBook(Integer id, String currentEmail) {
        Book book = findBookById(id);
        User user = findUserByEmail(currentEmail);
        bookLikeRepDAO.deleteByUserAndBook(user, book);
        LOGGER.info("User {} removed like from book id={}", user.getEmail(), book.getId());
        return toCatalogResponse(book, user);
    }

    @Transactional
    public BookCatalogResponse favoriteBook(Integer id, String currentEmail) {
        Book book = findBookById(id);
        User user = findUserByEmail(currentEmail);
        if (!bookFavoriteRepDAO.existsByUserAndBook(user, book)) {
            BookFavorite bookFavorite = new BookFavorite();
            bookFavorite.setBook(book);
            bookFavorite.setUser(user);
            bookFavorite.setCreatedAt(LocalDateTime.now());
            bookFavoriteRepDAO.save(bookFavorite);
            LOGGER.info("User {} favorited book id={}", user.getEmail(), book.getId());
        }
        return toCatalogResponse(book, user);
    }

    @Transactional
    public BookCatalogResponse unfavoriteBook(Integer id, String currentEmail) {
        Book book = findBookById(id);
        User user = findUserByEmail(currentEmail);
        bookFavoriteRepDAO.deleteByUserAndBook(user, book);
        LOGGER.info("User {} removed favorite from book id={}", user.getEmail(), book.getId());
        return toCatalogResponse(book, user);
    }

    @Transactional
    public BookAccessHistoryResponse accessEbook(Integer bookId, String currentEmail) {
        Book book = findBookById(bookId);
        User user = findUserByEmail(currentEmail);
        if (!book.isEbookAvailable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ebook access is not available for this title");
        }
        BookAccessHistoryResponse response = BookAccessHistoryResponse.from(createCompletedAccess(user, book, BookAccessType.EBOOK));
        LOGGER.info("User {} accessed ebook bookId={}", user.getEmail(), book.getId());
        return response;
    }

    @Transactional
    public BookAccessHistoryResponse accessInStoreRead(Integer bookId, String currentEmail) {
        Book book = findBookById(bookId);
        User user = findUserByEmail(currentEmail);
        if (!book.isInStoreReadAvailable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "In-store reading is not available for this title");
        }
        BookAccessHistoryResponse response = BookAccessHistoryResponse.from(createCompletedAccess(user, book, BookAccessType.IN_STORE_READ));
        LOGGER.info("User {} recorded in-store read for bookId={}", user.getEmail(), book.getId());
        return response;
    }

    @Transactional
    public BookReservationResponse reservePhysicalBook(Integer bookId, String currentEmail) {
        Book book = findBookById(bookId);
        User user = findUserByEmail(currentEmail);
        if (findActiveReservation(user, book) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have an active reservation for this book");
        }
        assertPhysicalCopyAvailable(book);

        BookReservation reservation = new BookReservation();
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setStatus(BookReservationStatus.ACTIVE);
        reservation.setReservedAt(LocalDateTime.now());

        BookReservation savedReservation = bookReservationRepDAO.save(reservation);
        LOGGER.info("User {} reserved physical book id={}", user.getEmail(), book.getId());
        return BookReservationResponse.from(savedReservation);
    }

    @Transactional
    public BookAccessHistoryResponse checkoutPhysicalBook(Integer bookId, String currentEmail) {
        Book book = findBookById(bookId);
        User user = findUserByEmail(currentEmail);
        if (user.getRole() != UserRole.CUSTOMER_WITH_READ_ALLOWED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only CUSTOMER_WITH_READ_ALLOWED users can take physical books home");
        }
        BookReservation reservation = findActiveReservation(user, book);
        if (reservation != null && reservation.getCheckedOutAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already have this physical book checked out");
        }

        long activePhysicalCount = bookReservationRepDAO.countByUserAndStatusAndCheckedOutAtIsNotNull(
                user,
                BookReservationStatus.ACTIVE
        );
        if (activePhysicalCount >= MAX_PHYSICAL_BOOKS_PER_USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Physical checkout limit reached. Maximum 3 active physical books are allowed");
        }

        if (reservation == null) {
            assertPhysicalCopyAvailable(book);
            reservation = new BookReservation();
            reservation.setUser(user);
            reservation.setBook(book);
            reservation.setStatus(BookReservationStatus.ACTIVE);
            reservation.setReservedAt(LocalDateTime.now());
        }
        reservation.setCheckedOutAt(LocalDateTime.now());
        BookReservation savedReservation = bookReservationRepDAO.save(reservation);

        BookAccessHistory history = new BookAccessHistory();
        history.setUser(user);
        history.setBook(book);
        history.setAccessType(BookAccessType.PHYSICAL_TAKE_HOME);
        history.setStatus(BookAccessStatus.ACTIVE);
        history.setStartedAt(LocalDateTime.now());
        history.setDueAt(LocalDateTime.now().plusMonths(1));

        BookAccessHistory savedHistory = bookAccessHistoryRepDAO.save(history);
        LOGGER.info(
                "User {} checked out physical book id={} reservationId={} dueAt={}",
                user.getEmail(),
                book.getId(),
                savedReservation.getId(),
                savedHistory.getDueAt()
        );
        return BookAccessHistoryResponse.from(savedHistory);
    }

    @Transactional
    public BookReservationResponse returnReservedBook(Integer bookId, String currentEmail) {
        Book book = findBookByIdIncludingArchived(bookId);
        User user = findUserByEmail(currentEmail);
        BookReservation reservation = findActiveReservation(user, book);
        if (reservation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No active reservation found for this book");
        }
        if (reservation.getCheckedOutAt() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use physical return for checked-out books");
        }

        reservation.setStatus(BookReservationStatus.CANCELLED);
        reservation.setCompletedAt(LocalDateTime.now());
        BookReservation savedReservation = bookReservationRepDAO.save(reservation);
        LOGGER.info("User {} returned reserved book id={} reservationId={}", user.getEmail(), book.getId(), savedReservation.getId());
        return BookReservationResponse.from(savedReservation);
    }

    @Transactional
    public BookAccessHistoryResponse returnPhysicalBook(Integer bookId, String currentEmail) {
        Book book = findBookByIdIncludingArchived(bookId);
        User user = findUserByEmail(currentEmail);
        BookAccessHistory history = bookAccessHistoryRepDAO.findFirstByUserAndBookAndAccessTypeAndStatusOrderByStartedAtDesc(
                        user,
                        book,
                        BookAccessType.PHYSICAL_TAKE_HOME,
                        BookAccessStatus.ACTIVE
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active physical checkout found for this book"));
        BookReservation reservation = findActiveReservation(user, book);
        if (reservation != null && reservation.getCheckedOutAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use reservation return for reserved books that were not checked out");
        }

        history.setStatus(BookAccessStatus.RETURNED);
        history.setCompletedAt(LocalDateTime.now());
        if (reservation != null) {
            reservation.setStatus(BookReservationStatus.FULFILLED);
            reservation.setCompletedAt(LocalDateTime.now());
            bookReservationRepDAO.save(reservation);
        }
        BookAccessHistory savedHistory = bookAccessHistoryRepDAO.save(history);
        LOGGER.info(
                "User {} returned physical book id={} reservationId={}",
                user.getEmail(),
                book.getId(),
                reservation != null ? reservation.getId() : null
        );
        return BookAccessHistoryResponse.from(savedHistory);
    }

    @Transactional
    public List<BookAccessHistoryResponse> getCurrentUserHistory(String currentEmail) {
        User user = findUserByEmail(currentEmail);
        return bookAccessHistoryRepDAO.findAllByUserOrderByStartedAtDesc(user)
                .stream()
                .map(BookAccessHistoryResponse::from)
                .toList();
    }

    @Transactional
    public List<BookAccessHistoryResponse> getUserHistory(Integer userId, Collection<? extends GrantedAuthority> actorAuthorities) {
        User user = findUserById(userId);
        assertCanManageUser(user, actorAuthorities);
        return bookAccessHistoryRepDAO.findAllByUserOrderByStartedAtDesc(user)
                .stream()
                .map(BookAccessHistoryResponse::from)
                .toList();
    }

    @Transactional
    public List<BookReservationResponse> getCurrentUserReservations(String currentEmail) {
        User user = findUserByEmail(currentEmail);
        return bookReservationRepDAO.findAllByUserOrderByReservedAtDesc(user)
                .stream()
                .map(BookReservationResponse::from)
                .toList();
    }

    @Transactional
    public List<BookReservationResponse> getUserReservations(Integer userId, Collection<? extends GrantedAuthority> actorAuthorities) {
        User user = findUserById(userId);
        assertCanManageUser(user, actorAuthorities);
        return bookReservationRepDAO.findAllByUserOrderByReservedAtDesc(user)
                .stream()
                .map(BookReservationResponse::from)
                .toList();
    }

    @Transactional
    public List<User> scanAndFlagUsersWithOverduePhysicalLoans() {
        Map<Integer, User> flaggedUsers = new LinkedHashMap<>();
        List<BookAccessHistory> overdueHistories = bookAccessHistoryRepDAO.findAllByAccessTypeAndStatusAndDueAtBeforeOrderByDueAtAsc(
                BookAccessType.PHYSICAL_TAKE_HOME,
                BookAccessStatus.ACTIVE,
                LocalDateTime.now()
        );

        for (BookAccessHistory overdueHistory : overdueHistories) {
            User user = overdueHistory.getUser();
            if (!user.isFlagged()) {
                user.setFlagged(true);
                userRepDAO.save(user);
                LOGGER.warn(
                        "Flagged overdue user id={} email={} bookId={} dueAt={}",
                        user.getId(),
                        user.getEmail(),
                        overdueHistory.getBook().getId(),
                        overdueHistory.getDueAt()
                );
            }
            flaggedUsers.put(user.getId(), user);
        }

        return List.copyOf(flaggedUsers.values());
    }

    @Transactional
    public BookAccessHistoryResponse forceReturnPhysicalBook(
            Integer userId,
            Integer bookId,
            Collection<? extends GrantedAuthority> actorAuthorities
    ) {
        User user = findUserById(userId);
        assertCanManageUser(user, actorAuthorities);
        Book book = findBookByIdIncludingArchived(bookId);
        BookAccessHistory history = findActivePhysicalCheckout(user, book);
        BookReservation reservation = findActiveReservation(user, book);

        history.setStatus(BookAccessStatus.RETURNED);
        history.setCompletedAt(LocalDateTime.now());
        if (reservation != null) {
            reservation.setStatus(BookReservationStatus.FULFILLED);
            reservation.setCompletedAt(LocalDateTime.now());
            bookReservationRepDAO.save(reservation);
        }
        BookAccessHistory savedHistory = bookAccessHistoryRepDAO.save(history);
        refreshFlaggedState(user, "Forced return processed");
        LOGGER.warn("Forced return processed targetUserId={} bookId={} actorScope=admin-or-manager", userId, bookId);
        return BookAccessHistoryResponse.from(savedHistory);
    }

    @Transactional
    public BookAccessHistoryResponse extendPhysicalCheckoutDuration(
            Integer userId,
            Integer bookId,
            int extraDays,
            Collection<? extends GrantedAuthority> actorAuthorities
    ) {
        if (extraDays < 1 || extraDays > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Extension days must be between 1 and 20");
        }

        User user = findUserById(userId);
        assertCanManageUser(user, actorAuthorities);
        Book book = findBookByIdIncludingArchived(bookId);
        BookAccessHistory history = findActivePhysicalCheckout(user, book);
        history.setDueAt(history.getDueAt().plusDays(extraDays));
        BookAccessHistory savedHistory = bookAccessHistoryRepDAO.save(history);
        refreshFlaggedState(user, "Checkout duration extended");
        LOGGER.info(
                "Extended physical checkout duration targetUserId={} bookId={} extraDays={} newDueAt={}",
                userId,
                bookId,
                extraDays,
                savedHistory.getDueAt()
        );
        return BookAccessHistoryResponse.from(savedHistory);
    }

    private BookCatalogResponse toCatalogResponse(Book book, User currentUser) {
        return BookCatalogResponse.from(
                book,
                bookLikeRepDAO.countByBook(book),
                bookFavoriteRepDAO.countByBook(book),
                currentUser != null && bookLikeRepDAO.existsByUserAndBook(currentUser, book),
                currentUser != null && bookFavoriteRepDAO.existsByUserAndBook(currentUser, book)
        );
    }

    private BookAccessHistory createCompletedAccess(User user, Book book, BookAccessType accessType) {
        BookAccessHistory history = new BookAccessHistory();
        history.setUser(user);
        history.setBook(book);
        history.setAccessType(accessType);
        history.setStatus(BookAccessStatus.COMPLETED);
        history.setStartedAt(LocalDateTime.now());
        history.setCompletedAt(LocalDateTime.now());
        return bookAccessHistoryRepDAO.save(history);
    }

    private void assertPhysicalCopyAvailable(Book book) {
        int availableCopies = book.getPhysicalStockQuantity() - (int) bookReservationRepDAO.countByBookAndStatus(book, BookReservationStatus.ACTIVE);
        if (availableCopies <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No physical copies are currently available because all copies are reserved or checked out"
            );
        }
    }

    private void refreshFlaggedState(User user, String reason) {
        boolean hasActiveOverdueLoan = bookAccessHistoryRepDAO.existsByUserAndAccessTypeAndStatusAndDueAtBefore(
                user,
                BookAccessType.PHYSICAL_TAKE_HOME,
                BookAccessStatus.ACTIVE,
                LocalDateTime.now()
        );
        if (user.isFlagged() == hasActiveOverdueLoan) {
            return;
        }
        user.setFlagged(hasActiveOverdueLoan);
        userRepDAO.save(user);
        LOGGER.info("Updated flagged state userId={} email={} flagged={} reason={}", user.getId(), user.getEmail(), hasActiveOverdueLoan, reason);
    }

    private void applyBookRequest(Book book, BookUpsertRequest request) {
        book.setTitle(request.title().trim());
        book.setAuthor(request.author().trim());
        book.setGenre(request.genre().trim());
        book.setPublisher(request.publisher().trim());
        book.setPublishedDate(request.publishedDate());
        book.setIsbn(request.isbn() == null || request.isbn().isBlank() ? null : request.isbn().trim());
        book.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        book.setPhysicalStockQuantity(request.physicalStockQuantity());
        book.setEbookAvailable(request.ebookAvailable());
        book.setInStoreReadAvailable(request.inStoreReadAvailable());
        book.setLanguage(request.language() == null || request.language().isBlank() ? null : request.language().trim());
        book.setPageCount(request.pageCount());
    }

    private Book findBookById(Integer id) {
        return bookRepDAO.findByIdAndArchivedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    private Book findBookByIdIncludingArchived(Integer id) {
        return bookRepDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Book not found"));
    }

    private User findUserByEmail(String email) {
        return userRepDAO.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private User findUserById(Integer id) {
        return userRepDAO.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private BookReservation findActiveReservation(User user, Book book) {
        return bookReservationRepDAO.findFirstByUserAndBookAndStatusOrderByReservedAtDesc(user, book, BookReservationStatus.ACTIVE)
                .orElse(null);
    }

    private BookAccessHistory findActivePhysicalCheckout(User user, Book book) {
        return bookAccessHistoryRepDAO.findFirstByUserAndBookAndAccessTypeAndStatusOrderByStartedAtDesc(
                        user,
                        book,
                        BookAccessType.PHYSICAL_TAKE_HOME,
                        BookAccessStatus.ACTIVE
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active physical checkout found for this book"));
    }

    private void assertCanManageUser(User targetUser, Collection<? extends GrantedAuthority> actorAuthorities) {
        if (hasRole(actorAuthorities, UserRole.ADMIN)) {
            return;
        }
        if (hasRole(actorAuthorities, UserRole.MANAGER)
                && (targetUser.getRole() == UserRole.CUSTOMER || targetUser.getRole() == UserRole.CUSTOMER_WITH_READ_ALLOWED)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this user's history");
    }

    private boolean hasRole(Collection<? extends GrantedAuthority> actorAuthorities, UserRole role) {
        String authority = "ROLE_" + role.name();
        return actorAuthorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
