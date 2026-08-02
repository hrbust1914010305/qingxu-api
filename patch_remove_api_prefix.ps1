$root = 'D:\project\qingxu\qingxu-api'
$targets = Get-ChildItem -Path (Join-Path $root 'src') -Recurse -File -Include *.java,*.yml
foreach ($file in $targets) {
    $content = [System.IO.File]::ReadAllText($file.FullName)
    $updated = $content.Replace('http://127.0.0.1:8081/api', 'http://127.0.0.1:8081')
    $updated = $updated.Replace('/api/', '/')
    if ($updated -ne $content) {
        [System.IO.File]::WriteAllText($file.FullName, $updated, [System.Text.UTF8Encoding]::new($false))
    }
}
