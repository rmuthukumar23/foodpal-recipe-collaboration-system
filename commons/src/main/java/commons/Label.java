package commons;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "labels")
public class Label {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @JsonProperty("category")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private LabelCategory category;

    public enum LabelCategory {
        DIETARY, PROTEIN, CUISINE, MEAL_TYPE, LANGUAGE, COOKING_TIME
    }

    public Label() { }
    public Label(String name, LabelCategory category) { this.name = name; this.category = category; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LabelCategory getCategory() { return category; }
    public void setCategory(LabelCategory category) { this.category = category; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Label)) return false;
        Label label = (Label) o;
        return Objects.equals(id, label.id) && Objects.equals(name, label.name);
    }

    @Override
    public int hashCode() { return Objects.hash(id, name); }

    @Override
    public String toString() {
        return "Label{" + "id=" + id + ", name='" + name + '\'' + ", category=" + category + '}';
    }
}