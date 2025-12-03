package com.example.solardataplotter.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
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
import javafx.scene.input.KeyEvent;

import java.io.File;
import java.net.URL;
import java.util.*;

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
    @FXML private LineChart<Number, Number> lineChart; // Changed to Number,Number
    @FXML private NumberAxis xAxis; // Changed to NumberAxis
    @FXML private NumberAxis yAxis; // Changed to NumberAxis
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

    @FXML private TextField yAxisStartField;

    private void setupResponsiveChart() {
        lineChart.prefWidthProperty().bind(graphContainer.widthProperty());
        lineChart.prefHeightProperty().bind(graphContainer.heightProperty());
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

        // Add grouped column units
        columnUnits.put("Voltage", "V");
        columnUnits.put("Current", "A");
        columnUnits.put("Power", "W");
        columnUnits.put("Efficiency", "%");
        columnUnits.put("Panel Temperature", "°C");

        // Set default values
        graphConfig.setXAxisLabel("");
        graphConfig.setYAxisLabel("");
        graphConfig.setExperimentLocation("");

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
        dataTable.setOnKeyPressed(this::handleTableKeyPress);
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
                if (currentRow < rowCount - 1) {
                    dataTable.getSelectionModel().clearAndSelect(currentRow + 1, dataTable.getColumns().get(currentCol));
                } else {
                    handleAddData();
                }
                event.consume();
                break;

            case TAB:
                if (event.isShiftDown()) {
                    if (currentCol > 0) {
                        dataTable.getSelectionModel().clearAndSelect(currentRow, dataTable.getColumns().get(currentCol - 1));
                    }
                } else {
                    if (currentCol < colCount - 1) {
                        dataTable.getSelectionModel().clearAndSelect(currentRow, dataTable.getColumns().get(currentCol + 1));
                    } else if (currentRow < rowCount - 1) {
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
        // Grouped columns for X-axis
        ObservableList<String> xAxisColumns = FXCollections.observableArrayList(
                "Time",
                "Solar Radiation",
                "Voltage",  // Combined: V_mono and V_poly
                "Current",  // Combined: I_mono and I_poly
                "Power",    // Combined: P_mono and P_poly
                "Efficiency", // Combined: Eff_mono and Eff_poly
                "RH",
                "Panel Temperature", // Combined: Panel Temp Mono and Panel Temp Poly
                "Ambient Temp",
                "Wind Speed"
        );

        // Separate columns for Y-axis (for selection)
        ObservableList<String> allYColumns = FXCollections.observableArrayList(
                "Time", "Solar Radiation", "V_mono", "V_poly", "I_mono", "I_poly",
                "P_mono", "P_poly", "Eff_mono", "Eff_poly", "RH", "Panel Temp Mono",
                "Panel Temp Poly", "Ambient Temp", "Wind Speed"
        );

        xAxisCombo.setItems(xAxisColumns);

        // Improved CheckBoxListView for Y-axis
        yAxisList.setCellFactory(param -> new ListCell<String>() {
            private final CheckBox checkBox = new CheckBox();
            private javafx.beans.value.ChangeListener<Boolean> listener;

            {
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
                checkBox.selectedProperty().addListener(listener);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
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
                checkBox.selectedProperty().addListener(listener);
            }
        });

        yAxisList.setItems(allYColumns);

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

        // Set chart title
        String title = buildGraphTitle();
        lineChart.setTitle(title);
        String xAxisGroup = xAxisCombo.getSelectionModel().getSelectedItem();

        // Setup time axis formatting if Time is selected
        setupTimeAxisFormatting(xAxisGroup);

        // Update X-axis label with proper unit
        String xUnit = columnUnits.get(xAxisGroup);
        if (xUnit == null) {
            // Set default units for grouped columns
            if ("Voltage".equals(xAxisGroup)) xUnit = "V";
            else if ("Current".equals(xAxisGroup)) xUnit = "A";
            else if ("Power".equals(xAxisGroup)) xUnit = "W";
            else if ("Efficiency".equals(xAxisGroup)) xUnit = "%";
            else if ("Panel Temperature".equals(xAxisGroup)) xUnit = "°C";
        }

        String currentXLabel = graphConfig.getXAxisLabel();
        if (currentXLabel == null || currentXLabel.trim().isEmpty() ||
                currentXLabel.equals("Time") || currentXLabel.equals("X-Axis")) {
            xAxis.setLabel(xAxisGroup + (xUnit != null ? " (" + xUnit + ")" : ""));
        } else {
            xAxis.setLabel(currentXLabel);
        }

        // Update Y-axis label
        String currentYLabel = graphConfig.getYAxisLabel();
        if (currentYLabel == null || currentYLabel.trim().isEmpty() ||
                currentYLabel.equals("Values") || currentYLabel.equals("Y-Axis")) {
            if (selectedYColumns.size() == 1) {
                String yColumn = selectedYColumns.iterator().next();
                String yUnit = columnUnits.get(yColumn);
                yAxis.setLabel(yColumn + (yUnit != null ? " (" + yUnit + ")" : ""));
            } else {
                yAxis.setLabel("Parameters");
            }
        } else {
            yAxis.setLabel(currentYLabel);
        }

        // Create series for each selected Y column
        String[] colors = {"#FF0000", "#0000FF", "#008000", "#FFA500", "#800080",
                "#00FFFF", "#FF00FF", "#A52A2A", "#808080", "#000000"};
        int colorIndex = 0;

        // Store min/max values for proper scaling
        double xMin = Double.MAX_VALUE;
        double xMax = Double.MIN_VALUE;
        double yMin = Double.MAX_VALUE;
        double yMax = Double.MIN_VALUE;

        List<XYChart.Series<Number, Number>> allSeries = new ArrayList<>();

        for (String yColumn : selectedYColumns) {
            XYChart.Series<Number, Number> series = new XYChart.Series<>();
            String yUnit = columnUnits.get(yColumn);
            String seriesName = yColumn + (yUnit != null ? " (" + yUnit + ")" : "");
            series.setName(seriesName);

            // Add data points with proper numeric values
            for (int i = 0; i < dataPoints.size(); i++) {
                SolarDataPoint point = dataPoints.get(i);
                // Use grouped X-value based on Y-column type
                Number xValue = getGroupedXValue(point, xAxisGroup, yColumn, i);
                Number yValue = getNumericColumnValue(point, yColumn);

                if (xValue != null && yValue != null) {
                    double xDouble = xValue.doubleValue();
                    double yDouble = yValue.doubleValue();

                    // Update min/max values
                    xMin = Math.min(xMin, xDouble);
                    xMax = Math.max(xMax, xDouble);
                    yMin = Math.min(yMin, yDouble);
                    yMax = Math.max(yMax, yDouble);

                    XYChart.Data<Number, Number> dataPoint = new XYChart.Data<>(xValue, yValue);
                    series.getData().add(dataPoint);

                    // MODIFICATION 3: Customize the data point node for better visibility
                    int finalColorIndex = colorIndex;
                    dataPoint.nodeProperty().addListener((obs, oldNode, newNode) -> {
                        if (newNode != null) {
                            newNode.setStyle("-fx-background-color: " + colors[finalColorIndex] + "; " +
                                    "-fx-background-radius: 4; " +
                                    "-fx-padding: 4px;");
                            newNode.setScaleX(1.5);
                            newNode.setScaleY(1.5);
                        }
                    });
                }
            }

            allSeries.add(series);

            // Apply color to the series (transparent line, colored points)
            if (colorIndex < colors.length) {
                final String color = colors[colorIndex];

                // MODIFICATION 3: Make line transparent, only show points
                series.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        // Set line to be completely transparent
                        newNode.setStyle("-fx-stroke: transparent; -fx-stroke-width: 0;");
                    }
                });

                // Style the symbols (points)
                Platform.runLater(() -> {
                    for (XYChart.Data<Number, Number> data : series.getData()) {
                        Node node = data.getNode();
                        if (node != null) {
                            node.setStyle("-fx-background-color: " + color + ", white; " +
                                    "-fx-background-radius: 4; " +
                                    "-fx-background-insets: 0, 2; " +
                                    "-fx-padding: 4px;");
                            node.setScaleX(1.5);
                            node.setScaleY(1.5);
                        }
                    }
                });
            }
            colorIndex = (colorIndex + 1) % colors.length;
        }

        // Add all series to chart
        lineChart.getData().addAll(allSeries);

        // Set proper axis scaling (starting from 0,0)
        setAxisScaling(xMin, xMax, yMin, yMax);

        // Apply enhanced styling
        applyEnhancedChartStyling();
        applyYAxisLabelMargins();
        styleYAxisLabel();
        adjustChartPadding();
    }


    /**
     * Get numeric value for X-axis - convert time to minutes for proper plotting
     */
    private Number getNumericXValue(SolarDataPoint point, String columnName, int index) {
        if ("Time".equals(columnName)) {
            // Convert time string "HH:mm" to total minutes
            String time = point.getTime();
            if (time != null && time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                String[] parts = time.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                return (double) (hours * 60 + minutes);
            }
        }

        // For other columns, use the actual numeric value
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
            default: return (double) index; // Fallback to index
        }
    }

    /**
     * Set proper axis scaling from min to max values
     */
    private void setAxisScaling(double xMin, double xMax, double yMin, double yMax) {
        if (xMin == Double.MAX_VALUE || xMax == Double.MIN_VALUE ||
                yMin == Double.MAX_VALUE || yMax == Double.MIN_VALUE) {
            return;
        }

        // Get the current X-axis selection
        String currentXGroup = xAxisCombo.getSelectionModel().getSelectedItem();

        // MODIFICATION 1: Always start from 0,0 for positive values
        // For X-axis: if values are positive, start from 0
        // But keep Time as-is (it's in minutes, 0 = 00:00)
        double xLowerBound;
        if ("Time".equals(currentXGroup)) {
            xLowerBound = 0; // Time starts from actual first time
        } else {
            xLowerBound = (xMin >= 0) ? 0 : xMin; // Others start from 0 if positive
        }

        // For Y-axis: always start from 0 for positive values
        double yLowerBound = yMin >= 0 ? 0 : yMin;


        // Add padding to axis ranges
        double xPadding = (xMax - xLowerBound) * 0.05; // Reduced padding
        double yPadding = (yMax - yLowerBound) * 0.05; // Reduced padding

        // Ensure we have at least some range
        if (xPadding == 0) xPadding = 1.0;
        if (yPadding == 0) yPadding = 1.0;

        // Set X-axis bounds
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(Math.max(0, xLowerBound - xPadding * 0.1));
        xAxis.setUpperBound(xMax + xPadding);

        // Set Y-axis bounds - always start from 0 for positive values
        yAxis.setAutoRanging(false);

        // If user specified Y-start, use it, otherwise use 0 for positive values
        try {
            double yStart = Double.parseDouble(yAxisStartField.getText());
            yAxis.setLowerBound(yStart);
        } catch (NumberFormatException e) {
            yAxis.setLowerBound(yLowerBound);
        }
        yAxis.setUpperBound(yMax + yPadding);

        // Set appropriate tick units
        xAxis.setTickUnit(Math.max(1.0, (xMax + xPadding) / 10));
        yAxis.setTickUnit(Math.max(0.1, (yMax + yPadding) / 10));
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

    /**
     * Set up time formatting for X-axis when Time is selected
     */
    private void setupTimeAxisFormatting(String xAxisColumn) {
        if ("Time".equals(xAxisColumn)) {
            xAxis.setTickLabelFormatter(new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    int totalMinutes = object.intValue();
                    int hours = totalMinutes / 60;
                    int minutes = totalMinutes % 60;
                    return String.format("%02d:%02d", hours, minutes);
                }

                @Override
                public Number fromString(String string) {
                    // Convert time string back to minutes
                    if (string != null && string.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                        String[] parts = string.split(":");
                        int hours = Integer.parseInt(parts[0]);
                        int minutes = Integer.parseInt(parts[1]);
                        return hours * 60 + minutes;
                    }
                    return 0;
                }
            });
        } else {
            // Reset to default numeric formatting for other columns
            xAxis.setTickLabelFormatter(new StringConverter<Number>() {
                @Override
                public String toString(Number object) {
                    return String.format("%.1f", object.doubleValue());
                }

                @Override
                public Number fromString(String string) {
                    try {
                        return Double.parseDouble(string);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            });
        }
    }

    private void applyEnhancedChartStyling() {
        // MODIFICATION 3: Hide lines, show only points
        lineChart.setStyle("-fx-background-color: white; -fx-border-color: #2c3e50; " +
                "-fx-border-width: 2; -fx-border-radius: 5; -fx-background-radius: 5;");
        lineChart.setLegendVisible(true);
        lineChart.setAnimated(false);
        lineChart.setCreateSymbols(true);  // Ensure symbols are created

        // Make lines transparent
        lineChart.lookupAll(".chart-series-line").forEach(node ->
                node.setStyle("-fx-stroke: transparent; -fx-stroke-width: 0;")
        );

        // Make symbols larger
        lineChart.lookupAll(".chart-line-symbol").forEach(node -> {
            node.setStyle("-fx-background-radius: 5; -fx-padding: 5px;");
            node.setScaleX(1.5);
            node.setScaleY(1.5);
        });

        // Enhanced styling
        lineChart.lookup(".chart-plot-background").setStyle(
                "-fx-background-color: transparent; -fx-border-color: #bdc3c7; -fx-border-width: 1;"
        );

        // Grid lines styling
        xAxis.setStyle("-fx-tick-label-fill: #2c3e50; -fx-border-color: transparent;");
        yAxis.setStyle("-fx-tick-label-fill: #2c3e50; -fx-border-color: transparent;");

        // Legend styling
        Node legend = lineChart.lookup(".chart-legend");
        if (legend != null) {
            legend.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; " +
                    "-fx-border-width: 1; -fx-padding: 10;");
        }
    }

    // Simple zoom methods for NumberAxis
    @FXML
    private void handleZoomIn() {
        // You can implement zoom by adjusting axis bounds
        updateStatus("Use mouse scroll to zoom on NumberAxis");
    }

    @FXML
    private void handleZoomOut() {
        updateStatus("Use mouse scroll to zoom on NumberAxis");
    }

    @FXML
    private void handleResetZoom() {
        generateGraph(); // Regenerate to reset scaling
        updateStatus("View reset to default scaling");
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

    @FXML
    private void handleApplyYStart() {
        try {
            if (!dataPoints.isEmpty() && !selectedYColumns.isEmpty()) {
                generateGraph();
                updateStatus("Y-axis start value applied");
            } else {
                showAlert("No Data", "Please add data and generate graph first.");
            }
        } catch (Exception e) {
            showAlert("Error", "Invalid Y-axis start value: " + e.getMessage());
        }
    }


    @FXML
    private void handleAutoScale() {
        yAxisStartField.setText("0.0");
        if (!dataPoints.isEmpty() && !selectedYColumns.isEmpty()) {
            generateGraph();
            updateStatus("Auto scaling applied");
        }
    }

    /**
     * Apply Y-axis label styling with proper margins
     */
    private void applyYAxisLabelMargins() {
        // Apply styling directly to the Y-axis
        yAxis.setStyle("-fx-tick-label-fill: #2c3e50; " +
                "-fx-tick-label-font-size: 11; " +
                "-fx-minor-tick-visible: false; " +
                "-fx-tick-label-gap: 10;"); // This creates space between tick labels and axis

        // We need to manipulate the Y-axis label node directly
        Platform.runLater(() -> {
            try {
                // Find the Y-axis label node
                Node yAxisLabel = yAxis.lookup(".axis-label");
                if (yAxisLabel != null) {
                    // Apply specific styling to create margin
                    yAxisLabel.setStyle("-fx-font-weight: bold; " +
                            "-fx-font-size: 14; " +
                            "-fx-text-fill: #2c3e50; " +
                            "-fx-padding: 0 0 0 30;"); // Left padding pushes label left

                    // Add translation to move the label further left
                    yAxisLabel.setTranslateX(-25);

                    // Also adjust the entire Y-axis position
                    yAxis.setTranslateX(30); // Move entire Y-axis right to create space
                }
            } catch (Exception e) {
                System.out.println("Error adjusting Y-axis label: " + e.getMessage());
            }
        });
    }

    /**
     * Adjust chart padding to create space for Y-axis
     */
    private void adjustChartPadding() {
        // Set the chart padding programmatically
        lineChart.setStyle("-fx-padding: 25 35 25 100;"); // Large left padding (100px)

        // Also adjust the chart plot area
        Platform.runLater(() -> {
            Node chartContent = lineChart.lookup(".chart-content");
            if (chartContent != null) {
                chartContent.setStyle("-fx-padding: 0 0 0 20;");
            }
        });
    }


    /**
     * Apply specific Y-axis label styling with proper separation
     */
    private void styleYAxisLabel() {
        // Add a custom style class to the Y-axis label
        Platform.runLater(() -> {
            Node yAxisLabel = yAxis.lookup(".axis-label");
            if (yAxisLabel != null) {
                // Add custom style class
                yAxisLabel.getStyleClass().add("y-axis-label-custom");

                // Apply styling directly
                yAxisLabel.setStyle(
                        "-fx-font-weight: bold; " +
                                "-fx-font-size: 14; " +
                                "-fx-text-fill: #2c3e50; " +
                                "-fx-padding: 0 0 0 0; " +
                                "-fx-translate-x: -50; " +  // Move only the label left
                                "-fx-translate-y: 0;"
                );
            }

            // Ensure Y-axis tick labels stay in place
            yAxis.setStyle(
                    "-fx-tick-label-fill: #2c3e50; " +
                            "-fx-tick-label-font-size: 11; " +
                            "-fx-minor-tick-visible: false; " +
                            "-fx-translate-x: 0;"  // Keep Y-axis in place
            );

            // Move the entire Y-axis slightly right to create space
            yAxis.setTranslateX(10);
        });
    }


    private Number getGroupedXValue(SolarDataPoint point, String xAxisGroup, String yAxisColumn, int index) {
        // For time column
        if ("Time".equals(xAxisGroup)) {
            String time = point.getTime();
            if (time != null && time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                String[] parts = time.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                return (double) (hours * 60 + minutes);
            }
            return (double) index;
        }

        // For grouped columns, check Y-axis column type (mono/poly)
        boolean isMono = yAxisColumn.toLowerCase().contains("mono");
        boolean isPoly = yAxisColumn.toLowerCase().contains("poly");

        switch (xAxisGroup) {
            case "Solar Radiation":
                return point.getSolarRadiation();

            case "Voltage":
                if (isMono) return point.getVMono();
                if (isPoly) return point.getVPoly();
                // Default to mono if cannot determine
                return point.getVMono();

            case "Current":
                if (isMono) return point.getIMono();
                if (isPoly) return point.getIPoly();
                return point.getIMono();

            case "Power":
                if (isMono) return point.getPMono();
                if (isPoly) return point.getPPoly();
                return point.getPMono();

            case "Efficiency":
                if (isMono) return point.getEffMono();
                if (isPoly) return point.getEffPoly();
                return point.getEffMono();

            case "RH":
                return point.getRh();

            case "Panel Temperature":
                if (isMono) return point.getPanelTempMono();
                if (isPoly) return point.getPanelTempPoly();
                return point.getPanelTempMono();

            case "Ambient Temp":
                return point.getAmbientTemp();

            case "Wind Speed":
                return point.getWindSpeed();

            default:
                return (double) index;
        }
    }
}