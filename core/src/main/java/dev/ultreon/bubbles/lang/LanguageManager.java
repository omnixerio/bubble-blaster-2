package dev.ultreon.bubbles.lang;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.ultreon.libs.commons.v0.Identifier;
import dev.ultreon.libs.commons.v0.Logger;
import dev.ultreon.libs.registries.v0.Registry;
import dev.ultreon.bubbles.resources.ResourceManager;

import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LanguageManager {
    public static final LanguageManager INSTANCE = new LanguageManager();
    public static final Registry<Language> REGISTRY = Registry.create(new Identifier("languages"));
    private static Locale currentLanguage;
    private final Map<Locale, Language> languages = new HashMap<>();
    private final Set<Locale> locales = new HashSet<>();
    private final Set<Identifier> ids = new HashSet<>();
    private final Map<Locale, Identifier> locale2id = new HashMap<>();
    private final Map<Identifier, Locale> id2locale = new HashMap<>();
    public Logger logger = (level, message, t) -> {};

    private LanguageManager() {

    }

    public static Locale getCurrentLanguage() {
        return currentLanguage;
    }

    public static void setCurrentLanguage(Locale currentLanguage) {
        LanguageManager.currentLanguage = currentLanguage;
    }

    public Language load(Locale locale, Identifier id, ResourceManager resourceManager) {
        var gson = new Gson();
        var newPath = "lang/" + id.path() + ".json";
        var assets = resourceManager.getAllDataById(id.withPath(newPath));
        Map<String, String> languageMap = new HashMap<>();
        for (var asset : assets) {
            var object = gson.fromJson(new StringReader(new String(asset, StandardCharsets.UTF_8)), JsonObject.class);
            this.loadFile(languageMap, object);
        }

        var language = new Language(locale, languageMap, id);
        this.languages.put(locale, language);
        REGISTRY.register(id, language);
        return language;
    }

    public Language load(Locale locale, Identifier id, Reader reader) {
        var gson = new Gson();
        var s = "languages/" + id + ".json";
        Map<String, String> languageMap = new HashMap<>();
        this.loadFile(languageMap, gson.fromJson(reader, JsonObject.class));
        var language = new Language(locale, languageMap, id);
        this.languages.put(locale, language);
        REGISTRY.register(id, language);
        return language;
    }

    public Language get(Locale locale) {
        return this.languages.get(locale);
    }

    public void register(Locale locale, Identifier id) {
        if (this.locales.contains(locale)) {
            this.logger.warn("Locale overridden: " + locale.getLanguage());
        }
        if (this.ids.contains(id)) {
            this.logger.warn("LanguageID overridden: " + id);
        }

        this.locales.add(locale);
        this.ids.add(id);
        this.locale2id.put(locale, id);
        this.id2locale.put(id, locale);
    }

    public Locale getLocale(Identifier id) {
        return this.id2locale.get(id);
    }

    public Identifier getLanguageID(Locale locale) {
        return this.locale2id.get(locale);
    }

    private void loadFile(Map<String, String> languageMap, JsonObject object) {
        for (var entry : object.entrySet()) {
            var value = entry.getValue();
            var key = entry.getKey();
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                languageMap.put(key, value.getAsString());
            }
        }
    }

    public Set<Locale> getLocales() {
        return new HashSet<>(this.locales);
    }

    public Set<Identifier> getLanguageIDs() {
        return new HashSet<>(this.ids);
    }

    public List<Language> getLanguages() {
        return new ArrayList<>(this.languages.values());
    }
}
