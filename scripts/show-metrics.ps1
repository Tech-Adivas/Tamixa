# Local dev: prints the backend's key Actuator metrics as a readable table.
# /actuator/metrics needs a login token, so this logs in first (dev seed admin by default).
# Counters live in memory: they start from 0 each time the backend restarts.
#
# Usage:  powershell -ExecutionPolicy Bypass -File scripts\show-metrics.ps1
#         powershell -ExecutionPolicy Bypass -File scripts\show-metrics.ps1 -Watch 10   # refresh every 10 s
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Email = $(if ($env:SEED_ADMIN_EMAIL) { $env:SEED_ADMIN_EMAIL } else { "admin@techadivas.com" }),
    [string]$Password = $(if ($env:SEED_ADMIN_PASSWORD) { $env:SEED_ADMIN_PASSWORD } else { "Admin123!" }),
    [int]$Watch = 0
)

$metrics = [ordered]@{
    "story.openai.tokens.total"             = "LLM tokens - story generation"
    "narration_ai_tokens_total"             = "LLM tokens - narration rewrite"
    "narration_tts_characters_total"        = "Text-to-speech characters"
    "narration_cost_estimate_usd_total"     = "Estimated AI cost (USD)"
    "story_generation_latency"              = "Story generation time"
    "tts_latency"                           = "Text-to-speech time"
    "tts_failure_count"                     = "Text-to-speech failures"
    "narration_failure_rate"                = "Narration failures"
    "stream_url_requests"                   = "Story plays (stream URLs)"
    "stream_audio_completions"              = "Stories listened to the end"
    "subscription.active_subscriptions_count" = "Active subscriptions"
    "subscription.free_limit_hits"          = "Free-plan limit hits"
    "http.server.requests"                  = "API requests"
    "jvm.memory.used"                       = "Backend memory used"
}

function Get-Token {
    $body = @{ email = $Email; password = $Password } | ConvertTo-Json
    try {
        (Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/v1/auth/login" -ContentType 'application/json' -Body $body).accessToken
    } catch {
        Write-Host "Login failed at $BaseUrl (is the backend running?): $($_.Exception.Message)" -ForegroundColor Red
        exit 1
    }
}

function Format-Value([string]$name, $m) {
    $byStat = @{}
    foreach ($x in $m.measurements) { $byStat[$x.statistic] = $x.value }
    if ($m.baseUnit -eq "seconds" -and $byStat.ContainsKey("COUNT")) {
        $count = [int]$byStat["COUNT"]
        if ($count -eq 0) { return "0 runs" }
        $avg = $byStat["TOTAL_TIME"] / $count
        # MAX only covers the last few minutes, so it's shown only while it's still set.
        if ($byStat["MAX"] -gt 0) { return "{0} runs, avg {1:N1} s, recent max {2:N1} s" -f $count, $avg, $byStat["MAX"] }
        return "{0} runs, avg {1:N1} s" -f $count, $avg
    }
    if ($m.baseUnit -eq "bytes") { return "{0:N0} MB" -f ($byStat["VALUE"] / 1MB) }
    if ($byStat.ContainsKey("COUNT")) { return "{0:N0}" -f $byStat["COUNT"] }
    if ($byStat.ContainsKey("VALUE")) { return "{0:N2}" -f $byStat["VALUE"] }
    return ($m.measurements | ForEach-Object { "$($_.statistic)=$($_.value)" }) -join ", "
}

$headers = @{ Authorization = "Bearer $(Get-Token)" }
do {
    $health = try { (Invoke-RestMethod "$BaseUrl/actuator/health").status } catch { "DOWN" }
    $rows = foreach ($name in $metrics.Keys) {
        $value = try { Format-Value $name (Invoke-RestMethod "$BaseUrl/actuator/metrics/$name" -Headers $headers) }
                 catch { if ($_.Exception.Response.StatusCode.value__ -eq 401) { $headers = @{ Authorization = "Bearer $(Get-Token)" } }; "- (not recorded yet)" }
        [pscustomobject]@{ Metric = $metrics[$name]; Value = $value; Name = $name }
    }
    if ($Watch -gt 0) { Clear-Host }
    Write-Host ("Tamixa backend {0}  |  health: {1}  |  {2}" -f $BaseUrl, $health, (Get-Date -Format "HH:mm:ss")) -ForegroundColor Cyan
    $rows | Format-Table -AutoSize | Out-String | Write-Host
    Write-Host "Counters reset when the backend restarts. History: admin dashboard (http://localhost:3001)." -ForegroundColor DarkGray
    if ($Watch -gt 0) { Start-Sleep -Seconds $Watch }
} while ($Watch -gt 0)
