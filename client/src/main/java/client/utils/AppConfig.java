package client.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppConfig {
    private String host = "http://localhost:8080/";
    private String language = "en";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final File DEFAULT_CONFIG_FILE = new File("config.json");
    private transient File configFile = DEFAULT_CONFIG_FILE;
    private List<String> languageFilters = new ArrayList<>(List.of("en", "nl"));
    private Set<Long> favoriteRecipeIds = new HashSet<>();
    private Set<String> activeLanguageFilter = new HashSet<>();

    public AppConfig() { }

    public void save() {
        try {
            ensureParentDirExists(configFile);
            mapper.writerWithDefaultPrettyPrinter().writeValue(configFile, this);
        } catch (IOException e) {
            System.err.println("Could not save config: " + e.getMessage());
        }
    }

    public static AppConfig load() { return load(DEFAULT_CONFIG_FILE); }

    public static AppConfig load(File file) {
        File resolved = normalize(file);
        if (!resolved.exists()) {
            AppConfig cfg = new AppConfig();
            cfg.configFile = resolved;
            return cfg;
        }
        try {
            AppConfig cfg = mapper.readValue(resolved, AppConfig.class);
            cfg.configFile = resolved;
            return cfg;
        } catch (IOException e) {
            AppConfig cfg = new AppConfig();
            cfg.configFile = resolved;
            return cfg;
        }
    }

    private static File normalize(File f) {
        if (f == null) return DEFAULT_CONFIG_FILE;
        if (f.isDirectory()) return new File(f, "config.json");
        return f;
    }

    private static void ensureParentDirExists(File f) {
        if (f == null) return;
        File parent = f.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
    }

    public String getHost() { return host; }
    public List<String> getLanguageFilters() { return languageFilters; }
    public Set<Long> getFavoriteRecipeIds() { return favoriteRecipeIds; }
    public boolean isFavorite(Long recipeId) { return recipeId != null && favoriteRecipeIds.contains(recipeId); }
    public void addFavorite(Long recipeId) { if (recipeId != null) { favoriteRecipeIds.add(recipeId); save(); } }
    public void removeFavorite(Long recipeId) { if (recipeId != null) { favoriteRecipeIds.remove(recipeId); save(); } }
    public boolean toggleFavorite(Long recipeId) {
        if (recipeId == null) return false;
        if (favoriteRecipeIds.contains(recipeId)) {
            favoriteRecipeIds.remove(recipeId); save(); return false;
        } else {
            favoriteRecipeIds.add(recipeId); save(); return true;
        }
    }
    public void clearFavorites() { favoriteRecipeIds.clear(); save(); }

    @JsonIgnore
    public void setLanguage(String lang) { this.language = lang; save(); }
    @JsonSetter("language")
    private void setLanguageFromJson(String lang) { this.language = lang; }
    @JsonProperty("language")
    public String getLanguage() { return language; }
    @JsonProperty("activeLanguageFilter")
    public Set<String> getActiveLanguageFilter() { return new HashSet<>(activeLanguageFilter); }
    @JsonIgnore
    public void setActiveLanguageFilter(Set<String> languages) {
        this.activeLanguageFilter = languages != null ? new HashSet<>(languages) : new HashSet<>(); save();
    }
    @JsonSetter("activeLanguageFilter")
    private void setActiveLanguageFilterFromJson(Set<String> languages) {
        this.activeLanguageFilter = languages != null ? new HashSet<>(languages) : new HashSet<>();
    }
    public void clearActiveLanguageFilter() { this.activeLanguageFilter.clear(); save(); }
}