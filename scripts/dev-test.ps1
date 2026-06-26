$env:SPRING_PROFILES_ACTIVE = "test"
Write-Host "Running Qingxu API tests..."
& "$PSScriptRoot\..\mvnw.cmd" test
