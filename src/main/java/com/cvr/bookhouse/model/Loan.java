package com.cvr.bookhouse.model;

import java.time.Instant;

public class Loan {
    String bookId;
    String userId;
    Instant borrowedDate;

    public Loan(String bookId, String userId,Instant borrowedDate) {
        this.bookId = bookId;
        this.userId = userId;
        this.borrowedDate = borrowedDate;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getBorrowedDate() {
        return borrowedDate;
    }

    public void setBorrowedDate(Instant borrowedDate) {
        this.borrowedDate = borrowedDate;
    }
    
}
