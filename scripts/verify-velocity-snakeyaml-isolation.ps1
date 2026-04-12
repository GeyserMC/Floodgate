param(
    [string]$JarPath = "velocity/build/libs/floodgate-velocity.jar"
)

if (-not (Test-Path -LiteralPath $JarPath)) {
    throw "Jar not found: $JarPath"
}

$entries = jar tf $JarPath
if ($LASTEXITCODE -ne 0) {
    throw "Failed to list jar contents for $JarPath"
}

$hasRelocatedYaml = $entries | Where-Object { $_ -like 'org/geysermc/floodgate/shadow/org/yaml/*' } | Select-Object -First 1
$hasTopLevelYaml = $entries | Where-Object { $_ -like 'org/yaml/*' } | Select-Object -First 1

if (-not $hasRelocatedYaml) {
    throw "Relocated SnakeYAML classes were not found in $JarPath"
}

if ($hasTopLevelYaml) {
    throw "Unrelocated SnakeYAML classes were found in ${JarPath}: $hasTopLevelYaml"
}

Write-Host "Velocity jar contains relocated SnakeYAML only."
