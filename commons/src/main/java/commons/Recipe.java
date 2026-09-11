package commons;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.*;

@Entity
@Table(name = "recipes")
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    @Column(name = "recipe_name", nullable = false, length = 100, unique = true)
    private String name;

    @JsonProperty("favorite")
    @Column(name = "favorite", nullable = false)
    private boolean favorite = false;

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonProperty("ingredients")
    @JsonManagedReference("recipe-ingredients")
    private Set<Ingredient> ingredients = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.MERGE})
    @JoinTable(name = "recipe_labels", joinColumns = @JoinColumn(name = "recipe_id"), inverseJoinColumns = @JoinColumn(name = "label_id"))
    @JsonProperty("labels")
    private Set<Label> labels = new HashSet<>();

    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("stepOrder ASC")
    @JsonProperty("preparationSteps")
    @JsonManagedReference("recipe-steps")
    private List<PreparationStep> preparationSteps = new ArrayList<>();

    public Recipe() { }
    public Recipe(String name) { this.name = name; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    @JsonIgnore
    public String getLanguage() {
        return labels.stream().filter(label -> label.getCategory() == Label.LabelCategory.LANGUAGE)
                .map(Label::getName).findFirst().orElse(null);
    }

    @JsonIgnore
    public Integer getCookingTimeMinutes() {
        return labels.stream().filter(label -> label.getCategory() == Label.LabelCategory.COOKING_TIME)
                .map(this::extractMaxTimeFromLabel).findFirst().orElse(null);
    }

    private Integer extractMaxTimeFromLabel(Label label) {
        String name = label.getName();
        if (name.contains("Under 15")) return 15;
        if (name.contains("15-30")) return 30;
        if (name.contains("30-45")) return 45;
        if (name.contains("45-60")) return 60;
        if (name.contains("60-90")) return 90;
        if (name.contains("90-120")) return 120;
        if (name.contains("Over 120")) return 240;
        return null;
    }

    public Set<Ingredient> getIngredients() { return ingredients; }
    public void setIngredients(Set<Ingredient> ingredients) {
        this.ingredients.clear();
        if (ingredients != null) ingredients.forEach(this::addIngredient);
    }
    public List<PreparationStep> getPreparationSteps() { return preparationSteps; }
    public void addIngredient(Ingredient ingredient) {
        if (ingredient == null) return;
        ingredient.setRecipe(this);
        ingredients.add(ingredient);
    }
    public void removeIngredient(Ingredient ingredient) {
        ingredients.remove(ingredient);
        ingredient.setRecipe(null);
    }
    public void addPreparationStep(PreparationStep step) {
        preparationSteps.add(step);
        step.setRecipe(this);
        step.setStepOrder(preparationSteps.size() - 1);
    }
    public void removePreparationStep(PreparationStep step) {
        preparationSteps.remove(step);
        step.setRecipe(null);
        for (int i = 0; i < preparationSteps.size(); i++) preparationSteps.get(i).setStepOrder(i);
    }
    public Set<Label> getLabels() { return labels; }
    public void setLabels(Set<Label> labels) { this.labels = labels; }
    public void addLabel(Label label) { labels.add(label); }
    public void removeLabel(Label label) { labels.remove(label); }
    public void clearLabels() { labels.clear(); }
    public boolean hasLabel(String labelName) {
        return labels.stream().anyMatch(label -> label.getName().equalsIgnoreCase(labelName));
    }
    public boolean hasAnyLabel(Set<String> labelNames) {
        return labels.stream().anyMatch(label -> labelNames.stream().anyMatch(name -> name.equalsIgnoreCase(label.getName())));
    }
    public void clearIngredients() {
        for (Ingredient i : ingredients) i.setRecipe(null);
        ingredients.clear();
    }
    public void clearPreparationSteps() {
        new ArrayList<>(preparationSteps).forEach(this::removePreparationStep);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Recipe)) return false;
        Recipe recipe = (Recipe) o;
        return Objects.equals(id, recipe.id);
    }
    @Override
    public int hashCode() { return Objects.hash(id); }
    @Override
    public String toString() {
        return "Recipe{" + "id='" + id + '\'' + ", name='" + name + '\'' + ", ingredients=" + ingredients.size()
                + ", steps=" + preparationSteps.size() + ", labels=" + labels.size() + '}';
    }

    @JsonIgnore
    public double getAverageCalories() {
        if (ingredients == null || ingredients.isEmpty()) return 0.0;
        double totalCalories = ingredients.stream().mapToDouble(i -> i.getCalories() != null ? i.getCalories() : 0.0).sum();
        return totalCalories / ingredients.size();
    }

    @JsonIgnore
    public double getCaloricDensity() {
        if (ingredients == null || ingredients.isEmpty()) return 0.0;
        double totalCalories = 0.0;
        double totalWeightGrams = 0.0;
        for (Ingredient i : ingredients) {
            if (i.getCalories() == null || i.getAmountValue() == null || i.getUnit() == null) continue;
            double amount = i.getAmountValue();
            double weightGrams;
            switch (i.getUnit()) {
                case G: weightGrams = amount; break;
                case KG: weightGrams = amount * 1000; break;
                case ML: weightGrams = amount; break;
                case L: weightGrams = amount * 1000; break;
                case TSP: weightGrams = amount * 5; break;
                case TBSP: weightGrams = amount * 15; break;
                case CUP: weightGrams = amount * 240; break;
                case PCS: weightGrams = amount * 50; break;
                default: continue;
            }
            totalCalories += (weightGrams / 100.0) * i.getCalories();
            totalWeightGrams += weightGrams;
        }
        if (totalWeightGrams == 0) return 0.0;
        return (totalCalories / totalWeightGrams) * 100.0;
    }
}