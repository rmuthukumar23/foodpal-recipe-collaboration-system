package client.scenes;

import client.utils.RecipeNutritionUtils;
import client.utils.ServerUtilsRecipe;
import commons.Ingredient;
import commons.PreparationStep;
import commons.Recipe;
import jakarta.inject.Inject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PrintRecipeCtrl {

    @FXML private ScrollPane scrollPane;
    @FXML private TextArea textArea;
    @FXML private Button downloadButton;
    @FXML private TextField scalingFactorField;
    @FXML private Button cancelButton;

    private final PrimaryCtrl pc;
    @SuppressWarnings("unused")
    private final ServerUtilsRecipe serverUtilsRecipe;
    private Recipe currentRecipe;
    private String printableContent;
    private double scaleFactor = 1.0;
    private boolean navigateHomeAfterDownload = true;

    @Inject
    public PrintRecipeCtrl(PrimaryCtrl pc) {
        this.pc = pc;
        this.serverUtilsRecipe = new ServerUtilsRecipe();
    }

    @FXML
    public void initialize() {
        if (scalingFactorField != null) {
            scalingFactorField.setText("1.0");
            scalingFactorField.textProperty().addListener((obs, oldVal, newVal) -> refreshPreview());
        }
        refreshPreview();
    }

    public void setNavigateHomeAfterDownload(boolean navigate) { this.navigateHomeAfterDownload = navigate; }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = (scaleFactor > 0) ? scaleFactor : 1.0;
        if (scalingFactorField != null) {
            String wanted = String.valueOf(this.scaleFactor);
            if (!wanted.equals(scalingFactorField.getText())) scalingFactorField.setText(wanted);
        }
        refreshPreview();
    }

    public void setRecipe(Recipe recipe) {
        if (recipe == null) {
            this.currentRecipe = null;
            this.printableContent = null;
            return;
        }
        this.currentRecipe = recipe;
        refreshPreview();
    }

    private void refreshPreview() {
        if (currentRecipe == null) {
            printableContent = "";
            if (textArea != null) textArea.setText("");
            return;
        }
        double scale = parseScaleFromFieldOrDefault();
        this.scaleFactor = scale;
        StringBuilder sb = new StringBuilder();
        sb.append("Recipe: ").append(nullSafe(currentRecipe.getName())).append("\n\n");
        sb.append("Ingredients:\n");
        if (currentRecipe.getIngredients() != null && !currentRecipe.getIngredients().isEmpty()) {
            for (Ingredient ing : currentRecipe.getIngredients()) {
                sb.append("- ");
                if (ing.getAmountValue() != null && ing.getUnit() != null) {
                    double scaledVal = ing.getAmountValue() * scale;
                    sb.append(RecipeNutritionUtils.fmt(scaledVal)).append(" ").append(ing.getUnit()).append(" ");
                } else if (ing.getAmount() != null && !ing.getAmount().isEmpty()) {
                    sb.append(scaleInformalAmount(ing.getAmount(), scale)).append(" ");
                }
                sb.append(nullSafe(ing.getName())).append("\n");
            }
        } else sb.append("- None\n");
        sb.append("\nPreparation:\n");
        if (currentRecipe.getPreparationSteps() != null && !currentRecipe.getPreparationSteps().isEmpty()) {
            int stepNum = 1;
            for (PreparationStep step : currentRecipe.getPreparationSteps()) {
                sb.append(stepNum++).append(". ").append(nullSafe(step.getDescription())).append("\n");
            }
        } else sb.append("- None\n");
        printableContent = sb.toString() + buildNutritionBlock();
        if (textArea != null) textArea.setText(printableContent);
    }

    private String buildNutritionBlock() {
        if (currentRecipe == null) return "";
        var totals = RecipeNutritionUtils.aggregateScaledNutrition(currentRecipe, scaleFactor);
        int servings = RecipeNutritionUtils.inferServings(totals.totalWeightGrams, 250.0);
        if (servings <= 0) servings = 1;
        double kcalPerServing = totals.kcal / servings;
        double proteinPerServing = totals.proteinG / servings;
        double carbsPerServing = totals.carbsG / servings;
        double fatPerServing = totals.fatG / servings;
        return "\n\n--- Nutrition (scaled) ---\n" +
                "Scale factor: " + RecipeNutritionUtils.fmt(scaleFactor) + "x\n" +
                "Estimated servings: " + servings + "\n" +
                "Total weight (approx): " + RecipeNutritionUtils.fmt(totals.totalWeightGrams) + " g\n\n" +
                "Total: " + RecipeNutritionUtils.fmt(totals.kcal) + " kcal, " + RecipeNutritionUtils.fmt(totals.proteinG) + " g protein, " + RecipeNutritionUtils.fmt(totals.carbsG) + " g carbs, " + RecipeNutritionUtils.fmt(totals.fatG) + " g fat\n" +
                "Per serving: " + RecipeNutritionUtils.fmt(kcalPerServing) + " kcal, " + RecipeNutritionUtils.fmt(proteinPerServing) + " g protein, " + RecipeNutritionUtils.fmt(carbsPerServing) + " g carbs, " + RecipeNutritionUtils.fmt(fatPerServing) + " g fat\n";
    }

    @FXML
    public void onDownload() throws IOException {
        if (currentRecipe == null) return;
        String contentToWrite = null;
        if (textArea != null && textArea.getText() != null && !textArea.getText().isBlank()) contentToWrite = textArea.getText();
        else contentToWrite = printableContent;
        if (contentToWrite == null) return;
        String base = nullSafe(currentRecipe.getName()).trim();
        if (base.isEmpty()) base = "recipe";
        String fileName = base.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
        File file = new File(fileName);
        Files.writeString(file.toPath(), contentToWrite);
        Runnable showAlert = () -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success"); alert.setHeaderText("Recipe Downloaded"); alert.setContentText("Recipe saved to: " + file.getAbsolutePath()); alert.show();
        };
        if (Platform.isFxApplicationThread()) showAlert.run();
        else {
            try { Platform.runLater(showAlert); } catch (Throwable ignored) { }
        }
        if (navigateHomeAfterDownload && pc != null) pc.showHome();
    }

    @FXML public void onCancel() { if (pc != null) pc.showHome(); }

    private double parseScaleFromFieldOrDefault() {
        if (scalingFactorField == null) return scaleFactor > 0 ? scaleFactor : 1.0;
        String text = scalingFactorField.getText();
        if (text == null || text.isBlank()) return 1.0;
        try {
            double parsed = Double.parseDouble(text.trim().replace(",", "."));
            return (parsed > 0) ? parsed : 1.0;
        } catch (NumberFormatException e) { return 1.0; }
    }

    private String scaleInformalAmount(String amount, double scale) {
        if (amount == null || amount.isEmpty()) return "";
        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(amount);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;
        while (matcher.find()) {
            result.append(amount, lastEnd, matcher.start());
            try {
                double val = Double.parseDouble(matcher.group());
                double scaledVal = val * scale;
                String formatted = (scaledVal % 1 == 0) ? String.valueOf((long) scaledVal) : String.format("%.2f", scaledVal).replace(",", ".");
                result.append(formatted);
            } catch (NumberFormatException e) { result.append(matcher.group()); }
            lastEnd = matcher.end();
        }
        result.append(amount.substring(lastEnd));
        return result.toString();
    }

    private static String nullSafe(String s) { return (s == null) ? "" : s; }
}
