package client.utils;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.ConnectException;
import java.util.List;
import java.util.Set;

import org.glassfish.jersey.client.ClientConfig;
import commons.PreparationStep;
import commons.Ingredient;
import commons.Recipe;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

public class ServerUtilsRecipe {
    private static final String SERVER = "http://localhost:8080/api/";

    public List<Recipe> getRecipes() {
        return ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/")
                .request(APPLICATION_JSON).get(new GenericType<List<Recipe>>() {});
    }

    public Recipe getRecipeById(long id) {
        try { return ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/" + id).request(APPLICATION_JSON).get(Recipe.class); }
        catch (Exception e) { return null; }
    }

    public Recipe addRecipe(Recipe recipe) {
        return ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/")
                .request(APPLICATION_JSON).post(Entity.entity(recipe, APPLICATION_JSON), Recipe.class);
    }

    public boolean deleteRecipe(long id) {
        Response response = ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/" + id)
                .request(APPLICATION_JSON).delete();
        return response.getStatus() == 200;
    }

    public Recipe updateIngredients(long id, Set<Ingredient> ingredients) {
        Response response = ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/" + id + "/ingredients")
                .request(APPLICATION_JSON).put(Entity.entity(ingredients, APPLICATION_JSON));
        return response.getStatus() == 200 ? response.readEntity(Recipe.class) : null;
    }

    public Recipe updatePreparationSteps(long id, List<PreparationStep> preparationSteps) {
        Response response = ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/" + id + "/preparation")
                .request(APPLICATION_JSON).put(Entity.entity(preparationSteps, APPLICATION_JSON));
        return response.getStatus() == 200 ? response.readEntity(Recipe.class) : null;
    }

    public boolean isServerAvailable() {
        try { ClientBuilder.newClient(new ClientConfig()).target(SERVER).request(APPLICATION_JSON).get(); }
        catch (ProcessingException e) { if (e.getCause() instanceof ConnectException) return false; }
        return true;
    }

    public Recipe cloneRecipe(long id, String newName) {
        Response response = ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/" + id + "/clone")
                .request(APPLICATION_JSON).post(Entity.entity(newName, APPLICATION_JSON));
        return response.getStatus() == 200 ? response.readEntity(Recipe.class) : null;
    }

    public String getPrintableRecipe(long id, double scale) {
        Response response = ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/" + id + "/print")
                .queryParam("scale", scale).request(APPLICATION_JSON).get();
        return response.getStatus() == 200 ? response.readEntity(String.class) : null;
    }

    public Recipe updateRecipeData(long id, Recipe recipe) {
        try {
            Response response = ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/" + id)
                    .request(APPLICATION_JSON).put(Entity.entity(recipe, APPLICATION_JSON));
            if (response.getStatus() != 200) return null;
            return response.readEntity(Recipe.class);
        } catch (Exception e) { return null; }
    }

    public Recipe addLabelsToRecipe(long recipeId, Set<String> labelNames) {
        try {
            Response response = ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("recipes/" + recipeId + "/labels")
                    .request(APPLICATION_JSON).post(Entity.entity(labelNames, APPLICATION_JSON));
            return response.getStatus() == 200 ? response.readEntity(Recipe.class) : null;
        } catch (Exception e) { return null; }
    }
}