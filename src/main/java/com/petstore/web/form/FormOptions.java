package com.petstore.web.form;

import java.time.Clock;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.stereotype.Component;

import com.petstore.catalog.SupportedLocales;

/**
 * Drop-down choices. The legacy forms hard-coded these per language (Japanese pages offered Japanese
 * prefectures, Chinese pages Chinese provinces), so they stay per locale.
 */
@Component("formOptions")
public class FormOptions {

    public record Option(String value, String label) {
    }

    private static final Map<String, List<Option>> STATES = Map.of(
            "en_US", options("California", "New York", "Texas"),
            "ja_JP", options("東京都", "大阪府", "長野県"),
            "zh_CN", options("江苏省", "北京市", "上海市"));

    private static final Map<String, List<Option>> COUNTRIES = Map.of(
            "en_US", List.of(new Option("USA", "United States"), new Option("Canada", "Canada"),
                    new Option("Japan", "Japan"), new Option("China", "China")),
            "ja_JP", options("アメリカ", "カナダ", "日本", "中国"),
            "zh_CN", options("美国", "加拿大", "日本", "中国"));

    // The legacy create form posted "Meow Club" under a "Meow Card" label, so the edit form could never
    // re-select it. One value now.
    private static final List<Option> CARD_TYPES = options("Java(TM) Card", "Duke Express", "Meow Card");

    private final Clock clock;

    public FormOptions(Clock clock) {
        this.clock = clock;
    }

    public List<Option> states(Locale locale, String current) {
        return withCurrent(STATES.get(SupportedLocales.key(locale)), current);
    }

    public List<Option> countries(Locale locale, String current) {
        return withCurrent(COUNTRIES.get(SupportedLocales.key(locale)), current);
    }

    public List<Option> cardTypes(String current) {
        return withCurrent(CARD_TYPES, current);
    }

    public List<String> months() {
        return IntStream.rangeClosed(1, 12).mapToObj(m -> "%02d".formatted(m)).toList();
    }

    public List<Option> years(String current) {
        int thisYear = Year.now(clock).getValue();
        List<Option> years = IntStream.rangeClosed(thisYear, thisYear + 10)
                .mapToObj(String::valueOf)
                .map(y -> new Option(y, y))
                .toList();
        return withCurrent(years, current);
    }

    /**
     * Seed customers have values that aren't in any list ("CA", expiry year "04"). The legacy selects
     * silently replaced them with the first option on save; we keep the stored value selectable instead.
     */
    static List<Option> withCurrent(List<Option> options, String current) {
        if (current == null || current.isBlank() || options.stream().anyMatch(o -> o.value().equals(current))) {
            return options;
        }
        List<Option> result = new ArrayList<>(options.size() + 1);
        result.add(new Option(current, current));
        result.addAll(options);
        return result;
    }

    private static List<Option> options(String... values) {
        return java.util.Arrays.stream(values).map(v -> new Option(v, v)).toList();
    }
}
