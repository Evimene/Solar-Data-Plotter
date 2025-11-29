package com.example.solardataplotter.util;

import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class GraphExporter {

    public static void exportChart(Node chart, File file) throws IOException {
        try {
            // Create snapshot parameters
            SnapshotParameters params = new SnapshotParameters();
            params.setDepthBuffer(true);

            // Get the current dimensions of the chart
            int width = (int) chart.getBoundsInParent().getWidth();
            int height = (int) chart.getBoundsInParent().getHeight();

            // Use default dimensions if current dimensions are too small
            if (width < 800 || height < 600) {
                width = 1200;
                height = 800;
            }

            // Create writable image and take snapshot
            WritableImage image = new WritableImage(width, height);
            image = chart.snapshot(params, image);

            // Save to file
            if (image != null) {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } else {
                throw new IOException("Failed to capture chart image");
            }
        } catch (Exception e) {
            throw new IOException("Failed to export graph: " + e.getMessage(), e);
        }
    }
}