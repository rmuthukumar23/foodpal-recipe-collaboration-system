package client.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javafx.application.Platform;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;

@Singleton
public class WebSocketClient {
    private StompSession session;
    private AppConfig config;
    private Runnable onRecipeListChange;
    private Runnable onRecipeContentChange;
    private StompSession.Subscription listSubscription;
    private StompSession.Subscription contentSubscription;

    @Inject
    public WebSocketClient(AppConfig config) { this.config = config; }

    public void connect() {
        if (isConnected()) return;
        String host = config.getHost().replace("http://", "").replace("https://", "");
        if (!host.endsWith("/")) host += "/";
        String url = "ws://" + host + "ws";
        var stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new StringMessageConverter());
        stomp.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                WebSocketClient.this.session = session;
                subscribeToRecipeList();
            }
            @Override
            public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable e) {
                System.err.println("WebSocket Error: " + e.getMessage());
            }
        });
    }

    private void subscribeToRecipeList() {
        if (listSubscription != null) listSubscription.unsubscribe();
        listSubscription = session.subscribe("/topic/recipes", new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return String.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                if (onRecipeListChange != null) Platform.runLater(onRecipeListChange);
            }
        });
    }

    public void subscribeToRecipe(long recipeId) {
        if (!isConnected()) {
            connect();
            if (!isConnected()) return;
        }
        if (contentSubscription != null) contentSubscription.unsubscribe();
        contentSubscription = session.subscribe("/topic/recipe/" + recipeId, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return String.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                if (onRecipeContentChange != null) Platform.runLater(onRecipeContentChange);
            }
        });
    }

    public void registerListUpdate(Runnable action) { this.onRecipeListChange = action; }
    public void registerContentUpdate(Runnable action) { this.onRecipeContentChange = action; }
    public boolean isConnected() { return session != null && session.isConnected(); }
    public void disconnect() { if (isConnected()) session.disconnect(); }
}