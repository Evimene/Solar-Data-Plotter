// DataValidator.java
package com.example.solardataplotter.util;

import java.util.regex.Pattern;

public class DataValidator {
    private static final Pattern TIME_PATTERN = Pattern.compile("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");

    public static boolean isValidTime(String time) {
        return time != null && TIME_PATTERN.matcher(time).matches();
    }

    public static boolean isValidNumber(String number) {
        try {
            Double.parseDouble(number);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isValidPercentage(String percentage) {
        if (!isValidNumber(percentage)) return false;
        double value = Double.parseDouble(percentage);
        return value >= 0 && value <= 100;
    }

    public static boolean isValidCoordinate(String coordinate) {
        // Basic coordinate validation
        return coordinate != null && !coordinate.trim().isEmpty();
    }
}