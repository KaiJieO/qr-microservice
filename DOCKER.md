# Docker Deployment Guide

**Backend microservice only. No frontend included.**

Complete containerization for QR Microservice with multi-stage builds and health checks.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│              Docker Compose Network                  │
│                   (qr-network)                       │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐  ┌───────────┐                  │
│  │   Backend    │  │ PostgreSQL│                  │
│  │   :8080      │  │  :5432    │                  │
│  │ (Spring Boot)│  │           │                  │
│  └──────────────┘  └───────────┘                  │
│         │                │                         │
│         └────────────────┘                         │
│                                                     │
│  ┌─────────────────────────────────────────────┐  │
│  │          pgAdmin (optional)                 │  │
│  │          :5050                              │  │
│  └─────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

## Quick Start

### 1. Build and Start All Services

```powershell
cd C:\Users\Kai Lin\Documents\GitHub\qr-microservice

# Build images
docker-compose build

# Start all services
docker-compose up -d

# Check status
docker-compose ps
```

### 2. Verify Services Running

```powershell
# All services should show "Up"
docker-compose ps

# Check logs
docker-compose logs -f backend
docker-compose logs -f frontend
```

### 3. Access Services

- **Backend API:** http://localhost:8080/api

## Service Details

### PostgreSQL Database

```
- Image: postgres:15-alpine
- Container: qr-postgres
- Port: 5432
- User: postgres
- Password: postgres
- Database: qrcode_db
- Healthcheck: Enabled (pg_isready)
- Volume: postgres_data (persistent)
- Init Script: schema.sql (auto-runs)
```

### Backend Service

```
- Image: Built from backend/Dockerfile
- Container: qr-backend
- Port: 8080
- Language: Java 17
- Framework: Spring Boot 3.2
- Build: Gradle (2-stage build)
- Depends On: postgres (healthy)
- Environment: 
  - SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/qrcode_db
  - SPRING_DATASOURCE_USERNAME: postgres
  - SPRING_DATASOURCE_PASSWORD: postgres
```

## Testing & Validation

### Test 1: Docker Images Built

```powershell
# List images
docker images | findstr qr-

# Expected output:
# qr-microservice:backend  ...
# qr-microservice:frontend ...
```

### Test 2: Services Running

```powershell
docker-compose ps

# Expected: All services "Up"
```

### Test 3: Database Connection

```powershell
# Connect to PostgreSQL
docker exec -it qr-postgres psql -U postgres -d qrcode_db -c "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';"

# Expected: qr_codes table listed
```

### Test 4: Backend Health

```powershell
# Check backend logs
docker-compose logs backend | Select-Object -Last 20

# Expected: "Started QrServiceApplication in X seconds"

# Test API
curl http://localhost:8080/api/qr-codes

# Expected: [] (empty array)
```

### Test 5: End-to-End QR Generation

```powershell
# Generate QR via API
$body = @{
    sublinkUrl = "https://example.com/test"
    expiresInMinutes = 60
    createdBy = "docker-test"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body

$qrId = ($response.Content | ConvertFrom-Json).id
Write-Output "Generated QR: $qrId"

# Verify in database
docker exec qr-postgres psql -U postgres -d qrcode_db -c "SELECT id, sublinkUrl, status FROM qr_codes WHERE id = '$qrId';"
```

### Test 6: Auto-Expiration Scheduler

```powershell
# Generate QR with 1 minute expiry
$body = @{
    sublinkUrl = "https://example.com/expire-test"
    expiresInMinutes = 1
    createdBy = "docker-test"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body

$qrId = ($response.Content | ConvertFrom-Json).id

# Wait 70 seconds
Start-Sleep -Seconds 70

# Check status (should be EXPIRED)
$qr = Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes/$qrId" | ConvertFrom-Json
Write-Output "QR Status: $($qr.status)"

# Expected: EXPIRED
```

### Test 7: Database Persistence

```powershell
# Stop containers
docker-compose down

# Start again
docker-compose up -d

# Check data still exists
docker exec qr-postgres psql -U postgres -d qrcode_db -c "SELECT COUNT(*) FROM qr_codes;"

# Expected: Previously generated QR codes still in database
```

## Stopping & Cleanup

### Stop Services (Keep Data)

```powershell
docker-compose down
```

### Remove Everything (Delete Data)

```powershell
docker-compose down -v
```

### Remove Images

```powershell
docker-compose down --rmi all
```

## Troubleshooting

### Backend won't connect to database

```powershell
# Check PostgreSQL is healthy
docker-compose logs postgres

# Wait 10+ seconds for health check to pass
Start-Sleep -Seconds 15
docker-compose restart backend
```

### Frontend shows "Failed to fetch"

```powershell
# Check backend is running
docker-compose logs backend

# Verify network connectivity
docker exec qr-frontend sh -c "curl http://backend:8080/api/qr-codes"

# Expected: [] (empty array)
```

### Port already in use

```powershell
# Find process on port 3000
netstat -ano | findstr :3000

# Kill process
taskkill /PID <PID> /F

# Or change port in docker-compose.yml
# "3000:3000" → "3001:3000"
```

### Build fails

```powershell
# Clean and rebuild
docker-compose down --rmi all
docker-compose build --no-cache
docker-compose up -d
```

## Multi-Stage Build Details

### Backend (Java)

```dockerfile
# Stage 1: Build with Gradle
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN ./gradlew build -x test

# Stage 2: Runtime with JRE only
FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Benefits:**
- JDK removed from final image (smaller size)
- Only compiled JAR needed at runtime
- ~200MB → ~150MB image size

## Production Deployment

### Update docker-compose for production

```yaml
# docker-compose.prod.yml
services:
  backend:
    # ... add resources limits
    deploy:
      resources:
        limits:
          cpus: '1'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
    # ... add restart policy
    restart: always
```

### Use secrets for sensitive data

```powershell
# Instead of hardcoded passwords in docker-compose.yml
# Use Docker secrets or .env files

# Create .env.prod
POSTGRES_PASSWORD=<secure-password>
SPRING_DATASOURCE_PASSWORD=<secure-password>

# Reference in compose
environment:
  POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

### Registry deployment

```powershell
# Tag images
docker tag qr-microservice:backend myregistry/qr-backend:1.0
docker tag qr-microservice:frontend myregistry/qr-frontend:1.0

# Push to registry
docker push myregistry/qr-backend:1.0
docker push myregistry/qr-frontend:1.0
```

## Performance Notes

**Image Sizes (approximate):**
- PostgreSQL: 80MB
- Backend: 150MB
- Total: ~230MB

**Startup Times:**
- PostgreSQL: 5-10 seconds
- Backend: 15-20 seconds
- Total: ~25-30 seconds

**Health Checks:**
- PostgreSQL: Every 10s, timeout 5s, max 5 retries
- Backend: Depends on PostgreSQL healthy
- Frontend: Depends on Backend running

## Volume Management

### Persistent Data

```powershell
# postgres_data volume stores database
docker volume ls | findstr postgres_data

# Inspect volume
docker volume inspect qr_microservice_postgres_data
```

### Backup Database

```powershell
# Export database
docker exec qr-postgres pg_dump -U postgres qrcode_db > backup.sql

# Restore database
docker exec -i qr-postgres psql -U postgres qrcode_db < backup.sql
```

## Next Steps

1. Configure CI/CD pipeline (GitHub Actions)
2. Add Kubernetes manifests
3. Setup monitoring (Prometheus, Grafana)
4. Add log aggregation (ELK stack)
5. Database backup strategy
6. SSL/TLS certificates
