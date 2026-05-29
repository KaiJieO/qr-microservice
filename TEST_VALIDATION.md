# QR Microservice - End-to-End Validation Plan

**Backend microservice only. Testing backend API endpoints.**

Complete testing checklist for all features after refactoring.

## Prerequisites

```powershell
# Terminal 1: Database
docker run --name postgres-qr `
  -e POSTGRES_PASSWORD=postgres `
  -e POSTGRES_DB=qrcode_db `
  -p 5432:5432 -d postgres:15

# Terminal 2: Backend
cd backend
.\gradlew.bat bootRun
```

## Test Cases

### 1. Backend: QR Code Generation

**Endpoint:** `POST http://localhost:8080/api/qr-codes`

**Request:**
```json
{
  "sublinkUrl": "https://example.com/test",
  "expiresInMinutes": 1,
  "createdBy": "test-user"
}
```

**Expected Response:**
- HTTP 201 Created
- Response includes: `id`, `status: "VALID"`, `createdAt`, `expiredAt`
- No `scanned_at` tracking (reserved for future use)

**PowerShell Test:**
```powershell
$body = @{
    sublinkUrl = "https://example.com/test"
    expiresInMinutes = 1
    createdBy = "test-user"
} | ConvertTo-Json

$response = Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body

$qrId = ($response.Content | ConvertFrom-Json).id
Write-Output "Created QR Code ID: $qrId"
```

### 2. Backend: Retrieve QR Code

**Endpoint:** `GET http://localhost:8080/api/qr-codes/{id}`

**Expected Response:**
- HTTP 200 OK
- QR code details with status

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes/$qrId" -Method GET | ConvertFrom-Json | Format-List
```

### 3. Backend: List All QR Codes

**Endpoint:** `GET http://localhost:8080/api/qr-codes`

**Expected Response:**
- HTTP 200 OK
- Array of QR codes (newest first)

### 4. Backend: Filter by Status

**Endpoint:** `GET http://localhost:8080/api/qr-codes/status/VALID`

**Expected Response:**
- HTTP 200 OK
- Only QR codes with status = VALID
- Should NOT include endpoint for SCANNED status

```powershell
# Valid codes
Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes/status/VALID" -Method GET

# Expired codes (empty initially)
Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes/status/EXPIRED" -Method GET

# Should fail: SCANNED endpoint removed
Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes/status/SCANNED" -Method GET
# Expected: 404 or error
```

### 5. Backend: Auto-Expiration Scheduler

**What to verify:**
- Wait 1 minute for QR code to expire (created with 1 minute expiry)
- Check status changes from VALID → EXPIRED automatically

```powershell
# Wait 70 seconds
Start-Sleep -Seconds 70

# Check status
$qr = Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes/$qrId" -Method GET | ConvertFrom-Json
Write-Output "Status: $($qr.status)"  # Should be EXPIRED
```

**Expected:**
- Scheduler runs every 60 seconds
- VALID QR codes past `expiredAt` time → status = EXPIRED in DB
- No manual endpoint call needed

### 6. Backend: Removed Endpoints

**Verify these no longer exist:**

```powershell
# Should return 404
Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes/$qrId/scan" -Method PUT
# Expected: HTTP 404 or Method Not Allowed
```

### 7. Microservice Integration Test

**Simulate another service generating QR:**

```powershell
# Service A: Order Service generates QR
$qr1 = Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes" `
  -Method POST `
  -ContentType "application/json" `
  -Body (@{
    sublinkUrl = "https://order-service/orders/ORD-12345"
    expiresInMinutes = 1440
    createdBy = "order-service"
  } | ConvertTo-Json)

$orderId = ($qr1.Content | ConvertFrom-Json).id

# Service B: Shipping Service retrieves QR
Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes/$orderId" -Method GET

# Service C: Inventory queries VALID codes
Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes/status/VALID" -Method GET
```

**Expected:**
- All services can use the same API
- QR code data is shared across services
- No SCANNED blocking (multi-use enabled)

## Validation Checklist

### Backend
- [ ] Java 17+ running
- [ ] PostgreSQL connected
- [ ] Spring Boot started on port 8080
- [ ] POST /qr-codes generates QR (HTTP 201)
- [ ] GET /qr-codes retrieves all (HTTP 200)
- [ ] GET /qr-codes/{id} retrieves single (HTTP 200)
- [ ] GET /qr-codes/status/VALID filters (HTTP 200)
- [ ] GET /qr-codes/status/EXPIRED filters (HTTP 200)
- [ ] PUT /qr-codes/{id}/scan endpoint removed (HTTP 404)
- [ ] Auto-expiration runs every 60 seconds
- [ ] VALID QR codes expire to EXPIRED after expiredAt time
- [ ] Schema allows only VALID, EXPIRED status

### Database
- [ ] PostgreSQL table created
- [ ] Status CHECK constraint: ('VALID', 'EXPIRED')
- [ ] No 'SCANNED' in constraint
- [ ] Indexes on expired_at, status, created_at

### Documentation
- [ ] README.md updated (SCANNED removed)
- [ ] INTEGRATION.md explains multi-use pattern
- [ ] backend/README.md updated
- [ ] SETUP.md updated

## Common Issues & Fixes

### Gradle won't run
```powershell
cd backend
Remove-Item -r .gradle
.\gradlew.bat bootRun
```

### Port already in use
```powershell
# Kill process on port 8080 (backend)
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Database connection refused
```powershell
# Check Docker is running
docker ps | findstr postgres-qr

# Or start it
docker start postgres-qr
```

## Pass/Fail Criteria

**PASS:** All 30 checkboxes checked ✓

**FAIL:** Any of:
- Backend won't start
- SCANNED status still in code/DB
- Auto-expiration not working
- Status filters wrong

---

Run this validation after each major change or before deployment.
