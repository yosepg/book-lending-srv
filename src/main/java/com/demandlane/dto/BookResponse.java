package com.demandlane.dto;

import com.demandlane.entity.Book;

public record BookResponse(
        Long id,
        String title,
        String author,
        String isbn,
        int totalCopies,
        int availableCopies
) {
    public static BookResponse from(Book book) {
        return new BookResponse(
                book.id, book.title, book.author,
                book.isbn, book.totalCopies, book.availableCopies
        );
    }
}
