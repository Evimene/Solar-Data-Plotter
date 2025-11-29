// SolarDataPlotter.java
package com.example.solardataplotter;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;

public class SolarDataPlotter extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/solardataplotter/MainView.fxml"));

            Scene scene = new Scene(root, 1400, 900);

            // Load CSS safely
            URL cssUrl = getClass().getResource("/com/example/solardataplotter/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            } else {
                System.err.println("CSS file not found!");
            }

            // Load icon safely
            try {
                Image icon = new Image(getClass().getResourceAsStream("/com/example/solardataplotter/icon.png"));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                System.err.println("Icon not found: " + e.getMessage());
            }

            primaryStage.setTitle("Solar Data Analysis and Visualization System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.setMaximized(true); // Start maximized for better experience
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Failed to load application: " + e.getMessage());
        }
    }

    // ADD THIS MISSING METHOD
    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Initialization Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}