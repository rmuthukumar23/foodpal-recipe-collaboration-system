package server.database;

import commons.GlobalIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GlobalIngredientRepository extends JpaRepository<GlobalIngredient, Long> {
    Optional<GlobalIngredient> findByName(String name);
}
