[CmdletBinding()]
param(
    [string]$OutputDirectory = "target\msix",
    [string]$IdentityName = "DockerWSLManager.Development",
    [string]$Publisher = "CN=DockerWSLManager Development",
    [string]$PublisherDisplayName = "Docker WSL Manager Development",
    [string]$PackageVersion,
    [string]$SigningThumbprint,
    [string]$JavaHome,
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path

function Invoke-Checked {
    param([string]$FilePath, [string[]]$Arguments)

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FilePath failed with exit code $LASTEXITCODE."
    }
}

function Remove-BuildDirectory {
    param([string]$Path)

    $fullPath = [System.IO.Path]::GetFullPath((Join-Path $projectRoot $Path))
    $targetRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot 'target'))
    if (-not $fullPath.StartsWith($targetRoot + [System.IO.Path]::DirectorySeparatorChar,
            [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to remove a path outside target: $fullPath"
    }
    if (Test-Path -LiteralPath $fullPath) {
        Remove-Item -LiteralPath $fullPath -Recurse -Force
    }
}

function Find-WindowsSdkTool {
    param([string]$Name)

    $sdkRoot = 'C:\Program Files (x86)\Windows Kits\10\bin'
    $tool = Get-ChildItem -LiteralPath $sdkRoot -Filter $Name -File -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\x64\\' } |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if ($null -eq $tool) {
        throw "$Name was not found in the Windows SDK x64 tools."
    }
    return $tool.FullName
}

function Convert-ToMsixVersion {
    param([string]$Version)

    $numericVersion = ($Version -split '-', 2)[0]
    $parts = @($numericVersion.Split('.'))
    if ($parts.Count -gt 4 -or $parts.Count -lt 1) {
        throw "Cannot map project version '$Version' to an MSIX version."
    }
    while ($parts.Count -lt 4) {
        $parts += '0'
    }
    foreach ($part in $parts) {
        $number = 0
        if (-not [int]::TryParse($part, [ref]$number) -or $number -lt 0 -or $number -gt 65535) {
            throw "Invalid MSIX version component '$part'."
        }
    }
    return $parts -join '.'
}

function New-PackageAsset {
    param([string]$Source, [string]$Destination, [int]$Width, [int]$Height)

    Add-Type -AssemblyName System.Drawing
    $sourceImage = [System.Drawing.Image]::FromFile($Source)
    try {
        $bitmap = New-Object System.Drawing.Bitmap($Width, $Height)
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
            try {
                $graphics.Clear([System.Drawing.Color]::Transparent)
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.DrawImage($sourceImage, 0, 0, $Width, $Height)
            }
            finally {
                $graphics.Dispose()
            }
            $bitmap.Save($Destination, [System.Drawing.Imaging.ImageFormat]::Png)
        }
        finally {
            $bitmap.Dispose()
        }
    }
    finally {
        $sourceImage.Dispose()
    }
}

