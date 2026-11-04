package com.petstore.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

class CatalogServiceTest {

    private final CatalogRepository repository = mock(CatalogRepository.class);
    private final CatalogService service = new CatalogService(repository);

    @Test
    void keywordsAreLowerCasedAndDeduplicated() {
        assertThat(CatalogService.keywords("  Fish fish   DOG ")).containsExactly("fish", "dog");
    }

    @Test
    void blankSearchNeverHitsTheDatabase() {
        assertThat(service.search("   ", 0, 2, Locale.US).list()).isEmpty();
        assertThat(service.search(null, 0, 2, Locale.US).list()).isEmpty();
        verify(repository, never()).searchItems(org.mockito.ArgumentMatchers.any(), anyInt(), anyInt(), anyString());
    }

    @Test
    void searchUsesTheLegacyLocaleKey() {
        when(repository.searchItems(Set.of("koi"), 0, 2, "ja_JP")).thenReturn(ResultPage.empty());

        service.search("KOI", 0, 2, Locale.JAPAN);

        verify(repository).searchItems(Set.of("koi"), 0, 2, "ja_JP");
    }

    @Test
    void pagingParametersAreClamped() {
        when(repository.findProducts("FISH", 0, CatalogService.MAX_PAGE_SIZE, "en_US")).thenReturn(ResultPage.empty());
        when(repository.findProducts("FISH", 0, 1, "en_US")).thenReturn(ResultPage.empty());

        service.products("FISH", -5, 1_000_000, Locale.US);
        service.products("FISH", 0, 0, Locale.US);

        verify(repository).findProducts("FISH", 0, CatalogService.MAX_PAGE_SIZE, "en_US");
        verify(repository).findProducts("FISH", 0, 1, "en_US");
    }

    @Test
    void unsupportedLocaleFallsBackToEnglish() {
        assertThat(SupportedLocales.key(Locale.GERMANY)).isEqualTo("en_US");
        assertThat(SupportedLocales.key(Locale.forLanguageTag("ja"))).isEqualTo("ja_JP");
        assertThat(SupportedLocales.parse("zh-CN")).isEqualTo(Locale.CHINA);
    }
}
