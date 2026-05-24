package com.project.ecomapp.ecommerce_Project.library.repository;

import com.project.ecomapp.ecommerce_Project.Bean.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepDAO extends JpaRepository<Book, Integer> {

    List<Book> findAllByArchivedFalseOrderByTitleAsc();

    Optional<Book> findByIdAndArchivedFalse(Integer id);

    Optional<Book> findById(Integer id);
}
