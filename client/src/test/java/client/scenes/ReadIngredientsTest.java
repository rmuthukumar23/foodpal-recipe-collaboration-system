package client.scenes;

import commons.Ingredient;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReadIngredientsTest {
    @Test
    public void readAndParseNullOrBlankReturnsEmptyList() throws Exception {
        ReadIngredients reader = new ReadIngredients();
        assertTrue(reader.readAndParse(null).isEmpty());
        assertTrue(reader.readAndParse("   ").isEmpty());
        assertTrue(reader.readAndParse("\n\n\t").isEmpty());
    }

    @Test
    public void parsesNamesAndAmountsUsingLastHyphen() throws Exception {
        ReadIngredients reader = new ReadIngredients();
        String input = "Eggs - 2\nSoy-sauce - 1 tbsp\n";
        List<Ingredient> ingredients = reader.readAndParse(input);
        assertEquals(2, ingredients.size());
        assertEquals("Eggs", ingredients.get(0).getName());
        assertEquals("2", ingredients.get(0).getAmount());
        assertEquals("Soy-sauce", ingredients.get(1).getName());
        assertEquals("1 tbsp", ingredients.get(1).getAmount());
    }

    @Test
    public void skipsEmptyLinesAndTrimsWhitespace() throws Exception {
        ReadIngredients reader = new ReadIngredients();
        String input = "\n  Milk - 1L  \n\n  Bread\n\t\n";
        List<Ingredient> ingredients = reader.readAndParse(input);
        assertEquals(2, ingredients.size());
        assertEquals("Milk", ingredients.get(0).getName());
        assertEquals("1 l", ingredients.get(0).getAmount());
        assertEquals("Bread", ingredients.get(1).getName());
        assertEquals("", ingredients.get(1).getAmount());
    }

    @Test
    public void duplicateIngredientNamesAreRejectedCaseInsensitive() {
        ReadIngredients reader = new ReadIngredients();
        ReadIngredients.IngredientsParseException ex = assertThrows(ReadIngredients.IngredientsParseException.class,
                () -> reader.readAndParse("Salt - pinch\nSALT - pinch\n"));
        assertTrue(ex.getMessage().toLowerCase().contains("duplicate"));
    }

    @Test
    public void invalidCharactersInNameAreRejected() {
        ReadIngredients reader = new ReadIngredients();
        assertThrows(ReadIngredients.IngredientsParseException.class, () -> reader.readAndParse("*Salt - pinch\n"));
    }

    @Test
    public void parsePrivateMethodSkipsBlankLinesAndParsesAmounts() throws Exception {
        ReadIngredients reader = new ReadIngredients();
        Method m = ReadIngredients.class.getDeclaredMethod("parse", String.class);
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.List<Ingredient> result = (java.util.List<Ingredient>) m.invoke(reader, "Eggs - 2\n\nMilk - 100ml\n");
        assertEquals(2, result.size());
        assertEquals("100 ml", result.get(1).getAmount());
    }

    @Test
    public void checkValidNameRejectsInvalidCharacters() throws Exception {
        ReadIngredients reader = new ReadIngredients();
        Method m = ReadIngredients.class.getDeclaredMethod("checkValidName", String.class, int.class);
        m.setAccessible(true);
        InvocationTargetException ex = assertThrows(InvocationTargetException.class, () -> m.invoke(reader, "$$$", 0));
        assertEquals(ReadIngredients.IngredientsParseException.class, ex.getCause().getClass());
    }
}