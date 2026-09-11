package server.api;

import commons.PreparationStep;
import commons.Recipe;
import org.springframework.web.bind.annotation.*;
import server.database.PreparationStepRepository;
import server.database.RecipeRepository;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/preparation-steps")
public class PreparationStepController {
    private final PreparationStepRepository preparationStepRepository;
    private final RecipeRepository recipeRepository;

    public PreparationStepController(PreparationStepRepository preparationStepRepository, RecipeRepository recipeRepository) {
        this.preparationStepRepository = preparationStepRepository;
        this.recipeRepository = recipeRepository;
    }

    @GetMapping("/")
    public List<PreparationStep> getAllPreparationSteps() {
        return preparationStepRepository.findAll();
    }

    @GetMapping("/{id}")
    public PreparationStep getPreparationStepById(@PathVariable String id) {
        return preparationStepRepository.findById(id).orElse(null);
    }

    @GetMapping("/recipe/{recipeId}")
    public List<PreparationStep> getPreparationStepsForRecipe(@PathVariable long recipeId) {
        List<PreparationStep> preparationSteps = new ArrayList<>();
        for (PreparationStep step : preparationStepRepository.findAll()) {
            Recipe recipe = step.getRecipe();
            if (recipe != null && recipe.getId() == recipeId) {
                preparationSteps.add(step);
            }
        }
        return preparationSteps;
    }

    @PostMapping("/recipe/{recipeId}")
    public PreparationStep addPreparationStepToRecipe(@PathVariable Long recipeId, @RequestBody PreparationStep preparationStep) {
        Recipe recipe = recipeRepository.findById(recipeId).orElse(null);
        if (recipe == null) return null;
        if (preparationStep.getStepOrder() == null) {
            Integer maxOrder = preparationStepRepository.findMaxStepOrderForRecipe(recipeId);
            preparationStep.setStepOrder(maxOrder == null ? 1 : maxOrder + 1);
        }
        String description = preparationStep.getDescription();
        preparationStep.setDescription(description == null ? "" : description.trim());
        preparationStep.setRecipe(recipe);
        preparationStep.setId(null);
        return preparationStepRepository.save(preparationStep);
    }

    public PreparationStep updatePreparationStep(@PathVariable String id, @RequestBody PreparationStep updatedStep) {
        PreparationStep existingStep = preparationStepRepository.findById(id).orElse(null);
        if (existingStep == null) return null;
        String description = updatedStep.getDescription();
        existingStep.setDescription(description == null ? "" : description.trim());
        if (updatedStep.getStepOrder() != null) existingStep.setStepOrder(updatedStep.getStepOrder());
        return preparationStepRepository.save(existingStep);
    }

    @DeleteMapping("/{id}")
    public void deletePreparationStepById(@PathVariable String id) {
        PreparationStep existingStep = preparationStepRepository.findById(id).orElse(null);
        if (existingStep != null) preparationStepRepository.delete(existingStep);
    }
}