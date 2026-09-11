package client.scenes;

import client.utils.ServerUtilsGlobalIngredient;
import com.google.inject.Inject;

import commons.Unit;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;

import commons.GlobalIngredient;
import commons.Ingredient;
import commons.PreparationStep;
import commons.Recipe;
import javafx.collections.FXCollections;

import client.utils.ServerUtilsRecipe;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ListCell;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Controller for adding recipes with tabs.
 */
public class AddRecipeTabCtrl {

    @FXML
    private ComboBox<GlobalIngredient> globalIngredientComboBox;
    @FXML
    private TextField ingredientValueField;
    @FXML
    private TabPane tabPane;
    @FXML
    private TextField recipeNameAdd;
    @FXML
    private CheckBox informalAmountCheckBox;
    @FXML
    private TextField informalAmountField;

    @FXML
    private TextArea preparationTextArea;
    @FXML
    private Button addRecipeNextNameButton;
    @FXML
    private Button addRecipeCancelNameButton;
    @FXML
    private Button addRecipeNextIngredientsButton;
    @FXML
    private Button addRecipeCancelIngredientsButton;
    @FXML
    private Button addRecipeNextPrepButton;
    @FXML
    private Button addRecipeCancelPrepButton;
    @FXML
    private Button addRecipeDoneButton;
    @FXML
    private Button addRecipeAdditionalCancelButton;
    @FXML
    private Button updateIngredientButton;
    @FXML
    private Button clearIngredientButton;
    @FXML
    private Button deleteIngredientButton;


    @FXML
    private ListView<Ingredient> ingredientsListView;

    @FXML
    private ComboBox<Unit> unitComboBox;
    @FXML
    private ComboBox<String> languageComboBox;
    @FXML
    private Label languageSelectedLabel;
    @FXML
    private ComboBox<String> cookingTimeComboBox;
    @FXML
    private ComboBox<String> dietaryComboBox;
    @FXML
    private ComboBox<String> proteinComboBox;
    @FXML
    private ComboBox<String> cuisineComboBox;
    @FXML
    private ComboBox<String> mealTypeComboBox;

    @FXML
    private FlowPane cookingTimeLabelsFlow;
    @FXML
    private FlowPane dietaryLabelsFlow;
    @FXML
    private FlowPane proteinLabelsFlow;
    @FXML
    private FlowPane cuisineLabelsFlow;
    @FXML
    private FlowPane mealTypeLabelsFlow;

    private PrimaryCtrl pc;
    private ServerUtilsRecipe server;
    private final ServerUtilsGlobalIngredient serverUtilsGlobalIngredient;
    private ReadRecipeName nameReader;
    private ReadIngredients ingredientsReader;
    private ReadPrepSteps prepStepsReader;
    private String recipeName;
    private List<Ingredient> ingredients;
    private List<PreparationStep> preparationSteps;
    private Set<String> selectedLabels = new HashSet<>();
    private String selectedLanguage = null;

    //The draft ingredientList to be displayed in the ingredients tab ListView
    private final ObservableList<Ingredient> draftIngredients = FXCollections.observableArrayList();

    private ObservableList<GlobalIngredient> allGlobalIngredients = FXCollections.observableArrayList();



    /**
     * Constructor for AddRecipeTabCtrl.
     *
     * @param pc     the primary controller
     * @param server the server utils for recipe operations
     */
    @Inject
    public AddRecipeTabCtrl(PrimaryCtrl pc, ServerUtilsRecipe server) {
        this.pc = pc;
        this.server = server;
        this.nameReader = new ReadRecipeName();
        this.ingredientsReader = new ReadIngredients();
        this.prepStepsReader = new ReadPrepSteps();
        this.serverUtilsGlobalIngredient = new ServerUtilsGlobalIngredient();
    }

    /**
     * Initializes the controller.
     */
    @FXML
    public void initialize() {
        setupButtons();
        disableTabSwitching();
        setExampleRecipe();

        setupGlobalIngredientDropdown();//set up the global ingredient dropdown so the user can select an ingredient
        setupEditableIngredientComboBox();//setup the editable field of the global ingredient dropdown
        setupUnitComboBox();//setup unit dropdown
        setupIngredientDraftListView();//Setup ingredient ListView

        setupIngredientSelectionBehavior();//Populate fields when selecting an ingredient
        setupInformalToggleBehavior();//Change scene when informal ingredient is toggled.
        setupIngredientButtons();
        enterAddMode();

        initializeLabels();
    }

