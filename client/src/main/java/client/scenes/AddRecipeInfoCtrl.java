package client.scenes;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class AddRecipeInfoCtrl {
    @FXML private Button understoodButton;
    @FXML public void initialize() { understoodButton.setOnAction(e -> close()); }
    private void close() {
        Stage stage = (Stage) understoodButton.getScene().getWindow();
        stage.close();
    }
}