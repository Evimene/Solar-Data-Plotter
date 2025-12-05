@echo off
echo === Solar Data Plotter Build Diagnostics ===
echo.

echo 1. Checking Java installation...
java -version
echo.

echo 2. Checking Maven installation...
mvn -version
echo.

echo 3. Cleaning previous build...
mvn clean
echo.

echo 4. Compiling project...
mvn compile
echo.

echo 5. Building JAR...
mvn package -DskipTests
echo.

echo 6. Listing built files...
dir target\
echo.

echo 7. Testing JAR file...
if exist "target\SolarDataPlotter.jar" (
    echo JAR file exists. Testing...
    java -jar target\SolarDataPlotter.jar --test
) else (
    echo ERROR: JAR file not created!
)

echo.
echo === Diagnostics Complete ===
pause