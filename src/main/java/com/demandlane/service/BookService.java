package com.demandlane.service;

import com.demandlane.dto.BookRequest;
import com.demandlane.entity.Book;
import com.demandlane.exception.ActiveLoanException;
import com.demandlane.exception.DuplicateEntityException;
import com.demandlane.exception.EntityNotFoundException;
import com.demandlane.repository.BookRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class BookService {

    @Inject
    BookRepository bookRepository;

    public List<Book> listAll() {
        return bookRepository.listAll();
    }

    public Book findById(Long id) {
        Book book = bookRepository.findById(id);
        if (book == null) {
            throw new EntityNotFoundException("Book", id);
        }
        return book;
    }

    @Transactional
    public Book create(BookRequest request) {
        if (bookRepository.findByIsbn(request.isbn()).isPresent()) {
            throw new DuplicateEntityException("A book with ISBN " + request.isbn() + " already exists");
        }
        Book book = new Book();
        book.title = request.title();
        book.author = request.author();
        book.isbn = request.isbn();
        book.totalCopies = request.totalCopies();
        book.availableCopies = request.totalCopies();
        bookRepository.persist(book);
        return book;
    }

    @Transactional
    public Book update(Long id, BookRequest request) {
        Book book = findById(id);
        book.title = request.title();
        book.author = request.author();
        book.isbn = request.isbn();
        book.adjustCopies(request.totalCopies());
        return book;
    }

    @Transactional
    public void delete(Long id) {
        Book book = findById(id);
        if (book.hasActiveLoans()) {
            throw new ActiveLoanException("Cannot delete book with active loans");
        }
        bookRepository.delete(book);
    }
}
