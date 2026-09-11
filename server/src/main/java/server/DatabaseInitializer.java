package server;

import commons.*;
import commons.Label.LabelCategory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import server.database.GlobalIngredientRepository;
import server.database.LabelRepository;
import server.database.RecipeRepository;

import static server.Helper.addIngredients;

@Configuration
public class DatabaseInitializer {

    /**
     * Initializes the database with test data.
     *
     * @param recipeRepository the recipe repository
     * @param globalRepository the global ingredient repository
     * @param labelRepository the label repository
     * @return a command line runner
     */
    @Bean
    public CommandLineRunner initDatabase(RecipeRepository recipeRepository,
                                          GlobalIngredientRepository globalRepository,
                                          LabelRepository labelRepository) {
        return args -> {
            System.out.println("=== Initializing database with test data ===");

            initializeDefaultLabels(labelRepository);
            System.out.println("=== Default labels initialized ===");

            initializeGlobalIngredients(globalRepository);
            System.out.println("=== Global ingredients initialized ===");

            initPastaCarbonara(recipeRepository, globalRepository, labelRepository);
            initChocolateChipCookies(recipeRepository, globalRepository, labelRepository);
            initCapreseSalad(recipeRepository, globalRepository, labelRepository);
            initPadThai(recipeRepository, globalRepository, labelRepository);
            initFrenchOnionSoup(recipeRepository, globalRepository, labelRepository);

            System.out.println("=== Database initialization complete ===");
        };
    }

    private void initializeGlobalIngredients(GlobalIngredientRepository globalRepository) {
        createGlobalIngredientIfNotExists("Spaghetti", 370, 13, 1.5, 75, globalRepository);
        createGlobalIngredientIfNotExists("Eggs", 155, 13, 11, 1.1, globalRepository);
        createGlobalIngredientIfNotExists("Pancetta", 460, 16, 43, 0.6, globalRepository);
        createGlobalIngredientIfNotExists("Parmesan Cheese", 431, 38, 29, 4, globalRepository);
        createGlobalIngredientIfNotExists("Bloem", 364, 10, 1, 76, globalRepository);
        createGlobalIngredientIfNotExists("Boter", 717, 0.9, 81, 0.1, globalRepository);
        createGlobalIngredientIfNotExists("Suiker", 387, 0, 0, 100, globalRepository);
        createGlobalIngredientIfNotExists("Chocoladechips", 479, 4, 25, 62, globalRepository);
        createGlobalIngredientIfNotExists("Eieren", 155, 13, 11, 1.1, globalRepository);
        createGlobalIngredientIfNotExists("Tomaten", 18, 0.9, 0.2, 3.9, globalRepository);
        createGlobalIngredientIfNotExists("Basilikum", 23, 3.2, 0.6, 2.7, globalRepository);
        createGlobalIngredientIfNotExists("Mozzarellakugeln", 280, 28, 17, 3.1, globalRepository);
        createGlobalIngredientIfNotExists("Olivenöl", 884, 0, 100, 0, globalRepository);
        createGlobalIngredientIfNotExists("Rice Noodles", 364, 3.4, 0.6, 82, globalRepository);
        createGlobalIngredientIfNotExists("Shrimp", 99, 24, 0.3, 0.2, globalRepository);
        createGlobalIngredientIfNotExists("Peanuts", 567, 26, 49, 16, globalRepository);
        createGlobalIngredientIfNotExists("Bean Sprouts", 30, 3, 0.2, 6, globalRepository);
        createGlobalIngredientIfNotExists("Tamarind Paste", 239, 2.8, 0.6, 63, globalRepository);
        createGlobalIngredientIfNotExists("Onions", 40, 1.1, 0.1, 9, globalRepository);
        createGlobalIngredientIfNotExists("Beef Broth", 15, 1, 0.5, 1, globalRepository);
        createGlobalIngredientIfNotExists("Gruyere Cheese", 413, 29, 32, 0.4, globalRepository);
        createGlobalIngredientIfNotExists("Baguette", 272, 9, 3.3, 53, globalRepository);
        createGlobalIngredientIfNotExists("Butter", 717, 0.9, 81, 0.1, globalRepository);
    }

