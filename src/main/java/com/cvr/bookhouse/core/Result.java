package com.cvr.bookhouse.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Result {
    private final boolean ok;
    private final List<Msg> msgs;

    private Result(boolean ok, List<Msg> msgs) {
        this.ok = ok;
        this.msgs = msgs;
    }

    public static Result success() { return new Result(true, new ArrayList<>()); }
    public static Result failure() { return new Result(false, new ArrayList<>()); }

    public static Result success(List<Msg> msgs) { return new Result(true, new ArrayList<>(msgs)); }
    public static Result failure(List<Msg> msgs) { return new Result(false, new ArrayList<>(msgs)); }

    public Result add(String code, Object... args) {
        msgs.add(new Msg(code, args));
        return this;
    }

    public boolean ok() { return ok; }

    public List<Msg> messages() { return Collections.unmodifiableList(msgs); }
}
