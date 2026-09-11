/*
 * Copyright 2021 Delft University of Technology
 * Licensed under the Apache License, Version 2.0.
 */
package client;

import client.scenes.*;
import client.utils.AppConfig;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;

public class MyModule implements Module {
    @Override
    public void configure(Binder binder) {
        binder.bind(AppConfig.class).toProvider(() -> Main.getConfig()).in(Scopes.SINGLETON);
        binder.bind(PrimaryCtrl.class).in(Scopes.SINGLETON);
        binder.bind(HomePageCtrl.class).in(Scopes.SINGLETON);
        binder.bind(AddRecipeCtrl.class).in(Scopes.SINGLETON);
        binder.bind(ConfirmRecipeDeletionCtrl.class).in(Scopes.SINGLETON);
        binder.bind(EditRecipeCtrl.class).in(Scopes.SINGLETON);
        binder.bind(AdvancedSearchCtrl.class).in(Scopes.SINGLETON);
        binder.bind(TimerCtrl.class).in(Scopes.SINGLETON);
        binder.bind(PrintRecipeCtrl.class).in(Scopes.SINGLETON);
        binder.bind(ShoppingListCtrl.class).in(Scopes.SINGLETON);
        binder.bind(ShoppingListConfirmationCtrl.class).in(Scopes.SINGLETON);
        binder.bind(NutritionalValueCtrl.class).in(Scopes.SINGLETON);
    }
}