Push-Location $projectRoot
try {
    [xml]$pom = Get-Content -LiteralPath 'pom.xml' -Raw
    $namespace = New-Object System.Xml.XmlNamespaceManager($pom.NameTable)
    $namespace.AddNamespace('m', 'http://maven.apache.org/POM/4.0.0')
    $projectVersion = $pom.SelectSingleNode('/m:project/m:version', $namespace).InnerText.Trim()
    if ([string]::IsNullOrWhiteSpace($PackageVersion)) {
        $PackageVersion = Convert-ToMsixVersion $projectVersion
    }

    if (-not $SkipTests) {
        Invoke-Checked 'mvn' @('test', '-Dnet.bytebuddy.experimental=true')
    }
    Invoke-Checked 'mvn' @('package', '-Prelease', '-DskipTests')

    if ([string]::IsNullOrWhiteSpace($JavaHome)) {
        $jpackageCommand = Get-Command 'jpackage.exe' -ErrorAction Stop
        $JavaHome = Split-Path -Parent (Split-Path -Parent $jpackageCommand.Source)
    }
    $jlink = Join-Path $JavaHome 'bin\jlink.exe'
    $jpackage = Join-Path $JavaHome 'bin\jpackage.exe'
    if (-not (Test-Path -LiteralPath $jlink) -or -not (Test-Path -LiteralPath $jpackage)) {
        throw "JavaHome must point to a full JDK containing jlink.exe and jpackage.exe: $JavaHome"
    }

    $workRoot = Join-Path $OutputDirectory 'work'
    Remove-BuildDirectory $OutputDirectory
    $inputDirectory = Join-Path $workRoot 'input'
    $runtimeDirectory = Join-Path $workRoot 'runtime'
    $imageDirectory = Join-Path $workRoot 'image'
    $stagingDirectory = Join-Path $workRoot 'staging'
    New-Item -ItemType Directory -Path $inputDirectory, $stagingDirectory -Force | Out-Null

    Copy-Item -LiteralPath "target\docker-wsl-manager-$projectVersion-standalone.jar" `
        -Destination (Join-Path $inputDirectory "docker-wsl-manager-$projectVersion-standalone.jar")
    Copy-Item -LiteralPath 'target\classes\META-INF\THIRD-PARTY.txt' `
        -Destination (Join-Path $inputDirectory 'THIRD_PARTY_NOTICES.txt')
    Copy-Item -LiteralPath 'LICENSE' -Destination (Join-Path $inputDirectory 'LICENSE.txt')
    Copy-Item -LiteralPath 'PRIVACY.md' -Destination (Join-Path $inputDirectory 'PRIVACY.md')

    $modules = 'java.base,java.compiler,java.desktop,java.instrument,java.logging,java.management,' +
        'java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.sql,' +
        'java.xml,jdk.unsupported,javafx.base,javafx.controls,javafx.fxml,javafx.graphics,' +
        'jdk.crypto.ec,jdk.charsets,jdk.crypto.cryptoki,jdk.crypto.mscapi,jdk.localedata,jdk.naming.dns'
    Invoke-Checked $jlink @(
        '--module-path', (Join-Path $JavaHome 'jmods'), '--add-modules', $modules,
        '--output', $runtimeDirectory, '--strip-debug', '--compress', 'zip-6',
        '--no-header-files', '--no-man-pages'
    )
    Invoke-Checked $jpackage @(
        '--type', 'app-image', '--dest', $imageDirectory, '--input', $inputDirectory,
        '--main-jar', "docker-wsl-manager-$projectVersion-standalone.jar",
        '--main-class', 'org.dreamabout.sw.dockerwslmanager.Main', '--name', 'DockerWSLManager',
        '--icon', 'src\main\resources\app_icon.ico', '--runtime-image', $runtimeDirectory,
        '--vendor', $PublisherDisplayName
    )

    $applicationImage = Join-Path $imageDirectory 'DockerWSLManager'
    Copy-Item -LiteralPath $applicationImage -Destination $stagingDirectory -Recurse
    $launcherConfig = Join-Path $stagingDirectory 'DockerWSLManager\app\DockerWSLManager.cfg'
    Add-Content -LiteralPath $launcherConfig `
        -Value "`njava-options=-Ddockerwslmanager.updateChannel=store" -Encoding utf8

    $assets = Join-Path $stagingDirectory 'Assets'
    New-Item -ItemType Directory -Path $assets | Out-Null
    $sourceIcon = (Resolve-Path 'src\main\resources\app_icon.png').Path
    New-PackageAsset $sourceIcon (Join-Path $assets 'StoreLogo.png') 50 50
    New-PackageAsset $sourceIcon (Join-Path $assets 'Square44x44Logo.png') 44 44
    New-PackageAsset $sourceIcon (Join-Path $assets 'Square150x150Logo.png') 150 150

    $manifest = Get-Content -LiteralPath 'packaging\msix\AppxManifest.xml.template' -Raw
    $manifest = $manifest.Replace('@@IDENTITY_NAME@@', [Security.SecurityElement]::Escape($IdentityName))
    $manifest = $manifest.Replace('@@PUBLISHER@@', [Security.SecurityElement]::Escape($Publisher))
    $manifest = $manifest.Replace(
        '@@PUBLISHER_DISPLAY_NAME@@', [Security.SecurityElement]::Escape($PublisherDisplayName))
    $manifest = $manifest.Replace('@@VERSION@@', [Security.SecurityElement]::Escape($PackageVersion))
    Set-Content -LiteralPath (Join-Path $stagingDirectory 'AppxManifest.xml') `
        -Value $manifest -Encoding utf8

    $makeAppx = Find-WindowsSdkTool 'makeappx.exe'
    $outputPath = Join-Path $OutputDirectory "DockerWSLManager-$PackageVersion-x64.msix"
    Invoke-Checked $makeAppx @('pack', '/d', $stagingDirectory, '/p', $outputPath, '/o', '/l')

    if (-not [string]::IsNullOrWhiteSpace($SigningThumbprint)) {
        $signTool = Find-WindowsSdkTool 'signtool.exe'
        Invoke-Checked $signTool @('sign', '/sha1', $SigningThumbprint, '/fd', 'SHA256', '/v', $outputPath)
    }

    $verifyArguments = @('-MsixPath', $outputPath)
    if (-not [string]::IsNullOrWhiteSpace($SigningThumbprint)) {
        $verifyArguments += '-RequireTrustedSignature'
    }
    $powershellArguments = @(
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
        (Join-Path $PSScriptRoot 'Verify-MsixPackage.ps1')
    ) + $verifyArguments
    Invoke-Checked 'powershell.exe' $powershellArguments

    $hash = Get-FileHash -LiteralPath $outputPath -Algorithm SHA256
    "{0}  {1}" -f $hash.Hash.ToLowerInvariant(), (Split-Path $outputPath -Leaf) |
        Set-Content -LiteralPath (Join-Path $OutputDirectory 'SHA256SUMS.txt') -Encoding ascii
    Write-Host "MSIX package: $outputPath" -ForegroundColor Green
    Write-Host "SHA-256: $($hash.Hash.ToLowerInvariant())"
}
finally {
    Pop-Location
}
