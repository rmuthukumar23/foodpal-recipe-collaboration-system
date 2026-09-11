package server.api;

import commons.Label;
import org.springframework.web.bind.annotation.*;
import server.database.LabelRepository;

import java.util.List;

/**
 * REST controller for managing labels.
 */
@RestController
@RequestMapping("/api/labels")
public class LabelController {
    private final LabelRepository labelRepository;

    public LabelController(LabelRepository labelRepository) {
        this.labelRepository = labelRepository;
    }

    @GetMapping("/")
    public List<Label> getAllLabels() {
        return labelRepository.findAll();
    }

    @GetMapping("/{id}")
    public Label getLabel(@PathVariable Long id) {
        return labelRepository.findById(id).orElse(null);
    }

    @PostMapping("/")
    public Label createLabel(@RequestBody Label label) {
        return labelRepository.save(label);
    }

    @DeleteMapping("/{id}")
    public boolean deleteLabel(@PathVariable Long id) {
        if (labelRepository.existsById(id)) {
            labelRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @PostMapping("/init")
    public List<Label> initializeDefaultLabels() {
        createLabelIfNotExists("Vegan", Label.LabelCategory.DIETARY);
        createLabelIfNotExists("Vegetarian", Label.LabelCategory.DIETARY);
        createLabelIfNotExists("Gluten-Free", Label.LabelCategory.DIETARY);
        createLabelIfNotExists("Dairy-Free", Label.LabelCategory.DIETARY);
        createLabelIfNotExists("Nut-Free", Label.LabelCategory.DIETARY);
        createLabelIfNotExists("Keto", Label.LabelCategory.DIETARY);
        createLabelIfNotExists("Low-Carb", Label.LabelCategory.DIETARY);
        createLabelIfNotExists("Halal", Label.LabelCategory.DIETARY);
        createLabelIfNotExists("Kosher", Label.LabelCategory.DIETARY);
        createLabelIfNotExists("Beef", Label.LabelCategory.PROTEIN);
        createLabelIfNotExists("Chicken", Label.LabelCategory.PROTEIN);
        createLabelIfNotExists("Lamb", Label.LabelCategory.PROTEIN);
        createLabelIfNotExists("Pork", Label.LabelCategory.PROTEIN);
        createLabelIfNotExists("Fish", Label.LabelCategory.PROTEIN);
        createLabelIfNotExists("Seafood", Label.LabelCategory.PROTEIN);
        createLabelIfNotExists("Tofu", Label.LabelCategory.PROTEIN);
        createLabelIfNotExists("Eggs", Label.LabelCategory.PROTEIN);
        createLabelIfNotExists("None (Vegetarian)", Label.LabelCategory.PROTEIN);
        createLabelIfNotExists("Italian", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("Chinese", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("Mexican", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("Indian", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("Japanese", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("French", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("Thai", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("Greek", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("American", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("Mediterranean", Label.LabelCategory.CUISINE);
        createLabelIfNotExists("Breakfast", Label.LabelCategory.MEAL_TYPE);
        createLabelIfNotExists("Lunch", Label.LabelCategory.MEAL_TYPE);
        createLabelIfNotExists("Dinner", Label.LabelCategory.MEAL_TYPE);
        createLabelIfNotExists("Snack", Label.LabelCategory.MEAL_TYPE);
        createLabelIfNotExists("Dessert", Label.LabelCategory.MEAL_TYPE);
        createLabelIfNotExists("Appetizer", Label.LabelCategory.MEAL_TYPE);
        createLabelIfNotExists("Brunch", Label.LabelCategory.MEAL_TYPE);
        createLabelIfNotExists("Dutch", Label.LabelCategory.LANGUAGE);
        createLabelIfNotExists("English", Label.LabelCategory.LANGUAGE);
        createLabelIfNotExists("German", Label.LabelCategory.LANGUAGE);
        createLabelIfNotExists("Under 15 minutes", Label.LabelCategory.COOKING_TIME);
        createLabelIfNotExists("15-30 minutes", Label.LabelCategory.COOKING_TIME);
        createLabelIfNotExists("30-45 minutes", Label.LabelCategory.COOKING_TIME);
        createLabelIfNotExists("45-60 minutes", Label.LabelCategory.COOKING_TIME);
        createLabelIfNotExists("60-90 minutes", Label.LabelCategory.COOKING_TIME);
        createLabelIfNotExists("90-120 minutes", Label.LabelCategory.COOKING_TIME);
        createLabelIfNotExists("Over 120 minutes", Label.LabelCategory.COOKING_TIME);
        return labelRepository.findAll();
    }

    private void createLabelIfNotExists(String name, Label.LabelCategory category) {
        if (!labelRepository.existsByName(name)) {
            labelRepository.save(new Label(name, category));
        }
    }
}