package client.scenes;

import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class AddRecipeCtrl {
    private PrimaryCtrl pc;
    @FXML private TabPane tabPane;
    @FXML private Tab ingredientsTab;
    private Stage dialogStage;

    @Inject
    public AddRecipeCtrl(PrimaryCtrl p) { this.pc = p; }
    public TabPane getTabPane() { return tabPane; }
    public void setTabPane(TabPane tabPane) { this.tabPane = tabPane; }
    public Tab getIngredientsTab() { return ingredientsTab; }
    public void setIngredientsTab(Tab ingredientsTab) { this.ingredientsTab = ingredientsTab; }
    public Stage getDialogStage() { return dialogStage; }
    public void setDialogStage(Stage dialogStage) { this.dialogStage = dialogStage; }
    public void saveRecipe() { System.out.println("Recipe saved"); pc.showHome(); }
    public void selectIngredientsTab() {
        if (tabPane != null && ingredientsTab != null) tabPane.getSelectionModel().select(ingredientsTab);
    }
    public static class ConfirmRecipeDeletionCtrl { }
}
