package commons;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonBackReference;
import java.util.Objects;

@Entity
@Table(name = "preparation_steps")
public class PreparationStep {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JsonProperty("id")
    private String id;

    @JsonProperty("description")
    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @JsonProperty("stepOrder")
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @ManyToOne
    @JoinColumn(name = "recipe_id", nullable = false)
    @JsonBackReference("recipe-steps")
    private Recipe recipe;

    public PreparationStep() { }
    public PreparationStep(String description) { this.description = description; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer stepOrder) { this.stepOrder = stepOrder; }
    public Recipe getRecipe() { return recipe; }
    public void setRecipe(Recipe recipe) { this.recipe = recipe; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PreparationStep that = (PreparationStep) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hashCode(id); }

    @Override
    public String toString() {
        return "PreparationStep{" + "id='" + id + '\'' + ", description='" + description + '\'' + ", stepOrder=" + stepOrder + ", recipe=" + recipe + '}';
    }
}