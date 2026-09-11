package client.scenes;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Collection;

public class ShoppingListStore {
    private static final ShoppingListStore INSTANCE = new ShoppingListStore();
    private final ObservableList<String> shoppingList = FXCollections.observableArrayList();
    private ShoppingListStore() {}
    public static ShoppingListStore getInstance() { return INSTANCE; }
    public ObservableList<String> getShoppingList() { return shoppingList; }
    public void addAll(Collection<String> items) {
        if (items == null) return;
        for (String item : items) {
            if (item != null && !item.trim().isEmpty()) shoppingList.add(item.trim());
        }
    }
    public void clear() { shoppingList.clear(); }
}