    private void createGlobalIngredientIfNotExists(String name, int calories, double protein,
                                                   double fat, double carbs,
                                                   GlobalIngredientRepository globalRepository) {
        if (!globalRepository.findByName(name).isPresent()) {
            GlobalIngredient ingredient = new GlobalIngredient(name, calories, protein, fat, carbs);
            globalRepository.save(ingredient);
            System.out.println("Created global ingredient: " + name + " (Calories: " + calories + " kcal/100g)");
        }
    }

    private void addIngredient(Recipe recipe, String name, String amount,
                               GlobalIngredientRepository globalRepository) {
        GlobalIngredient global = globalRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Global ingredient not found: " + name));
        Ingredient ingredient = new Ingredient(global, amount);
        recipe.addIngredient(ingredient);
    }

    private void initPastaCarbonara(RecipeRepository recipeRepository,
                                    GlobalIngredientRepository globalRepository,
                                    LabelRepository labelRepository) {
        Recipe recipe = new Recipe("Pasta Carbonara");
        addIngredients(recipe, "Spaghetti", 500.0, Unit.G, globalRepository);
        addIngredients(recipe, "Eggs", 4.0, Unit.PCS, globalRepository);
        addIngredients(recipe, "Pancetta", 150.0, Unit.G, globalRepository);
        addIngredients(recipe, "Parmesan Cheese", 100.0, Unit.G, globalRepository);
        PreparationStep step1 = new PreparationStep("Boil water and cook spaghetti according to package directions.");
        step1.setStepOrder(1); recipe.addPreparationStep(step1);
        PreparationStep step2 = new PreparationStep("Cook pancetta in a pan until crispy.");
        step2.setStepOrder(2); recipe.addPreparationStep(step2);
        PreparationStep step3 = new PreparationStep("Mix eggs and cheese, then combine with hot pasta and pancetta.");
        step3.setStepOrder(3); recipe.addPreparationStep(step3);
        addLabelToRecipe(recipe, "English", labelRepository);
        addLabelToRecipe(recipe, "Italian", labelRepository);
        addLabelToRecipe(recipe, "15-30 minutes", labelRepository);
        addLabelToRecipe(recipe, "Dinner", labelRepository);
        addLabelToRecipe(recipe, "Pork", labelRepository);
        saveRecipeIfMissing(recipeRepository, recipe);
    }

    private void initChocolateChipCookies(RecipeRepository recipeRepository,
                                          GlobalIngredientRepository globalRepository,
                                          LabelRepository labelRepository) {
        Recipe recipe = new Recipe("Chocoladechip Koekjes");
        addIngredients(recipe, "Bloem", 200.0, Unit.G, globalRepository);
        addIngredients(recipe, "Boter", 200.0, Unit.G, globalRepository);
        addIngredients(recipe, "Suiker", 100.0, Unit.G, globalRepository);
        addIngredients(recipe, "Chocoladechips", 100.0, Unit.G, globalRepository);
        addIngredients(recipe, "Eieren", 2.0, Unit.PCS, globalRepository);
        PreparationStep step1 = new PreparationStep("Meng boter en suiker tot een romig mengsel.");
        step1.setStepOrder(1); recipe.addPreparationStep(step1);
        PreparationStep step2 = new PreparationStep("Voeg eieren en bloem toe, en roer dan de chocoladechips erdoor.");
        step2.setStepOrder(2); recipe.addPreparationStep(step2);
        PreparationStep step3 = new PreparationStep("Bak op 180°C gedurende 12 minuten.");
        step3.setStepOrder(3); recipe.addPreparationStep(step3);
        addLabelToRecipe(recipe, "Dutch", labelRepository);
        addLabelToRecipe(recipe, "30-45 minutes", labelRepository);
        addLabelToRecipe(recipe, "Dessert", labelRepository);
        addLabelToRecipe(recipe, "Vegetarian", labelRepository);
        saveRecipeIfMissing(recipeRepository, recipe);
    }

