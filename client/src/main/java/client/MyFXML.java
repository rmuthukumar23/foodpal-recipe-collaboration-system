/*
 * Copyright 2021 Delft University of Technology
 * Licensed under the Apache License, Version 2.0.
 */
package client;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ResourceBundle;
import client.utils.TranslationManager;
import com.google.inject.Injector;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.util.Builder;
import javafx.util.BuilderFactory;
import javafx.util.Callback;
import javafx.util.Pair;

public class MyFXML {
    private Injector injector;
    public MyFXML(Injector injector) { this.injector = injector; }

    public <T> Pair<T, Parent> load(Class<T> c, String... parts) {
        ResourceBundle bundel = TranslationManager.getTranslationManager().getBundle();
        try {
            var loader = new FXMLLoader(getLocation(parts), bundel, null, new MyFactory(), StandardCharsets.UTF_8);
            Parent parent = loader.load();
            T ctrl = loader.getController();
            return new Pair<>(ctrl, parent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URL getLocation(String... parts) {
        var path = Path.of("", parts).toString();
        return MyFXML.class.getClassLoader().getResource(path);
    }

    private class MyFactory implements BuilderFactory, Callback<Class<?>, Object> {
        @Override
        @SuppressWarnings("rawtypes")
        public Builder<?> getBuilder(Class<?> type) {
            return new Builder() {
                @Override public Object build() { return injector.getInstance(type); }
            };
        }
        @Override public Object call(Class<?> type) { return injector.getInstance(type); }
    }
}