package com.cvr.bookhouse.core;

import java.util.*;
import java.util.function.Function;

public final class PrintUtil {
    private PrintUtil() {
    }

    private static final int MAX_COL_WIDTH = 40;

    public static <T> String renderTable(
            List<String> headers,
            List<T> items,
            Function<T, List<String>> rowMapper) {
        if (items == null || items.isEmpty())
            return "(no rows)";

        List<String> hdr = new ArrayList<>(headers.size() + 1);
        hdr.add("sl");
        hdr.addAll(headers);

        List<List<String>> rows = new ArrayList<>(items.size());
        int i = 1;
        for (T item : items) {
            List<String> cols = new ArrayList<>(rowMapper.apply(item));
            cols.add(0, String.valueOf(i++));
            rows.add(cols);
        }

        int colCount = hdr.size();
        for (List<String> r : rows) {
            while (r.size() < colCount)
                r.add("");
            if (r.size() > colCount)
                r.subList(colCount, r.size()).clear();
        }

        int[] widths = new int[colCount];
        for (int c = 0; c < colCount; c++) {
            widths[c] = Math.min(MAX_COL_WIDTH, safe(hdr.get(c)).length());
        }
        for (List<String> r : rows) {
            for (int c = 0; c < colCount; c++) {
                int len = Math.min(MAX_COL_WIDTH, safe(r.get(c)).length());
                if (len > widths[c])
                    widths[c] = len;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(formatRow(hdr, widths)).append('\n');
        sb.append(formatSeparator(widths)).append('\n');
        for (List<String> r : rows) {
            sb.append(formatRow(r, widths)).append('\n');
        }
        return sb.toString();
    }

    private static String formatRow(List<String> cols, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            if (c > 0)
                sb.append(" | ");
            String cell = ellipsize(safe(c < cols.size() ? cols.get(c) : ""), widths[c]);
            sb.append(padRight(cell, widths[c]));
        }
        return sb.toString();
    }

    private static String formatSeparator(int[] widths) {
        StringBuilder sb = new StringBuilder();
        if (widths.length == 0)
            return "";

        for (int k = 0; k < widths[0]; k++)
            sb.append('-');

        for (int c = 1; c < widths.length; c++) {
            sb.append("-+-");
            for (int k = 0; k < widths[c]; k++)
                sb.append('-');
        }
        return sb.toString();
    }

    private static String ellipsize(String s, int width) {
        if (s.length() <= width)
            return s;
        if (width <= 3)
            return ".".repeat(width);
        return s.substring(0, width - 3) + "...";
    }

    private static String padRight(String s, int width) {
        int pad = width - s.length();
        if (pad <= 0)
            return s;
        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < pad; i++)
            sb.append(' ');
        return sb.toString();
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
