package client.utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class TranslationManager {
    private static TranslationManager translationManager;
    private Locale locale;
    private ResourceBundle bundle;
    private AppConfig appConfig;

    private TranslationManager() { setLocale(new Locale("en")); }

    public static TranslationManager getTranslationManager() {
        if (translationManager == null) translationManager = new TranslationManager();
        return translationManager;
    }

    public void initialize(AppConfig appConfig) {
        this.appConfig = appConfig;
        setLocale(new Locale(appConfig.getLanguage()));
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        this.bundle = ResourceBundle.getBundle("text", locale);
    }

    public Locale getLocale() { return locale; }
    public ResourceBundle getBundle() { return bundle; }
    public void setLanguage(String language) {
        setLocale(new Locale(language));
        if (appConfig != null) appConfig.setLanguage(language);
    }
    public String getCurrentLanguage() { return locale.getLanguage(); }
    public String getCurrentFlag() {
        if (locale.getLanguage().equals("nl")) return "🇳🇱";
        if (locale.getLanguage().equals("de")) return "🇩🇪";
        return "🇬🇧";
    }
}