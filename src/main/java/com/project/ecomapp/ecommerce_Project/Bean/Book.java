package com.project.ecomapp.ecommerce_Project.Bean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 150)
    private String author;

    @Column(nullable = false, length = 100)
    private String genre;

    @Column(nullable = false, length = 150)
    private String publisher;

    @Column
    private LocalDate publishedDate;

    @Column(unique = true, length = 30)
    private String isbn;

    @Column(length = 4000)
    private String description;

    @Column(nullable = false)
    private int physicalStockQuantity;

    @Column(nullable = false)
    private boolean ebookAvailable;

    @Column(nullable = false)
    private boolean inStoreReadAvailable;

    @Column(length = 50)
    private String language;

    @Column
    private Integer pageCount;

    @Column(nullable = false)
    private boolean archived = false;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public LocalDate getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDate publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPhysicalStockQuantity() {
        return physicalStockQuantity;
    }

    public void setPhysicalStockQuantity(int physicalStockQuantity) {
        this.physicalStockQuantity = physicalStockQuantity;
    }

    public boolean isEbookAvailable() {
        return ebookAvailable;
    }

    public void setEbookAvailable(boolean ebookAvailable) {
        this.ebookAvailable = ebookAvailable;
    }

    public boolean isInStoreReadAvailable() {
        return inStoreReadAvailable;
    }

    public void setInStoreReadAvailable(boolean inStoreReadAvailable) {
        this.inStoreReadAvailable = inStoreReadAvailable;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
