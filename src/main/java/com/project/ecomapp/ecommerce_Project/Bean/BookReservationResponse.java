package com.project.ecomapp.ecommerce_Project.Bean;

import java.time.LocalDateTime;

public record BookReservationResponse(
        Integer id,
        Integer bookId,
        String bookTitle,
        Integer userId,
        String userEmail,
        BookReservationStatus status,
        LocalDateTime reservedAt,
        LocalDateTime checkedOutAt,
        LocalDateTime completedAt
) {
    public static BookReservationResponse from(BookReservation reservation) {
        return new BookReservationResponse(
                reservation.getId(),
                reservation.getBook().getId(),
                reservation.getBook().getTitle(),
                reservation.getUser().getId(),
                reservation.getUser().getEmail(),
                reservation.getStatus(),
                reservation.getReservedAt(),
                reservation.getCheckedOutAt(),
                reservation.getCompletedAt()
        );
    }
}
