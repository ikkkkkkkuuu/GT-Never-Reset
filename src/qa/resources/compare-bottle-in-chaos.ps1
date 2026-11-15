param(
    [Parameter(Mandatory = $true)][string]$Screenshots,
    [Parameter(Mandatory = $true)][string]$Report
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
public static class ChaosPixelComparison {
    public static long[] Compare(string left, string right, int x, int y, int width, int height) {
        using (var a = new Bitmap(left)) using (var b = new Bitmap(right)) {
            if (a.Size != b.Size) throw new InvalidOperationException("Screenshot sizes differ");
            if (a.Width != 1100 || a.Height != 740) throw new InvalidOperationException("Expected 1100 x 740 at GUI scale 2");
            long changed = 0;
            int minX = a.Width, minY = a.Height, maxX = -1, maxY = -1;
            for (int row = y; row < y + height; row++)
                for (int column = x; column < x + width; column++)
                    if (a.GetPixel(column, row).ToArgb() != b.GetPixel(column, row).ToArgb()) {
                        changed++;
                        minX = Math.Min(minX, column); minY = Math.Min(minY, row);
                        maxX = Math.Max(maxX, column); maxY = Math.Max(maxY, row);
                    }
            return new long[] { (long)width * height, changed, minX, minY, maxX, maxY };
        }
    }
}
'@

$regions = @(
    @{ Name = 'main-window-raw'; X = 102; Y = 34; W = 896; H = 670 },
    @{ Name = 'operation-tab'; X = 118; Y = 50; W = 160; H = 32 },
    @{ Name = 'pool-module-tabs'; X = 438; Y = 50; W = 320; H = 32 },
    @{ Name = 'inventory-and-machine-controls'; X = 146; Y = 480; W = 376; H = 194 },
    @{ Name = 'inventory-static-left'; X = 146; Y = 480; W = 108; H = 194 },
    @{ Name = 'inventory-static-right'; X = 290; Y = 480; W = 232; H = 194 },
    @{ Name = 'inventory-static-upper-center'; X = 254; Y = 480; W = 36; H = 158 },
    @{ Name = 'packaging-slots-and-labels'; X = 622; Y = 520; W = 332; H = 98 },
    @{ Name = 'terminal-content-raw'; X = 128; Y = 100; W = 844; H = 358 }
)
$results = foreach ($state in @('runtime', 'runtime-scrolled', 'groups', 'pools', 'pools-installed', 'modules', 'modules-scrolled', 'cache-limit')) {
    $original = Join-Path $Screenshots "original-$state.png"
    $port = Join-Path $Screenshots "port-$state.png"
    if (!(Test-Path -LiteralPath $original) -or !(Test-Path -LiteralPath $port)) { throw "Missing state: $state" }
    $stateRegions = $regions
    if ($state -eq 'cache-limit') { $stateRegions += @{ Name = 'cache-popup'; X = 386; Y = 290; W = 328; H = 156 } }
    foreach ($region in $stateRegions) {
        $counts = [ChaosPixelComparison]::Compare($original, $port, $region.X, $region.Y, $region.W, $region.H)
        [PSCustomObject]@{
            State = $state
            Region = $region.Name
            ComparedPixels = $counts[0]
            DifferentPixels = $counts[1]
            DifferenceBounds = if ($counts[1] -eq 0) { $null } else { @($counts[2], $counts[3], $counts[4], $counts[5]) }
        }
    }
}
$results | ConvertTo-Json | Set-Content -LiteralPath $Report -Encoding utf8
$results | Format-Table -AutoSize

$runtime = Split-Path $Screenshots -Parent
$layout = foreach ($state in @('runtime', 'runtime-scrolled', 'groups', 'pools', 'pools-installed', 'modules', 'modules-scrolled', 'cache-limit')) {
    $original = Get-Content -LiteralPath (Join-Path $runtime "original-$state-bounds.txt")
    $port = Get-Content -LiteralPath (Join-Path $runtime "port-$state-bounds.txt")
    $checks = [ordered]@{ main_window = '51,17,448,335'; terminal = '59,45,432,183' }
    for ($i = 0; $i -lt 4; $i++) { $checks["tab_$i"] = "$(59 + 80 * $i),25,80,16" }
    for ($i = 0; $i -lt 15; $i++) {
        $checks["group_$($i + 1)"] = "$(65 + 84 * ($i % 5)),$(52 + 35 * [Math]::Floor($i / 5)),80,31"
    }
    for ($i = 0; $i -lt 5; $i++) { $checks["group_action_$i"] = "$(223 + 53 * $i),165,48,18" }
    if ($state -eq 'cache-limit') { $checks['cache_popup'] = '193,145,164,78' }
    foreach ($entry in $checks.GetEnumerator()) {
        $needle = '|' + $entry.Value + '|'
        if (!($original | Where-Object { $_.Contains($needle) }) -or !($port | Where-Object { $_.Contains($needle) })) {
            throw "Layout differs: $state $($entry.Key)"
        }
        [PSCustomObject]@{ State = $state; Control = $entry.Key; Bounds = $entry.Value; BothMatch = $true }
    }
    $slots = 'com.cleanroommc.modularui.widgets.slot.ItemSlot|'
    $left = @($original | Where-Object { $_.StartsWith($slots) } | ForEach-Object { $_.Split('|')[1] })
    $right = @($port | Where-Object { $_.StartsWith($slots) } | ForEach-Object { $_.Split('|')[1] })
    if (($left -join ';') -ne ($right -join ';')) { throw "Item slot layout differs: $state" }
    [PSCustomObject]@{ State = $state; Control = 'all_item_slot_bounds'; Count = $left.Count; BothMatch = $true }
}
$layoutReport = Join-Path (Split-Path $Report -Parent) ([IO.Path]::GetFileNameWithoutExtension($Report) + '-layout.json')
$layout | ConvertTo-Json | Set-Content -LiteralPath $layoutReport -Encoding utf8
Write-Output "$($layout.Count) layout comparisons passed"
