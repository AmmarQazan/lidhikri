param(
    [string]$ProjectId = "sabbih-dhikr-ammar"
)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$Remote = Join-Path $Root "remote-content"
$Manifest = Get-Content (Join-Path $Remote "manifest.json") -Raw | ConvertFrom-Json
$Version = $Manifest.version
$ContentLocal = Join-Path $Remote "bundles\v$Version\content.json"
$Bucket = "$ProjectId.firebasestorage.app"

if (-not (Test-Path $ContentLocal)) {
    throw "لم يُعثر على $ContentLocal"
}

function Get-FirebaseAccessToken {
    $cfgPath = Join-Path $env:USERPROFILE ".config\configstore\firebase-tools.json"
    if (-not (Test-Path $cfgPath)) {
        throw "شغّل firebase login أولاً"
    }
    return (Get-Content $cfgPath -Raw | ConvertFrom-Json).tokens.access_token
}

function Upload-GcsFile {
    param(
        [string]$LocalPath,
        [string]$ObjectName,
        [string]$AccessToken
    )
    $uri = "https://storage.googleapis.com/upload/storage/v1/b/$Bucket/o?uploadType=media&name=$([uri]::EscapeDataString($ObjectName))"
    $ext = [IO.Path]::GetExtension($LocalPath).ToLower()
    $contentType = switch ($ext) {
        ".json" { "application/json" }
        ".mp3"  { "audio/mpeg" }
        ".m4a"  { "audio/mp4" }
        default { "application/octet-stream" }
    }
    $bytes = [System.IO.File]::ReadAllBytes($LocalPath)
    $headers = @{
        Authorization = "Bearer $AccessToken"
        "Content-Type" = $contentType
    }
    Invoke-RestMethod -Uri $uri -Method POST -Headers $headers -Body $bytes | Out-Null
}

$token = Get-FirebaseAccessToken

Write-Host "رفع manifest.json..."
Upload-GcsFile -LocalPath (Join-Path $Remote "manifest.json") -ObjectName "content/manifest.json" -AccessToken $token

Write-Host "رفع content.json v$Version..."
Upload-GcsFile -LocalPath $ContentLocal -ObjectName "content/bundles/v$Version/content.json" -AccessToken $token

$audioDir = Join-Path $Remote "audio"
if (Test-Path $audioDir) {
    $files = Get-ChildItem $audioDir -Recurse -File
    $i = 0
    foreach ($f in $files) {
        $i++
        $rel = $f.FullName.Substring($audioDir.Length).TrimStart('\', '/').Replace('\', '/')
        $remote = "content/audio/$rel"
        Write-Host "[$i/$($files.Count)] $rel"
        Upload-GcsFile -LocalPath $f.FullName -ObjectName $remote -AccessToken $token
    }
}

Write-Host "تم رفع المحتوى v$Version إلى Firebase Storage."
