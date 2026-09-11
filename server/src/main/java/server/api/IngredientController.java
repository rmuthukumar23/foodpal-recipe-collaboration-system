package server.api;

import commons.GlobalIngredient;
import commons.Ingredient;
import commons.Recipe;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import server.database.GlobalIngredientRepository;
import server.database.IngredientRepository;
import server.database.RecipeRepository;
import java.util.ArrayList;
import java.util.List;

//import static org.springframework.http.HttpStatus.CONFLICT;

/**
 * REST controller for managing ingredients.
 */
@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {
    private final IngredientRepository ingredientRepository;
    private final RecipeRepository recipeRepository;
    private final GlobalIngredientRepository globalIngredientRepository;

    public IngredientController(IngredientRepository ingredientRepository,
                                RecipeRepository recipeRepository,
                                GlobalIngredientRepository globalIngredientRepository) {
        this.ingredientRepository = ingredientRepository;
        this.recipeRepository = recipeRepository;
        this.globalIngredientRepository = globalIngredientRepository;
    }

    @GetMapping("/")
    public List<Ingredient> getAllIngredients() {
        return ingredientRepository.findAll();
    }

    @GetMapping("/global")
    public List<GlobalIngredient> getAllGlobalIngredients() {
        return globalIngredientRepository.findAll();
    }

    @GetMapping("/global/{id}/used")
    public boolean isGlobalIngredientUsed(@PathVariable Long id) {
        return ingredientRepository.existsByGlobalIngredientId(id);
    }

    @DeleteMapping("/global/{id}")
    @Transactional
    public ResponseEntity<Void> deleteGlobalIngredient(@PathVariable Long id){
        if (!globalIngredientRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        boolean used = ingredientRepository.existsByGlobalIngredientId(id);
        if (used) ingredientRepository.deleteByGlobalIngredientId(id);
        globalIngredientRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public Ingredient getIngredientById(@PathVariable String id) {
        return ingredientRepository.findById(id).orElse(null);
    }

    @GetMapping("/recipe/{recipeId}")
    public List<Ingredient> getIngredientsForRecipe(@PathVariable long recipeId) {
        return recipeRepository.findById(recipeId)
                .map(recipe -> new ArrayList<>(recipe.getIngredients()))
                .orElse(null);
    }

    @PostMapping("/recipe/{recipeId}")
    public Ingredient addIngredientToRecipe(@PathVariable Long recipeId, @RequestBody Ingredient ingredient) {
        Recipe recipe = recipeRepository.findById(recipeId).orElse(null);
        if (recipe == null) return null;
        String nameInput = ingredient.getName();
        if (nameInput != null && !nameInput.trim().isEmpty()) {
            GlobalIngredient global = resolveGlobalIngredient(nameInput, ingredient);
            ingredient.setGlobalIngredient(global);
        } else {
            return null;
        }
        String amount = ingredient.getAmount() != null ? ingredient.getAmount().trim() : "";
        ingredient.setAmount(amount);
        ingredient.setRecipe(recipe);
        ingredient.setId(null);
        return ingredientRepository.save(ingredient);
    }

    @PutMapping("/{id}")
    public Ingredient editIngredient(@PathVariable String id, @RequestBody Ingredient updated) {
        Ingredient existing = ingredientRepository.findById(id).orElse(null);
        if (existing == null) return null;
        if (updated.getName() != null && !updated.getName().trim().isEmpty()) {
            String newName = updated.getName().trim();
            if (!newName.equalsIgnoreCase(existing.getName())) {
                GlobalIngredient newGlobal = resolveGlobalIngredient(newName, updated);
                existing.setGlobalIngredient(newGlobal);
            }
        }
        if (updated.getAmount() != null) existing.setAmount(updated.getAmount().trim());
        return ingredientRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void deleteIngredient(@PathVariable String id) {
        if (ingredientRepository.existsById(id)) ingredientRepository.deleteById(id);
    }

    private GlobalIngredient resolveGlobalIngredient(String name, Ingredient source) {
        String cleanName = name.trim();
        return globalIngredientRepository.findByName(cleanName)
                .orElseGet(() -> {
                    GlobalIngredient newGlobal = new GlobalIngredient();
                    newGlobal.setName(cleanName);
                    if (source.getTempCalories() != null) newGlobal.setCalories(source.getTempCalories());
                    if (source.getTempProtein() != null) newGlobal.setProtein(source.getTempProtein());
                    if (source.getTempFat() != null) newGlobal.setFat(source.getTempFat());
                    if (source.getTempCarbs() != null) newGlobal.setCarbs(source.getTempCarbs());
                    return globalIngredientRepository.save(newGlobal);
                });
    }

    @PutMapping("/global/{id}")
    public ResponseEntity<GlobalIngredient> updateGlobalIngredient(@PathVariable Long id, @RequestBody GlobalIngredient updated) {
        return globalIngredientRepository.findById(id)
                .map(ingredient -> {
                    ingredient.setName(updated.getName());
                    ingredient.setCalories(updated.getCalories());
                    ingredient.setProtein(updated.getProtein());
                    ingredient.setFat(updated.getFat());
                    ingredient.setCarbs(updated.getCarbs());
                    return ResponseEntity.ok(globalIngredientRepository.save(ingredient));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/global")
    public ResponseEntity<GlobalIngredient> addGlobalIngredient(@RequestBody GlobalIngredient ingredient) {
        if (ingredient.getName() == null || ingredient.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (globalIngredientRepository.findByName(ingredient.getName()).isPresent()) {
            return ResponseEntity.status(409).build();
        }
        return ResponseEntity.ok(globalIngredientRepository.save(ingredient));
    }
}