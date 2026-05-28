package com.project.ecomapp.ecommerce_Project.publisher;

import java.time.LocalDate;

public class BookUploadPayload {
    private String eventId;
    private String title;
    private String author;
    private String publisher;
    private String publishedDate;
    private String language;
    private Integer pageCount;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
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

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
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

    public BookUploadPayload() {
    }

    public BookUploadPayload(String title, String author, String publisher, String publishedDate, String language, Integer pageCount) {
        this.eventId = title+author+publishedDate;
        this.title = title;
        this.author = author;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.language = language;
        this.pageCount = pageCount;
    }
}
