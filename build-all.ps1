# AltarLegendaryWH Multi-Version Build Script for PowerShell
$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "   AltarLegendaryWH Multi-Version Build Tool" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

if (Test-Path "C:\Program Files\Java\jdk-25.0.4") {
    $env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.4"
    $env:PATH = "C:\Program Files\Java\jdk-25.0.4\bin;$($env:PATH)"
}

if (-not (Test-Path "dist")) {
    New-Item -ItemType Directory -Path "dist" | Out-Null
}

Write-Host "`n[1/4] Building UNIVERSAL jar (Java 21 bytecode, runs on 1.21.11 / 26.1 / 26.3, Java 21 / 25, Paper / Purpur / Spigot)..." -ForegroundColor Yellow
cmd.exe /c "mvn clean package -Puniversal"
if ($LASTEXITCODE -ne 0) { throw "Build failed for universal jar" }
Copy-Item "target\AltarLegendaryWH-1.4.2.jar" "dist\AltarLegendaryWH-1.4.2.jar" -Force
Copy-Item "target\AltarLegendaryWH-1.4.2.jar" "dist\AltarLegendaryWH-1.4.2-1.21.11.jar" -Force

Write-Host "`n[2/4] Building for 1.21.11 on Java 25 bytecode..." -ForegroundColor Yellow
cmd.exe /c "mvn clean package -P1.21.11-java25"
if ($LASTEXITCODE -ne 0) { throw "Build failed for 1.21.11-java25" }
Copy-Item "target\AltarLegendaryWH-1.4.2-1.21.11-java25.jar" "dist\AltarLegendaryWH-1.4.2-1.21.11-java25.jar" -Force

Write-Host "`n[3/4] Building for Paper/Purpur/Spigot 26.1 (Java 25 bytecode)..." -ForegroundColor Yellow
cmd.exe /c "mvn clean package -P26.1"
if ($LASTEXITCODE -ne 0) { throw "Build failed for 26.1" }
Copy-Item "target\AltarLegendaryWH-1.4.2-26.1.jar" "dist\AltarLegendaryWH-1.4.2-26.1.jar" -Force

Write-Host "`n[4/4] Building for Paper/Purpur/Spigot 26.3 (Java 25 bytecode)..." -ForegroundColor Yellow
cmd.exe /c "mvn clean package -P26.3"
if ($LASTEXITCODE -ne 0) { throw "Build failed for 26.3" }
Copy-Item "target\AltarLegendaryWH-1.4.2-26.3.jar" "dist\AltarLegendaryWH-1.4.2-26.3.jar" -Force

Copy-Item "dist\*.jar" "target\" -Force

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host "   BUILD COMPLETED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
Write-Host "Built artifacts located in dist\ and target\:"
Write-Host "  - AltarLegendaryWH-1.4.2.jar                [RECOMMENDED UNIVERSAL JAR]"
Write-Host "  - AltarLegendaryWH-1.4.2-1.21.11.jar        (Paper/Purpur/Spigot 1.21.11, Java 21)"
Write-Host "  - AltarLegendaryWH-1.4.2-1.21.11-java25.jar (Paper/Purpur/Spigot 1.21.11, Java 25)"
Write-Host "  - AltarLegendaryWH-1.4.2-26.1.jar           (Paper/Purpur/Spigot 26.1, Java 25)"
Write-Host "  - AltarLegendaryWH-1.4.2-26.3.jar           (Paper/Purpur/Spigot 26.3, Java 25)"
Write-Host "========================================================" -ForegroundColor Green
