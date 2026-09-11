package client.scenes;

public class ReadRecipeName {
    private String name;
    public static class RecipeNameParseException extends Exception {
        public RecipeNameParseException(String message) { super(message); }
    }
    public String readAndParse(String text) throws RecipeNameParseException {
        if (text == null || text.trim().isEmpty()) throw new RecipeNameParseException("Recipe name cannot be empty");
        name = text.trim();
        return name;
    }
    public String getName() { return name; }
    public void clear() { name = null; }
    public boolean hasName() { return name != null && !name.isEmpty(); }
    public static boolean validate(String text) throws RecipeNameParseException { new ReadRecipeName().readAndParse(text); return true; }
}