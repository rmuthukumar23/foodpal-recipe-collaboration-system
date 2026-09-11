package client.scenes;

import client.Main;
import client.utils.*;
import com.google.inject.Inject;
import commons.Ingredient;
import commons.PreparationStep;
import commons.Recipe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class HomePageCtrl {

    @FXML private Button addStepButton;
    @FXML private Button editIngredientsButton;
    @FXML private TextArea ingredientsTextArea;
    @FXML private TextArea preparationTextArea;
    @FXML private ListView<Recipe> recipeListView;
    @FXML private Label recipeTitleLabel;
    @FXML private ListView<Ingredient> ingredientsListView;
    @FXML private ListView<PreparationStep> preparationListView;
    @FXML private TextField searchField;
    @FXML private Button makeFavoriteButton;
    @FXML private CheckBox filterFavoritesCheckBox;
    @FXML private Button shoppingListButton;
    @FXML private FlowPane labelsFlowPane;
    @FXML private MenuButton languageMenu;

    private PrimaryCtrl pc;
    private ObservableList<Recipe> allRecipes;
    private ObservableList<Ingredient> currentIngredients;
    private ObservableList<PreparationStep> currentPreparationSteps;
    private Recipe selectedRecipe;
    private ServerUtilsRecipe serverUtilsRecipe;
    private client.utils.RecipeFilter recipeFilter;
    private FavoritesManager favoritesManager;
    private AppConfig appConfig;
    private Long currentRecipeId;

    @Inject
    public HomePageCtrl(PrimaryCtrl p, FavoritesManager favoritesManager, AppConfig appConfig) {
        this.pc = p;
        this.favoritesManager = favoritesManager;
        this.appConfig = appConfig;
        this.allRecipes = FXCollections.observableArrayList();
        this.currentIngredients = FXCollections.observableArrayList();
        this.currentPreparationSteps = FXCollections.observableArrayList();
        this.serverUtilsRecipe = new ServerUtilsRecipe();
        this.recipeFilter = new client.utils.RecipeFilter(favoritesManager, appConfig);
    }

    @FXML
    public void initialize() {
        loadSavedLanguage();
        setupRecipeListView();
        setupIngredientListView();
        setupPreparationListView();
        setupFavoriteButton();
        setupFilterCheckbox();
        setupSearchField();
        checkServerConnection();
        loadRecipesFromServer();
        updateLanguageMenuIcon();
        setupWebSocket();
    }

    private void setupWebSocket() {
        WebSocketClient wsClient = Main.getWebSocketClient();
        if (wsClient == null || !wsClient.isConnected()) return;
        wsClient.registerListUpdate(() -> javafx.application.Platform.runLater(this::handleRefresh));
        wsClient.registerContentUpdate(() -> javafx.application.Platform.runLater(() -> {
            if (selectedRecipe != null) {
                Recipe updated = serverUtilsRecipe.getRecipeById(selectedRecipe.getId());
                if (updated != null) {
                    selectedRecipe = updated;
                    loadRecipeDetails(updated);
                    displayLabels(updated);
                }
            }
        }));
    }

    private void setupSearchField() {
        if (searchField != null) {
            searchField.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    searchField.clear();
                    applyFilters();
                }
            });
        }
    }

    private void loadSavedLanguage() {
        if (languageMenu == null) return;
        updateLanguageMenuIcon();
    }

    private void updateLanguageMenuIcon() {
        if (languageMenu == null) return;
        String currentLang = TranslationManager.getTranslationManager().getCurrentLanguage();
        String flagFile = getFlagFileName(currentLang);
        try {
            Image flagImage = new Image(getClass().getResourceAsStream("/client/images/flags/" + flagFile));
            ImageView imageView = new ImageView(flagImage);
            imageView.setFitWidth(28);
            imageView.setFitHeight(20);
            imageView.setPreserveRatio(true);
            languageMenu.setGraphic(imageView);
            languageMenu.setText("");
        } catch (Exception e) {
            languageMenu.setText(TranslationManager.getTranslationManager().getCurrentFlag());
            languageMenu.setGraphic(null);
        }
    }

    private String getFlagFileName(String langCode) {
        switch (langCode) {
            case "nl": return "nl.png";
            case "de": return "de.png";
            default: return "gb.png";
        }
    }

    private void setupFavoriteButton() {
        if (makeFavoriteButton != null) {
            makeFavoriteButton.setOnAction(e -> onToggleFavorite());
            updateFavoriteButton();
        }
    }

    @FXML
    public void onOpenShoppingList() { pc.showShoppingList(); }

    private void setupFilterCheckbox() {
        if (filterFavoritesCheckBox != null) filterFavoritesCheckBox.setOnAction(e -> applyFilters());
    }

    @FXML
    public void onToggleFavorite() {
        if (selectedRecipe == null) {
            showErrorDialog("No Selection", "Please select a recipe to favorite.");
            return;
        }
        favoritesManager.toggleFavorite(selectedRecipe);
        updateFavoriteButton();
        sortRecipesByFavorite();
        applyFilters();
        recipeListView.getSelectionModel().select(selectedRecipe);
        recipeListView.refresh();
    }

    private void applyFilters() {
        String searchTerm = searchField != null ? searchField.getText() : null;
        boolean favoritesOnly = filterFavoritesCheckBox != null && filterFavoritesCheckBox.isSelected();
        recipeFilter.setFavoritesOnly(favoritesOnly);
        recipeFilter.setSearchTerm(searchTerm);
        List<Recipe> filtered = recipeFilter.apply(new ArrayList<>(allRecipes));
        sortFilteredRecipes(filtered);
        recipeListView.setItems(FXCollections.observableArrayList(filtered));
        if (!recipeListView.getItems().isEmpty()) recipeListView.getSelectionModel().selectFirst();
        else clearRecipeDisplay();
    }

    private void sortFilteredRecipes(List<Recipe> recipes) {
        recipes.sort((a, b) -> {
            boolean aFav = favoritesManager.isFavorite(a);
            boolean bFav = favoritesManager.isFavorite(b);
            if (aFav && !bFav) return -1;
            if (!aFav && bFav) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
    }

    private void clearRecipeDisplay() {
        recipeTitleLabel.setText("");
        currentIngredients.clear();
        currentPreparationSteps.clear();
        clearLabelsDisplay();
    }

    private void updateFavoriteButton() {
        if (makeFavoriteButton == null) return;
        String buttonStyle = "-fx-background-color: #3B4CCA; -fx-border-color: #3B4CCA; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #FFFFFF; -fx-font-size: 18px;";
        if (selectedRecipe != null && favoritesManager.isFavorite(selectedRecipe)) makeFavoriteButton.setText("★");
        else makeFavoriteButton.setText("☆");
        makeFavoriteButton.setStyle(buttonStyle);
    }

    private void setupRecipeListView() {
        if (recipeListView == null) return;
        recipeListView.setItems(allRecipes);
        recipeListView.setCellFactory(lv -> createRecipeCell());
        recipeListView.getSelectionModel().selectedItemProperty().addListener((obs, oldRecipe, newRecipe) -> onRecipeSelected(newRecipe));
    }

    private ListCell<Recipe> createRecipeCell() {
        return new ListCell<Recipe>() {
            private final Label recipeName = new Label();
            private final Button deleteButton = new Button("✕");
            private final Region emptySpace = new Region();
            private final HBox format = new HBox(8, recipeName, emptySpace, deleteButton);
            {
                HBox.setHgrow(emptySpace, Priority.ALWAYS);
                deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-size: 14px; fx-font-weight: bold; -fx-cursor: hand;");
                deleteButton.setTooltip(new Tooltip("Delete recipe"));
                deleteButton.setOnAction(e -> handleRecipeDelete(getItem()));
                recipeName.setOnMouseClicked(e -> handleRecipeDoubleClick(e, getItem()));
                recipeName.setStyle("-fx-cursor: hand;");
                recipeName.setTooltip(new Tooltip("Double-click to favorite/unfavorite"));
            }
            @Override protected void updateItem(Recipe item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    boolean isFav = favoritesManager.isFavorite(item);
                    String caloriesText = String.format("%.2f kcal/100g", item.getCaloricDensity());
                    recipeName.setText((isFav ? item.getName() + " ★" : item.getName()) + " - " + caloriesText);
                    setGraphic(format);
                }
            }
        };
    }

    private void handleRecipeDelete(Recipe recipe) {
        if (recipe == null) return;
        boolean confirmed = createConfirmationAlert("Remove Recipe", "Do you want to remove '" + recipe.getName() + "' from the list of recipes?", "Remove");
        if (!confirmed) return;
        boolean deleted = serverUtilsRecipe.deleteRecipe(recipe.getId());
        if (!deleted) showErrorDialog("Delete failed", "Recipe could not be deleted on the server.");
        favoritesManager.removeFavorite(recipe);
        allRecipes.remove(recipe);
        if (!allRecipes.isEmpty()) recipeListView.getSelectionModel().selectFirst();
        else clearRecipeDisplay();
    }

    private void handleRecipeDoubleClick(javafx.scene.input.MouseEvent e, Recipe recipe) {
        if (e.getClickCount() == 2 && recipe != null) {
            favoritesManager.toggleFavorite(recipe);
            sortRecipesByFavorite();
            recipeListView.refresh();
            e.consume();
        }
    }

    private void sortRecipesByFavorite() {
        List<Recipe> sorted = new ArrayList<>(allRecipes);
        sortFilteredRecipes(sorted);
        allRecipes.setAll(sorted);
        recipeListView.refresh();
    }

    private void setupIngredientListView() {
        if (ingredientsListView == null) return;
        ingredientsListView.setItems(currentIngredients);
        ingredientsListView.setCellFactory(lv -> createIngredientCell());
    }

    private ListCell<Ingredient> createIngredientCell() {
        return new ListCell<>() {
            private final Label ingredientName = new Label();
            private final Button deleteButton = new Button("✕");
            private final Region emptySpace = new Region();
            private final HBox format = new HBox(8, ingredientName, emptySpace, deleteButton);
            {
                HBox.setHgrow(emptySpace, Priority.ALWAYS);
                deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-size: 16px; fx-font-weight: bold; -fx-cursor: hand;");
                deleteButton.setTooltip(new Tooltip("Delete Ingredient"));
                deleteButton.setOnAction(e -> handleIngredientDelete(getItem()));
            }
            @Override protected void updateItem(Ingredient item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (item.getAmount() != null && !item.getAmount().trim().isEmpty()) ingredientName.setText(item.getName() + " " + item.getAmount());
                    else ingredientName.setText(item.getName());
                    setGraphic(format);
                }
            }
        };
    }

    private void handleIngredientDelete(Ingredient ingredient) {
        if (ingredient == null || selectedRecipe == null) return;
        boolean confirmed = createConfirmationAlert("Remove Ingredient", "Do you want to remove '" + ingredient.getName() + "' from this recipe?", "Remove");
        if (!confirmed) return;
        currentIngredients.remove(ingredient);
        Recipe updatedRecipe = serverUtilsRecipe.updateIngredients(selectedRecipe.getId(), new HashSet<>(currentIngredients));
        if (updatedRecipe != null) {
            selectedRecipe = updatedRecipe;
            updateRecipeInList(updatedRecipe);
            recipeListView.refresh();
            loadRecipeDetails(updatedRecipe);
        }
    }

    private void updateRecipeInList(Recipe recipe) {
        for (int i = 0; i < allRecipes.size(); i++) {
            if (allRecipes.get(i).getId().equals(recipe.getId())) {
                allRecipes.set(i, recipe);
                break;
            }
        }
    }

    private void setupPreparationListView() {
        if (preparationListView == null) return;
        preparationListView.setItems(currentPreparationSteps);
        preparationListView.setCellFactory(lv -> createPreparationCell());
    }

    private ListCell<PreparationStep> createPreparationCell() {
        return new ListCell<>() {
            private final Label stepName = new Label();
            private final Button deleteButton = new Button("✕");
            private final Region emptySpace = new Region();
            private final HBox format = new HBox(8, stepName, emptySpace, deleteButton);
            {
                HBox.setHgrow(emptySpace, Priority.ALWAYS);
                deleteButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-size: 16px; fx-font-weight: bold; -fx-cursor: hand;");
                deleteButton.setTooltip(new Tooltip("Delete Preparation Step"));
                deleteButton.setOnAction(e -> handlePreparationStepDelete(getItem()));
            }
            @Override protected void updateItem(PreparationStep item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    stepName.setText(item.getDescription());
                    setGraphic(format);
                }
            }
        };
    }

    private void handlePreparationStepDelete(PreparationStep preparationStep) {
        if (preparationStep == null || selectedRecipe == null) return;
        boolean confirmed = createConfirmationAlert("Remove Preparation Step", "Do you want to remove '" + preparationStep.getDescription() + "' from this recipe?", "Remove");
        if (!confirmed) return;
        currentPreparationSteps.remove(preparationStep);
        Recipe updatedRecipe = serverUtilsRecipe.updatePreparationSteps(selectedRecipe.getId(), new ArrayList<>(currentPreparationSteps));
        if (updatedRecipe != null) {
            selectedRecipe = updatedRecipe;
            updateRecipeInList(updatedRecipe);
            recipeListView.refresh();
            loadRecipeDetails(updatedRecipe);
        }
    }

    private void onRecipeSelected(Recipe recipe) {
        WebSocketClient wsClient = Main.getWebSocketClient();
        this.selectedRecipe = recipe;
        if (recipe == null) {
            if (recipeTitleLabel != null) recipeTitleLabel.setText("");
            currentPreparationSteps.clear();
            currentIngredients.clear();
            clearLabelsDisplay();
            updateTextAreas();
            updateFavoriteButton();
            return;
        }
        currentRecipeId = recipe.getId();
        if (wsClient != null && wsClient.isConnected()) wsClient.subscribeToRecipe(currentRecipeId);
        if (recipeTitleLabel != null) recipeTitleLabel.setText(recipe.getName());
        loadRecipeDetails(recipe);
        displayLabels(recipe);
        updateFavoriteButton();
    }

    void loadRecipeDetails(Recipe recipe) {
        if (recipe == null) return;
        currentIngredients.clear();
        currentPreparationSteps.clear();
        if (recipe.getIngredients() != null) {
            Comparator<Ingredient> ingredientCmp = Comparator.nullsLast(Comparator.comparing(Ingredient::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
            currentIngredients.setAll(recipe.getIngredients().stream().sorted(ingredientCmp).toList());
        }
        if (recipe.getPreparationSteps() != null) currentPreparationSteps.addAll(recipe.getPreparationSteps());
        updateTextAreas();
    }

    private void displayLabels(Recipe recipe) {
        if (labelsFlowPane == null) return;
        labelsFlowPane.getChildren().clear();
        if (recipe == null || recipe.getLabels() == null || recipe.getLabels().isEmpty()) return;
        for (commons.Label label : recipe.getLabels()) {
            if (label != null && label.getName() != null) labelsFlowPane.getChildren().add(createLabelChip(label));
        }
    }

    private Label createLabelChip(commons.Label label) {
        Label chip = new Label(label.getName());
        if (label.getCategory() == commons.Label.LabelCategory.LANGUAGE) {
            chip.setStyle("-fx-background-color: #FFFFFF; -fx-text-fill: #000000; -fx-background-radius: 12; -fx-border-radius: 12; -fx-border-color: #3B4CCA; -fx-border-width: 2; -fx-padding: 4 12 4 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            chip.setStyle("-fx-background-color: #3B4CCA; -fx-text-fill: #FFFFFF; -fx-background-radius: 12; -fx-border-radius: 12; -fx-padding: 4 12 4 12; -fx-font-size: 11px;");
        }
        chip.setPadding(new Insets(4, 12, 4, 12));
        return chip;
    }

    private void clearLabelsDisplay() { if (labelsFlowPane != null) labelsFlowPane.getChildren().clear(); }

    private void updateTextAreas() {
        if (ingredientsTextArea != null) {
            StringBuilder sb = new StringBuilder();
            for (Ingredient ingredient : currentIngredients) {
                if (ingredient != null && ingredient.getName() != null) {
                    String amount = ingredient.getAmount() != null ? ingredient.getAmount() : "";
                    sb.append(ingredient.getName()).append(" - ").append(amount).append("\n");
                }
            }
            ingredientsTextArea.setText(sb.toString());
        }
        if (preparationTextArea != null) {
            StringBuilder sb = new StringBuilder();
            for (PreparationStep preparationStep : currentPreparationSteps) {
                if (preparationStep != null && preparationStep.getDescription() != null) sb.append(preparationStep.getDescription()).append("\n");
            }
            preparationTextArea.setText(sb.toString());
        }
    }

    public void loadRecipesFromServer() {
        try {
            if (!serverUtilsRecipe.isServerAvailable()) {
                showErrorDialog("Server Error", "Server is not available. Please start the server.");
                return;
            }
            var recipes = serverUtilsRecipe.getRecipes();
            allRecipes.setAll(recipes);
            sortRecipesByFavorite();
            if (allRecipes.isEmpty()) showInformationDialog("No Recipes", "No recipes found on server.\nAdd recipes using the 'Add Recipe' button.");
            else recipeListView.getSelectionModel().selectFirst();
        } catch (Exception e) {
            showErrorDialog("Server Error", "Failed to load recipes: " + e.getMessage());
        }
    }

    @FXML
    public void handleRefresh() {
        Recipe currentlySelected = recipeListView.getSelectionModel().getSelectedItem();
        Map<Long, String> favoritedRecipes = allRecipes.stream().filter(favoritesManager::isFavorite).collect(Collectors.toMap(Recipe::getId, Recipe::getName));
        var recipes = serverUtilsRecipe.getRecipes();
        allRecipes.setAll(recipes);
        sortRecipesByFavorite();
        Set<Long> newRecipeIds = allRecipes.stream().map(Recipe::getId).collect(Collectors.toSet());
        List<String> deletedFavorites = new ArrayList<>();
        for (Map.Entry<Long, String> entry : favoritedRecipes.entrySet()) {
            if (!newRecipeIds.contains(entry.getKey())) {
                deletedFavorites.add(entry.getValue());
                favoritesManager.removeFavoriteById(entry.getKey());
            }
        }
        if (!deletedFavorites.isEmpty()) showDeletedFavoritesWarning(deletedFavorites);
        if (searchField != null) searchField.clear();
        if (filterFavoritesCheckBox != null) filterFavoritesCheckBox.setSelected(false);
        recipeFilter.clear();
        applyFilters();
        reselectRecipe(currentlySelected);
    }

    private void showDeletedFavoritesWarning(List<String> deletedFavorites) {
        StringBuilder message = new StringBuilder();
        message.append("The following favorite recipe");
        message.append(deletedFavorites.size() > 1 ? "s have" : " has");
        message.append(" been deleted:\n\n");
        for (String recipeName : deletedFavorites) message.append("• ").append(recipeName).append("\n");
        message.append("\nRest in pieces...");
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Favorite Recipe" + (deletedFavorites.size() > 1 ? "s" : "") + " Deleted");
        alert.setHeaderText("Oh no...");
        alert.setContentText(message.toString());
        alert.showAndWait();
    }

    private void reselectRecipe(Recipe targetRecipe) {
        if (targetRecipe != null) {
            for (Recipe recipe : allRecipes) {
                if (recipe.getId().equals(targetRecipe.getId())) {
                    recipeListView.getSelectionModel().select(recipe);
                    break;
                }
            }
        }
    }

    @FXML public void handleSearch() { applyFilters(); }
    public void onAddRecipe(ActionEvent actionEvent) { pc.showAddRecipe(); }

    public void onRemoveRecipe(ActionEvent actionEvent) {
        Recipe selected = recipeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("No Selection", "Please select a recipe to remove.");
            return;
        }
        handleRecipeDelete(selected);
    }

    @FXML
    public void onCloneRecipe() {
        Recipe selected = recipeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("Nothing selected", "Please select a recipe to clone.");
            return;
        }
        String newName = selected.getName() + " - Copy";
        boolean nameExists = allRecipes.stream().anyMatch(r -> r.getName().equalsIgnoreCase(newName));
        if (nameExists) {
            showErrorDialog("Clone Error", "A copy of this recipe already exists ('" + newName + "').\nPlease rename the existing copy before cloning again.");
            return;
        }
        try {
            Recipe cloned = serverUtilsRecipe.cloneRecipe(selected.getId(), newName);
            if (cloned != null) {
                allRecipes.add(cloned);
                sortRecipesByFavorite();
                recipeListView.getSelectionModel().select(cloned);
                recipeListView.scrollTo(cloned);
                onRecipeSelected(cloned);
            } else showErrorDialog("Clone Failed", "The server was unable to clone the recipe.");
        } catch (Exception e) {
            showErrorDialog("Communication Error", "Failed to reach the server for cloning.");
        }
    }

    @FXML public void onOpenTimer() { pc.showTimer(); }

    public void onEditIngredients(ActionEvent actionEvent) {
        Recipe selected = recipeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("No Recipe Selected", "Please select a recipe to edit.");
            return;
        }
        openEditRecipeDialog(selected, EditRecipeCtrl::selectIngredientsTab);
    }

    public void onAdvancedSearch(ActionEvent actionEvent) { pc.showAdvancedSearch(); }

    @FXML
    public void onAddStep(ActionEvent actionEvent) {
        Recipe selected = recipeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("No Recipe Selected", "Please select a recipe to edit.");
            return;
        }
        openEditRecipeDialog(selected, EditRecipeCtrl::selectPreparationTab);
    }

    @FXML
    public void onEditName(ActionEvent actionEvent) {
        Recipe selected = recipeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("No Recipe Selected", "Please select a recipe to edit.");
            return;
        }
        openEditRecipeDialog(selected, EditRecipeCtrl::selectNameTab);
    }

    private void openEditRecipeDialog(Recipe recipe, java.util.function.Consumer<EditRecipeCtrl> tabSelector) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/scenes/EditRecipe.fxml"));
            Parent parent = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Edit Recipe: " + recipe.getName());
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(editIngredientsButton.getScene().getWindow());
            stage.setScene(new Scene(parent));
            EditRecipeCtrl recipeCtrl = loader.getController();
            recipeCtrl.setDialogStage(stage);
            recipeCtrl.setRecipeToEdit(recipe);
            tabSelector.accept(recipeCtrl);
            stage.showAndWait();
            handleRefresh();
        } catch (IOException exception) {
            System.out.println("Error while loading the Edit Recipe page: " + exception.getMessage());
        }
    }

    @FXML
    public void onPrintRecipe() {
        Recipe selected = recipeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("No Selection", "Please select a recipe to print.");
            return;
        }
        pc.showPrintRecipe(selected);
    }

    @FXML
    public void onAddToShoppingList() {
        Recipe selected = recipeListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorDialog("No Selection", "Please select a recipe to add to shopping list.");
            return;
        }
        pc.showShoppingListConfirmation(selected);
    }

    @FXML public void onEnglish() { TranslationManager.getTranslationManager().setLanguage("en"); pc.reloadHomePageWithLanguage(); }
    @FXML public void onDutch() { TranslationManager.getTranslationManager().setLanguage("nl"); pc.reloadHomePageWithLanguage(); }
    @FXML public void onGerman() { TranslationManager.getTranslationManager().setLanguage("de"); pc.reloadHomePageWithLanguage(); }

    private void checkServerConnection() {
        if (!serverUtilsRecipe.isServerAvailable()) showErrorDialog("Server Not Available", "Please start the server first.");
    }

    private boolean createConfirmationAlert(String title, String message, String confirmText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirmButton, cancelButton);
        return alert.showAndWait().orElse(cancelButton) == confirmButton;
    }

    private void showInformationDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public ObservableList<Ingredient> getCurrentIngredients() { return currentIngredients; }
    public ObservableList<PreparationStep> getCurrentPreparationSteps() { return currentPreparationSteps; }
    public Button getEditIngredientsButton() { return editIngredientsButton; }
    public void setEditIngredientsButton(Button editIngredientsButton) { this.editIngredientsButton = editIngredientsButton; }
    public TextArea getIngredientsTextArea() { return ingredientsTextArea; }
    public void setIngredientsTextArea(TextArea ingredientsTextArea) { this.ingredientsTextArea = ingredientsTextArea; }
    public TextArea getPreparationTextArea() { return preparationTextArea; }
    public void setPreparationTextArea(TextArea preparationTextArea) { this.preparationTextArea = preparationTextArea; }
    @FXML public void onShowNutritionalValue() { pc.showNutritionalValue(); }
    public ObservableList<Recipe> getAllRecipes() { return allRecipes; }
    public client.utils.RecipeFilter getRecipeFilter() { return recipeFilter; }
    public void applyCurrentFilters() { applyFilters(); }
    public boolean hasAdvancedFilters() { return recipeFilter.hasAdvancedFilters(); }
    public void clearAllFilters() {
        if (searchField != null) searchField.clear();
        if (filterFavoritesCheckBox != null) filterFavoritesCheckBox.setSelected(false);
        recipeFilter.clear();
        applyFilters();
    }
}