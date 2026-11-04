package com.petstore.catalog;

import java.util.List;

/**
 * One window of an ordered result. The previous/next arithmetic matches the legacy
 * {@code catalog.model.Page} exactly, since the paging links are built from it.
 */
public record ResultPage<T>(List<T> list, int start, boolean nextPageAvailable) {

    public ResultPage {
        list = List.copyOf(list);
    }

    public static <T> ResultPage<T> empty() {
        return new ResultPage<>(List.of(), 0, false);
    }

    /**
     * Adapters fetch {@code count + 1} rows; the extra row only tells us a next page exists. Paging past the
     * end gives the empty page at start 0 (no "Previous" link), as the legacy DAO did.
     */
    public static <T> ResultPage<T> fromLookAhead(List<T> rows, int start, int count) {
        if (rows.isEmpty()) {
            return empty();
        }
        boolean hasNext = rows.size() > count;
        return new ResultPage<>(hasNext ? rows.subList(0, count) : rows, start, hasNext);
    }

    public boolean isPreviousPageAvailable() {
        return start > 0;
    }

    public boolean isNextPageAvailable() {
        return nextPageAvailable;
    }

    public int getStartOfNextPage() {
        return start + list.size();
    }

    public int getStartOfPreviousPage() {
        return Math.max(start - list.size(), 0);
    }

    public int getSize() {
        return list.size();
    }

    public List<T> getList() {
        return list;
    }
}
