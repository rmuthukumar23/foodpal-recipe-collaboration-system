package server.api;

import commons.GlobalIngredient;
import commons.Ingredient;
import commons.PreparationStep;
import commons.Recipe;
import commons.Label;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import server.database.GlobalIngredientRepository;
import server.database.RecipeRepository;
import server.database.LabelRepository;

import java.util.*;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {
    private final RecipeRepository recipeRepository;
    private final GlobalIngredientRepository globalIngredientRepository;
    private final LabelRepository labelRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RecipeController(RecipeRepository recipeRepository,
                            GlobalIngredientRepository globalIngredientRepository,
                            LabelRepository labelRepository,
                            SimpMessagingTemplate messagingTemplate) {
        this.recipeRepository = recipeRepository;
        this.globalIngredientRepository = globalIngredientRepository;
        this.labelRepository = labelRepository;
        this.messagingTemplate = messagingTemplate;
    }

    private void notifyClients(String destination, String message) {
        if (messagingTemplate != null) messagingTemplate.convertAndSend(destination, message);
    }

    private void resolveGlobalIngredient(Ingredient ingredient) {
        String name = ingredient.getName();
        if (name == null || name.trim().isEmpty()) return;
        GlobalIngredient global = globalIngredientRepository.findByName(name)
                .orElseGet(() -> globalIngredientRepository.save(new GlobalIngredient(name)));
        ingredient.setGlobalIngredient(global);
    }

    @GetMapping("/")
    public List<Recipe> getRecipes() { return recipeRepository.findAll(); }

    @GetMapping("/{id}")
    public Recipe getRecipe(@PathVariable long id) { return recipeRepository.findById(id).orElse(null); }

    @PostMapping("/")
    public Recipe addRecipe(@RequestBody Recipe recipe) {
        processRecipeIngredients(recipe);
        processRecipePreparationSteps(recipe);
        Recipe saved = recipeRepository.save(recipe);
        notifyClients("/topic/recipes", "REFRESH");
        return saved;
    }

    private void processRecipeIngredients(Recipe recipe) {
        if (recipe.getIngredients() != null) {
            recipe.getIngredients().forEach(ingredient -> {
                ingredient.setRecipe(recipe);
                resolveGlobalIngredient(ingredient);
            });
        }
    }

    private void processRecipePreparationSteps(Recipe recipe) {
        if (recipe.getPreparationSteps() != null) {
            int stepOrderIndex = 0;
            for (PreparationStep preparationStep : recipe.getPreparationSteps()) {
                preparationStep.setRecipe(recipe);
                preparationStep.setStepOrder(stepOrderIndex++);
            }
        }
    }

    @PutMapping("/{id}")
    @Transactional
    public Recipe updateRecipe(@PathVariable long id, @RequestBody Recipe recipe) {
        Optional<Recipe> oldRecipeOpt = recipeRepository.findById(id);
        if (oldRecipeOpt.isEmpty()) return null;
        Recipe existing = oldRecipeOpt.get();
        try {
            updateExistingRecipe(existing, recipe);
            Recipe saved = recipeRepository.save(existing);
            notifyClients("/topic/recipes", "REFRESH");
            notifyClients("/topic/recipe/" + id, "REFRESH");
            return saved;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update recipe: " + e.getMessage(), e);
        }
    }

    private void updateExistingRecipe(Recipe existing, Recipe updated) {
        existing.clearIngredients();
        existing.clearPreparationSteps();
        recipeRepository.saveAndFlush(existing);
        existing.setName(updated.getName());
        copyIngredientsToRecipe(updated, existing);
        copyPreparationStepsToRecipe(updated, existing);
    }

    private void copyIngredientsToRecipe(Recipe source, Recipe target) {
        if (source.getIngredients() != null) {
            for (Ingredient ingredient : source.getIngredients()) {
                Ingredient newIngredient = new Ingredient();
                newIngredient.setName(ingredient.getName());
                newIngredient.setAmount(ingredient.getAmount());
                newIngredient.setAmountValue(ingredient.getAmountValue());
                newIngredient.setUnit(ingredient.getUnit());
                resolveGlobalIngredient(newIngredient);
                target.addIngredient(newIngredient);
            }
        }
    }

    private void copyPreparationStepsToRecipe(Recipe source, Recipe target) {
        if (source.getPreparationSteps() != null) {
            int order = 0;
            for (PreparationStep step : source.getPreparationSteps()) {
                PreparationStep newStep = new PreparationStep();
                newStep.setDescription(step.getDescription());
                newStep.setStepOrder(order++);
                target.addPreparationStep(newStep);
            }
        }
    }

    @DeleteMapping("/{id}")
    public boolean deleteRecipe(@PathVariable long id) {
        if (recipeRepository.existsById(id)) {
            recipeRepository.deleteById(id);
            notifyClients("/topic/recipes","REFRESH");
            return true;
        }
        return false;
    }

    @PostMapping("/{id}/clone")
    public Recipe cloneRecipe(@PathVariable long id, @RequestBody String newName) {
        Optional<Recipe> oldRecipeOpt = recipeRepository.findById(id);
        if (oldRecipeOpt.isEmpty()) return null;
        Recipe oldRecipe = oldRecipeOpt.get();
        Recipe newRecipe = new Recipe(newName);
        cloneLabels(oldRecipe, newRecipe);
        cloneIngredients(oldRecipe, newRecipe);
        clonePreparationSteps(oldRecipe, newRecipe);
        Recipe saved = recipeRepository.save(newRecipe);
        notifyClients("/topic/recipes", "REFRESH");
        return saved;
    }

    private void cloneIngredients(Recipe source, Recipe target) {
        if (source.getIngredients() != null) {
            for (Ingredient oldIngredient : source.getIngredients()) {
                Ingredient newIngredient = new Ingredient();
                newIngredient.setName(oldIngredient.getName());
                newIngredient.setAmount(oldIngredient.getAmount());
                newIngredient.setAmountValue(oldIngredient.getAmountValue());
                newIngredient.setUnit(oldIngredient.getUnit());
                newIngredient.setGlobalIngredient(oldIngredient.getGlobalIngredient());
                target.addIngredient(newIngredient);
            }
        }
    }

    private void clonePreparationSteps(Recipe source, Recipe target) {
        if (source.getPreparationSteps() != null) {
            for (PreparationStep oldStep : source.getPreparationSteps()) {
                PreparationStep newStep = new PreparationStep();
                newStep.setDescription(oldStep.getDescription());
                target.addPreparationStep(newStep);
            }
        }
    }

    private void cloneLabels(Recipe source, Recipe target) {
        if (source.getLabels() != null) target.setLabels(new HashSet<>(source.getLabels()));
    }

    @GetMapping("/search")
    public List<Recipe> searchRecipes(@RequestParam("name") String search) {
        return recipeRepository.findByNameContaining(search);
    }

    @PutMapping("/{id}/ingredients")
    public Recipe updateIngredients(@PathVariable Long id, @RequestBody Set<Ingredient> ingredients) {
        Recipe recipe = recipeRepository.findById(id).orElse(null);
        if (recipe == null) return null;
        recipe.clearIngredients();
        if (ingredients != null) {
            for (Ingredient ingredient : ingredients) {
                resolveGlobalIngredient(ingredient);
                recipe.addIngredient(ingredient);
            }
        }
        Recipe saved = recipeRepository.save(recipe);
        notifyClients("/topic/recipe/" + id, "REFRESH");
        return saved;
    }

    @PutMapping("/{id}/preparation")
    public Recipe updatePreparation(@PathVariable Long id, @RequestBody List<PreparationStep> preparationSteps) {
        Recipe recipe = recipeRepository.findById(id).orElse(null);
        if (recipe == null) return null;
        recipe.clearPreparationSteps();
        if (preparationSteps != null) {
            for (PreparationStep step : preparationSteps) recipe.addPreparationStep(step);
        }
        Recipe saved = recipeRepository.save(recipe);
        notifyClients("/topic/recipe/" + id, "REFRESH");
        return saved;
    }

    @PostMapping("/{id}/labels")
    public Recipe addLabelsToRecipe(@PathVariable Long id, @RequestBody Set<String> labelNames) {
        Recipe recipe = recipeRepository.findById(id).orElse(null);
        if (recipe == null) return null;
        recipe.clearLabels();
        for (String labelName : labelNames) recipe.addLabel(findOrCreateLabel(labelName));
        Recipe saved = recipeRepository.save(recipe);
        notifyClients("/topic/recipes", "REFRESH");
        notifyClients("/topic/recipe/" + id, "REFRESH");
        return saved;
    }

    private Label findOrCreateLabel(String labelName) {
        return labelRepository.findByName(labelName).orElseGet(() ->
                labelRepository.save(new Label(labelName, determineLabelCategory(labelName))));
    }

    private Label.LabelCategory determineLabelCategory(String labelName) {
        if (labelName.matches("Vegan|Vegetarian|Gluten-Free|Dairy-Free|Keto|Paleo|Pescatarian|Nut-Free|Low-Carb|Halal|Kosher")) return Label.LabelCategory.DIETARY;
        if (labelName.matches("Beef|Chicken|Lamb|Pork|Fish|Seafood|Tofu|Eggs|None.*")) return Label.LabelCategory.PROTEIN;
        if (labelName.matches("Italian|Chinese|Mexican|Indian|Japanese|French|Thai|Mediterranean|American|Korean|Greek|Spanish")) return Label.LabelCategory.CUISINE;
        if (labelName.matches("Breakfast|Lunch|Dinner|Snack|Dessert|Appetizer|Brunch")) return Label.LabelCategory.MEAL_TYPE;
        if (labelName.matches("Dutch|English|German")) return Label.LabelCategory.LANGUAGE;
        if (labelName.contains("minutes") || labelName.contains("Under") || labelName.contains("Over")) return Label.LabelCategory.COOKING_TIME;
        return Label.LabelCategory.DIETARY;
    }

    @GetMapping("/search/advanced")
    public List<Recipe> advancedSearch(@RequestParam(required = false) String name,
                                       @RequestParam(required = false) String ingredient,
                                       @RequestParam(required = false) String step) {
        List<Recipe> matchingRecipes = new ArrayList<>();
        for (Recipe recipe : recipeRepository.findAll()) {
            if (matchesAdvancedSearchCriteria(recipe, name, ingredient, step)) matchingRecipes.add(recipe);
        }
        return matchingRecipes;
    }

    private boolean matchesAdvancedSearchCriteria(Recipe recipe, String name, String ingredient, String step) {
        return matchesNameCriteria(recipe, name) && matchesIngredientCriteria(recipe, ingredient) && matchesStepCriteria(recipe, step);
    }

    private boolean matchesNameCriteria(Recipe recipe, String name) {
        if (name == null || name.trim().isEmpty()) return true;
        return recipe.getName() != null && recipe.getName().toLowerCase().contains(name.toLowerCase().trim());
    }

    private boolean matchesIngredientCriteria(Recipe recipe, String ingredient) {
        if (ingredient == null || ingredient.trim().isEmpty()) return true;
        if (recipe.getIngredients() == null) return false;
        String searchTerm = ingredient.toLowerCase().trim();
        for (Ingredient ing : recipe.getIngredients()) {
            if (ing.getName() != null && ing.getName().toLowerCase().contains(searchTerm)) return true;
        }
        return false;
    }

    private boolean matchesStepCriteria(Recipe recipe, String step) {
        if (step == null || step.trim().isEmpty()) return true;
        if (recipe.getPreparationSteps() == null) return false;
        String searchTerm = step.toLowerCase().trim();
        for (PreparationStep prepStep : recipe.getPreparationSteps()) {
            if (prepStep.getDescription() != null && prepStep.getDescription().toLowerCase().contains(searchTerm)) return true;
        }
        return false;
    }

    @GetMapping("/{id}/print")
    public String getPrintable(@PathVariable Long id, @RequestParam(defaultValue = "1.0") double scale) {
        Optional<Recipe> recipeOpt = recipeRepository.findById(id);
        if (recipeOpt.isEmpty()) return null;
        Recipe recipe = recipeOpt.get();
        StringBuilder sb = new StringBuilder();
        appendRecipeTitle(sb, recipe);
        appendIngredients(sb, recipe, scale);
        appendPreparationSteps(sb, recipe);
        return sb.toString();
    }

    private void appendIngredients(StringBuilder sb, Recipe recipe, double scale) {
        sb.append("Ingredients");
        if (scale != 1.0) sb.append(" (Scaled x").append(scale).append(")");
        sb.append(":\n");
        if (recipe.getIngredients() != null && !recipe.getIngredients().isEmpty()) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                String amountPrefix = "";
                if (ingredient.getAmountValue() != null && ingredient.getUnit() != null) {
                    Ingredient scaledIng = new Ingredient(ingredient.getName(), ingredient.getAmountValue() * scale, ingredient.getUnit());
                    amountPrefix = scaledIng.getAmount() + " ";
                } else if (ingredient.getAmount() != null) {
                    amountPrefix = ingredient.getAmount() + " ";
                }
                sb.append("- ").append(amountPrefix).append(ingredient.getName()).append("\n");
            }
        } else sb.append("- None\n");
    }

    private void appendRecipeTitle(StringBuilder sb, Recipe recipe) {
        sb.append("Recipe: ").append(recipe.getName()).append("\n\n");
    }

    private void appendPreparationSteps(StringBuilder sb, Recipe recipe) {
        sb.append("\nPreparation:\n");
        if (recipe.getPreparationSteps() != null && !recipe.getPreparationSteps().isEmpty()) {
            int stepNum = 1;
            for (PreparationStep step : recipe.getPreparationSteps()) {
                sb.append(stepNum++).append(". ").append(step.getDescription()).append("\n");
            }
        } else sb.append("- None\n");
    }

    @GetMapping("/usage-count/{id}")
    public long getUsageCount(@PathVariable Long id) {
        return recipeRepository.countByIngredientsGlobalIngredientId(id);
    }
}