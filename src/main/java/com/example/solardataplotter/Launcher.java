package com.example.solardataplotter;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Check if running with --test flag
        if (args.length > 0 && args[0].equals("--test")) {
            System.out.println("✅ Application test successful!");
            System.out.println("Java version: " + System.getProperty("java.version"));
            System.out.println("JavaFX available: " + checkJavaFX());
            return;
        }

        // Normal launch
        Application.launch(SolarDataPlotter.class, args);
    }

    private static boolean checkJavaFX() {
        try {
            Class.forName("javafx.application.Application");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}