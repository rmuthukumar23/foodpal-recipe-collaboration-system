package client.scenes;

import com.google.inject.Inject;
import commons.Ingredient;
import commons.Recipe;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class ShoppingListConfirmationCtrl {
    private PrimaryCtrl pc;
    @FXML private ListView<String> toBeAddedList;
    @FXML private TextField editSelectedField;
    @FXML private TextField addItemField;
    private final ObservableList<String> items = FXCollections.observableArrayList();

    @Inject
    public ShoppingListConfirmationCtrl(PrimaryCtrl pc) { this.pc = pc; }

    @FXML
    public void initialize() {
        toBeAddedList.setItems(items);
        toBeAddedList.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) editSelectedField.setText(newItem);
            else editSelectedField.clear();
        });
    }

    @FXML
    public void onApplyEdit(ActionEvent event) {
        int index = toBeAddedList.getSelectionModel().getSelectedIndex();
        if (index < 0) return;
        String updated = editSelectedField.getText();
        if (updated == null || updated.trim().isEmpty()) return;
        items.set(index, updated.trim());
    }

    @FXML
    public void onAddItem(ActionEvent event) {
        String newItem = addItemField.getText();
        if (newItem == null || newItem.trim().isEmpty()) return;
        items.add(newItem.trim());
        addItemField.clear();
    }

    @FXML
    public void onRemoveSelected(ActionEvent event) {
        int index = toBeAddedList.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            items.remove(index);
            editSelectedField.clear();
        }
    }

    @FXML
    public void onAddToShoppingList(ActionEvent event) {
        ShoppingListStore.getInstance().addAll(items);
        pc.showShoppingList();
    }

    public void setRecipe(Recipe recipe) {
        items.clear();
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                String item = ingredient.getName();
                if (ingredient.getAmount() != null && !ingredient.getAmount().isEmpty()) item += " " + ingredient.getAmount();
                item += " (" + recipe.getName() + ")";
                items.add(item);
            }
        }
    }

    @FXML
    public void onCancel(ActionEvent event) { pc.showShoppingList(); }
}