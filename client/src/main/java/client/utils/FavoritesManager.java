package client.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import commons.Recipe;

@Singleton
public class FavoritesManager {
    private final AppConfig appConfig;

    @Inject
    public FavoritesManager(AppConfig appConfig) { this.appConfig = appConfig; }
    public boolean isFavorite(Recipe recipe) { return recipe != null && appConfig.isFavorite(recipe.getId()); }
    public boolean isFavorite(Long recipeId) { return appConfig.isFavorite(recipeId); }
    public boolean toggleFavorite(Recipe recipe) { return recipe != null && appConfig.toggleFavorite(recipe.getId()); }
    public boolean toggleFavorite(Long recipeId) { return appConfig.toggleFavorite(recipeId); }
    public void addFavorite(Recipe recipe) { if (recipe != null) appConfig.addFavorite(recipe.getId()); }
    public void removeFavorite(Recipe recipe) { if (recipe != null) appConfig.removeFavorite(recipe.getId()); }
    public void removeFavoriteById(Long recipeId) { appConfig.removeFavorite(recipeId); }
    public void clearAllFavorites() { appConfig.clearFavorites(); }
}