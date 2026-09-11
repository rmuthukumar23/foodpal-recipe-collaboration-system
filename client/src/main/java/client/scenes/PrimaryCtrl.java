package client.scenes;

import client.Main;
import commons.Recipe;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.IOException;
import java.util.ResourceBundle;

public class PrimaryCtrl {

    private Stage primaryStage;
    private Scene homeScene;
    private Scene addRecipeScene;
    private Scene confirmDeletionScene;
    private Scene timerScene;
    private Scene editRecipeScene;
    private Scene advancedSearchScene;
    private Scene printRecipeScene;
    private Scene shoppingListScene;
    private Scene shoppingListConfirmationScene;
    private Scene nutritionalValueScene;

    private PrintRecipeCtrl printRecipeCtrl;
    private ShoppingListConfirmationCtrl shoppingListConfirmationCtrl;
    private HomePageCtrl homePageCtrl;
    private NutritionalValueCtrl nutritionalValueCtrl;

    public void initialize(Stage primaryStage, ScenePackage scenePackage) {
        this.primaryStage = primaryStage;
        this.homeScene = new Scene(scenePackage.getHome().getValue());
        this.addRecipeScene = new Scene(scenePackage.getAddRecipe().getValue());
        this.confirmDeletionScene = new Scene(scenePackage.getConfirmDeleteRecipe().getValue());
        this.advancedSearchScene = new Scene(scenePackage.getAdvancedSearch().getValue());
        this.timerScene = new Scene(scenePackage.getTimer().getValue());
        this.editRecipeScene = new Scene(scenePackage.getEditRecipe().getValue());
        this.printRecipeScene = new Scene(scenePackage.getPrintRecipe().getValue());
        this.shoppingListScene = new Scene(scenePackage.getShoppingList().getValue());
        this.shoppingListConfirmationScene = new Scene(scenePackage.getShoppingListConfirmation().getValue());
        this.nutritionalValueScene = new Scene(scenePackage.getNutritionalValue().getValue());
        this.printRecipeCtrl = scenePackage.getPrintRecipe().getKey();
        this.shoppingListConfirmationCtrl = scenePackage.getShoppingListConfirmation().getKey();
        this.homePageCtrl = scenePackage.getHome().getKey();
        this.nutritionalValueCtrl = scenePackage.getNutritionalValue().getKey();
        scenePackage.getTimer().getKey().setPrimaryCtrl(this);
        showHome();
        primaryStage.show();
    }

    public static class ScenePackage {
        private Pair<HomePageCtrl, Parent> home;
        private Pair<AddRecipeCtrl, Parent> addRecipe;
        private Pair<ConfirmRecipeDeletionCtrl, Parent> confirmDeleteRecipe;
        private Pair<AdvancedSearchCtrl, Parent> advancedSearch;
        private Pair<TimerCtrl, Parent> timer;
        private Pair<EditRecipeCtrl, Parent> editRecipe;
        private Pair<PrintRecipeCtrl, Parent> printRecipe;
        private Pair<ShoppingListCtrl, Parent> shoppingList;
        private Pair<ShoppingListConfirmationCtrl, Parent> shoppingListConfirmation;
        private Pair<NutritionalValueCtrl, Parent> nutritionalValue;

