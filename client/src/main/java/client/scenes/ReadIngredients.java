package client.scenes;

import commons.Ingredient;
import commons.Unit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ReadIngredients {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}][\\p{L}\\p{N}\\s'\\-.,()]*$");

    public static class IngredientsParseException extends Exception {
        public IngredientsParseException(String message) { super(message); }
    }

    public List<Ingredient> readAndParse(String text) throws IngredientsParseException {
        if (text == null || text.trim().isEmpty()) return new ArrayList<>();
        return parse(text);
    }

    private List<Ingredient> parse(String text) throws IngredientsParseException {
        String[] lines = text.split("\n");
        List<Ingredient> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String content = line;
            String name = content;
            String amount = "";
            int separatorIndex = content.lastIndexOf("-");
            if (separatorIndex != -1) {
                name = content.substring(0, separatorIndex).trim();
                amount = content.substring(separatorIndex + 1).trim();
            }
            checkValidName(name, i);
            if (seen.contains(name.toLowerCase())) {
                throw new IngredientsParseException("Duplicate ingredient '" + name + "' at line " + (i + 1));
            }
            seen.add(name.toLowerCase());
            Ingredient ingredient = new Ingredient(name, amount);
            parseAmountAndUnit(ingredient, amount);
            result.add(ingredient);
        }
        return result;
    }

    private void checkValidName(String name, int index) throws IngredientsParseException {
        if (name.isEmpty()) throw new IngredientsParseException("Ingredient name cannot be empty at line " + (index + 1));
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IngredientsParseException("Invalid characters in ingredient name at line " + (index + 1) + ": " + name);
        }
    }

    private void parseAmountAndUnit(Ingredient ingredient, String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) return;
        String clean = amountStr.trim().toLowerCase().replace(" ", "");
        Unit bestUnit = null;
        Double bestVal = null;
        int maxLen = -1;
        for (Unit unit : Unit.values()) {
            String suffix = unit.getCode().toLowerCase();
            if (clean.endsWith(suffix)) {
                String numberPart = clean.substring(0, clean.length() - suffix.length());
                try {
                    double val = Double.parseDouble(numberPart);
                    if (suffix.length() > maxLen) {
                        maxLen = suffix.length();
                        bestUnit = unit;
                        bestVal = val;
                    }
                } catch (NumberFormatException e) { }
            }
        }
        if (bestUnit != null) {
            ingredient.setAmountValue(bestVal);
            ingredient.setUnit(bestUnit);
        }
    }
}