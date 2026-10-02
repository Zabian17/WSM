package com.mycompany.tugas_akhir.config;

import javafx.fxml.FXMLLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom FXML Loader yang menghubungkan JavaFX dengan Spring Application Context.
 * Memungkinkan controller JavaFX di-inject dependency oleh Spring (@Autowired).
 */
@Component
public class SpringFXMLLoader {

    private final ApplicationContext context;

    public SpringFXMLLoader(ApplicationContext context) {
        this.context = context;
    }

    public <T> T load(String fxmlPath) throws IOException {
        FXMLLoader loader = getLoader(fxmlPath);
        return loader.load();
    }

    public FXMLLoader getLoader(String fxmlPath) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setControllerFactory(context::getBean);
        return loader;
    }
}
