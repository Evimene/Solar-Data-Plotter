package com.example.solardataplotter.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import com.example.solardataplotter.model.SolarDataPoint;
import com.example.solardataplotter.model.GraphConfig;
import com.example.solardataplotter.util.DataValidator;
import com.example.solardataplotter.util.GraphExporter;

public class MainController implements Initializable {
    @FXML private TextField locationField, latField, lonField;
    @FXML private TextField xAxisLabelField, yAxisLabelField;
    @FXML private TableView<SolarDataPoint> dataTable;
    @FXML private ComboBox<String> xAxisCombo;
    @FXML private ListView<String> yAxisList;
    @FXML private LineChart<String, Number> lineChart;
    @FXML private CategoryAxis xAxis; // Properly declare CategoryAxis
    @FXML private NumberAxis yAxis;   // Properly declare NumberAxis
    @FXML private VBox graphContainer;
    @FXML private Label statusLabel;

    // Table columns
    @FXML private TableColumn<SolarDataPoint, String> timeColumn;
    @FXML private TableColumn<SolarDataPoint, Number> solarRadColumn, vMonoColumn, vPolyColumn;
    @FXML private TableColumn<SolarDataPoint, Number> iMonoColumn, iPolyColumn, pMonoColumn, pPolyColumn;
    @FXML private TableColumn<SolarDataPoint, Number> effMonoColumn, effPolyColumn, rhColumn;
    @FXML private TableColumn<SolarDataPoint, Number> tempMonoColumn, tempPolyColumn, ambientTempColumn, windColumn;

    private ObservableList<SolarDataPoint> dataPoints;
    private GraphConfig graphConfig;
    private Set<String> selectedYColumns;
    private Map<String, String> columnUnits;
    private double zoomFactor = 1.0;
    private static final double ZOOM_INCREMENT = 0.2;
    private static final double MIN_ZOOM = 0.5;
    private static final double MAX_ZOOM = 5.0;


    private void setupResponsiveChart() {
        // Make chart responsive to container size
        lineChart.prefWidthProperty().bind(graphContainer.widthProperty());
        lineChart.prefHeightProperty().bind(graphContainer.heightProperty());

        // Set minimum sizes
        lineChart.setMinWidth(800);
        lineChart.setMinHeight(500);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initializing MainController...");
        try {
            initializeData();
            initializeTable();
            initializeComboBoxes();
            setupEventHandlers();
            setupZoomHandlers();
            setupResponsiveChart();
            updateStatus("Application ready");
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error during initialization: " + e.getMessage());
        }
    }

    private void initializeData() {
        dataPoints = FXCollections.observableArrayList();
        graphConfig = new GraphConfig();
        selectedYColumns = new HashSet<>();

        // Initialize column units
        columnUnits = new HashMap<>();
        columnUnits.put("Solar Radiation", "W/m²");
        columnUnits.put("V_mono", "V");
        columnUnits.put("V_poly", "V");
        columnUnits.put("I_mono", "A");
        columnUnits.put("I_poly", "A");
        columnUnits.put("P_mono", "W");
        columnUnits.put("P_poly", "W");
        columnUnits.put("Eff_mono", "%");
        columnUnits.put("Eff_poly", "%");
        columnUnits.put("RH", "%");
        columnUnits.put("Panel Temp Mono", "°C");
        columnUnits.put("Panel Temp Poly", "°C");
        columnUnits.put("Ambient Temp", "°C");
        columnUnits.put("Wind Speed", "m/s");

        // Set default values
        graphConfig.setXAxisLabel("Time");
        graphConfig.setYAxisLabel("Values");
        graphConfig.setExperimentLocation("Solar Lab");

        // Bind configuration fields
        locationField.textProperty().bindBidirectional(graphConfig.experimentLocationProperty());
        latField.textProperty().bindBidirectional(graphConfig.latitudeProperty());
        lonField.textProperty().bindBidirectional(graphConfig.longitudeProperty());
        xAxisLabelField.textProperty().bindBidirectional(graphConfig.xAxisLabelProperty());
        yAxisLabelField.textProperty().bindBidirectional(graphConfig.yAxisLabelProperty());
    }

