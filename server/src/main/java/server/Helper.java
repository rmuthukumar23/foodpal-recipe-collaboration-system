package server;

import commons.GlobalIngredient;
import commons.Ingredient;
import commons.Recipe;
import commons.Unit;
import server.database.GlobalIngredientRepository;

public class Helper {

    /**
     * Adds an ingredient to a recipe, creating a global ingredient if it does not already exist.
     *
     * @param recipe the recipe to which the ingredient is added
     * @param name the name of the global ingredient to find or create
     * @param amount the quantity or measurement of the ingredient used in the recipe
     * @param globalRepo the repository used to look up or persist global ingredients
     */
    public static void addIngredient(Recipe recipe, String name, String amount, GlobalIngredientRepository globalRepo) {
        GlobalIngredient global = globalRepo.findByName(name)
                .orElseGet(() -> globalRepo.save(new GlobalIngredient(name)));
        Ingredient ingredient = new Ingredient(global, amount);
        recipe.addIngredient(ingredient);
    }
    /**
     * Adds an ingredient with a formal amount (value and unit) to a recipe,
     * creating a global ingredient if it does not already exist.
     *
     * @param recipe the recipe to which the ingredient is added
     * @param name the name of the global ingredient to find or create
     * @param amountValue the numerical value of the amount
     * @param unit the unit of measurement
     * @param globalRepo the repository used to look up or persist global ingredients
     */
    public static void addIngredients(Recipe recipe, String name, Double amountValue, Unit unit, GlobalIngredientRepository globalRepo) {
        GlobalIngredient global = globalRepo.findByName(name)
                .orElseGet(() -> globalRepo.save(new GlobalIngredient(name)));

        // This assumes you added the constructor: Ingredient(GlobalIngredient, Double, Unit)
        Ingredient ingredient = new Ingredient(global, amountValue, unit);
        recipe.addIngredient(ingredient);
    }

}
