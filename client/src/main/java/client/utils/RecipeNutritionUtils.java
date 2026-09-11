package client.utils;

import commons.Ingredient;
import commons.Recipe;
import commons.Unit;

import java.util.Set;

public final class RecipeNutritionUtils {
    private RecipeNutritionUtils() {}

    public static final class NutritionTotals {
        public final double totalWeightGrams;
        public final double kcal;
        public final double proteinG;
        public final double carbsG;
        public final double fatG;
        public NutritionTotals(double totalWeightGrams, double kcal, double proteinG, double carbsG, double fatG) {
            this.totalWeightGrams = totalWeightGrams;
            this.kcal = kcal;
            this.proteinG = proteinG;
            this.carbsG = carbsG;
            this.fatG = fatG;
        }
    }

    public static NutritionTotals aggregateScaledNutrition(Recipe recipe, double scaleFactor) {
        if (recipe == null || recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return new NutritionTotals(0, 0, 0, 0, 0);
        }
        if (scaleFactor <= 0) scaleFactor = 1.0;
        double totalWeightG = 0.0;
        double totalKcal = 0.0;
        double totalProtein = 0.0;
        double totalCarbs = 0.0;
        double totalFat = 0.0;
        Set<Ingredient> ingredients = recipe.getIngredients();
        for (Ingredient i : ingredients) {
            if (i == null) continue;
            Double amountValue = i.getAmountValue();
            Unit unit = i.getUnit();
            Integer kcalPer100 = i.getCalories();
            Double proteinPer100 = i.getProtein();
            Double carbsPer100 = i.getCarbs();
            Double fatPer100 = i.getFat();
            if (amountValue == null || unit == null) continue;
            if (kcalPer100 == null || proteinPer100 == null || carbsPer100 == null || fatPer100 == null) continue;
            double scaledAmount = amountValue * scaleFactor;
            double weightGrams = toApproxGrams(scaledAmount, unit);
            if (weightGrams <= 0) continue;
            totalWeightG += weightGrams;
            double factor = weightGrams / 100.0;
            totalKcal += factor * kcalPer100;
            totalProtein += factor * proteinPer100;
            totalCarbs += factor * carbsPer100;
            totalFat += factor * fatPer100;
        }
        return new NutritionTotals(totalWeightG, totalKcal, totalProtein, totalCarbs, totalFat);
    }

    public static int inferServings(double totalWeightGrams, double gramsPerServing) {
        if (gramsPerServing <= 0) gramsPerServing = 250.0;
        if (totalWeightGrams <= 0) return 1;
        long rounded = Math.round(totalWeightGrams / gramsPerServing);
        return (int) Math.max(1, rounded);
    }

    public static double toApproxGrams(double amount, Unit unit) {
        if (amount <= 0 || unit == null) return 0.0;
        return switch (unit) {
            case G -> amount;
            case KG -> amount * 1000.0;
            case ML -> amount;
            case L -> amount * 1000.0;
            case TSP -> amount * 5.0;
            case TBSP -> amount * 15.0;
            case CUP -> amount * 240.0;
            case PCS -> amount * 50.0;
        };
    }

    public static String fmt(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "0";
        if (Math.abs(v - Math.rint(v)) < 1e-9) return String.valueOf((long) Math.rint(v));
        return String.format("%.2f", v);
    }
}