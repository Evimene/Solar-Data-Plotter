package com.example.solardataplotter;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

public class SolarDataPlotter extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("Starting Solar Data Plotter...");
            System.out.println("Java Version: " + System.getProperty("java.version"));
            System.out.println("JavaFX Version: " + System.getProperty("javafx.version"));

            // Load FXML
            URL fxmlUrl = getClass().getResource("/com/example/solardataplotter/MainView.fxml");
            if (fxmlUrl == null) {
                throw new RuntimeException("FXML file not found!");
            }

            Parent root = FXMLLoader.load(fxmlUrl);

            // Create scene
            Scene scene = new Scene(root, 1400, 900);

            // Load CSS
            URL cssUrl = getClass().getResource("/com/example/solardataplotter/styles.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            // Set stage properties
            primaryStage.setTitle("Solar Data Analysis and Visualization System");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);

            // Try to set icon
            try {
                Image icon = new Image(getClass().getResourceAsStream("/com/example/solardataplotter/icon.png"));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                System.err.println("Could not load icon: " + e.getMessage());
            }

            primaryStage.show();

            System.out.println("✅ Application started successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            showErrorDialog("Fatal Error", "Failed to start application", e);
            Platform.exit();
        }
    }

    private void showErrorDialog(String title, String header, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        alert.setContentText(e.getMessage() + "\n\n" + sw.toString());
        alert.showAndWait();
    }
}