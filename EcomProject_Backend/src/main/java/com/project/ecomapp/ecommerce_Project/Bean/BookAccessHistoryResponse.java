package com.project.ecomapp.ecommerce_Project.Bean;

import java.time.LocalDateTime;

public record BookAccessHistoryResponse(
        Integer id,
        Integer bookId,
        String bookTitle,
        Integer userId,
        String userEmail,
        BookAccessType accessType,
        BookAccessStatus status,
        LocalDateTime startedAt,
        LocalDateTime dueAt,
        LocalDateTime completedAt
) {
    public static BookAccessHistoryResponse from(BookAccessHistory history) {
        return new BookAccessHistoryResponse(
                history.getId(),
                history.getBook().getId(),
                history.getBook().getTitle(),
                history.getUser().getId(),
                history.getUser().getEmail(),
                history.getAccessType(),
                history.getStatus(),
                history.getStartedAt(),
                history.getDueAt(),
                history.getCompletedAt()
        );
    }
}
