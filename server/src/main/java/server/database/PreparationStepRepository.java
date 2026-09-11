package server.database;

import commons.PreparationStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PreparationStepRepository extends JpaRepository<PreparationStep, String> {
    @Query("SELECT MAX(p.stepOrder) FROM PreparationStep p WHERE p.recipe.id = :recipeId")
    Integer findMaxStepOrderForRecipe(Long recipeId);
}