        public Pair<HomePageCtrl, Parent> getHome() { return home; }
        public Pair<AddRecipeCtrl, Parent> getAddRecipe() { return addRecipe; }
        public Pair<ConfirmRecipeDeletionCtrl, Parent> getConfirmDeleteRecipe() { return confirmDeleteRecipe; }
        public Pair<AdvancedSearchCtrl, Parent> getAdvancedSearch() { return advancedSearch; }
        public Pair<TimerCtrl, Parent> getTimer() { return timer; }
        public Pair<EditRecipeCtrl, Parent> getEditRecipe() { return editRecipe; }
        public Pair<PrintRecipeCtrl, Parent> getPrintRecipe() { return printRecipe; }
        public Pair<ShoppingListCtrl, Parent> getShoppingList() { return shoppingList; }
        public Pair<ShoppingListConfirmationCtrl, Parent> getShoppingListConfirmation() { return shoppingListConfirmation; }
        public Pair<NutritionalValueCtrl, Parent> getNutritionalValue() { return nutritionalValue; }
        public void setShoppingListConfirmation(Pair<ShoppingListConfirmationCtrl, Parent> v) { this.shoppingListConfirmation = v; }
        public void setShoppingList(Pair<ShoppingListCtrl, Parent> v) { this.shoppingList = v; }
        public void setPrintRecipe(Pair<PrintRecipeCtrl, Parent> v) { this.printRecipe = v; }
        public void setEditRecipe(Pair<EditRecipeCtrl, Parent> v) { this.editRecipe = v; }
        public void setTimer(Pair<TimerCtrl, Parent> v) { this.timer = v; }
        public void setAdvancedSearch(Pair<AdvancedSearchCtrl, Parent> v) { this.advancedSearch = v; }
        public void setConfirmDeleteRecipe(Pair<ConfirmRecipeDeletionCtrl, Parent> v) { this.confirmDeleteRecipe = v; }
        public void setAddRecipe(Pair<AddRecipeCtrl, Parent> v) { this.addRecipe = v; }
        public void setHome(Pair<HomePageCtrl, Parent> v) { this.home = v; }
        public void setNutritionalValue(Pair<NutritionalValueCtrl, Parent> v) { this.nutritionalValue = v; }
    }

    public void reloadHomePageWithLanguage() {
        ResourceBundle bundle = client.utils.TranslationManager.getTranslationManager().getBundle();
        try {
            var loader = new javafx.fxml.FXMLLoader(getClass().getResource("/client/scenes/HomePage.fxml"), bundle);
            loader.setControllerFactory(type -> Main.getInjector().getInstance(type));
            javafx.scene.Parent root = loader.load();
            client.scenes.HomePageCtrl newController = loader.getController();
            if (newController != null) newController.loadRecipesFromServer();
            this.homeScene = new javafx.scene.Scene(root);
            this.homePageCtrl = newController;
            primaryStage.setTitle("HomePage");
            primaryStage.setScene(homeScene);
        } catch (java.io.IOException e) {
            System.err.println("Failed to reload HomePage with new language: " + e.getMessage());
        }
    }

    public void showHome() {
        primaryStage.setTitle("HomePage");
        primaryStage.setScene(homeScene);
        if (homePageCtrl != null) homePageCtrl.handleRefresh();
    }

    public void showTimer() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/scenes/Timer.fxml"));
            Parent root = loader.load();
            TimerCtrl timerCtrl = loader.getController();
            Stage timerStage = new Stage();
            timerStage.setTitle("Cooking Timer");
            timerStage.setScene(new Scene(root));
            timerStage.setResizable(false);
            timerCtrl.setTimerStage(timerStage);
            timerCtrl.setPrimaryCtrl(this);
            timerStage.initModality(Modality.NONE);
            timerStage.show();
        } catch (IOException e) {
            System.err.println("Failed to load Timer window: " + e.getMessage());
        }
    }

    public void showAddRecipe() { primaryStage.setTitle("AddRecipe"); primaryStage.setScene(addRecipeScene); }
    public void showConfirmDeletion() { primaryStage.setTitle("Confirm Deletion"); primaryStage.setScene(confirmDeletionScene); }
    public void showEditRecipe() { primaryStage.setTitle("EditRecipe"); primaryStage.setScene(editRecipeScene); }
    public void showAdvancedSearch() { primaryStage.setTitle("Advanced Search"); primaryStage.setScene(advancedSearchScene); }
    public void showPrintRecipe(Recipe recipe) { printRecipeCtrl.setRecipe(recipe); primaryStage.setTitle("Print Recipe"); primaryStage.setScene(printRecipeScene); }
    public void showShoppingList() { primaryStage.setTitle("Shopping List"); primaryStage.setScene(shoppingListScene); }
    public void showShoppingListConfirmation(Recipe recipe) { shoppingListConfirmationCtrl.setRecipe(recipe); primaryStage.setTitle("Add to Shopping List"); primaryStage.setScene(shoppingListConfirmationScene); }
    public void showNutritionalValue() {
        primaryStage.setTitle("Nutritional Value");
        primaryStage.setScene(nutritionalValueScene);
        if (nutritionalValueCtrl != null) nutritionalValueCtrl.onShow();
    }
}