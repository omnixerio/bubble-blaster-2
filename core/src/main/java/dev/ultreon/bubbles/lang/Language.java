package dev.ultreon.bubbles.lang;

import dev.ultreon.libs.commons.v0.Identifier;

import java.util.Locale;
import java.util.Map;

public class Language {
    private final Locale locale;
    private final Map<String, String> languageMap;
    private final Identifier id;

    public Language(Locale locale, Map<String, String> languageMap, Identifier id) {
        this.locale = locale;
        this.languageMap = languageMap;
        this.id = id;
    }

    public String get(String path, Object... args) {
        var split = path.split("/");

        var s = this.languageMap.get(path);
        return s == null ? null : String.format(s, args);
    }

    public Locale getLocale() {
        return this.locale;
    }

    public Identifier getId() {
        return this.id;
    }

    public static String translate(String path, Object... args) {
        var language = LanguageManager.INSTANCE.get(LanguageManager.getCurrentLanguage());
        if (language == null) return translateFallback(path, args);
        var s = language.get(path, args);
        return s == null ? translateFallback(path, args) : s;
    }

    private static String translateFallback(String path, Object[] args) {
        var english = LanguageManager.INSTANCE.get(new Locale("en", "us"));
        if (english == null) {
            throw new IllegalStateException("English language not loaded");
        }
        var s = english.get(path, args);
        return s == null ? path : s;
    }
}
