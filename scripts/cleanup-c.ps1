# Frees remaining Tamixa-related space on C: after the move to E:\Tamixa.
# Run AFTER closing Android Studio and Claude Code:
#   powershell -ExecutionPolicy Bypass -File E:\Tamixa\scripts\cleanup-c.ps1

if (Get-Process studio64 -ErrorAction SilentlyContinue) {
    Write-Host "Android Studio is still running - close it (File > Exit) and run this again." -ForegroundColor Yellow
    exit 1
}

$before = (Get-PSDrive C).Free
$targets = @(
    "$env:LOCALAPPDATA\Google\AndroidStudio2026.1.4",                  # old Studio caches/index (now E:\AndroidStudio)
    "$env:USERPROFILE\OneDrive\Desktop\New folder\Tamixa-main",         # old Desktop copy (Tamixa_Beginners_Guide.docx is kept)
    "C:\dev",                                                           # only empty leftover folders remain
    "$env:USERPROFILE\.gradle",                                         # one stale file; Gradle now uses E:\.gradle
    "E:\old-copies"                                                     # verified duplicates moved off C: earlier
)

foreach ($t in $targets) {
    if (Test-Path -LiteralPath $t) {
        attrib -R "$t\*" /S /D 2>$null | Out-Null
        Remove-Item -LiteralPath $t -Recurse -Force -ErrorAction SilentlyContinue
        if (Test-Path -LiteralPath $t) { Write-Host "Could not fully remove: $t" -ForegroundColor Yellow }
        else { Write-Host "Removed: $t" -ForegroundColor Green }
    }
}

"Freed on C: {0:N2} GB  (now {1:N1} GB free)" -f (((Get-PSDrive C).Free - $before) / 1GB), ((Get-PSDrive C).Free / 1GB)
Write-Host "Next: open Android Studio on E:\Tamixa, and start Claude Code with:  cd E:\Tamixa; claude"
