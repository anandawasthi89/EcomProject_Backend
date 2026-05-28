package com.project.ecomapp.ecommerce_Project.library.repository;

import com.project.ecomapp.ecommerce_Project.Bean.Book;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessHistory;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessStatus;
import com.project.ecomapp.ecommerce_Project.Bean.BookAccessType;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookAccessHistoryRepDAO extends JpaRepository<BookAccessHistory, Integer> {

    long countByUserAndAccessTypeAndStatus(User user, BookAccessType accessType, BookAccessStatus status);

    Optional<BookAccessHistory> findFirstByUserAndBookAndAccessTypeAndStatusOrderByStartedAtDesc(
            User user,
            Book book,
            BookAccessType accessType,
            BookAccessStatus status
    );

    List<BookAccessHistory> findAllByUserOrderByStartedAtDesc(User user);

    boolean existsByBookAndAccessTypeAndStatus(Book book, BookAccessType accessType, BookAccessStatus status);

    boolean existsByUserAndAccessTypeAndStatusAndDueAtBefore(
            User user,
            BookAccessType accessType,
            BookAccessStatus status,
            LocalDateTime dueAt
    );

    List<BookAccessHistory> findAllByAccessTypeAndStatusAndDueAtBeforeOrderByDueAtAsc(
            BookAccessType accessType,
            BookAccessStatus status,
            LocalDateTime dueAt
    );
}
