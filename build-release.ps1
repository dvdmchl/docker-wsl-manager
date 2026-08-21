# Docker WSL Manager release build script
# Produces verified artifacts without modifying tracked release documentation.

param(
    [string]$OutputDir = "release-output",
    [Parameter(Mandatory = $true)]
    [string]$ReleaseNotesPath,
    [switch]$BuildMsi
)

$ErrorActionPreference = "Stop"

function Invoke-Maven {
    param([string[]]$MavenArguments)

    & mvn @MavenArguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven command failed: mvn $($MavenArguments -join ' ')"
    }
}

function Get-ProjectVersion {
    [xml]$pom = Get-Content -LiteralPath "pom.xml" -Raw
    $namespaceManager = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
    $namespaceManager.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
    $versionNode = $pom.SelectSingleNode("/m:project/m:version", $namespaceManager)
    if ($null -eq $versionNode) {
        throw "Could not find project.version in pom.xml."
    }
    $version = $versionNode.InnerText.Trim()

    if ([string]::IsNullOrWhiteSpace($version)) {
        throw "Could not read project.version from pom.xml."
    }

    return $version
}

if (-not (Test-Path -LiteralPath $ReleaseNotesPath -PathType Leaf)) {
    throw "Release notes file was not found: $ReleaseNotesPath"
}

$version = Get-ProjectVersion
$packageName = "docker-wsl-manager-$version"
$packageDirectory = Join-Path $OutputDir $packageName
$releaseNotes = Resolve-Path -LiteralPath $ReleaseNotesPath

Write-Host "Building Docker WSL Manager $version" -ForegroundColor Cyan

Write-Host "[1/5] Running tests..." -ForegroundColor Yellow
Invoke-Maven -MavenArguments @("test", "-Dnet.bytebuddy.experimental=true")

Write-Host "[2/5] Building standalone JAR..." -ForegroundColor Yellow
Invoke-Maven -MavenArguments @("clean", "package", "-Prelease", "-DskipTests")

$standaloneJar = "target\docker-wsl-manager-$version-standalone.jar"
if (-not (Test-Path -LiteralPath $standaloneJar -PathType Leaf)) {
    throw "Standalone JAR was not created: $standaloneJar"
}

Write-Host "[3/5] Preparing distribution package..." -ForegroundColor Yellow
if (Test-Path -LiteralPath $OutputDir) {
    Remove-Item -LiteralPath $OutputDir -Recurse -Force
}
New-Item -ItemType Directory -Path $packageDirectory | Out-Null

Copy-Item -LiteralPath $standaloneJar -Destination (Join-Path $packageDirectory "docker-wsl-manager.jar")
Copy-Item -LiteralPath "run.bat" -Destination $packageDirectory
Copy-Item -LiteralPath "README.md" -Destination $packageDirectory
Copy-Item -LiteralPath "LICENSE" -Destination $packageDirectory
Copy-Item -LiteralPath $releaseNotes -Destination (Join-Path $packageDirectory "RELEASE_NOTES.md")

if ($BuildMsi) {
    Write-Host "[4/5] Building MSI..." -ForegroundColor Yellow
    Invoke-Maven -MavenArguments @("package", "-Pmsi", "-DskipTests")

    $deadline = (Get-Date).AddMinutes(5)
    $msi = $null
    do {
        if (Test-Path -LiteralPath "target\installer" -PathType Container) {
            $msi = Get-ChildItem -Path "target\installer" -Filter "*.msi" -File |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1
        }
        if ($null -eq $msi) {
            Start-Sleep -Seconds 2
        }
    } while ($null -eq $msi -and (Get-Date) -lt $deadline)

    if ($null -eq $msi) {
        throw "MSI was not created in target\installer within five minutes. Verify that JDK 25 and WiX Toolset v7+ are configured."
    }
    Copy-Item -LiteralPath $msi.FullName -Destination $packageDirectory
} else {
    Write-Host "[4/5] Skipping MSI build (use -BuildMsi to include it)." -ForegroundColor Yellow
}

Write-Host "[5/5] Creating ZIP archive..." -ForegroundColor Yellow
$zipPath = Join-Path $OutputDir "$packageName.zip"
Compress-Archive -Path (Join-Path $packageDirectory "*") -DestinationPath $zipPath

Write-Host "Release artifacts created:" -ForegroundColor Green
Write-Host "  Package: $packageDirectory"
Write-Host "  ZIP:     $zipPath"
Write-Host "Next: verify the artifacts, create tag v$version, then publish a GitHub Release."
