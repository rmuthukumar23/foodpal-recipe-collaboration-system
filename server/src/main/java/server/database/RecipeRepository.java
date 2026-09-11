package server.database;

import org.springframework.data.jpa.repository.JpaRepository;
import commons.Recipe;

import java.util.List;

public interface RecipeRepository extends JpaRepository<Recipe, Long> {
    List<Recipe> findByNameContaining(String name);
    boolean existsByName(String name);
    long countByIngredientsGlobalIngredientId(Long globalIngredientId);
}
