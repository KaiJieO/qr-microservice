# QR Microservice - End-to-End Validation Plan

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

# Terminal 3: Frontend
cd frontend
npm start
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

### 7. Frontend: QR Code Display

**URL:** `http://localhost:3000`

**Steps:**
1. Enter URL: `https://example.com/test`
2. Click "Generate QR Code"
3. **Verify:**
   - Visual QR code appears (canvas element)
   - QR code shows the entered URL
   - Metadata displayed: ID, URL, Status, Created date, Expiry date

### 8. Frontend: QR History Table

**Verify:**
1. Generated QR appears in history table
2. Table columns: ID, URL, Status, Created At, Expires At, Created By
3. **Removed columns:**
   - "Scanned At" column removed
   - "Actions" column removed
   - "Mark Scanned" button removed
4. Filter buttons: All, Valid, Expired (SCANNED button removed)

### 9. Frontend: Status Updates

**Steps:**
1. Generate QR with 1 minute expiry
2. Wait 70 seconds
3. Click "Expired" filter button
4. **Verify:**
   - QR code moves from "Valid" to "Expired" filter
   - Status badge shows "EXPIRED"
   - No manual action needed (auto-updated by backend)

### 10. Microservice Integration Test

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

### Frontend
- [ ] React app running on port 3000
- [ ] QR code generation form works
- [ ] Visual QR code displays (canvas)
- [ ] History table shows generated QRs
- [ ] Filter buttons: All, Valid, Expired (no Scanned)
- [ ] Scanned At column removed
- [ ] Mark Scanned button removed
- [ ] Status updates without page refresh
- [ ] Expired QRs move to Expired filter automatically

### Database
- [ ] PostgreSQL table created
- [ ] Status CHECK constraint: ('VALID', 'EXPIRED')
- [ ] No 'SCANNED' in constraint
- [ ] Indexes on expired_at, status, created_at

### Documentation
- [ ] README.md updated (SCANNED removed)
- [ ] INTEGRATION.md explains multi-use pattern
- [ ] backend/README.md updated
- [ ] frontend/README.md updated
- [ ] SETUP.md updated
- [ ] schema.sql updated

## Common Issues & Fixes

### Gradle won't run
```powershell
cd backend
Remove-Item -r .gradle
.\gradlew.bat bootRun
```

### npm won't start
```powershell
cd frontend
npm install --legacy-peer-deps
npm start
```

### Port already in use
```powershell
# Kill process on port 8080 (backend)
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Kill process on port 3000 (frontend)
netstat -ano | findstr :3000
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
- Frontend won't connect to backend
- QR code not displaying visually
- SCANNED status still in code/DB
- Mark Scanned button still showing
- Auto-expiration not working
- Status filters wrong

---

Run this validation after each major change or before deployment.
