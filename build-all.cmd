@echo off
setlocal enabledelayedexpansion

echo ========================================================
echo   AltarLegendaryWH Multi-Version Build Tool
echo ========================================================

rem Look for Java 25 if JAVA_HOME is not set or not Java 25
if exist "C:\Program Files\Java\jdk-25.0.4" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-25.0.4"
    set "PATH=C:\Program Files\Java\jdk-25.0.4\bin;%PATH%"
) else if not defined JAVA_HOME (
    echo [WARNING] JAVA_HOME is not set and C:\Program Files\Java\jdk-25.0.4 was not found. Using default java in PATH.
)

if not exist dist mkdir dist

echo.
echo [1/4] Building UNIVERSAL jar (Java 21 bytecode, runs on 1.21.11 / 26.1 / 26.3, Java 21 / 25, Paper / Purpur / Spigot)...
call mvn clean package -Puniversal
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed for universal jar!
    exit /b %ERRORLEVEL%
)
copy /y target\AltarLegendaryWH-1.4.2.jar dist\AltarLegendaryWH-1.4.2.jar >nul
copy /y target\AltarLegendaryWH-1.4.2.jar dist\AltarLegendaryWH-1.4.2-1.21.11.jar >nul

echo.
echo [2/4] Building for 1.21.11 on Java 25 bytecode...
call mvn clean package -P1.21.11-java25
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed for 1.21.11-java25!
    exit /b %ERRORLEVEL%
)
copy /y target\AltarLegendaryWH-1.4.2-1.21.11-java25.jar dist\AltarLegendaryWH-1.4.2-1.21.11-java25.jar >nul

echo.
echo [3/4] Building for Paper/Purpur/Spigot 26.1 (Java 25 bytecode)...
call mvn clean package -P26.1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed for 26.1!
    exit /b %ERRORLEVEL%
)
copy /y target\AltarLegendaryWH-1.4.2-26.1.jar dist\AltarLegendaryWH-1.4.2-26.1.jar >nul

echo.
echo [4/4] Building for Paper/Purpur/Spigot 26.3 (Java 25 bytecode)...
call mvn clean package -P26.3
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Build failed for 26.3!
    exit /b %ERRORLEVEL%
)
copy /y target\AltarLegendaryWH-1.4.2-26.3.jar dist\AltarLegendaryWH-1.4.2-26.3.jar >nul

rem Populate target directory with all jars as well
copy /y dist\*.jar target\ >nul

echo.
echo ========================================================
echo   BUILD COMPLETED SUCCESSFULLY!
echo ========================================================
echo Built artifacts located in dist\ and target\:
echo   - AltarLegendaryWH-1.4.2.jar                [RECOMMENDED UNIVERSAL JAR]
echo   - AltarLegendaryWH-1.4.2-1.21.11.jar        (Paper/Purpur/Spigot 1.21.11, Java 21)
echo   - AltarLegendaryWH-1.4.2-1.21.11-java25.jar (Paper/Purpur/Spigot 1.21.11, Java 25)
echo   - AltarLegendaryWH-1.4.2-26.1.jar           (Paper/Purpur/Spigot 26.1, Java 25)
echo   - AltarLegendaryWH-1.4.2-26.3.jar           (Paper/Purpur/Spigot 26.3, Java 25)
echo ========================================================
