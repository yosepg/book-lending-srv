CREATE TABLE book (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(255) NOT NULL,
    author     VARCHAR(255) NOT NULL,
    isbn       VARCHAR(13)  NOT NULL,
    total_copies     INT    NOT NULL DEFAULT 1,
    available_copies INT    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    CONSTRAINT uk_book_isbn UNIQUE (isbn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE member (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    name  VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE loan (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    book_id     BIGINT   NOT NULL,
    member_id   BIGINT   NOT NULL,
    borrowed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    due_date    DATE     NOT NULL,
    returned_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_loan_book   FOREIGN KEY (book_id)   REFERENCES book(id),
    CONSTRAINT fk_loan_member FOREIGN KEY (member_id)  REFERENCES member(id),
    INDEX idx_loan_book_id     (book_id),
    INDEX idx_loan_member_id   (member_id),
    INDEX idx_loan_returned_at (returned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
