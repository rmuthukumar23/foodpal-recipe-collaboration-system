package client.utils;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import jakarta.ws.rs.client.ClientBuilder;
import commons.GlobalIngredient;
import jakarta.ws.rs.core.GenericType;
import java.util.List;

public class ServerUtilsGlobalIngredient {
    private final static String SERVER_URL = "http://localhost:8080";

    public List<GlobalIngredient> getGlobalIngredients() {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER_URL).path("api/ingredients/global")
                .request(APPLICATION_JSON)
                .get(new GenericType<List<GlobalIngredient>>() {});
    }

    public boolean isGlobalIngredientUsed(long id) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER_URL)
                .path("api/ingredients/global/" + id + "/used")
                .request(APPLICATION_JSON)
                .get(Boolean.class);
    }

    public void deleteGlobalIngredient(long id) {
        Response response = ClientBuilder.newClient(new ClientConfig())
                .target(SERVER_URL)
                .path("api/ingredients/global/" + id)
                .request()
                .delete();
        if (response.getStatus() >= 400) {
            throw new RuntimeException("Delete failed. HTTP " + response.getStatus());
        }
    }

    public GlobalIngredient updateGlobalIngredient(GlobalIngredient ingredient) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER_URL)
                .path("api/ingredients/global/" + ingredient.getId())
                .request(APPLICATION_JSON)
                .put(jakarta.ws.rs.client.Entity.entity(ingredient, APPLICATION_JSON), GlobalIngredient.class);
    }

    public int getGlobalIngredientUsageCount(Long id) {
        return ClientBuilder.newClient()
                .target(SERVER_URL).path("api/recipes/usage-count/" + id)
                .request(APPLICATION_JSON)
                .get(Integer.class);
    }

    public GlobalIngredient addGlobalIngredient(GlobalIngredient ingredient) {
        return ClientBuilder.newClient(new ClientConfig())
                .target(SERVER_URL)
                .path("api/ingredients/global")
                .request(APPLICATION_JSON)
                .post(jakarta.ws.rs.client.Entity.entity(ingredient, APPLICATION_JSON), GlobalIngredient.class);
    }
}