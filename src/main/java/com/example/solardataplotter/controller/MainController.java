package com.example.solardataplotter.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
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
import com.example.solardataplotter.util.ExcelImporter;


public class MainController implements Initializable {
    @FXML private TextField locationField, latField, lonField;
    @FXML private TextField xAxisLabelField, yAxisLabelField;
    @FXML private TableView<SolarDataPoint> dataTable;
    @FXML private ComboBox<String> xAxisCombo;
    @FXML private ListView<String> yAxisList;
    @FXML private LineChart<Number, Number> lineChart; // Changed to String,String for consistent display
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis; // Changed to CategoryAxis for consistent display
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

    // Track checkbox listeners to prevent duplicates
    private Map<CheckBox, javafx.beans.value.ChangeListener<Boolean>> checkboxListeners = new HashMap<>();


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
            setupKeyboardNavigation();
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
        columnUnits.put("Time", "HH:mm");
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

        // Enable cell selection for keyboard navigation
        dataTable.getSelectionModel().setCellSelectionEnabled(true);
    }

    private void setupKeyboardNavigation() {
        // Add keyboard navigation for arrow keys
        dataTable.setOnKeyPressed(this::handleTableKeyPress);

        // Make sure table is focusable
        dataTable.setFocusTraversable(true);
    }

    private void handleTableKeyPress(KeyEvent event) {
        TablePosition<?, ?> focusedCell = dataTable.getFocusModel().getFocusedCell();
        int currentRow = focusedCell.getRow();
        int currentCol = focusedCell.getColumn();
        int rowCount = dataTable.getItems().size();
        int colCount = dataTable.getColumns().size();

        switch (event.getCode()) {
            case UP:
                if (currentRow > 0) {
                    dataTable.getSelectionModel().clearAndSelect(currentRow - 1, dataTable.getColumns().get(currentCol));
                }
                event.consume();
                break;

            case DOWN:
                if (currentRow < rowCount - 1) {
                    dataTable.getSelectionModel().clearAndSelect(currentRow + 1, dataTable.getColumns().get(currentCol));
                } else if (event.isShiftDown()) {
                    // Shift+Down adds new row at the end
                    handleAddData();
                }
                event.consume();
                break;

            case LEFT:
                if (currentCol > 0) {
                    dataTable.getSelectionModel().clearAndSelect(currentRow, dataTable.getColumns().get(currentCol - 1));
                }
                event.consume();
                break;

            case RIGHT:
                if (currentCol < colCount - 1) {
                    dataTable.getSelectionModel().clearAndSelect(currentRow, dataTable.getColumns().get(currentCol + 1));
                }
                event.consume();
                break;

            case ENTER:
                // Move down when Enter is pressed (like Excel)
                if (currentRow < rowCount - 1) {
                    dataTable.getSelectionModel().clearAndSelect(currentRow + 1, dataTable.getColumns().get(currentCol));
                } else {
                    handleAddData();
                }
                event.consume();
                break;

            case TAB:
                // Move right when Tab is pressed, or left with Shift+Tab
                if (event.isShiftDown()) {
                    if (currentCol > 0) {
                        dataTable.getSelectionModel().clearAndSelect(currentRow, dataTable.getColumns().get(currentCol - 1));
                    }
                } else {
                    if (currentCol < colCount - 1) {
                        dataTable.getSelectionModel().clearAndSelect(currentRow, dataTable.getColumns().get(currentCol + 1));
                    } else if (currentRow < rowCount - 1) {
                        // Wrap to next row, first column
                        dataTable.getSelectionModel().clearAndSelect(currentRow + 1, dataTable.getColumns().get(0));
                    }
                }
                event.consume();
                break;
        }
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


    @FXML
    private void handleImportExcel() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Import Excel/CSV Data");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Excel Files", "*.xlsx", "*.xls"),
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                    new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File file = fileChooser.showOpenDialog(null);
            if (file != null) {
                List<SolarDataPoint> importedData = ExcelImporter.importData(file);

                if (importedData != null && !importedData.isEmpty()) {
                    dataPoints.setAll(importedData);
                    updateStatus("Successfully imported " + importedData.size() + " data points from " + file.getName());

                    // Auto-generate graph after import if we have selections
                    if (!selectedYColumns.isEmpty() && xAxisCombo.getSelectionModel().getSelectedItem() != null) {
                        generateGraph();
                    }
                } else {
                    showAlert("Import Error", "No data was imported. Please check the file format.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Import Error", "Failed to import data: " + e.getMessage());
        }
    }

    private void initializeComboBoxes() {
        // Available columns for plotting
        ObservableList<String> allColumns = FXCollections.observableArrayList(
                "Time", "Solar Radiation", "V_mono", "V_poly", "I_mono", "I_poly",
                "P_mono", "P_poly", "Eff_mono", "Eff_poly", "RH", "Panel Temp Mono",
                "Panel Temp Poly", "Ambient Temp", "Wind Speed"
        );

        xAxisCombo.setItems(allColumns);

        // Improved CheckBoxListView
        yAxisList.setCellFactory(param -> new ListCell<String>() {
            private final CheckBox checkBox = new CheckBox();
            private javafx.beans.value.ChangeListener<Boolean> listener;

            {
                // Create listener once when the cell is created
                listener = (obs, wasSelected, isSelected) -> {
                    String currentItem = getItem();
                    if (currentItem != null) {
                        if (isSelected) {
                            selectedYColumns.add(currentItem);
                        } else {
                            selectedYColumns.remove(currentItem);
                        }

                        // Auto-regenerate graph when Y-axis selection changes
                        if (!dataPoints.isEmpty() && xAxisCombo.getSelectionModel().getSelectedItem() != null) {
                            generateGraph();
                            updateStatus("Y-axis selection updated");
                        }
                    }
                };

                // Add the listener to the checkbox
                checkBox.selectedProperty().addListener(listener);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                // Remove listener temporarily to prevent recursive updates during cell refresh
                checkBox.selectedProperty().removeListener(listener);

                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    checkBox.setText(item);
                    checkBox.setSelected(selectedYColumns.contains(item));
                    setGraphic(checkBox);
                    setText(null);
                }

                // Re-add listener after update
                checkBox.selectedProperty().addListener(listener);
            }
        });

        yAxisList.setItems(allColumns);

        // Set default selections
        xAxisCombo.getSelectionModel().select("Time");

        // Select multiple default Y columns
        selectedYColumns.add("Solar Radiation");
        selectedYColumns.add("V_mono");
        selectedYColumns.add("V_poly");

        // Update list view to reflect initial selections
        yAxisList.refresh();
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

        // Auto-regenerate graph when X-axis selection changes
        xAxisCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Update X-axis label with proper unit
                String xUnit = columnUnits.get(newVal);
                String currentXLabel = graphConfig.getXAxisLabel();
                if (currentXLabel == null || currentXLabel.trim().isEmpty() || currentXLabel.equals("Time") || currentXLabel.equals("X-Axis")) {
                    xAxis.setLabel(newVal + (xUnit != null ? " (" + xUnit + ")" : ""));
                }

                // Auto-regenerate graph with new X-axis
                if (!dataPoints.isEmpty() && !selectedYColumns.isEmpty()) {
                    generateGraph();
                    updateStatus("X-axis changed to: " + newVal + (xUnit != null ? " (" + xUnit + ")" : ""));
                }
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
        selectedYColumns.clear();
        yAxisList.refresh();
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

        String xAxisColumn = xAxisCombo.getSelectionModel().getSelectedItem();

        // Update axis labels with units
        String xUnit = columnUnits.get(xAxisColumn);
        String currentXLabel = graphConfig.getXAxisLabel();
        if (currentXLabel == null || currentXLabel.trim().isEmpty() || currentXLabel.equals("Time") || currentXLabel.equals("X-Axis")) {
            xAxis.setLabel(xAxisColumn + (xUnit != null ? " (" + xUnit + ")" : ""));
        } else {
            xAxis.setLabel(currentXLabel);
        }

        String currentYLabel = graphConfig.getYAxisLabel();
        if (currentYLabel == null || currentYLabel.trim().isEmpty() || currentYLabel.equals("Values") || currentYLabel.equals("Y-Axis")) {
            if (selectedYColumns.size() == 1) {
                String yColumn = selectedYColumns.iterator().next();
                String yUnit = columnUnits.get(yColumn);
                yAxis.setLabel(yColumn + (yUnit != null ? " (" + yUnit + ")" : ""));
            } else {
                yAxis.setLabel("Multiple Parameters");
            }
        } else {
            yAxis.setLabel(currentYLabel);
        }

        // Use data in table order
        List<SolarDataPoint> tableOrderDataPoints = new ArrayList<>(dataPoints);

        // Create series for each selected Y column
        String[] colors = {"#FF0000", "#0000FF", "#008000", "#FFA500", "#800080",
                "#00FFFF", "#FF00FF", "#A52A2A", "#808080", "#000000"};
        int colorIndex = 0;

        for (String yColumn : selectedYColumns) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            String yUnit = columnUnits.get(yColumn);
            String seriesName = yColumn + (yUnit != null ? " (" + yUnit + ")" : "");
            series.setName(seriesName);

            // Use sequential index for X-axis to maintain order
            int index = 0;
            for (SolarDataPoint point : tableOrderDataPoints) {
                Number xValue = getNumericAxisValue(point, xAxisColumn, index);
                Number yValue = getNumericColumnValue(point, yColumn);

                if (xValue != null && yValue != null) {
                    series.getData().add(new XYChart.Data<>(xValue, yValue));
                }
                index++;
            }

            lineChart.getData().add(series);

            // Apply color to the series
            if (colorIndex < colors.length) {
                final String color = colors[colorIndex];

                series.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-stroke: " + color + ";");
                    }
                });
            }
            colorIndex++;
        }

        // Auto-scale axes properly
        autoScaleAxes();
        applyEnhancedChartStyling();
    }

    /**
     * Get numeric value for X-axis - use index for ordering, actual value for display
     */
    private Number getNumericAxisValue(SolarDataPoint point, String columnName, int index) {
        // For proper scaling, we use the index to maintain data order
        // But we could use actual numeric values if the column is numeric
        if (columnName.equals("Time")) {
            // For time, use index to maintain order but we'll format the labels
            return index;
        } else {
            // For numeric columns, use the actual value
            return getNumericColumnValue(point, columnName);
        }
    }

    /**
     * Auto-scale both X and Y axes properly
     */
    private void autoScaleAxes() {
        if (dataPoints.isEmpty() || selectedYColumns.isEmpty()) return;

        // Auto-range both axes
        xAxis.setAutoRanging(true);
        yAxis.setAutoRanging(true);
    }

    /**
     * Returns formatted axis value WITHOUT units (just the numeric value or time)
     */
    private String getFormattedAxisValue(SolarDataPoint point, String columnName) {
        switch (columnName) {
            case "Time":
                return point.getTime(); // Display time as entered (e.g., "08:00")
            case "Solar Radiation":
                return String.format("%.1f", point.getSolarRadiation());
            case "V_mono":
                return String.format("%.2f", point.getVMono());
            case "V_poly":
                return String.format("%.2f", point.getVPoly());
            case "I_mono":
                return String.format("%.2f", point.getIMono());
            case "I_poly":
                return String.format("%.2f", point.getIPoly());
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

    // Zoom methods
    @FXML
    private void handleZoomIn() {
        updateStatus("Zoom not available with current axis configuration");
    }

    @FXML
    private void handleZoomOut() {
        updateStatus("Zoom not available with current axis configuration");
    }

    @FXML
    private void handleResetZoom() {
        updateStatus("View reset");
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