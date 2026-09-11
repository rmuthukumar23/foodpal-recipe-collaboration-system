package commons;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

@Entity
@Table(name = "ingredients", uniqueConstraints = @UniqueConstraint(columnNames = {"recipe_id", "global_ingredient_id"}))
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty("id")
    private String id;

    @ManyToOne
    @JoinColumn(name = "global_ingredient_id", nullable = false)
    @JsonIgnore
    private GlobalIngredient globalIngredient;

    @Transient private String tempName;
    @Transient private Integer tempCalories;
    @Transient private Double tempProtein;
    @Transient private Double tempFat;
    @Transient private Double tempCarbs;

    @ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false)
    @JsonBackReference("recipe-ingredients")
    private Recipe recipe;

    @JsonProperty("amount")
    @Column(name = "amount")
    private String amount;

    @JsonProperty("amountValue")
    @Column(name = "amount_value")
    private Double amountValue;

    @JsonProperty("unit")
    @Enumerated(EnumType.STRING)
    @Column(name = "unit")
    private Unit unit;

    public Ingredient() { }
    public Ingredient(String name, Double amountValue, Unit unit) {
        this.tempName = name; this.amountValue = amountValue; this.unit = unit; standardiseAmountString();
    }
    public Ingredient(String name, String amount, Recipe recipe) {
        this.tempName = name; this.amount = amount; this.recipe = recipe;
    }
    public Ingredient(GlobalIngredient globalIngredient, Double amountValue, Unit unit) {
        this.globalIngredient = globalIngredient; this.amountValue = amountValue; this.unit = unit;
        if (globalIngredient != null) this.tempName = globalIngredient.getName();
        standardiseAmountString();
    }
    public Ingredient(String name, String amount) { this.tempName = name; this.amount = amount; }
    public Ingredient(GlobalIngredient globalIngredient, String amount) {
        this.globalIngredient = globalIngredient; this.amount = amount;
        if (globalIngredient != null) this.tempName = globalIngredient.getName();
    }

    @JsonProperty("name")
    public String getName() { return globalIngredient != null ? globalIngredient.getName() : tempName; }
    @JsonProperty("name")
    public void setName(String name) { this.tempName = name; }
    @JsonProperty("calories")
    public Integer getCalories() { return globalIngredient != null ? globalIngredient.getCalories() : tempCalories; }
    @JsonProperty("calories")
    public void setCalories(Integer calories) { this.tempCalories = calories; }
    @JsonProperty("protein")
    public Double getProtein() { return globalIngredient != null ? globalIngredient.getProtein() : tempProtein; }
    @JsonProperty("protein")
    public void setProtein(Double protein) { this.tempProtein = protein; }
    @JsonProperty("fat")
    public Double getFat() { return globalIngredient != null ? globalIngredient.getFat() : tempFat; }
    @JsonProperty("fat")
    public void setFat(Double fat) { this.tempFat = fat; }
    @JsonProperty("carbs")
    public Double getCarbs() { return globalIngredient != null ? globalIngredient.getCarbs() : tempCarbs; }
    @JsonProperty("carbs")
    public void setCarbs(Double carbs) { this.tempCarbs = carbs; }

    public GlobalIngredient getGlobalIngredient() { return globalIngredient; }
    public void setGlobalIngredient(GlobalIngredient globalIngredient) {
        this.globalIngredient = globalIngredient;
        if (globalIngredient != null) this.tempName = globalIngredient.getName();
    }
    public Integer getTempCalories() { return tempCalories; }
    public Double getTempProtein() { return tempProtein; }
    public Double getTempFat() { return tempFat; }
    public Double getTempCarbs() { return tempCarbs; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }
    public Double getAmountValue() { return amountValue; }
    public void setAmountValue(Double amountValue) { this.amountValue = amountValue; standardiseAmountString(); }
    public Unit getUnit() { return unit; }
    public void setUnit(Unit unit) { this.unit = unit; standardiseAmountString(); }
    public String getAmount() { return amount; }
    @JsonSetter("amount")
    public void setAmount(String amount) { this.amount = amount; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id) && Objects.equals(globalIngredient, that.globalIngredient)
                && Objects.equals(tempName, that.tempName) && Objects.equals(tempCalories, that.tempCalories)
                && Objects.equals(tempProtein, that.tempProtein) && Objects.equals(tempFat, that.tempFat)
                && Objects.equals(tempCarbs, that.tempCarbs) && Objects.equals(recipe, that.recipe)
                && Objects.equals(amount, that.amount) && Objects.equals(amountValue, that.amountValue) && unit == that.unit;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, globalIngredient, tempName, tempCalories, tempProtein, tempFat, tempCarbs, recipe, amount, amountValue, unit);
    }

    @Override
    public String toString() {
        return "Ingredient{" + "id='" + id + '\'' + ", globalIngredient=" + globalIngredient + ", tempName='" + tempName + '\''
                + ", tempCalories=" + tempCalories + ", tempProtein=" + tempProtein + ", tempFat=" + tempFat
                + ", tempCarbs=" + tempCarbs + ", recipe=" + recipe + ", amount='" + amount + '\''
                + ", amountValue=" + amountValue + ", unit=" + unit + '}';
    }

    private void standardiseAmountString() {
        if (amountValue == null || unit == null) return;
        if (unit == Unit.G && amountValue >= 1000) { amountValue /= 1000; unit = Unit.KG; }
        else if (unit == Unit.KG && amountValue < 1 && amountValue > 0) { amountValue *= 1000; unit = Unit.G; }
        else if (unit == Unit.ML && amountValue >= 1000) { amountValue /= 1000; unit = Unit.L; }
        else if (unit == Unit.L && amountValue < 1 && amountValue > 0) { amountValue *= 1000; unit = Unit.ML; }
        String valueStr = (amountValue % 1 == 0) ? String.valueOf(amountValue.longValue()) : String.valueOf(amountValue);
        this.amount = valueStr + " " + unit.getCode();
    }
}