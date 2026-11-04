package com.petstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ResultPageTest {

    @Test
    void lookAheadRowIsDroppedAndSignalsNextPage() {
        ResultPage<String> page = ResultPage.fromLookAhead(List.of("a", "b", "c"), 0, 2);

        assertThat(page.list()).containsExactly("a", "b");
        assertThat(page.isNextPageAvailable()).isTrue();
        assertThat(page.getStartOfNextPage()).isEqualTo(2);
    }

    @Test
    void lastPageHasNoNext() {
        ResultPage<String> page = ResultPage.fromLookAhead(List.of("e"), 4, 2);

        assertThat(page.isNextPageAvailable()).isFalse();
        assertThat(page.isPreviousPageAvailable()).isTrue();
    }

    @Test
    void previousPageStartMatchesLegacyArithmetic() {
        // legacy Page: max(start - size, 0), where size is the size of the *current* page
        assertThat(new ResultPage<>(List.of("x"), 4, false).getStartOfPreviousPage()).isEqualTo(3);
        assertThat(new ResultPage<>(List.of("x", "y"), 1, false).getStartOfPreviousPage()).isZero();
    }

    @Test
    void pagingPastTheEndOffersNoPreviousLink() {
        ResultPage<String> page = ResultPage.fromLookAhead(List.of(), 40, 2);

        assertThat(page.isPreviousPageAvailable()).isFalse();
        assertThat(page.getStartOfNextPage()).isZero();
    }

    @Test
    void firstPageHasNoPrevious() {
        assertThat(new ResultPage<>(List.of("a"), 0, true).isPreviousPageAvailable()).isFalse();
    }
}
