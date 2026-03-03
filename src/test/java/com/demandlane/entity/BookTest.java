package com.demandlane.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookTest {

    private Book bookWith(int total, int available) {
        Book book = new Book();
        book.title = "Test Book";
        book.totalCopies = total;
        book.availableCopies = available;
        return book;
    }

    @Test
    void isAvailable_withCopies_returnsTrue() {
        assertTrue(bookWith(3, 2).isAvailable());
    }

    @Test
    void isAvailable_zeroCopies_returnsFalse() {
        assertFalse(bookWith(3, 0).isAvailable());
    }

    @Test
    void hasActiveLoans_whenSomeBorrowed_returnsTrue() {
        assertTrue(bookWith(3, 1).hasActiveLoans());
    }

    @Test
    void hasActiveLoans_whenAllAvailable_returnsFalse() {
        assertFalse(bookWith(3, 3).hasActiveLoans());
    }

    @Test
    void checkout_decrements() {
        Book book = bookWith(3, 2);
        book.checkout();
        assertEquals(1, book.availableCopies);
    }

    @Test
    void checkout_lastCopy_decrements() {
        Book book = bookWith(3, 1);
        book.checkout();
        assertEquals(0, book.availableCopies);
    }

    @Test
    void checkout_noneAvailable_throws() {
        Book book = bookWith(3, 0);
        IllegalStateException ex = assertThrows(IllegalStateException.class, book::checkout);
        assertTrue(ex.getMessage().contains("No available copies"));
    }

    @Test
    void checkin_increments() {
        Book book = bookWith(3, 1);
        book.checkin();
        assertEquals(2, book.availableCopies);
    }

    @Test
    void checkin_allReturned_throws() {
        Book book = bookWith(3, 3);
        IllegalStateException ex = assertThrows(IllegalStateException.class, book::checkin);
        assertTrue(ex.getMessage().contains("All copies already returned"));
    }

    @Test
    void adjustCopies_increase_addsToAvailable() {
        Book book = bookWith(3, 2);
        book.adjustCopies(5);
        assertEquals(5, book.totalCopies);
        assertEquals(4, book.availableCopies);
    }

    @Test
    void adjustCopies_decrease_reducesAvailable() {
        Book book = bookWith(5, 3);
        book.adjustCopies(3);
        assertEquals(3, book.totalCopies);
        assertEquals(1, book.availableCopies);
    }

    @Test
    void adjustCopies_decrease_floorsAtZero() {
        Book book = bookWith(5, 1);
        book.adjustCopies(2);
        assertEquals(2, book.totalCopies);
        assertEquals(0, book.availableCopies);
    }
}
