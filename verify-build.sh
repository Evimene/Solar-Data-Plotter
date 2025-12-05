#!/bin/bash
echo "=== Solar Data Plotter Build Verification ==="
echo ""

# Check Java
echo "1. Java Installation:"
java -version 2>&1 | head -3
echo ""

# Check Maven
echo "2. Maven Installation:"
mvn -version 2>&1 | head -3
echo ""

# Check project structure
echo "3. Project Structure:"
[ -f "pom.xml" ] && echo "✓ pom.xml exists" || echo "✗ pom.xml missing"
[ -d "src/main/java" ] && echo "✓ Java source exists" || echo "✗ Java source missing"
[ -d "src/main/resources" ] && echo "✓ Resources exist" || echo "✗ Resources missing"
echo ""

# Build
echo "4. Building project..."
mvn clean compile 2>&1 | tail -20
echo ""

# Package
echo "5. Packaging..."
mvn package -DskipTests 2>&1 | tail -20
echo ""

# Check output
echo "6. Build Output:"
if [ -f "target/SolarDataPlotter.jar" ]; then
    echo "✓ JAR file created"
    echo "  Size: $(du -h target/SolarDataPlotter.jar | cut -f1)"
    echo ""
    echo "7. Testing JAR..."
    java -jar target/SolarDataPlotter.jar --test 2>&1
else
    echo "✗ JAR file NOT created"
    echo ""
    echo "Troubleshooting:"
    echo "  - Check Maven dependencies"
    echo "  - Check JavaFX is included"
    echo "  - Check module-info.java (remove if causing issues)"
fi