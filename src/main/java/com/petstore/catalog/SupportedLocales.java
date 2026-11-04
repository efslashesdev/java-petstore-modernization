package com.petstore.catalog;

import java.util.List;
import java.util.Locale;

/**
 * The store ships in three locales and the catalog rows (including prices - yen, yuan) are stored
 * per locale, so the locale is part of every catalog query, not just a display concern.
 */
public final class SupportedLocales {

    public static final Locale DEFAULT = Locale.US;
    public static final List<Locale> ALL = List.of(Locale.US, Locale.JAPAN, Locale.CHINA);

    private SupportedLocales() {
    }

    public static Locale resolve(Locale requested) {
        if (requested == null) {
            return DEFAULT;
        }
        return ALL.stream().filter(requested::equals).findFirst()
                .or(() -> ALL.stream().filter(l -> l.getLanguage().equals(requested.getLanguage())).findFirst())
                .orElse(DEFAULT);
    }

    public static String key(Locale locale) {
        return resolve(locale).toString();
    }

    public static Locale parse(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        return resolve(Locale.forLanguageTag(value.trim().replace('_', '-')));
    }
}
