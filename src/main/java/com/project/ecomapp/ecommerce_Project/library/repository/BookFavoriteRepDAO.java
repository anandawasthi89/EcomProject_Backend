package com.project.ecomapp.ecommerce_Project.library.repository;

import com.project.ecomapp.ecommerce_Project.Bean.Book;
import com.project.ecomapp.ecommerce_Project.Bean.BookFavorite;
import com.project.ecomapp.ecommerce_Project.Bean.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookFavoriteRepDAO extends JpaRepository<BookFavorite, Integer> {

    long countByBook(Book book);

    boolean existsByUserAndBook(User user, Book book);

    void deleteByUserAndBook(User user, Book book);
}
