#!/bin/bash
# build-windows.sh - Build Windows installer from Ubuntu

echo "=== Building Solar Data Plotter Windows Installer ==="

# Clean previous builds
echo "Cleaning previous builds..."
mvn clean

# Build the project
echo "Building project..."
mvn package -DskipTests

# Download Windows JDK for cross-compilation
echo "Downloading Windows JDK for cross-compilation..."
if [ ! -d "jdk-windows" ]; then
  wget -q https://download.java.net/java/GA/jdk21.0.2/f2283984656d49d69e91c558476027ac/13/GPL/openjdk-21.0.2_windows-x64_bin.zip
  unzip -q openjdk-21.0.2_windows-x64_bin.zip -d jdk-windows
  rm openjdk-21.0.2_windows-x64_bin.zip
fi

# Install Wine for Windows cross-compilation
echo "Installing Wine for Windows environment..."
sudo apt-get update
sudo apt-get install -y wine wine32

# Create a simple Windows executable wrapper
echo "Creating Windows launcher scripts..."
cat > target/run.bat << 'EOF'
@echo off
title Solar Data Plotter (by Ernest Evimene)
java -jar "SolarDataPlotter.jar"
pause
EOF

cat > target/SolarDataPlotter.bat << 'EOF'
@echo off
start javaw -jar "SolarDataPlotter.jar"
EOF

# Create a simple installer script using NSIS (optional)
echo "Creating NSIS installer script..."
cat > target/installer.nsi << 'EOF'
!include "MUI2.nsh"

Name "Solar Data Plotter"
OutFile "SolarDataPlotter-Setup.exe"
InstallDir "$PROGRAMFILES\SolarDataPlotter"
RequestExecutionLevel admin

!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH

!insertmacro MUI_LANGUAGE "English"

Section "Main Application"
  SetOutPath "$INSTDIR"
  File "SolarDataPlotter.jar"
  File "run.bat"
  File "SolarDataPlotter.bat"

  ; Create shortcuts
  CreateDirectory "$SMPROGRAMS\SolarDataPlotter"
  CreateShortcut "$SMPROGRAMS\SolarDataPlotter\SolarDataPlotter.lnk" "$INSTDIR\SolarDataPlotter.bat"
  CreateShortcut "$DESKTOP\SolarDataPlotter.lnk" "$INSTDIR\SolarDataPlotter.bat"

  ; Write uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\SolarDataPlotter" \
                   "DisplayName" "Solar Data Plotter"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\SolarDataPlotter" \
                   "UninstallString" '"$INSTDIR\Uninstall.exe"'
SectionEnd

Section "Uninstall"
  Delete "$INSTDIR\SolarDataPlotter.jar"
  Delete "$INSTDIR\run.bat"
  Delete "$INSTDIR\SolarDataPlotter.bat"
  Delete "$INSTDIR\Uninstall.exe"
  RMDir "$INSTDIR"

  Delete "$SMPROGRAMS\SolarDataPlotter\SolarDataPlotter.lnk"
  RMDir "$SMPROGRAMS\SolarDataPlotter"
  Delete "$DESKTOP\SolarDataPlotter.lnk"

  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\SolarDataPlotter"
SectionEnd
EOF

echo "=== Build Complete ==="
echo "Your application files are in: target/"
echo "To create an installer on Windows:"
echo "1. Copy target/SolarDataPlotter-1.0.0.jar to a Windows machine"
echo "2. Use Inno Setup or NSIS to create an installer"
echo ""
echo "Alternative: Use GitHub Actions for automated Windows builds"