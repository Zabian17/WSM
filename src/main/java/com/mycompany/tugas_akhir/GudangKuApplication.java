package com.mycompany.tugas_akhir;

import atlantafx.base.theme.PrimerLight;
import com.mycompany.tugas_akhir.config.SpringFXMLLoader;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class GudangKuApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(GudangKuApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        // Set tema modern AtlantaFX Primer Light
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        SpringFXMLLoader fxmlLoader = springContext.getBean(SpringFXMLLoader.class);
        Parent root = fxmlLoader.load("/fxml/login.fxml");

        Scene scene = new Scene(root, 1024, 680);
        stage.setTitle("GudangKu | Sistem Manajemen Gudang");
        stage.setMinWidth(900);
        stage.setMinHeight(560);
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (springContext != null) {
            springContext.close();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
