package com.project.ecomapp.ecommerce_Project.Bean;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

public record BookUpsertRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,
        @NotBlank(message = "Author is required")
        @Size(max = 150, message = "Author must be at most 150 characters")
        String author,
        @NotBlank(message = "Genre is required")
        @Size(max = 100, message = "Genre must be at most 100 characters")
        String genre,
        @NotBlank(message = "Publisher is required")
        @Size(max = 150, message = "Publisher must be at most 150 characters")
        String publisher,
        LocalDate publishedDate,
        @Size(max = 30, message = "ISBN must be at most 30 characters")
        String isbn,
        @Size(max = 4000, message = "Description must be at most 4000 characters")
        String description,
        @Min(value = 0, message = "Physical stock quantity cannot be negative")
        int physicalStockQuantity,
        @NotNull(message = "Ebook availability is required")
        Boolean ebookAvailable,
        @NotNull(message = "In-store read availability is required")
        Boolean inStoreReadAvailable,
        @Size(max = 50, message = "Language must be at most 50 characters")
        String language,
        @Min(value = 1, message = "Page count must be at least 1")
        Integer pageCount
) {
}