    private void disableTabSwitching() {
        tabPane.getTabs().forEach(tab -> {
            tab.setOnSelectionChanged(e -> {
                if (tab.isSelected() && !isValidToSwitch(tab)) {
                    e.consume();
                    tabPane.getSelectionModel().select(0);
                }
            });
        });
    }

    private boolean isValidToSwitch(Tab tab) {
        int targetIndex = tabPane.getTabs().indexOf(tab);
        int currentIndex = tabPane.getSelectionModel().getSelectedIndex();
        return targetIndex <= currentIndex;
    }

    private void setExampleRecipe() {
        if (recipeNameAdd != null) {
            recipeNameAdd.setText("Scrambled Eggs");
        }


        if (preparationTextArea != null) {
            preparationTextArea.setText(
                    "1) \nCrack eggs into a bowl and whisk until combined\n\n" +
                            "2)\nMelt butter in a pan over medium heat\n\n" +
                            "3)\nPour eggs into pan and stir gently until cooked"
            );
        }
    }

    private void setupButtons() {
        addRecipeNextNameButton.setOnAction(e -> nextFromName());
        addRecipeNextIngredientsButton.setOnAction(e -> nextFromIngredients());
        addRecipeNextPrepButton.setOnAction(e -> nextFromPreparation());
        addRecipeDoneButton.setOnAction(e -> saveRecipe());
        addRecipeCancelNameButton.setOnAction(e -> cancel());
        addRecipeCancelIngredientsButton.setOnAction(e -> cancel());
        addRecipeCancelPrepButton.setOnAction(e -> cancel());
        addRecipeAdditionalCancelButton.setOnAction(e -> cancel());
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
        if (globalIngredientComboBox == null) return; //Null check in case dropdown hasnt loaded.

        refreshGlobalIngredientDropdown();//

        //Configuring dropdown cells
        globalIngredientComboBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(GlobalIngredient globalIngredient, boolean empty) {//Update the list cells
                super.updateItem(globalIngredient, empty);
                //If the cell is empty or has null ingredient, display nothing. Otherwise display ingredient name.
                setText(empty || globalIngredient == null ? null : globalIngredient.getName());
            }
        });

        //Configuring the cell when dropdown is closed.
        //No lambda needed as param for setButtonCell since it only sets 1 cell.We dont need a function for making cells
        globalIngredientComboBox.setButtonCell(new ListCell<GlobalIngredient>() {
            @Override
            protected void updateItem(GlobalIngredient globalIngredient, boolean empty) {//Update the list cells
                super.updateItem(globalIngredient, empty);
                //If the cell is empty or has null ingredient, display nothing. Otherwise display ingredient name.
                setText(empty || globalIngredient == null ? null : globalIngredient.getName());
            }
        });

    }

    private void refreshGlobalIngredientDropdown() {
        try {
            //Obtaining global ingredients list from server.
            var globalIngredientList = serverUtilsGlobalIngredient.getGlobalIngredients();
            globalIngredientList.sort((a, b) ->
                    a.getName().compareToIgnoreCase(b.getName())); //Sorting the list from a to z
            //Setting cells of the dropdown as items from the globalIngredientList obtained from server
            allGlobalIngredients.setAll(globalIngredientList);
            globalIngredientComboBox.setItems(allGlobalIngredients);


        } catch (Exception e) {
            System.err.println("Error while getting global ingredient dropdown" + e.getMessage());
        }
    }

    private void setupEditableIngredientComboBox() {
        if (globalIngredientComboBox == null) return;

        globalIngredientComboBox.setEditable(true);

        //Creating our master list that we will filter (copy of global ingredients list)
        ObservableList<GlobalIngredient> masterList =
                FXCollections.observableArrayList(globalIngredientComboBox.getItems());
        //Creating our filtered list
        FilteredList<GlobalIngredient> filteredList = new FilteredList<>(masterList, gi -> true);
        globalIngredientComboBox.setItems(filteredList);

        globalIngredientComboBox.setConverter(new StringConverter<>() {//Ensure the editable field stores a global
            // ingredient object instead of a string
            @Override
            public String toString(GlobalIngredient globalIngredient) {//To string will display the name of the global I
                return globalIngredient == null ? "" : globalIngredient.getName();
            }

            @Override
            public GlobalIngredient fromString(String text) {
                //Null and empty checks
                if (text == null) return null;
                String name = text.trim();
                if (name.isEmpty()) return null;

                //Check if the typed ingredient already exists in which case we return it
                for (GlobalIngredient globalIngredient : masterList) {
                    if (globalIngredient != null && globalIngredient.getName() != null && globalIngredient.getName().equalsIgnoreCase(name)) {
                        return globalIngredient;
                    }
                }

                //Construct our global ingredient from the string.
                GlobalIngredient globalIngredient = new GlobalIngredient();
                globalIngredient.setName(name);
                return globalIngredient;
            }
        });

        // ADDED: Live filtering while typing in the ComboBox editor.
        if (globalIngredientComboBox.getEditor() != null) {
            //Creating a listener that allows us to constantly access the "newText" as the user is typing
            globalIngredientComboBox.getEditor().textProperty().addListener((obs, oldText, newText) -> {

                //Form the normalised search query from the user search.
                String query = (newText == null) ? "" : newText.trim().toLowerCase();

                //Set the predicate for the filtered list to be filtered on
                filteredList.setPredicate(gi -> {
                    if (query.isEmpty()) return true;//List will not be filtered

                    String n = (gi != null && gi.getName() != null) ? gi.getName().toLowerCase() : "";
                    return n.contains(query);//Ingredient is included in the filtered list if it contains the query
                });

                // Automatically dropdown the list of global ingredients when the user is typing
                Platform.runLater(() -> {
                    if (!query.isEmpty() && !filteredList.isEmpty()) {
                        globalIngredientComboBox.show();
                    }
                });
            });
        }
    }


    private void setupUnitComboBox() {//Setup Unit dropdown
        if (unitComboBox == null) return;//Null check
        unitComboBox.setItems(FXCollections.observableArrayList(Unit.values()));//Set items of the unit dropdown to the
        // enum values e.g. kg, g. tbsp, ml etc...

        // Show the prompt text Unit... Becuase it was becoming blank aftering adding an ingredient
        unitComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Unit unit, boolean empty) {
                super.updateItem(unit, empty);
                setText((empty || unit == null) ? "Unit..." : unit.getCode());
            }
        });

        // (Optional but recommended) Use the same display text inside the dropdown list
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

        ingredientsListView.setCellFactory(lv -> new ListCell<>() {//Configure each listview cell
            @Override
            protected void updateItem(Ingredient item, boolean empty) {//Method to update cells with new contents
                super.updateItem(item, empty);
                if (empty || item == null) {//Configure empty cells
                    setText(null);
                    return;
                }

                String name = item.getName() != null ? item.getName() : "";//Null check: If name exists, String name is
                // set as item name, otherwise we set String name as empty string ""
                String displayAmountAndUnit;

                if (item.getAmountValue() != null && item.getUnit() != null) {//If amount value and unit both exist...
                    double value = item.getAmountValue();

                    //Converts the amount value into a string. If it ends in .0 e.g. 200.0, we remove the decimal and becomes 200
                    String valueString = (value % 1 == 0) ? String.valueOf((long) value) : String.valueOf(value);
                    displayAmountAndUnit = valueString + " " + item.getUnit().getCode();//Construct string
                } else if (item.getAmount() != null) {
                    displayAmountAndUnit = item.getAmount();
                } else {
                    displayAmountAndUnit = ""; //Empty string if value and unit not configured
                }

                setText(name + " — " + displayAmountAndUnit); //Set cell text to display the name, amount and unit
            }
        });
    }

    /**
     * Enter's add mode or edit mode depending on whether an item from the listview is selected or not. If nothing is
     * selected, we assume the user is adding an ingredient to the list. If something is selected we assume the user is
     * trying to edit it.
     */
    private void setupIngredientSelectionBehavior() {
        if (ingredientsListView == null) return;

        ingredientsListView.getSelectionModel().selectedItemProperty().addListener(//Check if user selected a list item
                (obs, oldV, selectedIngredient) -> {
                    if (selectedIngredient == null) {
                        enterAddMode();
                    } else {
                        enterEditMode(selectedIngredient);
                    }
                });
    }

    /**
     * Allows the mode to be changed to informal mode when the user clicks the informal mode checkbox
     * Also sets up the default mode upon startup
     */
    private void setupInformalToggleBehavior() {
        if (informalAmountCheckBox == null) return;

        informalAmountCheckBox.selectedProperty().addListener((obs, oldV, isInformal) -> {
            applyInformalMode(isInformal);//Listen to the checkbox with a boolean. If true we apply informal mode.
        });

        // apply the default state upon startup
        applyInformalMode(informalAmountCheckBox.isSelected());
    }

    /**
     * Applies informal mode on the UI allowing the user to enter informal amounts depending on the informal amount
     * checkbox
     */
    private void applyInformalMode(boolean isInformal) {
        //If informalMode is ON we disable unit and value field
        if (ingredientValueField != null) ingredientValueField.setDisable(isInformal);
        if (unitComboBox != null) unitComboBox.setDisable(isInformal);

        //And we enable informal amount field
        if (informalAmountField != null) informalAmountField.setDisable(!isInformal);
    }

    /**
     * Disables the update and delete buttons and clears the editing fields to allow for new ingredient addition
     */
    private void enterAddMode() {
        if (updateIngredientButton != null) updateIngredientButton.setDisable(true);
        if (deleteIngredientButton != null) deleteIngredientButton.setDisable(true);
        if (clearIngredientButton != null) clearIngredientButton.setDisable(true);



        clearIngredientEditorFieldsOnly();
    }

    /**
     * Takes a generic object and a generic function. If the object is not null, the function can run on the object.
     * this allows us to avoid null checks increasing cyclomatic complexity
     * @param obj generic object
     * @param action generic funciton to be performed on the object (e.g. clear a textfield)
     */
    private static <T> void ifNotNull(T obj, java.util.function.Consumer<T> action) {
        if (obj != null) action.accept(obj);
    }

    /**
     * Enters edit mode by enabling the update and delete buttons and then populating the fields like name, value
     * and amount with the information from the ingredient selected on the listView.
     * It also considers whether the selected ingredient has an informal or formal amount and displays the amount
     * accordingly.
     * @param selected selected ingredient from the listview that will populate our fields
     */
    private void enterEditMode(Ingredient selected) {
        if (selected == null) return;

        //Enable update and delete buttons
        ifNotNull(updateIngredientButton, b -> b.setDisable(false));
        ifNotNull(deleteIngredientButton, b -> b.setDisable(false));
        ifNotNull(clearIngredientButton, b -> b.setDisable(false));

        //Obtain the ingredient name and populate the name field with it in the editor.
        String name = selected.getName() == null ? "" : selected.getName();
        ifNotNull(globalIngredientComboBox, globalIngredientComboBox -> {
            globalIngredientComboBox.getSelectionModel().clearSelection();
            if (globalIngredientComboBox.getEditor() != null) globalIngredientComboBox.getEditor().setText(name);
        });

        //Boolean indicating if ingredient is formal or informal
        boolean formal = selected.getAmountValue() != null && selected.getUnit() != null;

        //Null check and then change the checkbox to the correct mode and run applly informal mode accordingly
        ifNotNull(informalAmountCheckBox, cb -> cb.setSelected(!formal));
        applyInformalMode(!formal);

        // clear amount/unit fields. Pass the object and the function to be applied to our ifNotNull helper
        ifNotNull(ingredientValueField, TextField::clear);
        ifNotNull(unitComboBox, cb -> cb.getSelectionModel().clearSelection());
        ifNotNull(informalAmountField, TextField::clear);

        if (formal) {//If ingredient is formal we populate the amount and unit fields
            ifNotNull(ingredientValueField, tf -> tf.setText(formatNumber(selected.getAmountValue())));
            ifNotNull(unitComboBox, cb -> cb.getSelectionModel().select(selected.getUnit()));
        } else {//if ingredient is informal we set the amount to the informal amount string of the ingredient
            String amount = selected.getAmount() == null ? "" : selected.getAmount();
            ifNotNull(informalAmountField, tf -> tf.setText(amount));
        }
    }

    private void clearIngredientEditorFieldsOnly() {//Resets all fields and modes.
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

    private String formatNumber(Double value) {//formats the number so decimal places are only shown when needed.
        if (value == null) return "";
        return (value % 1 == 0) ? String.valueOf(value.longValue()) : String.valueOf(value);
    }


    /**
     * Adds the configured ingredient with its name, amount value and unit to the draftingredient ListView
     */
    public void onAddIngredient() {
        if (globalIngredientComboBox == null) return;

        Ingredient newIngredient = buildIngredientFromEditorInputs();//Construct ingredient using helper
        if (newIngredient == null) return;

        //Obtain the ingredient name, and use it to check for duplicates by streaming the current draft list.
        String name = newIngredient.getName() != null ? newIngredient.getName().trim() : "";
        boolean alreadyExists = draftIngredients.stream()
                .anyMatch(i -> i.getName() != null && i.getName().equalsIgnoreCase(name));

        if (alreadyExists) {
            showError("That ingredient is already added.");
            return;
        }

        draftIngredients.add(newIngredient);

        //Clear and enter add mode again (in case we were in edit mode)
        ingredientsListView.getSelectionModel().clearSelection();
        enterAddMode();
    }

    private Ingredient buildIngredientFromEditorInputs() {
        if (globalIngredientComboBox == null) {
            return null;
        }

        GlobalIngredient selectedIngredient = globalIngredientComboBox.getValue();//Get selected ingredient from drpdown

        String typedIngredient = null;//Obtain typed ingredient from editor
        if (globalIngredientComboBox.getEditor() != null) {
            typedIngredient = globalIngredientComboBox.getEditor().getText();
        }

        //Use the seletec global ingredient name if present, otherwise use the name typed by the user.
        String name = "";
        if (selectedIngredient != null && selectedIngredient.getName() != null
                && !selectedIngredient.getName().isBlank()) {
            name = selectedIngredient.getName().trim();
        } else if (typedIngredient != null) {
            name = typedIngredient.trim();
        }

        if (name.isBlank()) {
            showError("Ingredient name is required.");
            return null;
        }

        boolean isInformal = informalAmountCheckBox != null && informalAmountCheckBox.isSelected();

        Ingredient ing = new Ingredient(); //Create new ingredient and set its name
        ing.setName(name);

        return applyAmountFromEditorInputs(ing, isInformal);//Call helper to set amount (depending on ingredient formal)
    }

    private Ingredient applyAmountFromEditorInputs(Ingredient ing, boolean isInformal) {
        if (isInformal) {//if the ingredient is informal...
            if (informalAmountField == null) {
                return null;
            }

            String informal = informalAmountField.getText();
            if (informal == null || informal.trim().isEmpty()) {
                showError("Informal amount is required (e.g. \"to taste\", \"a pinch\").");
                return null;
            }
            //set the amount string to the informal amount and null values for amount value and unit
            ing.setAmount(informal.trim());
            ing.setAmountValue(null);
            ing.setUnit(null);
            return ing;
        }
        //Continue if ingredient is informal
        if (ingredientValueField == null || unitComboBox == null) {
            return null;
        }

        String ingredientValue = ingredientValueField.getText();//Obtain string ingredient value
        if (ingredientValue == null) {
            ingredientValue = "";
        }
        ingredientValue = ingredientValue.trim();

        //Parse the string value into a double
        double value;
        try {
            value = Double.parseDouble(ingredientValue);
        } catch (NumberFormatException e) {
            showError("Ingredient value must be a number.");
            return null;
        }

        //Check for negative values
        if (value <= 0) {
            showError("Ingredient value must be greater than 0.");
            return null;
        }

        Unit unit = unitComboBox.getSelectionModel().getSelectedItem();//Obtain unit
        if (unit == null) {
            showError("Unit is required.");
            return null;
        }

        //Set amountvalue and unit for our formal ingredient.
        ing.setAmountValue(value);
        ing.setUnit(unit);
        return ing;
    }

    /**
     * This method allows the user to update the ingredient that they were editing. This will update what it looks like
     * on the draft ingredient listview
     */
    public void onUpdateIngredient() {
        if (ingredientsListView == null) return;

        //Obtain the index and use it to check if an ingredient has even been selected. index above -1 means selected
        int index = ingredientsListView.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            showError("Select an ingredient to update.");
            return;
        }

        Ingredient updated = buildIngredientFromEditorInputs();//Build our updated ingredient from edit mode
        if (updated == null) return;

        //Obtain the updated ingredient name so we can check for duplicates
        String updatedName = updated.getName() != null ? updated.getName().trim() : "";
        boolean duplicatePresent = false;

        //Iterate through the list of ingredients already presetn
        for (int i = 0; i < draftIngredients.size(); i++) {
            if (i == index) continue;//Ignore the index we are currently editing
            Ingredient existing = draftIngredients.get(i);

            //Compare names to see if duplicate is present
            if (existing.getName() != null && existing.getName().equalsIgnoreCase(updatedName)) {
                duplicatePresent = true;
                break;
            }
        }

        if (duplicatePresent) {
            showError("That ingredient name already exists in the list.");
            return;
        }

        draftIngredients.set(index, updated);//Update the list using the updated ingredient

        ingredientsListView.getSelectionModel().clearSelection();//Clear selection and then enter add mode
        enterAddMode();
    }

    /**
     * This method allows the user to delete an ingredient from the draft ingredient listview
     */
    public void onDeleteSelectedIngredient() {
        if (ingredientsListView == null) return;

        //Ensure an ingredient is actually selected when we try to delete it
        int index = ingredientsListView.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            showError("Select an ingredient to delete.");
            return;
        }

        //Remove the index from the list and clear selection and reset UI to add mode
        draftIngredients.remove(index);
        ingredientsListView.getSelectionModel().clearSelection();
        enterAddMode();
    }

    /**
     * This method allows the user to clear the editing fields so they can go back to adding a new ingredient instead
     * of editing an existing one.
     */
    public void onClearIngredientEditor() {//Clears selection and enters add mode
        if (ingredientsListView != null) {
            ingredientsListView.getSelectionModel().clearSelection();
        }
        enterAddMode();
    }



    private void nextFromName() {
        try {
            recipeName = nameReader.readAndParse(recipeNameAdd.getText());

            if (recipeNameExists(recipeName)) {
                showFormatRules();
                return;
            }

            enableTab(1);
            tabPane.getSelectionModel().select(1);
        } catch (ReadRecipeName.RecipeNameParseException e) {
            showFormatRules();
        }
    }

    private boolean recipeNameExists(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        try {
            List<Recipe> allRecipes = server.getRecipes();
            return allRecipes.stream()
                    .anyMatch(r -> r.getName() != null && r.getName().equalsIgnoreCase(name.trim()));
        } catch (Exception e) {
            System.err.println("Failed to check for duplicate recipe name: " + e.getMessage());
            return false;
        }
    }

    private void nextFromIngredients() {
        if (draftIngredients.isEmpty()) {//Show error if they try to proceed without ingredients
            showError("Please add at least one ingredient.");
            return;
        }
        ingredients = List.copyOf(draftIngredients);//Copy the listview list to the official ingredients list
        enableTab(2);
        tabPane.getSelectionModel().select(2);//Swithc to preparation tab.
    }


    private void nextFromPreparation() {
        try {
            preparationSteps = prepStepsReader.readAndParse(preparationTextArea.getText());
            enableTab(3);
            tabPane.getSelectionModel().select(3);
        } catch (ReadPrepSteps.PrepStepsParseException e) {
            showFormatRules();
        }
    }

    private void enableTab(int index) {
        if (index < tabPane.getTabs().size()) {
            tabPane.getTabs().get(index).setDisable(false);
        }
    }

    private void saveRecipe() {
        if (!validateLanguageSelected()) {
            showError("Language is required. Please select a language for this recipe.");
            return;
        }

        try {
            Recipe recipe = buildRecipeWithoutLabels();
            System.out.println("Saving recipe: " + recipe.getName());
            Recipe saved = server.addRecipe(recipe);

            if (saved == null) {
                showError("Failed to save recipe. Please check server connection.");
                return;
            }

            System.out.println("Recipe saved successfully with ID: " + saved.getId());

            Set<String> allLabels = new HashSet<>(selectedLabels);
            allLabels.add(selectedLanguage);

            if (!allLabels.isEmpty()) {
                System.out.println("Adding " + allLabels.size() + " labels to recipe...");
                Recipe withLabels = server.addLabelsToRecipe(saved.getId(), allLabels);

                if (withLabels != null) {
                    System.out.println("Successfully added " + withLabels.getLabels().size() + " labels");
                } else {
                    System.err.println("Warning: Recipe saved but labels failed to attach");
                }
            }

            showSuccess("Recipe created successfully with language: " + selectedLanguage + "!");
            resetAndGoHome();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Error creating recipe: " + e.getMessage());
        }
    }

    private boolean validateLanguageSelected() {
        return selectedLanguage != null && !selectedLanguage.trim().isEmpty();
    }

    private Recipe buildRecipeWithoutLabels() {
        Recipe recipe = new Recipe(recipeName);

        if (ingredients != null) {
            for (Ingredient ing : ingredients) {
                recipe.addIngredient(ing);
            }
        }

        if (preparationSteps != null) {
            for (PreparationStep step : preparationSteps) {
                recipe.addPreparationStep(step);
            }
        }

        return recipe;
    }

    private void cancel() {
        if (confirmCancel()) {
            resetAndGoHome();
        }
    }

    private boolean confirmCancel() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Cancel Recipe");
        alert.setHeaderText("Are you sure?");
        alert.setContentText("All entered data will be lost.");
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showFormatRules() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/client/scenes/AddRecipeInfo.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Format Rules");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load format rules");
        }
    }

    private void resetAndGoHome() {
        clearData();
        clearLabels();
        clearLanguage();
        disableAllTabsExceptFirst();
        tabPane.getSelectionModel().select(0);
        pc.showHome();
    }

    private void disableAllTabsExceptFirst() {
        for (int i = 1; i < tabPane.getTabs().size(); i++) {
            tabPane.getTabs().get(i).setDisable(true);
        }
    }

    private void initializeLabels() {
        initializeLanguageComboBox();
        initializeCookingTimeLabels();
        initializeDietaryLabels();
        initializeProteinLabels();
        initializeCuisineLabels();
        initializeMealTypeLabels();
    }

    private void initializeLanguageComboBox() {
        if (languageComboBox != null) {
            languageComboBox.getItems().addAll("Dutch", "English", "German");
            languageComboBox.setOnAction(e -> {
                String selected = languageComboBox.getValue();
                if (selected != null && !selected.isEmpty()) {
                    selectedLanguage = selected;
                    updateLanguageLabel(selected);
                    updateLanguageComboBoxStyle();
                }
            });
        }
    }

    private void updateLanguageLabel(String language) {
        if (languageSelectedLabel != null) {
            languageSelectedLabel.setText("✓ Selected: " + language);
        }
    }

    private void updateLanguageComboBoxStyle() {
        if (languageComboBox != null) {
            languageComboBox.setStyle(
                    "-fx-border-color: #3B4CCA; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 6; " +
                            "-fx-background-radius: 6;"
            );
        }
    }

    private void initializeCookingTimeLabels() {
        if (cookingTimeComboBox != null) {
            cookingTimeComboBox.getItems().addAll(
                    "Under 15 minutes",
                    "15-30 minutes",
                    "30-45 minutes",
                    "45-60 minutes",
                    "60-90 minutes",
                    "90-120 minutes",
                    "Over 120 minutes"
            );
            setupComboBoxListener(cookingTimeComboBox, cookingTimeLabelsFlow);
        }
    }

    private void initializeDietaryLabels() {
        if (dietaryComboBox != null) {
            dietaryComboBox.getItems().addAll(
                    "Vegan", "Vegetarian", "Gluten-Free", "Dairy-Free",
                    "Keto", "Paleo", "Pescatarian", "Nut-Free", "Low-Carb",
                    "Halal", "Kosher"
            );
            setupComboBoxListener(dietaryComboBox, dietaryLabelsFlow);
        }
    }

    private void initializeProteinLabels() {
        if (proteinComboBox != null) {
            proteinComboBox.getItems().addAll(
                    "Beef", "Chicken", "Lamb", "Pork",
                    "Fish", "Seafood", "Tofu", "Eggs", "None (Vegetarian)"
            );
            setupComboBoxListener(proteinComboBox, proteinLabelsFlow);
        }
    }

    private void initializeCuisineLabels() {
        if (cuisineComboBox != null) {
            cuisineComboBox.getItems().addAll(
                    "Italian", "Chinese", "Mexican", "Indian",
                    "Japanese", "French", "Thai", "Mediterranean",
                    "American", "Korean", "Greek", "Spanish"
            );
            setupComboBoxListener(cuisineComboBox, cuisineLabelsFlow);
        }
    }

    private void initializeMealTypeLabels() {
        if (mealTypeComboBox != null) {
            mealTypeComboBox.getItems().addAll(
                    "Breakfast", "Lunch", "Dinner", "Snack",
                    "Dessert", "Appetizer", "Brunch"
            );
            setupComboBoxListener(mealTypeComboBox, mealTypeLabelsFlow);
        }
    }

    private void setupComboBoxListener(ComboBox<String> comboBox, FlowPane flowPane) {
        if (comboBox == null || flowPane == null) {
            return;
        }

        comboBox.setOnAction(e -> {
            String selected = comboBox.getValue();
            if (selected != null && !selected.isEmpty()) {
                addLabelToFlow(selected, flowPane);
                comboBox.setValue(null);
            }
        });
    }

    private void addLabelToFlow(String labelText, FlowPane flowPane) {
        if (selectedLabels.contains(labelText)) {
            return;
        }

        selectedLabels.add(labelText);

        Button labelChip = new Button(labelText + " ✕");
        labelChip.setStyle(
                "-fx-background-color: #3B4CCA; " +
                        "-fx-text-fill: #FFFFFF; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-padding: 5 15 5 15; " +
                        "-fx-cursor: hand; " +
                        "-fx-font-size: 12px;"
        );

        labelChip.setOnAction(e -> {
            flowPane.getChildren().remove(labelChip);
            selectedLabels.remove(labelText);
        });

        flowPane.getChildren().add(labelChip);
    }

    /**
     * Clears all selected labels.
     */
    public void clearLabels() {
        selectedLabels.clear();
        clearLabelFlowPanes();
        resetComboBoxes();
    }

    private void clearLanguage() {
        selectedLanguage = null;
        if (languageComboBox != null) {
            languageComboBox.setValue(null);
            languageComboBox.setStyle(
                    "-fx-border-color: #FF0000; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 6; " +
                            "-fx-background-radius: 6;"
            );
        }
        if (languageSelectedLabel != null) {
            languageSelectedLabel.setText("");
        }
    }

    private void clearLabelFlowPanes() {
        if (cookingTimeLabelsFlow != null) {
            cookingTimeLabelsFlow.getChildren().clear();
        }
        if (dietaryLabelsFlow != null) {
            dietaryLabelsFlow.getChildren().clear();
        }
        if (proteinLabelsFlow != null) {
            proteinLabelsFlow.getChildren().clear();
        }
        if (cuisineLabelsFlow != null) {
            cuisineLabelsFlow.getChildren().clear();
        }
        if (mealTypeLabelsFlow != null) {
            mealTypeLabelsFlow.getChildren().clear();
        }
    }

    private void resetComboBoxes() {
        if (cookingTimeComboBox != null) {
            cookingTimeComboBox.setValue(null);
        }
        if (dietaryComboBox != null) {
            dietaryComboBox.setValue(null);
        }
        if (proteinComboBox != null) {
            proteinComboBox.setValue(null);
        }
        if (cuisineComboBox != null) {
            cuisineComboBox.setValue(null);
        }
        if (mealTypeComboBox != null) {
            mealTypeComboBox.setValue(null);
        }
    }

    private void clearData() {
        recipeName = null;
        ingredients = null;
        preparationSteps = null;
        nameReader.clear();
        prepStepsReader.clear();
        setExampleRecipe();
    }

    private void showError(String msg) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Recipe Created");
        alert.setContentText(msg);
        alert.showAndWait();
    }
}