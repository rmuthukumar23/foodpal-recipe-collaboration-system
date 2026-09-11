package client.utils;

import commons.Recipe;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeFilter {
    private boolean favoritesOnly = false;
    private Set<String> requiredLabels = new HashSet<>();
    private String searchTerm = null;
    private String additionalKeyword = null;
    private Integer maxCookingTime = null;
    private Set<String> requiredLanguages = new HashSet<>();
    private FavoritesManager favoritesManager;
    private AppConfig appConfig;

    public RecipeFilter(FavoritesManager favoritesManager, AppConfig appConfig) {
        this.favoritesManager = favoritesManager;
        this.appConfig = appConfig;
        this.requiredLanguages = new HashSet<>(appConfig.getActiveLanguageFilter());
    }

    public RecipeFilter setFavoritesOnly(boolean favoritesOnly) { this.favoritesOnly = favoritesOnly; return this; }
    public RecipeFilter setRequiredLabels(Set<String> labels) { this.requiredLabels = labels != null ? new HashSet<>(labels) : new HashSet<>(); return this; }
    public RecipeFilter addRequiredLabel(String label) { if (label != null && !label.trim().isEmpty()) this.requiredLabels.add(label.trim()); return this; }
    public RecipeFilter removeRequiredLabel(String label) { this.requiredLabels.remove(label); return this; }
    public RecipeFilter setSearchTerm(String searchTerm) { this.searchTerm = searchTerm; return this; }
    public RecipeFilter setAdditionalKeyword(String additionalKeyword) { this.additionalKeyword = additionalKeyword; return this; }
    public RecipeFilter setMaxCookingTime(Integer maxCookingTime) { this.maxCookingTime = maxCookingTime; return this; }
    public RecipeFilter setRequiredLanguages(Set<String> languages) {
        this.requiredLanguages = languages != null ? new HashSet<>(languages) : new HashSet<>();
        appConfig.setActiveLanguageFilter(this.requiredLanguages);
        return this;
    }
    public RecipeFilter addRequiredLanguage(String language) {
        if (language != null && !language.trim().isEmpty()) {
            this.requiredLanguages.add(language.trim());
            appConfig.setActiveLanguageFilter(this.requiredLanguages);
        }
        return this;
    }

    public List<Recipe> apply(List<Recipe> recipes) {
        List<Recipe> filtered = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (recipe != null && matches(recipe)) filtered.add(recipe);
        }
        sortRecipes(filtered);
        return filtered;
    }

    private void sortRecipes(List<Recipe> recipes) {
        recipes.sort((a, b) -> {
            boolean aFav = favoritesManager.isFavorite(a);
            boolean bFav = favoritesManager.isFavorite(b);
            if (aFav && !bFav) return -1;
            if (!aFav && bFav) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
    }

    private boolean matches(Recipe recipe) {
        return matchesFavoriteFilter(recipe) && matchesLabelFilter(recipe) && matchesSearchFilter(recipe)
                && matchesAdditionalKeywordFilter(recipe) && matchesCookingTimeFilter(recipe) && matchesLanguageFilter(recipe);
    }
    private boolean matchesFavoriteFilter(Recipe recipe) { return !favoritesOnly || favoritesManager.isFavorite(recipe); }
    private boolean matchesLabelFilter(Recipe recipe) { return requiredLabels.isEmpty() || recipe.hasAnyLabel(requiredLabels); }
    private boolean matchesSearchFilter(Recipe recipe) { return searchTerm == null || searchTerm.trim().isEmpty() || matchesSearchTerm(recipe, searchTerm); }
    private boolean matchesAdditionalKeywordFilter(Recipe recipe) { return additionalKeyword == null || additionalKeyword.trim().isEmpty() || matchesSearchTerm(recipe, additionalKeyword); }
    private boolean matchesCookingTimeFilter(Recipe recipe) {
        if (maxCookingTime == null || maxCookingTime <= 0) return true;
        Integer cookingTime = recipe.getCookingTimeMinutes();
        return cookingTime == null || cookingTime <= maxCookingTime;
    }
    private boolean matchesLanguageFilter(Recipe recipe) {
        if (requiredLanguages.isEmpty()) return true;
        String recipeLanguage = recipe.getLanguage();
        if (recipeLanguage == null) return false;
        return requiredLanguages.stream().anyMatch(lang -> lang.equalsIgnoreCase(recipeLanguage));
    }
    private boolean matchesSearchTerm(Recipe recipe, String search) {
        String[] terms = search.toLowerCase().trim().split("\\s+");
        String searchableContent = buildSearchableContent(recipe);
        for (String term : terms) if (!searchableContent.contains(term)) return false;
        return true;
    }
    private String buildSearchableContent(Recipe recipe) {
        StringBuilder sb = new StringBuilder();
        if (recipe.getName() != null) sb.append(recipe.getName()).append(" ");
        if (recipe.getIngredients() != null) recipe.getIngredients().forEach(i -> { if (i != null && i.getName() != null) sb.append(i.getName()).append(" "); });
        if (recipe.getPreparationSteps() != null) recipe.getPreparationSteps().forEach(p -> { if (p != null && p.getDescription() != null) sb.append(p.getDescription()).append(" "); });
        if (recipe.getLabels() != null) recipe.getLabels().forEach(label -> { if (label != null && label.getName() != null) sb.append(label.getName()).append(" "); });
        return sb.toString().toLowerCase().trim();
    }

    public void clear() { favoritesOnly = false; requiredLabels.clear(); searchTerm = null; additionalKeyword = null; maxCookingTime = null; }
    public void clearAdvancedFilters() { requiredLabels.clear(); additionalKeyword = null; maxCookingTime = null; }
    public void clearLanguageFilter() { requiredLanguages.clear(); appConfig.clearActiveLanguageFilter(); }
    public boolean isFavoritesOnly() { return favoritesOnly; }
    public Set<String> getRequiredLabels() { return new HashSet<>(requiredLabels); }
    public String getSearchTerm() { return searchTerm; }
    public String getAdditionalKeyword() { return additionalKeyword; }
    public Integer getMaxCookingTime() { return maxCookingTime; }
    public Set<String> getRequiredLanguages() { return new HashSet<>(requiredLanguages); }
    public boolean hasFilters() {
        return favoritesOnly || !requiredLabels.isEmpty() || (searchTerm != null && !searchTerm.trim().isEmpty())
                || (additionalKeyword != null && !additionalKeyword.trim().isEmpty()) || (maxCookingTime != null && maxCookingTime > 0)
                || !requiredLanguages.isEmpty();
    }
    public boolean hasAdvancedFilters() {
        return !requiredLabels.isEmpty() || (additionalKeyword != null && !additionalKeyword.trim().isEmpty())
                || (maxCookingTime != null && maxCookingTime > 0) || !requiredLanguages.isEmpty();
    }
}