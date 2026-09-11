package client.scenes;

import client.utils.ServerUtilsRecipe;
import com.google.inject.Inject;
import commons.Recipe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class ShoppingListCtrl {
    @FXML private TextField newItemField;
    @FXML private TextField newAmountField;
    @FXML private ListView<String> shoppingListView;
    @FXML private ComboBox<Recipe> recipeComboBox;
    private PrimaryCtrl pc;
    private ServerUtilsRecipe serverUtilsRecipe;

    @Inject
    public ShoppingListCtrl(PrimaryCtrl pc) {
        this.pc = pc;
        this.serverUtilsRecipe = new ServerUtilsRecipe();
    }

    @FXML
    public void initialize() {
        shoppingListView.setItems(ShoppingListStore.getInstance().getShoppingList());
        shoppingListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                newItemField.setText(newVal);
                newAmountField.clear();
            }
        });
        loadRecipes();
        recipeComboBox.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Recipe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
        recipeComboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(Recipe item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName());
            }
        });
    }

    private void loadRecipes() {
        try {
            List<Recipe> recipes = serverUtilsRecipe.getRecipes();
            ObservableList<Recipe> recipeList = FXCollections.observableArrayList(recipes);
            recipeComboBox.setItems(recipeList);
        } catch (Exception e) {
            System.err.println("Error loading recipes: " + e.getMessage());
        }
    }

    @FXML public void onBack(ActionEvent e) { pc.showHome(); }
    @FXML public void onReset(ActionEvent e) { ShoppingListStore.getInstance().clear(); }

    @FXML
    public void onPrint(ActionEvent e) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Shopping List\n\n");
            int count = 1;
            for (String item : ShoppingListStore.getInstance().getShoppingList()) {
                sb.append(count++).append(". ").append(item).append("\n");
            }
            File file = new File("shopping_list.txt");
            Files.writeString(file.toPath(), sb.toString());
            System.out.println("Shopping list saved to: " + file.getAbsolutePath());
        } catch (IOException ex) {
            System.err.println("Error saving shopping list: " + ex.getMessage());
        }
    }

    @FXML
    public void onAddManualItem(ActionEvent e) {
        String name = newItemField.getText();
        String amount = newAmountField.getText();
        if (name == null || name.isBlank()) return;
        String finalItem = name.trim();
        if (amount != null && !amount.isBlank()) finalItem += " " + amount.trim();
        ShoppingListStore.getInstance().getShoppingList().add(finalItem);
        newItemField.clear();
        newAmountField.clear();
    }

    @FXML
    public void onAddFromRecipe(ActionEvent e) {
        Recipe selected = recipeComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) {
            System.err.println("No recipe selected");
            return;
        }
        pc.showShoppingListConfirmation(selected);
    }

    @FXML
    public void onRemoveSelected(ActionEvent e) {
        int idx = shoppingListView.getSelectionModel().getSelectedIndex();
        if (idx >= 0) ShoppingListStore.getInstance().getShoppingList().remove(idx);
    }

    @FXML
    public void onEditItem(ActionEvent e) {
        int idx = shoppingListView.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        String name = newItemField.getText();
        String amount = newAmountField.getText();
        if (name != null && !name.isBlank()) {
            String updatedItem = name.trim();
            if (amount != null && !amount.isBlank()) updatedItem += " " + amount.trim();
            ShoppingListStore.getInstance().getShoppingList().set(idx, updatedItem);
            newItemField.clear();
            newAmountField.clear();
        }
    }
}