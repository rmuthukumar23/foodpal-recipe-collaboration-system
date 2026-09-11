package client.scenes;

import client.utils.ServerUtilsGlobalIngredient;
import client.utils.ServerUtilsLabel;
import client.utils.ServerUtilsRecipe;
import com.google.inject.Inject;
import commons.*;
import commons.Label;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.*;
import java.util.stream.Collectors;

public class EditRecipeCtrl {

    private PrimaryCtrl pc;

    @FXML private TabPane tabPane;
    @FXML private Tab ingredientsTab;
    @FXML private Tab preparationTab;
    @FXML private Tab additionalTab;
    @FXML private Tab nameTab;
    @FXML private TextArea preparationTextArea;
    @FXML private TextArea ingredientsTextArea1;

    // Label selection UI elements
    @FXML private VBox labelsContainer;
    @FXML private ComboBox<String> languageComboBox;
    @FXML private ComboBox<String> cookingTimeComboBox;
    @FXML private ComboBox<String> cuisineComboBox;
    @FXML private VBox dietaryCheckBoxContainer;
    @FXML private VBox proteinCheckBoxContainer;
    @FXML private VBox mealTypeCheckBoxContainer;

    //Ingredient tab FXML fields
    @FXML private ComboBox<GlobalIngredient> globalIngredientComboBox;
    @FXML private TextField ingredientValueField;
    @FXML private ComboBox<Unit> unitComboBox;

    @FXML private CheckBox informalAmountCheckBox;
    @FXML private TextField informalAmountField;

    @FXML private ListView<Ingredient> ingredientsListView;

    @FXML private Button updateIngredientButton;
    @FXML private Button clearIngredientButton;
    @FXML private Button deleteIngredientButton;


    private Recipe currentRecipe;
    private Stage dialogStage;
    private ServerUtilsRecipe serverUtils;
    private ServerUtilsLabel labelUtils;

    private List<CheckBox> dietaryCheckBoxes = new ArrayList<>();
    private List<CheckBox> proteinCheckBoxes = new ArrayList<>();
    private List<CheckBox> mealTypeCheckBoxes = new ArrayList<>();
    private Map<String, Label> availableLabels = new HashMap<>();

    private final ObservableList<Ingredient> draftIngredients = FXCollections.observableArrayList();
    private final ObservableList<GlobalIngredient> allGlobalIngredients = FXCollections.observableArrayList();
    private final ServerUtilsGlobalIngredient serverUtilsGlobalIngredient = new ServerUtilsGlobalIngredient();


    /**
     * Empty constructor.
     */
    public EditRecipeCtrl() {
    }

    /**
     * Constructor for EditRecipeCtrl.
     * @param p the primary controller
     */
    @Inject
    public EditRecipeCtrl(PrimaryCtrl p) {
        this.pc = p;
    }

    /**
     * Initialize the controller.
     */
    @FXML
    public void initialize() {
        initializeServerUtils();
        loadLabels();

        setupGlobalIngredientDropdown();
        setupEditableIngredientComboBox();
        setupUnitComboBox();
        setupIngredientDraftListView();

        setupIngredientSelectionBehavior();
        setupInformalToggleBehavior();
        setupIngredientButtons();
        enterAddMode();

    }

    private void setupIngredientButtons() {
        if (updateIngredientButton != null) {
            updateIngredientButton.setOnAction(e -> onUpdateIngredient());
        }
        if (clearIngredientButton != null) {
            clearIngredientButton.setOnAction(e -> onClearIngredientEditor());
        }
        if (deleteIngredientButton != null) {
            deleteIngredientButton.setOnAction(e -> onDeleteSelectedIngredient());
        }
    }

