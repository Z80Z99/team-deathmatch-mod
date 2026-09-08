[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleTasks = @('build')
)

$ErrorActionPreference = 'Stop'
$tdmBuildExitCode = 1
$tdmOriginalGradleUserHome = $env:GRADLE_USER_HOME
$tdmOriginalJavaOptions = $env:JAVA_TOOL_OPTIONS
$tdmSocketFallback = Join-Path ([IO.Path]::GetTempPath()) ('tdm-socket-fallback-' + [guid]::NewGuid())

try {
    # A missing socket directory selects the JDK's TCP fallback on affected Windows systems.
    $env:JAVA_TOOL_OPTIONS = ($tdmOriginalJavaOptions + ' "-Djdk.net.unixdomain.tmpdir=' + $tdmSocketFallback + '"').Trim()
    if ([string]::IsNullOrWhiteSpace($env:GRADLE_USER_HOME)) {
        $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
    }
    Push-Location -LiteralPath $PSScriptRoot
    try {
        & (Join-Path $PSScriptRoot 'gradlew.bat') --no-daemon --console=plain @GradleTasks
        $tdmBuildExitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    $env:GRADLE_USER_HOME = $tdmOriginalGradleUserHome
    $env:JAVA_TOOL_OPTIONS = $tdmOriginalJavaOptions
}

exit $tdmBuildExitCode
