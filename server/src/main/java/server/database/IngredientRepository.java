package server.database;

import org.springframework.data.jpa.repository.JpaRepository;
import commons.Ingredient;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface IngredientRepository extends JpaRepository<Ingredient, String> {
    boolean existsByGlobalIngredientId(Long globalIngredientId);

    @Modifying
    @Transactional
    @Query("delete from Ingredient i where i.globalIngredient.id = :gid")
    void deleteByGlobalIngredientId(@Param("gid") Long globalIngredientId);
}
