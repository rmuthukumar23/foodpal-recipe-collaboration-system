package commons;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class RecipeTest {
    @Test public void addIngredientSetsRecipeBackReference() {
        Recipe recipe = new Recipe("Soup");
        Ingredient ing = new Ingredient("Salt", "1 tsp");
        recipe.addIngredient(ing);
        assertTrue(recipe.getIngredients().contains(ing));
        assertSame(recipe, ing.getRecipe());
    }

    @Test public void removeIngredientRemovesAndClearsBackReference() {
        Recipe recipe = new Recipe("Soup");
        Ingredient ing = new Ingredient("Salt", "1 tsp");
        recipe.addIngredient(ing);
        recipe.removeIngredient(ing);
        assertFalse(recipe.getIngredients().contains(ing));
        assertNull(ing.getRecipe());
    }

    @Test public void addPreparationStepAssignsRecipeAndSequentialOrder() {
        Recipe recipe = new Recipe("Cake");
        PreparationStep a = new PreparationStep("Mix");
        PreparationStep b = new PreparationStep("Bake");
        recipe.addPreparationStep(a);
        recipe.addPreparationStep(b);
        assertSame(recipe, a.getRecipe());
        assertSame(recipe, b.getRecipe());
        assertEquals(0, a.getStepOrder());
        assertEquals(1, b.getStepOrder());
    }

    @Test public void languageAndCookingTimeAreDerivedFromLabels() {
        Recipe recipe = new Recipe("Pasta");
        recipe.addLabel(new Label("English", Label.LabelCategory.LANGUAGE));
        recipe.addLabel(new Label("15-30 minutes", Label.LabelCategory.COOKING_TIME));
        assertEquals("English", recipe.getLanguage());
        assertEquals(30, recipe.getCookingTimeMinutes());
    }

    @Test public void hasLabelAndHasAnyLabelIgnoreCase() {
        Recipe recipe = new Recipe("Pasta");
        recipe.addLabel(new Label("Vegan", Label.LabelCategory.DIETARY));
        assertTrue(recipe.hasLabel("vegan"));
        assertTrue(recipe.hasAnyLabel(Set.of("keto", "VEGAN")));
    }

    @Test public void clearIngredientsAndStepsDetachChildren() {
        Recipe recipe = new Recipe("Pasta");
        Ingredient ing = new Ingredient("Salt", "1 tsp");
        PreparationStep step = new PreparationStep("Boil water");
        recipe.addIngredient(ing);
        recipe.addPreparationStep(step);
        recipe.clearIngredients();
        recipe.clearPreparationSteps();
        assertTrue(recipe.getIngredients().isEmpty());
        assertTrue(recipe.getPreparationSteps().isEmpty());
        assertNull(ing.getRecipe());
        assertNull(step.getRecipe());
    }
}