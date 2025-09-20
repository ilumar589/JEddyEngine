# run-project.ps1
param(
    [switch]$Jdwp,
    [int]$JdwpPort = 5005
)

$ErrorActionPreference = 'Stop'
$orig = Get-Location
try {
    $root = Split-Path -Parent $MyInvocation.MyCommand.Path
    Set-Location $root

    $target = Join-Path $root "target"
    if (-not (Test-Path $target)) {
        Write-Error "Target directory not found: $target"
        exit 1
    }
    Set-Location $target

    # Prefer shaded → exact → any matching
    $jar = Get-ChildItem -File "*-shaded.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $jar) { $jar = Get-ChildItem -File "JEddyEngine-1.0-SNAPSHOT.jar" -ErrorAction SilentlyContinue | Select-Object -First 1 }
    if (-not $jar) { $jar = Get-ChildItem -File "JEddyEngine-*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1 }
    if (-not $jar) {
        Write-Error "No runnable JAR found in $target"
        exit 1
    }

    # JVM args MUST be before -jar
    $defaultJvmArgs = @(
        '-XX:+UseCompactObjectHeaders',
        '-XX:+UseZGC',
        '--enable-native-access=ALL-UNNAMED'
    )

    # Optional remote debugger
    $debugArgs = @()
    if ($Jdwp) {
        $debugArgs = @("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:$JdwpPort")
        Write-Host "==> JDWP enabled on port $JdwpPort (suspend=y)"
    }

    # Allow extra JVM args before `--`, and app args after `--`
    $split = $args.IndexOf("--")
    $jvmArgs = @()
    $appArgs = @()
    if ($split -ge 0) {
        if ($split -gt 0) { $jvmArgs = $args[0..($split-1)] }
        if ($split+1 -lt $args.Count) { $appArgs = $args[($split+1)..($args.Count-1)] }
    } else {
        $jvmArgs = $args
    }

    Write-Host "Running $($jar.Name)"
    & java @defaultJvmArgs @debugArgs @jvmArgs -jar $jar.Name @appArgs
    $rc = $LASTEXITCODE
}
finally {
    Set-Location $orig
}
exit $rc
