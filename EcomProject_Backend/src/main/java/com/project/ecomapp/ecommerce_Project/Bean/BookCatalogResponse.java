package com.project.ecomapp.ecommerce_Project.Bean;

import java.time.LocalDate;

public record BookCatalogResponse(
        Integer id,
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
        Integer pageCount,
        long likeCount,
        long favoriteCount,
        boolean likedByCurrentUser,
        boolean favoritedByCurrentUser
) {
    public static BookCatalogResponse from(
            Book book,
            long likeCount,
            long favoriteCount,
            boolean likedByCurrentUser,
            boolean favoritedByCurrentUser
    ) {
        return new BookCatalogResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getGenre(),
                book.getPublisher(),
                book.getPublishedDate(),
                book.getIsbn(),
                book.getDescription(),
                book.getPhysicalStockQuantity(),
                book.isEbookAvailable(),
                book.isInStoreReadAvailable(),
                book.getLanguage(),
                book.getPageCount(),
                likeCount,
                favoriteCount,
                likedByCurrentUser,
                favoritedByCurrentUser
        );
    }
}
