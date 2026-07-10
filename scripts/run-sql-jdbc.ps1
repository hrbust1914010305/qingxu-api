param(
    [string]$Url = "jdbc:postgresql://localhost:5432/qingxu",
    [string]$Username = "qingxu",
    [string]$Password = "123456",
    [string]$SqlFile = ".\rebuild-backoffice-data.sql"
)

$ErrorActionPreference = "Stop"

$resolvedSql = Resolve-Path $SqlFile
$postgresJar = Get-ChildItem -Path "$env:USERPROFILE\.m2\repository\org\postgresql\postgresql" -Recurse -Filter "postgresql-*.jar" |
    Sort-Object FullName -Descending |
    Select-Object -First 1

if (-not $postgresJar) {
    throw "PostgreSQL JDBC driver was not found in Maven cache."
}

$workDir = Join-Path (Resolve-Path "..").Path "target\sql-runner"
if (-not (Test-Path $workDir)) {
    New-Item -ItemType Directory -Force -Path $workDir | Out-Null
}

$javaFile = Join-Path $workDir "SqlRunner.java"
$classFile = Join-Path $workDir "SqlRunner.class"

$source = @'
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class SqlRunner {
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: SqlRunner <jdbc-url> <username> <password> <sql-file>");
        }
        String url = args[0];
        String username = args[1];
        String password = args[2];
        String sql = Files.readString(Path.of(args[3]), StandardCharsets.UTF_8);

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
        System.out.println("SQL executed successfully.");
    }
}
'@

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($javaFile, $source, $utf8NoBom)

if (Test-Path $classFile) {
    Remove-Item -Path $classFile -Force
}

javac -encoding UTF-8 -cp $postgresJar.FullName -d $workDir $javaFile
java -cp "$workDir;$($postgresJar.FullName)" SqlRunner $Url $Username $Password $resolvedSql.Path