    private void setupGlobalIngredientDropdown() {
        if (globalIngredientComboBox == null) return;
        refreshGlobalIngredientDropdown();
        globalIngredientComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GlobalIngredient globalIngredient, boolean empty) {
                super.updateItem(globalIngredient, empty);
                setText(empty || globalIngredient == null ? null : globalIngredient.getName());
            }
        });
        globalIngredientComboBox.setButtonCell(new ListCell<GlobalIngredient>() {
            @Override
            protected void updateItem(GlobalIngredient globalIngredient, boolean empty) {
                super.updateItem(globalIngredient, empty);
                setText(empty || globalIngredient == null ? null : globalIngredient.getName());
            }
        });
    }

    private void refreshGlobalIngredientDropdown() {
        try {
            var globalIngredientList = serverUtilsGlobalIngredient.getGlobalIngredients();
            globalIngredientList.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            allGlobalIngredients.setAll(globalIngredientList);
            globalIngredientComboBox.setItems(allGlobalIngredients);
        } catch (Exception e) {
            System.err.println("Error while getting global ingredient dropdown" + e.getMessage());
        }
    }

    private void setupEditableIngredientComboBox() {
        if (globalIngredientComboBox == null) return;
        globalIngredientComboBox.setEditable(true);
        ObservableList<GlobalIngredient> masterList = FXCollections.observableArrayList(globalIngredientComboBox.getItems());
        FilteredList<GlobalIngredient> filteredList = new FilteredList<>(masterList, gi -> true);
        globalIngredientComboBox.setItems(filteredList);
        globalIngredientComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(GlobalIngredient globalIngredient) {
                return globalIngredient == null ? "" : globalIngredient.getName();
            }
            @Override
            public GlobalIngredient fromString(String text) {
                if (text == null) return null;
                String name = text.trim();
                if (name.isEmpty()) return null;
                for (GlobalIngredient globalIngredient : masterList) {
                    if (globalIngredient != null && globalIngredient.getName() != null && globalIngredient.getName().equalsIgnoreCase(name)) {
                        return globalIngredient;
                    }
                }
                GlobalIngredient globalIngredient = new GlobalIngredient();
                globalIngredient.setName(name);
                return globalIngredient;
            }
        });
        if (globalIngredientComboBox.getEditor() != null) {
            globalIngredientComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {
                String query = (newText == null) ? "" : newText.trim().toLowerCase();
                filteredList.setPredicate(gi -> {
                    if (query.isEmpty()) return true;
                    String n = (gi != null && gi.getName() != null) ? gi.getName().toLowerCase() : "";
                    return n.contains(query);
                });
                Platform.runLater(() -> {
                    if (!query.isEmpty() && !filteredList.isEmpty()) {
                        globalIngredientComboBox.show();
                    }
                });
            });
        }
    }

    private void setupUnitComboBox() {
        if (unitComboBox == null) return;
        unitComboBox.setItems(FXCollections.observableArrayList(Unit.values()));
        unitComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Unit unit, boolean empty) {
                super.updateItem(unit, empty);
                setText((empty || unit == null) ? "Unit..." : unit.getCode());
            }
        });
        unitComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Unit unit, boolean empty) {
                super.updateItem(unit, empty);
                setText((empty || unit == null) ? null : unit.getCode());
            }
        });
    }

    private void setupIngredientDraftListView() {
        if (ingredientsListView == null) return;
        ingredientsListView.setItems(draftIngredients);
        ingredientsListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ingredient item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String name = item.getName() != null ? item.getName() : "";
                String displayAmountAndUnit;
                if (item.getAmountValue() != null && item.getUnit() != null) {
                    double value = item.getAmountValue();
                    String valueString = (value % 1 == 0) ? String.valueOf((long) value) : String.valueOf(value);
                    displayAmountAndUnit = valueString + " " + item.getUnit().getCode();
                } else if (item.getAmount() != null) {
                    displayAmountAndUnit = item.getAmount();
                } else {
                    displayAmountAndUnit = "";
                }
                setText(name + " — " + displayAmountAndUnit);
            }
        });
    }

    private void setupIngredientSelectionBehavior() {
        if (ingredientsListView == null) return;
        ingredientsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldV, selectedIngredient) -> {
                    if (selectedIngredient == null) enterAddMode();
                    else enterEditMode(selectedIngredient);
                });
    }

    private void setupInformalToggleBehavior() {
        if (informalAmountCheckBox == null) return;
        informalAmountCheckBox.selectedProperty().addListener((obs, oldV, isInformal) -> applyInformalMode(isInformal));
        applyInformalMode(informalAmountCheckBox.isSelected());
    }

    private void applyInformalMode(boolean isInformal) {
        if (ingredientValueField != null) ingredientValueField.setDisable(isInformal);
        if (unitComboBox != null) unitComboBox.setDisable(isInformal);
        if (informalAmountField != null) informalAmountField.setDisable(!isInformal);
    }

    private void enterAddMode() {
        if (updateIngredientButton != null) updateIngredientButton.setDisable(true);
        if (deleteIngredientButton != null) deleteIngredientButton.setDisable(true);
        if (clearIngredientButton != null) clearIngredientButton.setDisable(true);
        clearIngredientEditorFieldsOnly();
    }

    private static <T> void ifNotNull(T obj, java.util.function.Consumer<T> action) {
        if (obj != null) action.accept(obj);
    }

    private void enterEditMode(Ingredient selected) {
        if (selected == null) return;
        ifNotNull(updateIngredientButton, b -> b.setDisable(false));
        ifNotNull(deleteIngredientButton, b -> b.setDisable(false));
        ifNotNull(clearIngredientButton, b -> b.setDisable(false));
        String name = selected.getName() == null ? "" : selected.getName();
        ifNotNull(globalIngredientComboBox, globalIngredientComboBox -> {
            globalIngredientComboBox.getSelectionModel().clearSelection();
            if (globalIngredientComboBox.getEditor() != null) globalIngredientComboBox.getEditor().setText(name);
        });
        boolean formal = selected.getAmountValue() != null && selected.getUnit() != null;
        ifNotNull(informalAmountCheckBox, cb -> cb.setSelected(!formal));
        applyInformalMode(!formal);
        ifNotNull(ingredientValueField, TextField::clear);
        ifNotNull(unitComboBox, cb -> cb.getSelectionModel().clearSelection());
        ifNotNull(informalAmountField, TextField::clear);
        if (formal) {
            ifNotNull(ingredientValueField, tf -> tf.setText(formatNumber(selected.getAmountValue())));
            ifNotNull(unitComboBox, cb -> cb.getSelectionModel().select(selected.getUnit()));
        } else {
            String amount = selected.getAmount() == null ? "" : selected.getAmount();
            ifNotNull(informalAmountField, tf -> tf.setText(amount));
        }
    }

    private void clearIngredientEditorFieldsOnly() {
        if (globalIngredientComboBox != null) {
            globalIngredientComboBox.getSelectionModel().clearSelection();
            if (globalIngredientComboBox.getEditor() != null) globalIngredientComboBox.getEditor().clear();
        }
        if (ingredientValueField != null) ingredientValueField.clear();
        if (unitComboBox != null) unitComboBox.getSelectionModel().clearSelection();
        if (informalAmountField != null) informalAmountField.clear();
        if (informalAmountCheckBox != null) informalAmountCheckBox.setSelected(false);
        applyInformalMode(false);
    }

    private String formatNumber(Double value) {
        if (value == null) return "";
        return (value % 1 == 0) ? String.valueOf(value.longValue()) : String.valueOf(value);
    }

    public void onAddIngredient() {
        if (globalIngredientComboBox == null) return;
        Ingredient newIngredient = buildIngredientFromEditorInputs();
        if (newIngredient == null) return;
        String name = newIngredient.getName() != null ? newIngredient.getName().trim() : "";
        boolean alreadyExists = draftIngredients.stream().anyMatch(i -> i.getName() != null && i.getName().equalsIgnoreCase(name));
        if (alreadyExists) {
            showError("That ingredient is already added.");
            return;
        }
        draftIngredients.add(newIngredient);
        ingredientsListView.getSelectionModel().clearSelection();
        enterAddMode();
    }

    private Ingredient buildIngredientFromEditorInputs() {
        if (globalIngredientComboBox == null) return null;
        GlobalIngredient selectedIngredient = globalIngredientComboBox.getValue();
        String typedIngredient = null;
        if (globalIngredientComboBox.getEditor() != null) typedIngredient = globalIngredientComboBox.getEditor().getText();
        String name = "";
        if (selectedIngredient != null && selectedIngredient.getName() != null && !selectedIngredient.getName().isBlank()) {
            name = selectedIngredient.getName().trim();
        } else if (typedIngredient != null) {
            name = typedIngredient.trim();
        }
        if (name.isBlank()) {
            showError("Ingredient name is required.");
            return null;
        }
        boolean isInformal = informalAmountCheckBox != null && informalAmountCheckBox.isSelected();
        Ingredient ing = new Ingredient();
        ing.setName(name);
        return applyAmountFromEditorInputs(ing, isInformal);
    }

    private Ingredient applyAmountFromEditorInputs(Ingredient ing, boolean isInformal) {
        if (isInformal) {
            if (informalAmountField == null) return null;
            String informal = informalAmountField.getText();
            if (informal == null || informal.trim().isEmpty()) {
                showError("Informal amount is required (e.g. \"to taste\", \"a pinch\").");
                return null;
            }
            ing.setAmount(informal.trim());
            ing.setAmountValue(null);
            ing.setUnit(null);
            return ing;
        }
        if (ingredientValueField == null || unitComboBox == null) return null;
        String ingredientValue = ingredientValueField.getText();
        if (ingredientValue == null) ingredientValue = "";
        ingredientValue = ingredientValue.trim();
        double value;
        try {
            value = Double.parseDouble(ingredientValue);
        } catch (NumberFormatException e) {
            showError("Ingredient value must be a number.");
            return null;
        }
        if (value <= 0) {
            showError("Ingredient value must be greater than 0.");
            return null;
        }
        Unit unit = unitComboBox.getSelectionModel().getSelectedItem();
        if (unit == null) {
            showError("Unit is required.");
            return null;
        }
        ing.setAmountValue(value);
        ing.setUnit(unit);
        return ing;
    }

    public void onUpdateIngredient() {
        if (ingredientsListView == null) return;
        int index = ingredientsListView.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            showError("Select an ingredient to update.");
            return;
        }
        Ingredient updated = buildIngredientFromEditorInputs();
        if (updated == null) return;
        String updatedName = updated.getName() != null ? updated.getName().trim() : "";
        boolean duplicatePresent = false;
        for (int i = 0; i < draftIngredients.size(); i++) {
            if (i == index) continue;
            Ingredient existing = draftIngredients.get(i);
            if (existing.getName() != null && existing.getName().equalsIgnoreCase(updatedName)) {
                duplicatePresent = true;
                break;
            }
        }
        if (duplicatePresent) {
            showError("That ingredient name already exists in the list.");
            return;
        }
        draftIngredients.set(index, updated);
        ingredientsListView.getSelectionModel().clearSelection();
        enterAddMode();
    }

    public void onDeleteSelectedIngredient() {
        if (ingredientsListView == null) return;
        int index = ingredientsListView.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            showError("Select an ingredient to delete.");
            return;
        }
        draftIngredients.remove(index);
        ingredientsListView.getSelectionModel().clearSelection();
        enterAddMode();
    }

    public void onClearIngredientEditor() {
        if (ingredientsListView != null) ingredientsListView.getSelectionModel().clearSelection();
        enterAddMode();
    }

    private void initializeServerUtils() {
        if (this.serverUtils == null) this.serverUtils = new ServerUtilsRecipe();
        if (this.labelUtils == null) this.labelUtils = new ServerUtilsLabel();
    }

    private void loadLabels() {
        try {
            List<Label> labels = labelUtils.getAllLabels();
            for (Label label : labels) availableLabels.put(label.getName(), label);
            populateLabelUI(labels);
        } catch (Exception e) {
            System.err.println("Error loading labels: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void populateLabelUI(List<Label> labels) {
        Map<Label.LabelCategory, List<Label>> labelsByCategory = labels.stream().collect(Collectors.groupingBy(Label::getCategory));
        if (languageComboBox != null) {
            List<String> languages = labelsByCategory.getOrDefault(Label.LabelCategory.LANGUAGE, new ArrayList<>()).stream().map(Label::getName).sorted().collect(Collectors.toList());
            languageComboBox.getItems().addAll(languages);
        }
        if (cookingTimeComboBox != null) {
            List<String> cookingTimes = labelsByCategory.getOrDefault(Label.LabelCategory.COOKING_TIME, new ArrayList<>()).stream().map(Label::getName).collect(Collectors.toList());
            cookingTimeComboBox.getItems().add("");
            cookingTimeComboBox.getItems().addAll(cookingTimes);
        }
        if (cuisineComboBox != null) {
            List<String> cuisines = labelsByCategory.getOrDefault(Label.LabelCategory.CUISINE, new ArrayList<>()).stream().map(Label::getName).sorted().collect(Collectors.toList());
            cuisineComboBox.getItems().add("");
            cuisineComboBox.getItems().addAll(cuisines);
        }
        if (dietaryCheckBoxContainer != null) {
            List<Label> dietaryLabels = labelsByCategory.getOrDefault(Label.LabelCategory.DIETARY, new ArrayList<>());
            dietaryLabels.sort(Comparator.comparing(Label::getName));
            for (Label label : dietaryLabels) {
                CheckBox cb = new CheckBox(label.getName());
                dietaryCheckBoxes.add(cb);
                dietaryCheckBoxContainer.getChildren().add(cb);
            }
        }
        if (proteinCheckBoxContainer != null) {
            List<Label> proteinLabels = labelsByCategory.getOrDefault(Label.LabelCategory.PROTEIN, new ArrayList<>());
            proteinLabels.sort(Comparator.comparing(Label::getName));
            for (Label label : proteinLabels) {
                CheckBox cb = new CheckBox(label.getName());
                proteinCheckBoxes.add(cb);
                proteinCheckBoxContainer.getChildren().add(cb);
            }
        }
        if (mealTypeCheckBoxContainer != null) {
            List<Label> mealTypeLabels = labelsByCategory.getOrDefault(Label.LabelCategory.MEAL_TYPE, new ArrayList<>());
            mealTypeLabels.sort(Comparator.comparing(Label::getName));
            for (Label label : mealTypeLabels) {
                CheckBox cb = new CheckBox(label.getName());
                mealTypeCheckBoxes.add(cb);
                mealTypeCheckBoxContainer.getChildren().add(cb);
            }
        }
    }

    public void setRecipeToEdit(Recipe recipe) {
        this.currentRecipe = recipe;
        populateFieldsWithRecipeData();
    }

    private void populateFieldsWithRecipeData() {
        if (currentRecipe == null) return;
        if (ingredientsTextArea1 != null) ingredientsTextArea1.setText(currentRecipe.getName());
        draftIngredients.clear();
        if (currentRecipe.getIngredients() != null) draftIngredients.addAll(currentRecipe.getIngredients());
        if (preparationTextArea != null) {
            StringBuilder stepsBuilder = new StringBuilder();
            if (currentRecipe.getPreparationSteps() != null && !currentRecipe.getPreparationSteps().isEmpty()) {
                List<PreparationStep> sortedSteps = new ArrayList<>(currentRecipe.getPreparationSteps());
                sortedSteps.sort(Comparator.comparingInt(step -> step.getStepOrder() != null ? step.getStepOrder() : 0));
                for (PreparationStep step : sortedSteps) {
                    String description = step.getDescription() != null ? step.getDescription() : "";
                    stepsBuilder.append(description).append("\n");
                }
            }
            preparationTextArea.setText(stepsBuilder.toString());
        }
        populateLabelsFromRecipe();
    }

    private void populateLabelsFromRecipe() {
        if (currentRecipe == null || currentRecipe.getLabels() == null) return;
        Set<String> recipeLabels = currentRecipe.getLabels().stream().map(Label::getName).collect(Collectors.toSet());
        if (languageComboBox != null) {
            String language = currentRecipe.getLanguage();
            if (language != null) languageComboBox.setValue(language);
        }
        if (cookingTimeComboBox != null) {
            currentRecipe.getLabels().stream().filter(label -> label.getCategory() == Label.LabelCategory.COOKING_TIME).findFirst().ifPresent(label -> cookingTimeComboBox.setValue(label.getName()));
        }
        if (cuisineComboBox != null) {
            currentRecipe.getLabels().stream().filter(label -> label.getCategory() == Label.LabelCategory.CUISINE).findFirst().ifPresent(label -> cuisineComboBox.setValue(label.getName()));
        }
        for (CheckBox cb : dietaryCheckBoxes) cb.setSelected(recipeLabels.contains(cb.getText()));
        for (CheckBox cb : proteinCheckBoxes) cb.setSelected(recipeLabels.contains(cb.getText()));
        for (CheckBox cb : mealTypeCheckBoxes) cb.setSelected(recipeLabels.contains(cb.getText()));
    }

    @FXML
    public void onNext() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab == nameTab) selectIngredientsTab();
        else if (currentTab == ingredientsTab) selectPreparationTab();
        else if (currentTab == preparationTab) {
            selectAdditionalTab();
            updateButtonTextInCurrentTab("Done");
        } else if (currentTab == additionalTab) saveRecipeToServer();
    }

    private void updateButtonTextInCurrentTab(String text) {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null && currentTab.getContent() instanceof BorderPane) {
            BorderPane borderPane = (BorderPane) currentTab.getContent();
            if (borderPane.getBottom() instanceof ButtonBar) {
                ButtonBar buttonBar = (ButtonBar) borderPane.getBottom();
                for (javafx.scene.Node node : buttonBar.getButtons()) {
                    if (node instanceof Button) {
                        Button button = (Button) node;
                        if (button.getText().equals("Next") || button.getText().equals("Done")) {
                            button.setText(text);
                            break;
                        }
                    }
                }
            }
        }
    }

    @FXML
    public void onCancel() {
        System.out.println("Edit cancelled.");
        if (dialogStage != null) dialogStage.close();
    }

    public void selectIngredientsTab() { if (tabPane != null && ingredientsTab != null) tabPane.getSelectionModel().select(ingredientsTab); }
    public void selectPreparationTab() { if (tabPane != null && preparationTab != null) tabPane.getSelectionModel().select(preparationTab); }
    public void selectNameTab() { if (tabPane != null && nameTab != null) tabPane.getSelectionModel().select(nameTab); }
    private void selectAdditionalTab() { if (tabPane != null && additionalTab != null) tabPane.getSelectionModel().select(additionalTab); }

    private void saveRecipeToServer() {
        if (this.serverUtils == null) initializeServerUtils();
        if (currentRecipe == null) {
            showError("No recipe selected for editing");
            return;
        }
        try {
            if (languageComboBox == null || languageComboBox.getValue() == null || languageComboBox.getValue().trim().isEmpty()) {
                showError("Language is required. Please select a language.");
                return;
            }
            String recipeName = currentRecipe.getName();
            if (ingredientsTextArea1 != null) {
                String newName = ingredientsTextArea1.getText().trim();
                if (newName.isEmpty()) {
                    showError("Recipe name cannot be empty");
                    return;
                }
                recipeName = newName;
            }
            currentRecipe.clearIngredients();
            currentRecipe.clearPreparationSteps();
            currentRecipe.setName(recipeName);
            for (Ingredient ing : draftIngredients) currentRecipe.addIngredient(ing);
            parseAndAddPreparationSteps();
            Recipe savedRecipe = serverUtils.updateRecipeData(currentRecipe.getId(), currentRecipe);
            if (savedRecipe == null) {
                showError("Failed to save recipe to server. Please check server connection and logs.");
                return;
            }
            Set<String> labelNames = collectLabelNames();
            Recipe updatedWithLabels = serverUtils.addLabelsToRecipe(currentRecipe.getId(), labelNames);
            if (updatedWithLabels == null) {
                showError("Recipe saved but failed to update labels. Please try again.");
                return;
            }
            showSuccess("Recipe updated successfully!");
            if (dialogStage != null) dialogStage.close();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error saving recipe: " + e.getMessage());
        }
    }

    private Set<String> collectLabelNames() {
        Set<String> labelNames = new HashSet<>();
        if (languageComboBox != null && languageComboBox.getValue() != null && !languageComboBox.getValue().trim().isEmpty()) labelNames.add(languageComboBox.getValue());
        if (cookingTimeComboBox != null && cookingTimeComboBox.getValue() != null && !cookingTimeComboBox.getValue().trim().isEmpty()) labelNames.add(cookingTimeComboBox.getValue());
        if (cuisineComboBox != null && cuisineComboBox.getValue() != null && !cuisineComboBox.getValue().trim().isEmpty()) labelNames.add(cuisineComboBox.getValue());
        for (CheckBox cb : dietaryCheckBoxes) if (cb.isSelected()) labelNames.add(cb.getText());
        for (CheckBox cb : proteinCheckBoxes) if (cb.isSelected()) labelNames.add(cb.getText());
        for (CheckBox cb : mealTypeCheckBoxes) if (cb.isSelected()) labelNames.add(cb.getText());
        return labelNames;
    }

    private void parseAndAddPreparationSteps() {
        if (preparationTextArea != null) {
            String stepsText = preparationTextArea.getText().trim();
            if (!stepsText.isEmpty()) {
                String[] lines = stepsText.split("\n");
                int stepOrder = 0;
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        PreparationStep step = new PreparationStep(trimmed);
                        step.setStepOrder(stepOrder++);
                        currentRecipe.addPreparationStep(step);
                    }
                }
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public TabPane getTabPane() { return tabPane; }
    public void setTabPane(TabPane tabPane) { this.tabPane = tabPane; }
    public Tab getIngredientsTab() { return ingredientsTab; }
    public void setIngredientsTab(Tab ingredientsTab) { this.ingredientsTab = ingredientsTab; }
    public Tab getPreparationTab() { return preparationTab; }
    public void setPreparationTab(Tab preparationTab) { this.preparationTab = preparationTab; }
    public Tab getAdditionalTab() { return additionalTab; }
    public void setAdditionalTab(Tab additionalTab) { this.additionalTab = additionalTab; }
    public TextArea getPreparationTextArea() { return preparationTextArea; }
    public void setPreparationTextArea(TextArea preparationTextArea) { this.preparationTextArea = preparationTextArea; }
    public Stage getDialogStage() { return dialogStage; }
    public void setDialogStage(Stage dialogStage) { this.dialogStage = dialogStage; }
}