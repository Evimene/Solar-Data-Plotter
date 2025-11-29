// GraphConfig.java
package com.example.solardataplotter.model;

import java.util.List;
import javafx.beans.property.*;

public class GraphConfig {
    private final StringProperty experimentLocation;
    private final StringProperty latitude;
    private final StringProperty longitude;
    private final StringProperty xAxisLabel;
    private final StringProperty yAxisLabel;
    private List<String> xAxisColumns;
    private List<String> yAxisColumns;
    private final StringProperty graphTitle;

    public GraphConfig() {
        this.experimentLocation = new SimpleStringProperty("");
        this.latitude = new SimpleStringProperty("");
        this.longitude = new SimpleStringProperty("");
        this.xAxisLabel = new SimpleStringProperty("");
        this.yAxisLabel = new SimpleStringProperty("");
        this.graphTitle = new SimpleStringProperty("");
    }

    // Getters and Setters
    public String getExperimentLocation() { return experimentLocation.get(); }
    public void setExperimentLocation(String location) { this.experimentLocation.set(location); }
    public StringProperty experimentLocationProperty() { return experimentLocation; }

    public String getLatitude() { return latitude.get(); }
    public void setLatitude(String latitude) { this.latitude.set(latitude); }
    public StringProperty latitudeProperty() { return latitude; }

    public String getLongitude() { return longitude.get(); }
    public void setLongitude(String longitude) { this.longitude.set(longitude); }
    public StringProperty longitudeProperty() { return longitude; }

    public String getXAxisLabel() { return xAxisLabel.get(); }
    public void setXAxisLabel(String label) { this.xAxisLabel.set(label); }
    public StringProperty xAxisLabelProperty() { return xAxisLabel; }

    public String getYAxisLabel() { return yAxisLabel.get(); }
    public void setYAxisLabel(String label) { this.yAxisLabel.set(label); }
    public StringProperty yAxisLabelProperty() { return yAxisLabel; }

    public List<String> getXAxisColumns() { return xAxisColumns; }
    public void setXAxisColumns(List<String> columns) { this.xAxisColumns = columns; }

    public List<String> getYAxisColumns() { return yAxisColumns; }
    public void setYAxisColumns(List<String> columns) { this.yAxisColumns = columns; }

    public String getGraphTitle() { return graphTitle.get(); }
    public void setGraphTitle(String title) { this.graphTitle.set(title); }
    public StringProperty graphTitleProperty() { return graphTitle; }
}