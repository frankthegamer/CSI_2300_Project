@echo off
setlocal

REM Set paths
set DIR=%~dp0
set LIB_DIR=%DIR%lib
set JAVAFX_LIB=%DIR%javafx-sdk-24\lib
set JAVAFX_BIN=%DIR%javafx-sdk-24\bin

REM Add JavaFX DLLs to path
set PATH=%JAVAFX_BIN%;%PATH%

REM Launch the app
java ^
--module-path "%JAVAFX_LIB%" ^
--add-modules javafx.controls,javafx.fxml ^
-jar "%DIR%NeatFile.jar"

pause
