package client.utils;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.ConnectException;
import java.util.List;
import org.glassfish.jersey.client.ClientConfig;
import commons.Label;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

public class ServerUtilsLabel {
    private static final String SERVER = "http://localhost:8080/api/";

    public List<Label> getAllLabels() {
        return ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("labels/")
                .request(APPLICATION_JSON).get(new GenericType<List<Label>>() {});
    }
    public Label getLabel(long id) {
        try { return ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("labels/" + id).request(APPLICATION_JSON).get(Label.class); }
        catch (Exception e) { return null; }
    }
    public Label createLabel(Label label) {
        try { return ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("labels/").request(APPLICATION_JSON).post(Entity.entity(label, APPLICATION_JSON), Label.class); }
        catch (Exception e) { return null; }
    }
    public boolean deleteLabel(long id) {
        try {
            Response response = ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("labels/" + id).request(APPLICATION_JSON).delete();
            return response.getStatus() == 200;
        } catch (Exception e) { return false; }
    }
    public List<Label> initializeDefaultLabels() {
        try { return ClientBuilder.newClient(new ClientConfig()).target(SERVER).path("labels/init").request(APPLICATION_JSON).post(Entity.entity("", APPLICATION_JSON), new GenericType<List<Label>>() {}); }
        catch (Exception e) { return null; }
    }
    public boolean isServerAvailable() {
        try { ClientBuilder.newClient(new ClientConfig()).target(SERVER).request(APPLICATION_JSON).get(); }
        catch (ProcessingException e) { if (e.getCause() instanceof ConnectException) return false; }
        return true;
    }
}