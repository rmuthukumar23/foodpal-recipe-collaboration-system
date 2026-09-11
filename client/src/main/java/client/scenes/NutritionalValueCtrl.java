package client.scenes;

import client.utils.ServerUtilsGlobalIngredient;
import com.google.inject.Inject;
import commons.GlobalIngredient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;

public class NutritionalValueCtrl {
    private final PrimaryCtrl pc;
    private final ServerUtilsGlobalIngredient serverMethods;
    @FXML private TextField searchField;
    @FXML private ListView<GlobalIngredient> ingredientsList;
    @FXML private TextField nameField;
    @FXML private TextField caloriesField;
    @FXML private TextField proteinField;
    @FXML private TextField carbsField;
    @FXML private TextField fatField;
    @FXML private TextField recipeCountField;

    private ObservableList<GlobalIngredient> globalIngredientsList;
    private boolean isUpdatingFields = false;

    @Inject
    public NutritionalValueCtrl(PrimaryCtrl pc, ServerUtilsGlobalIngredient serverMethods) {
        this.pc = pc;
        this.serverMethods = serverMethods;
    }

    public void initialize() {
        globalIngredientsList = FXCollections.observableArrayList();
        ingredientsList.setItems(globalIngredientsList);
        refresh();
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> applyFilter(newValue));
        }
        ingredientsList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue != null) saveCurrentIngredient(oldValue);
            showDetails(newValue);
        });
        addAutoSaveListener(nameField);
        addAutoSaveListener(caloriesField);
        addAutoSaveListener(proteinField);
        addAutoSaveListener(carbsField);
        addAutoSaveListener(fatField);
    }

    private void addAutoSaveListener(TextField field) {
        field.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) saveCurrentIngredient(ingredientsList.getSelectionModel().getSelectedItem());
        });
    }

    private double extractNumber(String text) {
        if (text == null || text.isBlank()) return 0.0;
        String cleaned = text.trim().toLowerCase().replaceAll("kcal.*", "").replaceAll("g.*", "").replaceAll("[^0-9.]", "").trim();
        if (cleaned.isEmpty()) return 0.0;
        try { return Double.parseDouble(cleaned); }
        catch (NumberFormatException e) { return 0.0; }
    }

    private int safeParseInt(String text) { return (int) Math.round(extractNumber(text)); }
    private double safeParseDouble(String text) { return extractNumber(text); }

    private void saveCurrentIngredient(GlobalIngredient ingredient) {
        if (isUpdatingFields || ingredient == null) return;
        try {
            String newName = nameField.getText().trim();
            if (newName.isEmpty()) {
                showError("Ingredient name cannot be empty.", "Invalid Input");
                showDetails(ingredient);
                return;
            }
            ingredient.setName(newName);
            ingredient.setCalories(safeParseInt(caloriesField.getText()));
            ingredient.setProtein(safeParseDouble(proteinField.getText()));
            ingredient.setCarbs(safeParseDouble(carbsField.getText()));
            ingredient.setFat(safeParseDouble(fatField.getText()));
            serverMethods.updateGlobalIngredient(ingredient);
            isUpdatingFields = true;
            showDetails(ingredient);
            isUpdatingFields = false;
            ingredientsList.refresh();
        } catch (Exception e) {
            showError("Failed to save changes: " + e.getMessage(), "Server Error");
            showDetails(ingredient);
        }
    }

    @FXML public void onRefresh() { refresh(); }
    public void onShow() { refresh(); }
    public void showRecipes() { pc.showHome(); }

    private void refresh() {
        try {
            List<GlobalIngredient> latestServerList = serverMethods.getGlobalIngredients();
            globalIngredientsList.setAll(latestServerList);
            selectFirstIfAvailable();
        } catch (Exception e) {
            showError("Failed to update the ListView from server", e.getMessage());
        }
    }

    private void selectFirstIfAvailable() {
        if (ingredientsList.getItems() != null && !ingredientsList.getItems().isEmpty()) {
            ingredientsList.getSelectionModel().selectFirst();
            ingredientsList.scrollTo(0);
        } else showDetails(null);
    }

    private void applyFilter(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            ingredientsList.setItems(globalIngredientsList);
            return;
        }
        String normalisedUserInput = userInput.toLowerCase().trim();
        ObservableList<GlobalIngredient> filteredList = FXCollections.observableArrayList();
        for (GlobalIngredient globalIngredient : globalIngredientsList) {
            String name = globalIngredient.getName();
            name = name == null ? "" : name.toLowerCase();
            if (name.contains(normalisedUserInput)) filteredList.add(globalIngredient);
        }
        ingredientsList.setItems(filteredList);
    }

    private void showDetails(GlobalIngredient ingredient) {
        isUpdatingFields = true;
        if (ingredient != null) {
            nameField.setText(ingredient.getName());
            double recommended = (ingredient.getCarbs() * 4) + (ingredient.getProtein() * 4) + (ingredient.getFat() * 9);
            caloriesField.setText(ingredient.getCalories() + " kcal / calculated " + (int)Math.round(recommended) + " kcal");
            proteinField.setText(ingredient.getProtein() + " g");
            carbsField.setText(ingredient.getCarbs() + " g");
            fatField.setText(ingredient.getFat() + " g");
            try { recipeCountField.setText(String.valueOf(serverMethods.getGlobalIngredientUsageCount(ingredient.getId()))); }
            catch (Exception e) { recipeCountField.setText("N/A"); }
        } else {
            nameField.setText(""); caloriesField.setText(""); proteinField.setText(""); carbsField.setText(""); fatField.setText(""); recipeCountField.setText("");
        }
        isUpdatingFields = false;
    }

    @FXML public void handleSearch() { applyFilter(searchField.getText()); }

    private void showError(String message, String title) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message); alert.showAndWait();
    }

    @FXML
    public void onDeleteGlobalIngredient() {
        GlobalIngredient selectedIngredient = ingredientsList.getSelectionModel().getSelectedItem();
        if (selectedIngredient == null) {
            showError("Select an ingredient first.", "No selection");
            return;
        }
        boolean used;
        try { used = serverMethods.isGlobalIngredientUsed(selectedIngredient.getId()); }
        catch (Exception e) { showError("Failed to check whether ingredient is used.", e.getMessage()); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        if (used) {
            confirm.setTitle("Ingredient in use!");
            confirm.setHeaderText("This ingredient is currently being used in at least one recipe.");
            confirm.setContentText("If you click OK, it will be removed from all recipes and deleted globally.");
        } else {
            confirm.setTitle("Delete ingredient");
            confirm.setHeaderText(null);
            confirm.setContentText("Do you want to delete \"" + selectedIngredient.getName() + "\" ?");
        }
        if (confirm.showAndWait().filter(b -> b == ButtonType.OK).isEmpty()) return;
        try { serverMethods.deleteGlobalIngredient(selectedIngredient.getId()); }
        catch (Exception e) { showError("Failed to delete ingredient.", e.getMessage()); return; }
        refresh();
    }

    @FXML
    public void onAddGlobalIngredient() {
        try {
            GlobalIngredient newIngredient = new GlobalIngredient("New Ingredient", 0, 0.0, 0.0, 0.0);
            serverMethods.addGlobalIngredient(newIngredient);
            refresh();
            for (GlobalIngredient i : globalIngredientsList) {
                if ("New Ingredient".equals(i.getName())) {
                    ingredientsList.getSelectionModel().select(i);
                    break;
                }
            }
        } catch (Exception e) {
            showError("Failed to add ingredient: " + e.getMessage(), "Server Error");
        }
    }
}