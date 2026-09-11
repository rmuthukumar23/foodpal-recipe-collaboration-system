/*
 * Copyright 2021 Delft University of Technology
 * Licensed under the Apache License, Version 2.0.
 */
package client;

import static com.google.inject.Guice.createInjector;

import java.io.File;
import java.util.List;
import client.scenes.*;
import client.utils.ServerUtilsRecipe;
import client.utils.AppConfig;
import client.utils.TranslationManager;
import client.utils.WebSocketClient;
import com.google.inject.Injector;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {
    private static final Injector INJECTOR = createInjector(new MyModule());
    private static final MyFXML FXML = new MyFXML(INJECTOR);
    private static AppConfig config;
    private static WebSocketClient webSocketClient;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage primaryStage) throws Exception {
        File cfgFile = resolveConfigFile(getParameters().getRaw());
        config = (cfgFile == null) ? AppConfig.load() : AppConfig.load(cfgFile);
        TranslationManager.getTranslationManager().initialize(config);
        webSocketClient = new WebSocketClient(config);
        webSocketClient.connect();
        var serverUtils = INJECTOR.getInstance(ServerUtilsRecipe.class);
        if (!serverUtils.isServerAvailable()) {
            System.err.println("Server needs to be started before the client, but it does not seem to be available. Shutting down.");
            return;
        }
        var home = FXML.load(HomePageCtrl.class, "client", "scenes", "HomePage.fxml");
        var addRecipe = FXML.load(AddRecipeCtrl.class, "client", "scenes", "AddRecipe.fxml");
        var confirmDeleteRecipe = FXML.load(ConfirmRecipeDeletionCtrl.class, "client", "scenes", "ConfirmRecipeDeletion.fxml");
        var timer = FXML.load(TimerCtrl.class, "client", "scenes", "Timer.fxml");
        var advancedSearch = FXML.load(AdvancedSearchCtrl.class, "client", "scenes", "AdvancedSearch.fxml");
        var editRecipe = FXML.load(EditRecipeCtrl.class, "client", "scenes", "EditRecipe.fxml");
        var printRecipe = FXML.load(PrintRecipeCtrl.class, "client", "scenes", "PrintRecipe.fxml");
        var shoppingList = FXML.load(ShoppingListCtrl.class, "client", "scenes", "ShoppingList.fxml");
        var shoppingListConfirmation = FXML.load(ShoppingListConfirmationCtrl.class, "client", "scenes", "ShoppingListConfirmation.fxml");
        var nutritionalValue = FXML.load(NutritionalValueCtrl.class, "client", "scenes", "NutritionalValue.fxml");
        var primaryCtrl = INJECTOR.getInstance(PrimaryCtrl.class);
        var scenePackage = new PrimaryCtrl.ScenePackage();
        scenePackage.setHome(home);
        scenePackage.setAddRecipe(addRecipe);
        scenePackage.setConfirmDeleteRecipe(confirmDeleteRecipe);
        scenePackage.setTimer(timer);
        scenePackage.setAdvancedSearch(advancedSearch);
        scenePackage.setEditRecipe(editRecipe);
        scenePackage.setPrintRecipe(printRecipe);
        scenePackage.setShoppingList(shoppingList);
        scenePackage.setShoppingListConfirmation(shoppingListConfirmation);
        scenePackage.setNutritionalValue(nutritionalValue);
        primaryCtrl.initialize(primaryStage, scenePackage);
    }

    private static File resolveConfigFile(List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            String a = args.get(i);
            if (a.equals("–cfg") || a.equals("-cfg") || a.equals("--cfg")) {
                if (i + 1 < args.size()) return new File(args.get(i + 1));
                return null;
            }
            if (a.startsWith("–cfg=") || a.startsWith("-cfg=") || a.startsWith("--cfg=")) {
                return new File(a.substring(a.indexOf('=') + 1));
            }
        }
        return null;
    }

    public static Injector getInjector() { return INJECTOR; }
    public static WebSocketClient getWebSocketClient() { return webSocketClient; }
    @Override public void stop() { if (webSocketClient != null) webSocketClient.disconnect(); }
    public static AppConfig getConfig() { return config; }
}