    private void initCapreseSalad(RecipeRepository recipeRepository,
                                  GlobalIngredientRepository globalRepository,
                                  LabelRepository labelRepository) {
        Recipe recipe = new Recipe("Caprese-Salat");
        addIngredients(recipe, "Tomaten", 4.0, Unit.PCS, globalRepository);
        addIngredients(recipe, "Basilikum", 2.0, Unit.PCS, globalRepository);
        addIngredients(recipe, "Mozzarellakugeln", 200.0, Unit.G, globalRepository);
        addIngredients(recipe, "Olivenöl", 30.0, Unit.ML, globalRepository);
        PreparationStep step1 = new PreparationStep("Schneiden Sie die Mozzarellakugeln und Tomaten in Scheiben.");
        step1.setStepOrder(1); recipe.addPreparationStep(step1);
        PreparationStep step2 = new PreparationStep("Legen Sie sie mit dem Basilikum schichtweise auf einen Teller und beträufeln Sie sie mit Olivenöl.");
        step2.setStepOrder(2); recipe.addPreparationStep(step2);
        addLabelToRecipe(recipe, "German", labelRepository);
        addLabelToRecipe(recipe, "Italian", labelRepository);
        addLabelToRecipe(recipe, "Lunch", labelRepository);
        addLabelToRecipe(recipe, "Vegetarian", labelRepository);
        saveRecipeIfMissing(recipeRepository, recipe);
    }

    private void initPadThai(RecipeRepository recipeRepository,
                             GlobalIngredientRepository globalRepository,
                             LabelRepository labelRepository) {
        Recipe recipe = new Recipe("Pad Thai");
        addIngredients(recipe, "Rice Noodles", 200.0, Unit.G, globalRepository);
        addIngredients(recipe, "Shrimp", 150.0, Unit.G, globalRepository);
        addIngredients(recipe, "Eggs", 2.0, Unit.PCS, globalRepository);
        addIngredients(recipe, "Peanuts", 50.0, Unit.G, globalRepository);
        addIngredients(recipe, "Bean Sprouts", 100.0, Unit.G, globalRepository);
        addIngredients(recipe, "Tamarind Paste", 30.0, Unit.ML, globalRepository);
        PreparationStep step1 = new PreparationStep("Soak rice noodles in warm water for 20 minutes.");
        step1.setStepOrder(1); recipe.addPreparationStep(step1);
        PreparationStep step2 = new PreparationStep("Stir-fry shrimp and eggs in a wok.");
        step2.setStepOrder(2); recipe.addPreparationStep(step2);
        PreparationStep step3 = new PreparationStep("Add noodles, tamarind paste, and bean sprouts. Top with peanuts.");
        step3.setStepOrder(3); recipe.addPreparationStep(step3);
        addLabelToRecipe(recipe, "English", labelRepository);
        addLabelToRecipe(recipe, "Thai", labelRepository);
        addLabelToRecipe(recipe, "45-60 minutes", labelRepository);
        addLabelToRecipe(recipe, "Dinner", labelRepository);
        addLabelToRecipe(recipe, "Seafood", labelRepository);
        saveRecipeIfMissing(recipeRepository, recipe);
    }

    private void initFrenchOnionSoup(RecipeRepository recipeRepository,
                                     GlobalIngredientRepository globalRepository,
                                     LabelRepository labelRepository) {
        Recipe recipe = new Recipe("French Onion Soup");
        addIngredients(recipe, "Onions", 6.0, Unit.PCS, globalRepository);
        addIngredients(recipe, "Beef Broth", 1000.0, Unit.ML, globalRepository);
        addIngredients(recipe, "Gruyere Cheese", 200.0, Unit.G, globalRepository);
        addIngredients(recipe, "Baguette", 1.0, Unit.PCS, globalRepository);
        addIngredients(recipe, "Butter", 50.0, Unit.G, globalRepository);
        PreparationStep step1 = new PreparationStep("Slice onions thinly and caramelize in butter for 30 minutes.");
        step1.setStepOrder(1); recipe.addPreparationStep(step1);
        PreparationStep step2 = new PreparationStep("Add beef broth and simmer for 20 minutes.");
        step2.setStepOrder(2); recipe.addPreparationStep(step2);
        PreparationStep step3 = new PreparationStep("Pour into oven-safe bowls, top with baguette and cheese, then broil until golden.");
        step3.setStepOrder(3); recipe.addPreparationStep(step3);
        addLabelToRecipe(recipe, "English", labelRepository);
        addLabelToRecipe(recipe, "French", labelRepository);
        addLabelToRecipe(recipe, "Dinner", labelRepository);
        addLabelToRecipe(recipe, "Beef", labelRepository);
        saveRecipeIfMissing(recipeRepository, recipe);
    }

    private void saveRecipeIfMissing(RecipeRepository repo, Recipe recipe) {
        if (repo.existsByName(recipe.getName())) {
            System.out.println("Skipped: " + recipe.getName() + " (already exists)");
        } else {
            repo.save(recipe);
            System.out.println("Saved recipe: " + recipe.getName() + " (ID: " + recipe.getId() + ")");
        }
    }

