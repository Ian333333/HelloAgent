# HelloAgent lightweight Maven Wrapper: downloads Apache Maven on first run
$ErrorActionPreference = 'Stop'

$MavenVersion = '3.9.9'
$DownloadUrls = @(
    "https://mirrors.huaweicloud.com/apache/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip",
    "https://archive.apache.org/dist/maven/maven-3/$MavenVersion/binaries/apache-maven-$MavenVersion-bin.zip"
)

# Resolve JAVA_HOME (derive it from the java command if the env var is missing)
if (-not $env:JAVA_HOME) {
    $line = & java -XshowSettings:properties -version 2>&1 | Select-String 'java\.home\s*=\s*(.+)'
    if ($line) { $env:JAVA_HOME = $line.Matches[0].Groups[1].Value.Trim() }
}
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    Write-Error "JDK not found. Please install JDK 17+ or set JAVA_HOME."
    exit 1
}

$MvnHome = Join-Path $env:USERPROFILE ".m2\wrapper\dists\apache-maven-$MavenVersion"
$MvnCmd  = Join-Path $MvnHome 'bin\mvn.cmd'

if (-not (Test-Path $MvnCmd)) {
    Write-Host "[mvnw] First run: downloading Apache Maven $MavenVersion ..." -ForegroundColor Cyan
    $zip = Join-Path $env:TEMP "apache-maven-$MavenVersion-bin.zip"
    $downloaded = $false
    foreach ($url in $DownloadUrls) {
        try {
            Write-Host "[mvnw] $url"
            Invoke-WebRequest -Uri $url -OutFile $zip -UseBasicParsing
            $downloaded = $true
            break
        } catch {
            Write-Warning "Download failed from this URL: $($_.Exception.Message)"
        }
    }
    if (-not $downloaded) { Write-Error "Maven download failed. Please check your network and retry."; exit 1 }

    $dists = Split-Path $MvnHome -Parent
    New-Item -ItemType Directory -Force -Path $dists | Out-Null
    Expand-Archive -Path $zip -DestinationPath $dists -Force
    Remove-Item $zip -Force -ErrorAction SilentlyContinue
    if (-not (Test-Path $MvnCmd)) { Write-Error "Maven extracted but $MvnCmd was not found."; exit 1 }
    Write-Host "[mvnw] Maven is ready." -ForegroundColor Green
}

& $MvnCmd @args
exit $LASTEXITCODE
