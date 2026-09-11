package server.api;

import commons.GlobalIngredient;
import commons.Ingredient;
import commons.Label;
import commons.PreparationStep;
import commons.Recipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import server.database.GlobalIngredientRepository;
import server.database.LabelRepository;
import server.database.RecipeRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;

public class RecipeControllerTest {
    private RecipeRepository recipeRepository;
    private GlobalIngredientRepository globalIngredientRepository;
    private LabelRepository labelRepository;
    private SimpMessagingTemplate messagingTemplate;
    private RecipeController controller;

    @BeforeEach
    public void setUp() {
        recipeRepository = mock(RecipeRepository.class);
        globalIngredientRepository = mock(GlobalIngredientRepository.class);
        labelRepository = mock(LabelRepository.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        controller = new RecipeController(recipeRepository, globalIngredientRepository, labelRepository, messagingTemplate);
    }

    @Test
    public void getRecipesReturnsAllFromRepository() {
        Recipe r = new Recipe("R"); when(recipeRepository.findAll()).thenReturn(List.of(r));
        List<Recipe> result = controller.getRecipes();
        assertEquals(1, result.size()); assertSame(r, result.getFirst()); verify(recipeRepository).findAll();
    }

    @Test
    public void addRecipeAssignsStepOrderAndResolvesGlobalIngredients() {
        GlobalIngredient existingGlobal = new GlobalIngredient("Salt");
        when(globalIngredientRepository.findByName("Salt")).thenReturn(Optional.of(existingGlobal));
        when(recipeRepository.save(any(Recipe.class))).then(returnsFirstArg());
        Recipe input = new Recipe("Soup"); Ingredient ing = new Ingredient("Salt", "1 tsp"); input.setIngredients(Set.of(ing));
        PreparationStep a = new PreparationStep("Boil water"); PreparationStep b = new PreparationStep("Add salt"); input.getPreparationSteps().addAll(List.of(a, b));
        Recipe saved = controller.addRecipe(input);
        assertNotNull(saved); assertSame(saved, ing.getRecipe()); assertSame(existingGlobal, ing.getGlobalIngredient());
        assertEquals(0, saved.getPreparationSteps().get(0).getStepOrder()); assertEquals(1, saved.getPreparationSteps().get(1).getStepOrder());
        verify(messagingTemplate).convertAndSend("/topic/recipes", "REFRESH");
    }

    @Test
    public void addRecipeCreatesNewGlobalIngredientWhenNotFound() {
        when(globalIngredientRepository.findByName("Pepper")).thenReturn(Optional.empty());
        when(globalIngredientRepository.save(any(GlobalIngredient.class))).then(returnsFirstArg());
        when(recipeRepository.save(any(Recipe.class))).then(returnsFirstArg());
        Recipe input = new Recipe("Salad"); Ingredient ing = new Ingredient("Pepper", "1 tsp"); input.setIngredients(Set.of(ing));
        Recipe saved = controller.addRecipe(input);
        assertNotNull(saved); assertNotNull(ing.getGlobalIngredient()); assertEquals("Pepper", ing.getGlobalIngredient().getName());
    }

    @Test
    public void updateRecipeReturnsNullWhenOldRecipeMissing() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(controller.updateRecipe(1L, new Recipe("X"))); verify(recipeRepository, never()).save(any());
    }

