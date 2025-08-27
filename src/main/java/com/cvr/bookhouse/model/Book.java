package com.cvr.bookhouse.model;

import java.util.ArrayList;
import java.util.List;

public class Book {
    String bookId;
    int copies;
    //List<String> waitlist = new ArrayList<>();
    
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
    /*
    public boolean addToWaitlist(String userId) {
        if (userId == null || userId.isBlank()) return false;
        if (waitlist.contains(userId)) return false;
        waitlist.add(userId);
        return true;
    }
    public boolean cancelWaitlist(String userId) {
        return waitlist.remove(userId);
    }

    public List<String> getWaitlist() {
        return waitlist;
    }*/
}
