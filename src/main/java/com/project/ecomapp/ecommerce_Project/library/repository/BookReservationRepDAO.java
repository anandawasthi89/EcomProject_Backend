package com.project.ecomapp.ecommerce_Project.library.repository;

import com.project.ecomapp.ecommerce_Project.Bean.Book;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservation;
import com.project.ecomapp.ecommerce_Project.Bean.BookReservationStatus;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookReservationRepDAO extends JpaRepository<BookReservation, Integer> {

    boolean existsByUserAndBookAndStatus(User user, Book book, BookReservationStatus status);

    boolean existsByBookAndStatus(Book book, BookReservationStatus status);

    long countByBookAndStatus(Book book, BookReservationStatus status);

    long countByUserAndStatusAndCheckedOutAtIsNotNull(User user, BookReservationStatus status);

    Optional<BookReservation> findFirstByUserAndBookAndStatusOrderByReservedAtDesc(
            User user,
            Book book,
            BookReservationStatus status
    );

    List<BookReservation> findAllByUserOrderByReservedAtDesc(User user);
}
