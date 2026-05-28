# Docker Validation Script
Write-Host "=== Docker Validation ===" -ForegroundColor Cyan

# Test Docker
Write-Host "[1] Docker version" -ForegroundColor Yellow
docker --version

# Test Compose
Write-Host "[2] Docker Compose" -ForegroundColor Yellow
docker-compose --version

# Build
Write-Host "[3] Building images..." -ForegroundColor Yellow
docker-compose build
if ($LASTEXITCODE -ne 0) {
    Write-Host "Build failed" -ForegroundColor Red
    exit 1
}

# Start
Write-Host "[4] Starting services..." -ForegroundColor Yellow
docker-compose up -d

# Wait
Write-Host "[5] Waiting 15 seconds..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Check
Write-Host "[6] Container status:" -ForegroundColor Yellow
docker-compose ps

# Test DB
Write-Host "[7] Database test:" -ForegroundColor Yellow
docker exec qr-postgres psql -U postgres -d qrcode_db -c "SELECT COUNT(*) as tables FROM information_schema.tables WHERE table_schema='public';"

# Test API
Write-Host "[8] Backend API test:" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes" -ErrorAction Stop
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor Green
} catch {
    Write-Host "API not ready yet" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Access Services ===" -ForegroundColor Cyan
Write-Host "Frontend:  http://localhost:3000"
Write-Host "Backend:   http://localhost:8080/api"
Write-Host "pgAdmin:   http://localhost:5050"
Write-Host ""
Write-Host "Stop: docker-compose down"
Write-Host "Remove: docker-compose down -v"
