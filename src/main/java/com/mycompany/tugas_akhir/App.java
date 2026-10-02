package com.mycompany.tugas_akhir;

import javafx.application.Application;
<<<<<<< HEAD
=======
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader fxml = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml")
        );

        Scene scene = new Scene(fxml.load(), 950, 600);

        stage.setTitle("GudangKu - Sistem Management Gudang");
        stage.setResizable(true);
        stage.setScene(scene);
        stage.show();
    }
>>>>>>> origin/main

public class App {
    public static void main(String[] args) {
        Application.launch(GudangKuApplication.class, args);
    }
}