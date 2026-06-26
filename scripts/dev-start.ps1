$env:SPRING_PROFILES_ACTIVE = "dev"
Write-Host "Starting Qingxu API with dev profile..."
Write-Host "Swagger: http://localhost:8080/swagger-ui/index.html"
& "$PSScriptRoot\..\mvnw.cmd" spring-boot:run
