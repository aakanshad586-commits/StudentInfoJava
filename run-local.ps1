$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$h2Version = "2.3.232"
$dependencyDirectory = Join-Path $env:LOCALAPPDATA "StudentManagement\lib"
$h2Jar = Join-Path $dependencyDirectory "h2-$h2Version.jar"
$classesDirectory = Join-Path $projectRoot "backend\target\local-classes"
$javaSourceDirectory = Join-Path $projectRoot "backend\src\main\java\com\studentmanagement"

New-Item -ItemType Directory -Force -Path $dependencyDirectory, $classesDirectory | Out-Null
if (-not (Test-Path -LiteralPath $h2Jar)) {
    $downloadUrl = "https://repo.maven.apache.org/maven2/com/h2database/h2/$h2Version/h2-$h2Version.jar"
    Write-Host "Downloading the lightweight H2 JDBC driver..."
    Invoke-WebRequest -Uri $downloadUrl -OutFile $h2Jar
}

$sources = @(
    (Join-Path $javaSourceDirectory "DatabaseConnection.java"),
    (Join-Path $javaSourceDirectory "StudentDAO.java"),
    (Join-Path $javaSourceDirectory "LocalServer.java")
)

& javac --release 11 -cp $h2Jar -d $classesDirectory $sources
if ($LASTEXITCODE -ne 0) {
    throw "Java compilation failed with exit code $LASTEXITCODE."
}

Push-Location $projectRoot
try {
    $env:STUDENT_DB_MODE = "h2"
    & java -cp "$classesDirectory;$h2Jar" com.studentmanagement.LocalServer (Join-Path $projectRoot "frontend")
    if ($LASTEXITCODE -ne 0) {
        throw "The local server stopped with exit code $LASTEXITCODE."
    }
} finally {
    Remove-Item Env:\STUDENT_DB_MODE -ErrorAction SilentlyContinue
    Pop-Location
}
