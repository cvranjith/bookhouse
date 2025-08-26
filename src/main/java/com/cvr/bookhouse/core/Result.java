package com.cvr.bookhouse.core;

public record Result(boolean ok, String msg) {
    public static Result ok(String msg)    { return new Result(true, msg); }
    public static Result error(String msg) { return new Result(false, msg); }
}