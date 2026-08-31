[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$AppImagePath,

    [string]$MsiPath,

    [switch]$SkipMsiExtraction
)

$ErrorActionPreference = 'Stop'

function Test-PackagedApplication {
    param([Parameter(Mandatory = $true)][string]$ApplicationPath)

    $runtimeModules = Join-Path $ApplicationPath 'runtime\lib\modules'
    $java = Join-Path $ApplicationPath 'runtime\bin\java.exe'
    $duplicateRuntime = Join-Path $ApplicationPath 'app\runtime'

    if (-not (Test-Path -LiteralPath $runtimeModules -PathType Leaf)) {
        throw "Missing jpackage runtime modules file: $runtimeModules"
    }
    if (-not (Test-Path -LiteralPath $java -PathType Leaf)) {
        throw "Missing jpackage runtime Java executable: $java"
    }
    if (Test-Path -LiteralPath $duplicateRuntime) {
        throw "Unexpected duplicate application runtime: $duplicateRuntime"
    }

    & $java -version
    if ($LASTEXITCODE -ne 0) {
        throw "Packaged runtime Java failed with exit code ${LASTEXITCODE}: $java -version"
    }
}

Test-PackagedApplication -ApplicationPath $AppImagePath
Write-Host "Verified jpackage app image: $AppImagePath"

if ($SkipMsiExtraction) {
    return
}

if (-not (Test-Path -LiteralPath $MsiPath -PathType Leaf)) {
    throw "MSI was not created: $MsiPath"
}

$resolvedMsiPath = (Resolve-Path -LiteralPath $MsiPath).Path
$extractionPath = Join-Path ([System.IO.Path]::GetDirectoryName($resolvedMsiPath)) 'msi-verification'
if (Test-Path -LiteralPath $extractionPath) {
    Remove-Item -LiteralPath $extractionPath -Recurse -Force
}

try {
    New-Item -ItemType Directory -Path $extractionPath | Out-Null
    $msiArguments = "/a `"$resolvedMsiPath`" /qn TARGETDIR=`"$extractionPath`""
    $msiExitCode = $null
    for ($attempt = 1; $attempt -le 5; $attempt++) {
        $msiProcess = Start-Process -FilePath 'msiexec.exe' -ArgumentList $msiArguments -Wait -PassThru
        $msiExitCode = $msiProcess.ExitCode
        if ($msiExitCode -eq 0) {
            break
        }
        if ($msiExitCode -ne 1619 -or $attempt -eq 5) {
            throw "Administrative MSI extraction failed with exit code $msiExitCode."
        }
        Write-Warning "MSI is not ready for verification yet; retrying ($attempt/5)."
        Start-Sleep -Seconds 2
    }

    $installedApplication = Get-ChildItem -LiteralPath $extractionPath -Directory -Recurse |
        Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'runtime\lib\modules') -PathType Leaf } |
        Select-Object -First 1
    if ($null -eq $installedApplication) {
        throw "Could not find the application runtime after extracting MSI to: $extractionPath"
    }

    Test-PackagedApplication -ApplicationPath $installedApplication.FullName
    Write-Host "Verified MSI application layout: $($installedApplication.FullName)"
}
finally {
    if (Test-Path -LiteralPath $extractionPath) {
        Remove-Item -LiteralPath $extractionPath -Recurse -Force
    }
}
