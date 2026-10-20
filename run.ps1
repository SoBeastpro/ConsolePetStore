$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Find-Jdk {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        $version = & (Join-Path $env:JAVA_HOME "bin\java.exe") -version 2>&1 | Out-String
        if ($version -notmatch 'version "1\.[0-8]') {
            return $env:JAVA_HOME
        }
    }
    $jdks = Join-Path $env:USERPROFILE ".jdks"
    if (Test-Path $jdks) {
        $candidate = Get-ChildItem $jdks -Directory | Where-Object { $_.Name -notlike ".*" } | Sort-Object Name -Descending | Select-Object -First 1
        if ($candidate) {
            return $candidate.FullName
        }
    }
    throw "Не найден JDK 21 или новее. Укажите его в JAVA_HOME."
}

function Find-Maven {
    $cmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }
    $roots = @(
        "D:\JetBrains",
        "C:\Program Files\JetBrains",
        (Join-Path $env:LOCALAPPDATA "Programs")
    )
    foreach ($root in $roots) {
        if (-not (Test-Path $root)) {
            continue
        }
        $found = Get-ChildItem $root -Recurse -Filter "mvn.cmd" -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($found) {
            return $found.FullName
        }
    }
    throw "Не найден Maven 3.9+. Добавьте mvn в PATH или откройте проект в IntelliJ IDEA."
}

$env:JAVA_HOME = Find-Jdk
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
Write-Host "JAVA_HOME=$env:JAVA_HOME"
& (Find-Maven) javafx:run