    private void addLabelToRecipe(Recipe recipe, String labelName, LabelRepository labelRepository) {
        Label label = labelRepository.findByName(labelName)
                .orElseThrow(() -> new RuntimeException("Label not found: " + labelName));
        recipe.addLabel(label);
    }

    private void initializeDefaultLabels(LabelRepository labelRepository) {
        createLabelIfNotExists("Vegan", LabelCategory.DIETARY, labelRepository);
        createLabelIfNotExists("Vegetarian", LabelCategory.DIETARY, labelRepository);
        createLabelIfNotExists("Gluten-Free", LabelCategory.DIETARY, labelRepository);
        createLabelIfNotExists("Dairy-Free", LabelCategory.DIETARY, labelRepository);
        createLabelIfNotExists("Nut-Free", LabelCategory.DIETARY, labelRepository);
        createLabelIfNotExists("Keto", LabelCategory.DIETARY, labelRepository);
        createLabelIfNotExists("Low-Carb", LabelCategory.DIETARY, labelRepository);
        createLabelIfNotExists("Halal", LabelCategory.DIETARY, labelRepository);
        createLabelIfNotExists("Kosher", LabelCategory.DIETARY, labelRepository);
        createLabelIfNotExists("Beef", LabelCategory.PROTEIN, labelRepository);
        createLabelIfNotExists("Chicken", LabelCategory.PROTEIN, labelRepository);
        createLabelIfNotExists("Lamb", LabelCategory.PROTEIN, labelRepository);
        createLabelIfNotExists("Pork", LabelCategory.PROTEIN, labelRepository);
        createLabelIfNotExists("Fish", LabelCategory.PROTEIN, labelRepository);
        createLabelIfNotExists("Seafood", LabelCategory.PROTEIN, labelRepository);
        createLabelIfNotExists("Tofu", LabelCategory.PROTEIN, labelRepository);
        createLabelIfNotExists("Eggs", LabelCategory.PROTEIN, labelRepository);
        createLabelIfNotExists("None (Vegetarian)", LabelCategory.PROTEIN, labelRepository);
        createLabelIfNotExists("Italian", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("Chinese", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("Mexican", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("Indian", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("Japanese", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("French", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("Thai", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("Greek", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("American", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("Mediterranean", LabelCategory.CUISINE, labelRepository);
        createLabelIfNotExists("Breakfast", LabelCategory.MEAL_TYPE, labelRepository);
        createLabelIfNotExists("Lunch", LabelCategory.MEAL_TYPE, labelRepository);
        createLabelIfNotExists("Dinner", LabelCategory.MEAL_TYPE, labelRepository);
        createLabelIfNotExists("Snack", LabelCategory.MEAL_TYPE, labelRepository);
        createLabelIfNotExists("Dessert", LabelCategory.MEAL_TYPE, labelRepository);
        createLabelIfNotExists("Appetizer", LabelCategory.MEAL_TYPE, labelRepository);
        createLabelIfNotExists("Brunch", LabelCategory.MEAL_TYPE, labelRepository);
        createLabelIfNotExists("Dutch", LabelCategory.LANGUAGE, labelRepository);
        createLabelIfNotExists("English", LabelCategory.LANGUAGE, labelRepository);
        createLabelIfNotExists("German", LabelCategory.LANGUAGE, labelRepository);
        createLabelIfNotExists("Under 15 minutes", LabelCategory.COOKING_TIME, labelRepository);
        createLabelIfNotExists("15-30 minutes", LabelCategory.COOKING_TIME, labelRepository);
        createLabelIfNotExists("30-45 minutes", LabelCategory.COOKING_TIME, labelRepository);
        createLabelIfNotExists("45-60 minutes", LabelCategory.COOKING_TIME, labelRepository);
        createLabelIfNotExists("60-90 minutes", LabelCategory.COOKING_TIME, labelRepository);
        createLabelIfNotExists("90-120 minutes", LabelCategory.COOKING_TIME, labelRepository);
        createLabelIfNotExists("Over 120 minutes", LabelCategory.COOKING_TIME, labelRepository);
    }

    private void createLabelIfNotExists(String name, LabelCategory category,
                                        LabelRepository labelRepository) {
        if (!labelRepository.existsByName(name)) {
            labelRepository.save(new Label(name, category));
        }
    }
}