    @Test
    public void updateRecipeCopiesNameIngredientsAndSteps() {
        Recipe existing = new Recipe("Old"); existing.setId(1L); existing.addIngredient(new Ingredient("OldIng", "1")); existing.addPreparationStep(new PreparationStep("OldStep"));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(existing)); when(recipeRepository.saveAndFlush(existing)).thenReturn(existing); when(recipeRepository.save(existing)).thenReturn(existing);
        GlobalIngredient pepperGlobal = new GlobalIngredient("Pepper"); when(globalIngredientRepository.findByName("Pepper")).thenReturn(Optional.of(pepperGlobal));
        Recipe updated = new Recipe("New"); Ingredient newIng = new Ingredient("Pepper", "2 g"); updated.setIngredients(Set.of(newIng)); updated.addPreparationStep(new PreparationStep("New step"));
        Recipe result = controller.updateRecipe(1L, updated);
        assertNotNull(result); assertEquals("New", existing.getName()); assertEquals(1, existing.getIngredients().size()); assertEquals(1, existing.getPreparationSteps().size());
        verify(messagingTemplate).convertAndSend("/topic/recipes", "REFRESH"); verify(messagingTemplate).convertAndSend("/topic/recipe/1", "REFRESH");
    }

    @Test
    public void updateIngredientsReturnsNullWhenRecipeMissing() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(controller.updateIngredients(1L, Set.of(new Ingredient("Salt", "1")))); verify(recipeRepository, never()).save(any());
    }

    @Test
    public void updateIngredientsClearsAndAddsResolvedIngredients() {
        Recipe recipe = new Recipe("R"); recipe.setId(1L); recipe.addIngredient(new Ingredient("Old", "x"));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe)); when(recipeRepository.save(any(Recipe.class))).then(returnsFirstArg());
        when(globalIngredientRepository.findByName("Salt")).thenReturn(Optional.empty()); when(globalIngredientRepository.save(any(GlobalIngredient.class))).then(returnsFirstArg());
        Recipe saved = controller.updateIngredients(1L, Set.of(new Ingredient("Salt", "1 tsp")));
        assertNotNull(saved); assertEquals(1, saved.getIngredients().size()); assertEquals("Salt", saved.getIngredients().iterator().next().getName());
    }

    @Test
    public void updatePreparationClearsAndRebuildsStepOrders() {
        Recipe recipe = new Recipe("R"); recipe.setId(1L); recipe.addPreparationStep(new PreparationStep("Old"));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe)); when(recipeRepository.save(any(Recipe.class))).then(returnsFirstArg());
        Recipe saved = controller.updatePreparation(1L, List.of(new PreparationStep("A"), new PreparationStep("B")));
        assertNotNull(saved); assertEquals(2, saved.getPreparationSteps().size()); assertEquals(0, saved.getPreparationSteps().get(0).getStepOrder()); assertEquals(1, saved.getPreparationSteps().get(1).getStepOrder());
    }

    @Test
    public void addLabelsToRecipeCreatesMissingLabelsWithDeterminedCategory() {
        Recipe recipe = new Recipe("R"); recipe.setId(1L); recipe.addLabel(new Label("Old", Label.LabelCategory.DIETARY));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe)); when(recipeRepository.save(any(Recipe.class))).then(returnsFirstArg());
        Label existing = new Label("English", Label.LabelCategory.LANGUAGE); existing.setId(10L);
        when(labelRepository.findByName("English")).thenReturn(Optional.of(existing)); when(labelRepository.findByName("Italian")).thenReturn(Optional.empty()); when(labelRepository.save(any(Label.class))).then(returnsFirstArg());
        Recipe saved = controller.addLabelsToRecipe(1L, Set.of("English", "Italian"));
        assertNotNull(saved); assertTrue(saved.hasLabel("English")); assertTrue(saved.hasLabel("Italian"));
        ArgumentCaptor<Label> captor = ArgumentCaptor.forClass(Label.class); verify(labelRepository).save(captor.capture()); assertEquals(Label.LabelCategory.CUISINE, captor.getValue().getCategory());
    }

    @Test
    public void advancedSearchFiltersByNameIngredientAndStep() {
        Recipe soup = new Recipe("Chicken Soup"); soup.addIngredient(new Ingredient("Chicken", "1")); soup.addPreparationStep(new PreparationStep("Boil"));
        Recipe steak = new Recipe("Beef Steak"); steak.addIngredient(new Ingredient("Beef", "1")); steak.addPreparationStep(new PreparationStep("Grill"));
        when(recipeRepository.findAll()).thenReturn(List.of(soup, steak));
        List<Recipe> result = controller.advancedSearch("soup", "chick", "boi"); assertEquals(1, result.size()); assertEquals("Chicken Soup", result.getFirst().getName());
    }

    @Test
    public void advancedSearchReturnsAllWhenNoFiltersProvided() {
        Recipe soup = new Recipe("Chicken Soup"); Recipe steak = new Recipe("Beef Steak"); when(recipeRepository.findAll()).thenReturn(List.of(soup, steak));
        assertEquals(2, controller.advancedSearch(null, null, null).size());
    }

    @Test
    public void getPrintableReturnsFormattedText() {
        Recipe recipe = new Recipe("Cake"); recipe.addIngredient(new Ingredient("Flour", "1")); recipe.addIngredient(new Ingredient("Sugar", "1")); recipe.addPreparationStep(new PreparationStep("Mix")); recipe.addPreparationStep(new PreparationStep("Bake"));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(recipe));
        String printable = controller.getPrintable(1L, 1.0);
        assertNotNull(printable); assertTrue(printable.contains("Recipe: Cake")); assertTrue(printable.contains("Ingredients:")); assertTrue(printable.contains("Preparation:")); assertTrue(printable.contains("1. Mix"));
    }

    @Test
    public void getPrintableReturnsNullWhenRecipeNotFound() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty()); assertNull(controller.getPrintable(1L, 1.0));
    }

    @Test
    public void cloneRecipeCopiesIngredientsStepsAndLabelsSavesNewRecipe() {
        Recipe original = new Recipe("Original"); GlobalIngredient global = new GlobalIngredient("Salt"); Ingredient ing = new Ingredient(global, "1"); original.addIngredient(ing); original.addPreparationStep(new PreparationStep("Mix")); original.addLabel(new Label("Italian", Label.LabelCategory.CUISINE));
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(original)); when(recipeRepository.save(any(Recipe.class))).then(returnsFirstArg());
        Recipe cloned = controller.cloneRecipe(1L, "Clone");
        assertNotNull(cloned); assertEquals("Clone", cloned.getName()); assertEquals(1, cloned.getIngredients().size()); assertEquals(1, cloned.getPreparationSteps().size()); assertTrue(cloned.hasLabel("Italian"));
    }

    @Test
    public void cloneRecipeReturnsNullWhenOriginalNotFound() {
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty()); assertNull(controller.cloneRecipe(1L, "Clone")); verify(recipeRepository, never()).save(any());
    }

    @Test
    public void deleteRecipeReturnsTrueWhenRecipeExists() {
        when(recipeRepository.existsById(1L)).thenReturn(true); assertTrue(controller.deleteRecipe(1L)); verify(recipeRepository).deleteById(1L); verify(messagingTemplate).convertAndSend("/topic/recipes", "REFRESH");
    }

    @Test
    public void deleteRecipeReturnsFalseWhenRecipeDoesNotExist() {
        when(recipeRepository.existsById(1L)).thenReturn(false); assertFalse(controller.deleteRecipe(1L)); verify(recipeRepository, never()).deleteById(any());
    }

    @Test
    public void getUsageCountReturnsCorrectCount() {
        when(recipeRepository.countByIngredientsGlobalIngredientId(5L)).thenReturn(3L); assertEquals(3L, controller.getUsageCount(5L));
    }

    @Test
    public void searchRecipesByNameReturnsMatchingRecipes() {
        Recipe pasta = new Recipe("Pasta Carbonara"); when(recipeRepository.findByNameContaining("Past")).thenReturn(List.of(pasta));
        List<Recipe> result = controller.searchRecipes("Past"); assertEquals(1, result.size()); assertEquals("Pasta Carbonara", result.getFirst().getName());
    }
}
