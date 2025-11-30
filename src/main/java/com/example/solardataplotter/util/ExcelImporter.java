package com.example.solardataplotter.util;

import com.example.solardataplotter.model.SolarDataPoint;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ExcelImporter {

    public static List<SolarDataPoint> importData(File file) {
        String fileName = file.getName().toLowerCase();
        System.out.println("Importing file: " + fileName);

        try {
            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                return importExcelData(file, fileName);
            } else if (fileName.endsWith(".csv")) {
                return importCsvData(file);
            } else {
                throw new IllegalArgumentException("Unsupported file format: " + fileName);
            }
        } catch (Exception e) {
            System.err.println("Error in importData: " + e.getMessage());
            throw new RuntimeException("Failed to import data: " + e.getMessage(), e);
        }
    }

    private static List<SolarDataPoint> importExcelData(File file, String fileName) {
        List<SolarDataPoint> dataPoints = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = fileName.endsWith(".xlsx") ?
                     new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Get first sheet
            System.out.println("Excel sheet: " + sheet.getSheetName());
            System.out.println("Total rows: " + (sheet.getLastRowNum() + 1));

            // Check header row to verify format
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                System.out.print("Header columns: ");
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    if (cell != null) {
                        System.out.print("[" + i + "]=" + getCellValue(cell) + " ");
                    }
                }
                System.out.println();
            }

            // Skip header row (row 0) and start from row 1
            int validRows = 0;
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    SolarDataPoint point = createDataPointFromRow(row);
                    if (point != null) {
                        dataPoints.add(point);
                        validRows++;
                    }
                }
            }

            System.out.println("Successfully imported " + validRows + " data points from Excel");

        } catch (Exception e) {
            System.err.println("Error reading Excel file: " + e.getMessage());
            throw new RuntimeException("Error reading Excel file: " + e.getMessage(), e);
        }

        return dataPoints;
    }

    private static List<SolarDataPoint> importCsvData(File file) {
        List<SolarDataPoint> dataPoints = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean isFirstLine = true;
            int lineNumber = 0;
            int validRows = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (isFirstLine) {
                    System.out.println("CSV Header: " + line);
                    isFirstLine = false;
                    continue; // Skip header row
                }

                if (!line.trim().isEmpty()) {
                    SolarDataPoint point = createDataPointFromCsv(line);
                    if (point != null) {
                        dataPoints.add(point);
                        validRows++;
                    }
                }
            }

            System.out.println("Successfully imported " + validRows + " data points from CSV (total lines: " + lineNumber + ")");

        } catch (Exception e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
            throw new RuntimeException("Error reading CSV file: " + e.getMessage(), e);
        }

        return dataPoints;
    }

    private static SolarDataPoint createDataPointFromRow(Row row) {
        try {
            SolarDataPoint point = new SolarDataPoint();
            int colIndex = 0;

            // Time (column 0)
            String timeValue = getCellValue(row.getCell(colIndex));
            if (timeValue != null && !timeValue.trim().isEmpty()) {
                point.setTime(timeValue.trim());
            } else {
                point.setTime("00:00"); // Default value
            }
            colIndex++;

            // Solar Radiation (column 1)
            Double solarRad = getNumericCellValue(row.getCell(colIndex));
            point.setSolarRadiation(solarRad != null ? solarRad : 0.0);
            colIndex++;

            // V_mono (column 2)
            Double vMono = getNumericCellValue(row.getCell(colIndex));
            point.setVMono(vMono != null ? vMono : 0.0);
            colIndex++;

            // V_poly (column 3)
            Double vPoly = getNumericCellValue(row.getCell(colIndex));
            point.setVPoly(vPoly != null ? vPoly : 0.0);
            colIndex++;

            // I_mono (column 4)
            Double iMono = getNumericCellValue(row.getCell(colIndex));
            point.setIMono(iMono != null ? iMono : 0.0);
            colIndex++;

            // I_poly (column 5)
            Double iPoly = getNumericCellValue(row.getCell(colIndex));
            point.setIPoly(iPoly != null ? iPoly : 0.0);
            colIndex++;

            // P_mono (column 6)
            Double pMono = getNumericCellValue(row.getCell(colIndex));
            point.setPMono(pMono != null ? pMono : 0.0);
            colIndex++;

            // P_poly (column 7)
            Double pPoly = getNumericCellValue(row.getCell(colIndex));
            point.setPPoly(pPoly != null ? pPoly : 0.0);
            colIndex++;

            // Eff_mono (column 8)
            Double effMono = getNumericCellValue(row.getCell(colIndex));
            point.setEffMono(effMono != null ? effMono : 0.0);
            colIndex++;

            // Eff_poly (column 9)
            Double effPoly = getNumericCellValue(row.getCell(colIndex));
            point.setEffPoly(effPoly != null ? effPoly : 0.0);
            colIndex++;

            // RH (column 10)
            Double rh = getNumericCellValue(row.getCell(colIndex));
            point.setRh(rh != null ? rh : 0.0);
            colIndex++;

            // Panel Temp Mono (column 11)
            Double tempMono = getNumericCellValue(row.getCell(colIndex));
            point.setPanelTempMono(tempMono != null ? tempMono : 0.0);
            colIndex++;

            // Panel Temp Poly (column 12)
            Double tempPoly = getNumericCellValue(row.getCell(colIndex));
            point.setPanelTempPoly(tempPoly != null ? tempPoly : 0.0);
            colIndex++;

            // Ambient Temp (column 13)
            Double ambientTemp = getNumericCellValue(row.getCell(colIndex));
            point.setAmbientTemp(ambientTemp != null ? ambientTemp : 0.0);
            colIndex++;

            // Wind Speed (column 14)
            Double windSpeed = getNumericCellValue(row.getCell(colIndex));
            point.setWindSpeed(windSpeed != null ? windSpeed : 0.0);

            return point;

        } catch (Exception e) {
            System.err.println("Error creating data point from row: " + e.getMessage());
            return null;
        }
    }

    private static SolarDataPoint createDataPointFromCsv(String csvLine) {
        try {
            String[] values = csvLine.split(",", -1); // -1 to keep trailing empty values
            if (values.length < 15) {
                System.err.println("CSV line has only " + values.length + " columns, expected 15");
                return null;
            }

            SolarDataPoint point = new SolarDataPoint();

            // Trim all values and handle empty strings
            point.setTime(values[0].trim().isEmpty() ? "00:00" : values[0].trim());
            point.setSolarRadiation(parseDoubleSafe(values[1]));
            point.setVMono(parseDoubleSafe(values[2]));
            point.setVPoly(parseDoubleSafe(values[3]));
            point.setIMono(parseDoubleSafe(values[4]));
            point.setIPoly(parseDoubleSafe(values[5]));
            point.setPMono(parseDoubleSafe(values[6]));
            point.setPPoly(parseDoubleSafe(values[7]));
            point.setEffMono(parseDoubleSafe(values[8]));
            point.setEffPoly(parseDoubleSafe(values[9]));
            point.setRh(parseDoubleSafe(values[10]));
            point.setPanelTempMono(parseDoubleSafe(values[11]));
            point.setPanelTempPoly(parseDoubleSafe(values[12]));
            point.setAmbientTemp(parseDoubleSafe(values[13]));
            point.setWindSpeed(parseDoubleSafe(values[14]));

            return point;

        } catch (Exception e) {
            System.err.println("Error creating data point from CSV: " + e.getMessage());
            return null;
        }
    }

    private static String getCellValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue().trim();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        // Format time as HH:mm if it's a date
                        java.util.Date date = cell.getDateCellValue();
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
                        return sdf.format(date);
                    } else {
                        // Format numeric value without decimals if it's a whole number
                        double value = cell.getNumericCellValue();
                        if (value == Math.floor(value)) {
                            return String.valueOf((int) value);
                        } else {
                            return String.valueOf(value);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    // Try to evaluate formula
                    try {
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(cell);
                        switch (cellValue.getCellType()) {
                            case STRING: return cellValue.getStringValue();
                            case NUMERIC: return String.valueOf(cellValue.getNumberValue());
                            case BOOLEAN: return String.valueOf(cellValue.getBooleanValue());
                            default: return cell.getCellFormula();
                        }
                    } catch (Exception e) {
                        return cell.getCellFormula();
                    }
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error getting cell value: " + e.getMessage());
            return null;
        }
    }

    private static Double getNumericCellValue(Cell cell) {
        if (cell == null) return null;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    return parseDoubleSafe(cell.getStringCellValue());
                case FORMULA:
                    FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                    CellValue cellValue = evaluator.evaluate(cell);
                    if (cellValue.getCellType() == CellType.NUMERIC) {
                        return cellValue.getNumberValue();
                    } else if (cellValue.getCellType() == CellType.STRING) {
                        return parseDoubleSafe(cellValue.getStringValue());
                    }
                    return null;
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error getting numeric cell value: " + e.getMessage());
            return null;
        }
    }

    private static Double parseDoubleSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim().replace(",", "")); // Handle commas in numbers
        } catch (NumberFormatException e) {
            System.err.println("Cannot parse as double: '" + value + "'");
            return 0.0;
        }
    }
}