    private void setupZoomHandlers() {
        // Mouse scroll with Ctrl for zooming (Y-axis only)
        lineChart.setOnScroll((ScrollEvent event) -> {
            if (event.isControlDown()) {
                if (event.getDeltaY() > 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
                event.consume();
            }
        });

        // Keyboard shortcuts for zooming (Y-axis only)
        lineChart.setOnKeyPressed((KeyEvent event) -> {
            if (event.isShiftDown()) {
                if (event.getCode() == KeyCode.PLUS || event.getCode() == KeyCode.EQUALS) {
                    zoomIn();
                    event.consume();
                } else if (event.getCode() == KeyCode.MINUS) {
                    zoomOut();
                    event.consume();
                }
            }
        });

        // Ensure the chart is focusable for keyboard events
        lineChart.setFocusTraversable(true);
    }

    private void zoomIn() {
        if (zoomFactor < MAX_ZOOM) {
            zoomFactor += ZOOM_INCREMENT;
            applyZoom();
            updateStatus("Zoom: " + String.format("%.1fx", zoomFactor));
        }
    }

    private void zoomOut() {
        if (zoomFactor > MIN_ZOOM) {
            zoomFactor -= ZOOM_INCREMENT;
            applyZoom();
            updateStatus("Zoom: " + String.format("%.1fx", zoomFactor));
        }
    }

    private void applyZoom() {
        // For CategoryAxis (X-axis), we can't easily zoom, so we'll zoom Y-axis only
        if (!dataPoints.isEmpty() && !lineChart.getData().isEmpty()) {
            autoRangeYAxis();
        }
    }


    private void autoRangeYAxis() {
        if (dataPoints.isEmpty() || selectedYColumns.isEmpty()) return;

        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        // Calculate min and max values for all selected Y columns
        for (SolarDataPoint point : dataPoints) {
            for (String yColumn : selectedYColumns) {
                Number value = getNumericColumnValue(point, yColumn);
                if (value != null) {
                    double doubleValue = value.doubleValue();
                    minY = Math.min(minY, doubleValue);
                    maxY = Math.max(maxY, doubleValue);
                }
            }
        }

        if (minY == Double.MAX_VALUE || maxY == Double.MIN_VALUE) return;

        // Apply zoom to Y-axis range
        double range = maxY - minY;
        double padding = range * 0.1;
        double zoomedRange = range / zoomFactor;
        double center = (minY + maxY) / 2;

        yAxis.setLowerBound(center - zoomedRange / 2 - padding);
        yAxis.setUpperBound(center + zoomedRange / 2 + padding);
    }

    private void initializeTable() {
        dataTable.setItems(dataPoints);

        // Initialize columns with proper property names
        timeColumn.setCellValueFactory(cellData -> cellData.getValue().timeProperty());
        solarRadColumn.setCellValueFactory(cellData -> cellData.getValue().solarRadiationProperty());
        vMonoColumn.setCellValueFactory(cellData -> cellData.getValue().vMonoProperty());
        vPolyColumn.setCellValueFactory(cellData -> cellData.getValue().vPolyProperty());
        iMonoColumn.setCellValueFactory(cellData -> cellData.getValue().iMonoProperty());
        iPolyColumn.setCellValueFactory(cellData -> cellData.getValue().iPolyProperty());
        pMonoColumn.setCellValueFactory(cellData -> cellData.getValue().pMonoProperty());
        pPolyColumn.setCellValueFactory(cellData -> cellData.getValue().pPolyProperty());
        effMonoColumn.setCellValueFactory(cellData -> cellData.getValue().effMonoProperty());
        effPolyColumn.setCellValueFactory(cellData -> cellData.getValue().effPolyProperty());
        rhColumn.setCellValueFactory(cellData -> cellData.getValue().rhProperty());
        tempMonoColumn.setCellValueFactory(cellData -> cellData.getValue().panelTempMonoProperty());
        tempPolyColumn.setCellValueFactory(cellData -> cellData.getValue().panelTempPolyProperty());
        ambientTempColumn.setCellValueFactory(cellData -> cellData.getValue().ambientTempProperty());
        windColumn.setCellValueFactory(cellData -> cellData.getValue().windSpeedProperty());

        // Make table editable
        dataTable.setEditable(true);
        enableCellEditing();
    }

    private void enableCellEditing() {
        // Enable editing for all columns
        setCellFactory(timeColumn);

        // Numeric columns with proper property mapping
        setNumericCellFactory(solarRadColumn, "solarRadiation");
        setNumericCellFactory(vMonoColumn, "vMono");
        setNumericCellFactory(vPolyColumn, "vPoly");
        setNumericCellFactory(iMonoColumn, "iMono");
        setNumericCellFactory(iPolyColumn, "iPoly");
        setNumericCellFactory(pMonoColumn, "pMono");
        setNumericCellFactory(pPolyColumn, "pPoly");
        setNumericCellFactory(effMonoColumn, "effMono");
        setNumericCellFactory(effPolyColumn, "effPoly");
        setNumericCellFactory(rhColumn, "rh");
        setNumericCellFactory(tempMonoColumn, "panelTempMono");
        setNumericCellFactory(tempPolyColumn, "panelTempPoly");
        setNumericCellFactory(ambientTempColumn, "ambientTemp");
        setNumericCellFactory(windColumn, "windSpeed");
    }

    private void setCellFactory(TableColumn<SolarDataPoint, String> column) {
        column.setCellFactory(TextFieldTableCell.forTableColumn());
        column.setOnEditCommit(event -> {
            SolarDataPoint point = event.getRowValue();
            point.setTime(event.getNewValue());
        });
    }

    private void setNumericCellFactory(TableColumn<SolarDataPoint, Number> column, String propertyName) {
        column.setCellFactory(TextFieldTableCell.forTableColumn(new StringConverter<Number>() {
            @Override
            public String toString(Number object) {
                return object == null ? "" : String.format("%.2f", object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                try {
                    return string.isEmpty() ? 0 : Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }));

        // Add specific edit commit handler for each column
        column.setOnEditCommit(event -> {
            SolarDataPoint point = event.getRowValue();
            double newValue = event.getNewValue().doubleValue();

            switch (propertyName) {
                case "solarRadiation": point.setSolarRadiation(newValue); break;
                case "vMono": point.setVMono(newValue); break;
                case "vPoly": point.setVPoly(newValue); break;
                case "iMono": point.setIMono(newValue); break;
                case "iPoly": point.setIPoly(newValue); break;
                case "pMono": point.setPMono(newValue); break;
                case "pPoly": point.setPPoly(newValue); break;
                case "effMono": point.setEffMono(newValue); break;
                case "effPoly": point.setEffPoly(newValue); break;
                case "rh": point.setRh(newValue); break;
                case "panelTempMono": point.setPanelTempMono(newValue); break;
                case "panelTempPoly": point.setPanelTempPoly(newValue); break;
                case "ambientTemp": point.setAmbientTemp(newValue); break;
                case "windSpeed": point.setWindSpeed(newValue); break;
            }
        });
    }

    private void initializeComboBoxes() {
        // Available columns for plotting
        ObservableList<String> allColumns = FXCollections.observableArrayList(
                "Time", "Solar Radiation", "V_mono", "V_poly", "I_mono", "I_poly",
                "P_mono", "P_poly", "Eff_mono", "Eff_poly", "RH", "Panel Temp Mono",
                "Panel Temp Poly", "Ambient Temp", "Wind Speed"
        );

        xAxisCombo.setItems(allColumns);

        // Use CheckBoxListView for better multiple selection
        yAxisList.setCellFactory(param -> new ListCell<String>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    checkBox.setText(item);
                    checkBox.setSelected(selectedYColumns.contains(item));

                    // Add listener for checkbox changes
                    checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                        if (isSelected) {
                            selectedYColumns.add(item);
                        } else {
                            selectedYColumns.remove(item);
                        }
                    });

                    setGraphic(checkBox);
                    setText(null);
                }
            }
        });

