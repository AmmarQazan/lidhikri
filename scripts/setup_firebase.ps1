# إعداد مشروع Firebase للذكري — شغّل مرة واحدة من PowerShell
# المتطلبات: Node.js + firebase-tools (npm install -g firebase-tools)

$ErrorActionPreference = "Stop"
$Root = Split-Path $PSScriptRoot -Parent
$ProjectId = "sabbih-dhikr-ammar"
$Package = "com.greendome.adhkar"
$AdminEmail = "ammarqazan@gmail.com"
$AdminPassword = "SabbihAdmin2026!"

Set-Location $Root

Write-Host "=== لذكري — إعداد Firebase ===" -ForegroundColor Cyan

if (-not (Get-Command firebase -ErrorAction SilentlyContinue)) {
    Write-Host "تثبيت firebase-tools..." -ForegroundColor Yellow
    npm install -g firebase-tools
}

Write-Host "`n1) تسجيل الدخول إلى Google (يفتح المتصفح)..." -ForegroundColor Green
firebase login

Write-Host "`n2) إنشاء المشروع (إن لم يكن موجوداً)..." -ForegroundColor Green
$exists = firebase projects:list 2>$null | Select-String $ProjectId
if (-not $exists) {
    firebase projects:create $ProjectId --display-name "Lidhikri Content"
} else {
    Write-Host "المشروع $ProjectId موجود مسبقاً"
}
firebase use $ProjectId

Write-Host "`n3) تفعيل Storage ونشر قواعد الأمان..." -ForegroundColor Green
firebase deploy --only storage --project $ProjectId

Write-Host "`n4) تسجيل تطبيق Android..." -ForegroundColor Green
$appsJson = firebase apps:list ANDROID --project $ProjectId --json 2>$null | ConvertFrom-Json
$app = $appsJson.result | Where-Object { $_.namespace -eq $Package } | Select-Object -First 1
if (-not $app) {
    firebase apps:create ANDROID $Package --project $ProjectId --display-name "Lidhikri"
    $appsJson = firebase apps:list ANDROID --project $ProjectId --json | ConvertFrom-Json
    $app = $appsJson.result | Where-Object { $_.namespace -eq $Package } | Select-Object -First 1
}
$appId = $app.appId
Write-Host "App ID: $appId"

Write-Host "`n5) تحميل google-services.json..." -ForegroundColor Green
firebase apps:sdkconfig ANDROID $appId --project $ProjectId --out "app/google-services.json"

Write-Host "`n6) تفعيل تسجيل الدخول بالبريد..." -ForegroundColor Green
Write-Host "   افتح: https://console.firebase.google.com/project/$ProjectId/authentication/providers"
Write-Host "   فعّل Email/Password ثم أنشئ مستخدماً:"
Write-Host "   البريد: $AdminEmail"
Write-Host "   كلمة المرور: $AdminPassword"
Write-Host "   (غيّرها بعد أول استخدام)"

Write-Host "`n7) رفع المحتوى الأولي من remote-content..." -ForegroundColor Green
& "$PSScriptRoot\seed_firebase_storage.ps1" -ProjectId $ProjectId

$bucket = "$ProjectId.firebasestorage.app"
$gradleProps = Join-Path $Root "gradle.properties"
$line = "FIREBASE_STORAGE_BUCKET=$bucket"
if (Test-Path $gradleProps) {
    if (Select-String -Path $gradleProps -Pattern "^FIREBASE_STORAGE_BUCKET=" -Quiet) {
        (Get-Content $gradleProps) -replace "^FIREBASE_STORAGE_BUCKET=.*", $line | Set-Content $gradleProps
    } else {
        Add-Content $gradleProps "`n$line"
    }
} else {
    Set-Content $gradleProps $line
}

Write-Host "`n=== تم الإعداد ===" -ForegroundColor Cyan
Write-Host "المشروع: $ProjectId"
Write-Host "Bucket: $bucket"
Write-Host "بيانات المدير في التطبيق:"
Write-Host "  $AdminEmail / $AdminPassword"
Write-Host "`nأعد بناء التطبيق ثم ارفع التحديثات من شاشة المدير."
