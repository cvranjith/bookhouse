package com.cvr.bookhouse.model;

public class Book {
    String bookId;
    int copies;

    public Book(String bookId, int copies) {
        this.bookId = bookId;
        this.copies = copies;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public int getCopies() {
        return copies;
    }

    public void setCopies(int copies) {
        this.copies = copies;
    }
}
