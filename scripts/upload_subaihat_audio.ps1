param(
    [string]$ProjectId = "sabbih-dhikr-ammar"
)

$ErrorActionPreference = "Stop"
python (Join-Path $PSScriptRoot "upload_subaihat_audio.py")
