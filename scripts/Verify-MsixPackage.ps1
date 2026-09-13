[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$MsixPath,
    [switch]$RequireTrustedSignature
)

$ErrorActionPreference = 'Stop'

function Find-MakeAppx {
    $tool = Get-ChildItem -LiteralPath 'C:\Program Files (x86)\Windows Kits\10\bin' `
        -Filter 'makeappx.exe' -File -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match '\\x64\\' } |
        Sort-Object FullName -Descending |
        Select-Object -First 1
    if ($null -eq $tool) {
        throw 'makeappx.exe was not found in the Windows SDK x64 tools.'
    }
    return $tool.FullName
}

$resolvedPackage = (Resolve-Path -LiteralPath $MsixPath).Path
$makeAppx = Find-MakeAppx
$verificationRoot = Join-Path ([System.IO.Path]::GetTempPath()) `
    ("DockerWSLManager-msix-verify-" + [guid]::NewGuid().ToString('N'))
try {
    & $makeAppx unpack /p $resolvedPackage /d $verificationRoot /o | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "MSIX unpack validation failed with exit code $LASTEXITCODE."
    }

    [xml]$manifest = Get-Content -LiteralPath (Join-Path $verificationRoot 'AppxManifest.xml') -Raw
    $manifestNamespace = New-Object System.Xml.XmlNamespaceManager($manifest.NameTable)
    $manifestNamespace.AddNamespace(
        'f', 'http://schemas.microsoft.com/appx/manifest/foundation/windows10')
    $manifestNamespace.AddNamespace(
        'rescap', 'http://schemas.microsoft.com/appx/manifest/foundation/windows10/restrictedcapabilities')
    $identity = $manifest.SelectSingleNode('/f:Package/f:Identity', $manifestNamespace)
    if ($null -eq $identity -or $identity.ProcessorArchitecture -ne 'x64') {
        throw 'The package manifest does not contain an x64 identity.'
    }
    $capabilities = $manifest.SelectNodes('/f:Package/f:Capabilities/*', $manifestNamespace)
    $runFullTrust = $manifest.SelectSingleNode(
        '/f:Package/f:Capabilities/rescap:Capability[@Name="runFullTrust"]',
        $manifestNamespace)
    if ($null -eq $runFullTrust -or $capabilities.Count -ne 1) {
        throw 'The package must declare runFullTrust as its only capability.'
    }

    $applicationRoot = Join-Path $verificationRoot 'DockerWSLManager'
    $requiredFiles = @(
        (Join-Path $applicationRoot 'DockerWSLManager.exe'),
        (Join-Path $applicationRoot 'runtime\bin\java.exe'),
        (Join-Path $applicationRoot 'runtime\lib\modules'),
        (Join-Path $applicationRoot 'app\THIRD_PARTY_NOTICES.txt'),
        (Join-Path $applicationRoot 'app\LICENSE.txt'),
        (Join-Path $applicationRoot 'app\PRIVACY.md')
    )
    foreach ($requiredFile in $requiredFiles) {
        if (-not (Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
            throw "Required MSIX payload file is missing: $requiredFile"
        }
    }

    $launcherConfig = Get-Content `
        -LiteralPath (Join-Path $applicationRoot 'app\DockerWSLManager.cfg') -Raw
    if ($launcherConfig -notmatch 'dockerwslmanager\.updateChannel=store') {
        throw 'The MSIX launcher is not configured for Store-managed updates.'
    }

    $javaStartInfo = New-Object System.Diagnostics.ProcessStartInfo
    $javaStartInfo.FileName = Join-Path $applicationRoot 'runtime\bin\java.exe'
    $javaStartInfo.Arguments = '-version'
    $javaStartInfo.UseShellExecute = $false
    $javaStartInfo.CreateNoWindow = $true
    $javaStartInfo.RedirectStandardError = $true
    $javaProcess = [System.Diagnostics.Process]::Start($javaStartInfo)
    $javaProcess.StandardError.ReadToEnd() | Out-Null
    $javaProcess.WaitForExit()
    if ($javaProcess.ExitCode -ne 0) {
        throw "The bundled Java runtime failed to start with exit code $($javaProcess.ExitCode)."
    }
}
finally {
    if (Test-Path -LiteralPath $verificationRoot) {
        Remove-Item -LiteralPath $verificationRoot -Recurse -Force
    }
}

$signature = Get-AuthenticodeSignature -LiteralPath $resolvedPackage
if ($RequireTrustedSignature -and $signature.Status -ne 'Valid') {
    throw "MSIX signature is not trusted: $($signature.Status)"
}

$hash = Get-FileHash -LiteralPath $resolvedPackage -Algorithm SHA256
Write-Host "Validated MSIX: $resolvedPackage"
Write-Host "Signature: $($signature.Status)"
Write-Host "SHA-256: $($hash.Hash.ToLowerInvariant())"