        yAxisList.setItems(allColumns);

        // Set default selections
        xAxisCombo.getSelectionModel().select("Time");

        // Select multiple default Y columns
        yAxisList.getSelectionModel().selectIndices(1, 2, 3); // Solar Radiation, V_mono, V_poly
    }

    private void updateYAxisLabel() {
        // USE CUSTOM Y-AXIS LABEL FROM INPUT FIELD
        String yAxisLabel = graphConfig.getYAxisLabel();
        if (yAxisLabel == null || yAxisLabel.trim().isEmpty()) {
            // Only use fallback logic if custom label is empty
            if (selectedYColumns.isEmpty()) {
                yAxis.setLabel("Values");
            } else if (selectedYColumns.size() == 1) {
                String column = selectedYColumns.iterator().next();
                String unit = columnUnits.getOrDefault(column, "");
                yAxis.setLabel(column + (unit.isEmpty() ? "" : " (" + unit + ")"));
            } else {
                yAxis.setLabel("Multiple Parameters");
            }
        } else {
            // Use the custom label from input field
            yAxis.setLabel(yAxisLabel);
        }
    }

    private void setupEventHandlers() {
        // Update axis labels when input fields change
        xAxisLabelField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                xAxis.setLabel(newVal);
            }
        });

        yAxisLabelField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                yAxis.setLabel(newVal);
            }
        });
    }

    @FXML
    private void handleAddData() {
        SolarDataPoint newPoint = new SolarDataPoint();
        newPoint.setTime("00:00");
        dataPoints.add(newPoint);
        dataTable.getSelectionModel().select(newPoint);
        dataTable.scrollTo(newPoint);
        updateStatus("New data point added");
    }

    @FXML
    private void handleRemoveData() {
        SolarDataPoint selected = dataTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            dataPoints.remove(selected);
            updateStatus("Data point removed");
        } else {
            showAlert("No Selection", "Please select a data point to remove.");
        }
    }

    @FXML
    private void handleCalculate() {
        try {
            if (!validateInputs()) return;
            if (!validateGraphConfig()) return;

            generateGraph();
            resetZoom();
            updateStatus("Graph generated successfully with " + selectedYColumns.size() + " Y-axis series");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Graph Generation Error", "Failed to generate graph: " + e.getMessage());
        }
    }

    @FXML
    private void handleExportGraph() {
        if (lineChart.getData().isEmpty()) {
            showAlert("No Graph", "Please generate a graph before exporting.");
            return;
        }

        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Graph as Image");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("PNG Image", "*.png")
            );
            fileChooser.setInitialFileName("solar_data_graph.png");

            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                GraphExporter.exportChart(lineChart, file);
                updateStatus("Graph exported to: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Export Error", "Failed to export graph: " + e.getMessage());
        }
    }


    @FXML
    private void handleClearAll() {
        dataPoints.clear();
        lineChart.getData().clear();
        updateStatus("All data cleared");
    }


    private boolean validateInputs() {
        if (dataPoints.isEmpty()) {
            showAlert("No Data", "Please add some data points before generating the graph.");
            return false;
        }

        for (SolarDataPoint point : dataPoints) {
            if (!DataValidator.isValidTime(point.getTime())) {
                showAlert("Invalid Time", "Please enter valid time format (HH:mm) for all data points.");
                return false;
            }
        }
        return true;
    }



    private boolean validateGraphConfig() {
        if (xAxisCombo.getSelectionModel().getSelectedItem() == null) {
            showAlert("X-Axis Required", "Please select an X-axis column.");
            return false;
        }

        if (selectedYColumns.isEmpty()) {
            showAlert("Y-Axis Required", "Please select at least one Y-axis column.");
            return false;
        }

        return true;
    }

    private void generateGraph() {
        lineChart.getData().clear();

        // Set chart title with location and coordinates
        String title = buildGraphTitle();
        lineChart.setTitle(title);

        // USE CUSTOM LABELS FROM INPUT FIELDS
        String xAxisLabel = graphConfig.getXAxisLabel();
        String yAxisLabel = graphConfig.getYAxisLabel();

        xAxis.setLabel(xAxisLabel != null && !xAxisLabel.trim().isEmpty() ? xAxisLabel : "X-Axis");
        yAxis.setLabel(yAxisLabel != null && !yAxisLabel.trim().isEmpty() ? yAxisLabel : "Y-Axis");

        String xAxisColumn = xAxisCombo.getSelectionModel().getSelectedItem();

        // Create series for each selected Y column with distinct colors
        String[] colors = {"#FF0000", "#0000FF", "#008000", "#FFA500", "#800080",
                "#00FFFF", "#FF00FF", "#A52A2A", "#808080", "#000000"};
        int colorIndex = 0;

        for (String yColumn : selectedYColumns) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            // Include units in series name for legend
            String seriesName = yColumn + " (" + columnUnits.getOrDefault(yColumn, "") + ")";
            series.setName(seriesName);

            // Add data points using ACTUAL DATA from table columns
            for (SolarDataPoint point : dataPoints) {
                String xValue = getXAxisValue(point, xAxisColumn);
                Number yValue = getNumericColumnValue(point, yColumn);

                if (xValue != null && yValue != null) {
                    series.getData().add(new XYChart.Data<>(xValue, yValue));
                }
            }

            lineChart.getData().add(series);
            colorIndex++;
        }

        // Apply enhanced styling
        applyEnhancedChartStyling();
    }

    private String buildGraphTitle() {
        StringBuilder title = new StringBuilder("Solar Data Analysis");

        // Add location if available
        if (!graphConfig.getExperimentLocation().isEmpty()) {
            title.append(" - ").append(graphConfig.getExperimentLocation());
        }

        // Add coordinates as sub-title if available
        boolean hasLat = !graphConfig.getLatitude().isEmpty();
        boolean hasLon = !graphConfig.getLongitude().isEmpty();

        if (hasLat || hasLon) {
            title.append("\n"); // New line for sub-title
            if (hasLat && hasLon) {
                title.append("Coordinates: ").append(graphConfig.getLatitude())
                        .append(", ").append(graphConfig.getLongitude());
            } else if (hasLat) {
                title.append("Latitude: ").append(graphConfig.getLatitude());
            } else if (hasLon) {
                title.append("Longitude: ").append(graphConfig.getLongitude());
            }
        }

        return title.toString();
    }


    private String getXAxisValue(SolarDataPoint point, String columnName) {
        // Use actual data from the selected column for X-axis
        switch (columnName) {
            case "Time":
                return point.getTime(); // Display time as entered
            case "Solar Radiation":
                return String.format("%.1f", point.getSolarRadiation());
            case "V_mono":
                return String.format("%.1f", point.getVMono());
            case "V_poly":
                return String.format("%.1f", point.getVPoly());
            case "I_mono":
                return String.format("%.1f", point.getIMono());
            case "I_poly":
                return String.format("%.1f", point.getIPoly());
            case "P_mono":
                return String.format("%.1f", point.getPMono());
            case "P_poly":
                return String.format("%.1f", point.getPPoly());
            case "Eff_mono":
                return String.format("%.1f", point.getEffMono());
            case "Eff_poly":
                return String.format("%.1f", point.getEffPoly());
            case "RH":
                return String.format("%.1f", point.getRh());
            case "Panel Temp Mono":
                return String.format("%.1f", point.getPanelTempMono());
            case "Panel Temp Poly":
                return String.format("%.1f", point.getPanelTempPoly());
            case "Ambient Temp":
                return String.format("%.1f", point.getAmbientTemp());
            case "Wind Speed":
                return String.format("%.1f", point.getWindSpeed());
            default:
                return null;
        }
    }


    private String getColumnValue(SolarDataPoint point, String columnName) {
        switch (columnName) {
            case "Time": return point.getTime();
            case "Solar Radiation": return String.format("%.1f", point.getSolarRadiation());
            case "V_mono": return String.format("%.1f", point.getVMono());
            case "V_poly": return String.format("%.1f", point.getVPoly());
            case "I_mono": return String.format("%.1f", point.getIMono());
            case "I_poly": return String.format("%.1f", point.getIPoly());
            case "P_mono": return String.format("%.1f", point.getPMono());
            case "P_poly": return String.format("%.1f", point.getPPoly());
            case "Eff_mono": return String.format("%.1f", point.getEffMono());
            case "Eff_poly": return String.format("%.1f", point.getEffPoly());
            case "RH": return String.format("%.1f", point.getRh());
            case "Panel Temp Mono": return String.format("%.1f", point.getPanelTempMono());
            case "Panel Temp Poly": return String.format("%.1f", point.getPanelTempPoly());
            case "Ambient Temp": return String.format("%.1f", point.getAmbientTemp());
            case "Wind Speed": return String.format("%.1f", point.getWindSpeed());
            default: return null;
        }
    }

    private Number getNumericColumnValue(SolarDataPoint point, String columnName) {
        switch (columnName) {
            case "Solar Radiation": return point.getSolarRadiation();
            case "V_mono": return point.getVMono();
            case "V_poly": return point.getVPoly();
            case "I_mono": return point.getIMono();
            case "I_poly": return point.getIPoly();
            case "P_mono": return point.getPMono();
            case "P_poly": return point.getPPoly();
            case "Eff_mono": return point.getEffMono();
            case "Eff_poly": return point.getEffPoly();
            case "RH": return point.getRh();
            case "Panel Temp Mono": return point.getPanelTempMono();
            case "Panel Temp Poly": return point.getPanelTempPoly();
            case "Ambient Temp": return point.getAmbientTemp();
            case "Wind Speed": return point.getWindSpeed();
            default: return null;
        }
    }

    private void applyEnhancedChartStyling() {
        lineChart.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-width: 1;");
        lineChart.setLegendVisible(true);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(true); // Show data points

        // Enhanced legend styling
        lineChart.lookup(".chart-legend").setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6;");

        // Grid lines styling
        xAxis.setStyle("-fx-tick-label-fill: #2c3e50;");
        yAxis.setStyle("-fx-tick-label-fill: #2c3e50;");
    }

    @FXML
    private void handleZoomIn() {
        zoomIn();
    }

    @FXML
    private void handleZoomOut() {
        zoomOut();
    }

    @FXML
    private void handleResetZoom() {
        resetZoom();
    }

    private void resetZoom() {
        zoomFactor = 1.0;
        yAxis.setAutoRanging(true);
        updateStatus("Zoom reset");
    }

    private void updateStatus(String message) {
        statusLabel.setText("Status: " + message);
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}