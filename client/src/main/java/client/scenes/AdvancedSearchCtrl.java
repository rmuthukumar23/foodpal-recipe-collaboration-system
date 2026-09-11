package client.scenes;

import client.utils.RecipeFilter;
import com.google.inject.Inject;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;

import java.util.HashSet;
import java.util.Set;

/**
 * Controller for advanced search functionality.
 */
public class AdvancedSearchCtrl {

    @FXML
    private FlowPane cuisineFlowPane;

    @FXML
    private FlowPane proteinFlowPane;

    @FXML
    private FlowPane dietaryFlowPane;

    @FXML
    private FlowPane mealTypeFlowPane;

    @FXML
    private FlowPane languageFlowPane;

    @FXML
    private Slider maxTimeSlider;

    @FXML
    private Label maxTimeLabel;

    @FXML
    private TextField keywordField;

    private PrimaryCtrl primaryCtrl;
    private HomePageCtrl homePageCtrl;

    @Inject
    public AdvancedSearchCtrl(PrimaryCtrl primaryCtrl, HomePageCtrl homePageCtrl) {
        this.primaryCtrl = primaryCtrl;
        this.homePageCtrl = homePageCtrl;
    }

    @FXML
    public void initialize() {
        if (maxTimeSlider != null && maxTimeLabel != null) {
            maxTimeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                int time = newVal.intValue();
                if (time == 0) {
                    maxTimeLabel.setText("No limit");
                } else {
                    maxTimeLabel.setText(time + " minutes");
                }
            });
            maxTimeLabel.setText("No limit");
        }
        restoreLanguageFilterSelection();
    }

    private void restoreLanguageFilterSelection() {
        if (languageFlowPane == null || homePageCtrl == null) return;
        RecipeFilter filter = homePageCtrl.getRecipeFilter();
        Set<String> savedLanguages = filter.getRequiredLanguages();
        for (var node : languageFlowPane.getChildren()) {
            if (node instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) node;
                if (savedLanguages.contains(checkBox.getText())) {
                    checkBox.setSelected(true);
                }
            }
        }
    }

    @FXML
    public void onSearch() {
        Set<String> allLabels = new HashSet<>();
        allLabels.addAll(getSelectedCheckBoxes(cuisineFlowPane));
        allLabels.addAll(getSelectedCheckBoxes(proteinFlowPane));
        allLabels.addAll(getSelectedCheckBoxes(dietaryFlowPane));
        allLabels.addAll(getSelectedCheckBoxes(mealTypeFlowPane));

        Set<String> selectedLanguages = getSelectedCheckBoxes(languageFlowPane);
        int maxTime = (int) maxTimeSlider.getValue();
        String additionalKeyword = keywordField.getText().trim();

        RecipeFilter filter = homePageCtrl.getRecipeFilter();
        filter.setRequiredLabels(allLabels);
        filter.setRequiredLanguages(selectedLanguages);
        filter.setMaxCookingTime(maxTime > 0 ? maxTime : null);
        filter.setAdditionalKeyword(additionalKeyword.isEmpty() ? null : additionalKeyword);
        homePageCtrl.applyCurrentFilters();
        primaryCtrl.showHome();
    }

    private Set<String> getSelectedCheckBoxes(FlowPane flowPane) {
        Set<String> selected = new HashSet<>();
        if (flowPane != null) {
            for (var node : flowPane.getChildren()) {
                if (node instanceof CheckBox) {
                    CheckBox checkBox = (CheckBox) node;
                    if (checkBox.isSelected()) {
                        selected.add(checkBox.getText());
                    }
                }
            }
        }
        return selected;
    }

    @FXML
    public void onClearAll() {
        clearCheckBoxes(cuisineFlowPane);
        clearCheckBoxes(proteinFlowPane);
        clearCheckBoxes(dietaryFlowPane);
        clearCheckBoxes(mealTypeFlowPane);
        clearCheckBoxes(languageFlowPane);
        keywordField.clear();
        if (maxTimeSlider != null) {
            maxTimeSlider.setValue(0);
        }
        RecipeFilter filter = homePageCtrl.getRecipeFilter();
        filter.clearLanguageFilter();
    }

    private void clearCheckBoxes(FlowPane flowPane) {
        if (flowPane != null) {
            for (var node : flowPane.getChildren()) {
                if (node instanceof CheckBox) {
                    ((CheckBox) node).setSelected(false);
                }
            }
        }
    }

    @FXML
    public void onCancel() {
        primaryCtrl.showHome();
    }
}