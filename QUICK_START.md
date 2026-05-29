# Quick Start - 5 Minute Setup

**Backend microservice only. No frontend included. Use REST API for integration.**

## Prerequisite Check

```powershell
java -version          # Java 17+
docker --version       # Optional but recommended
```

## Start PostgreSQL

### Docker (Recommended)
```powershell
docker run --name postgres-qr `
  -e POSTGRES_PASSWORD=postgres `
  -e POSTGRES_DB=qrcode_db `
  -p 5432:5432 -d postgres:15
```

### Or use docker-compose
```powershell
docker-compose up -d postgres
```

## Start Backend

```powershell
cd C:\Users\Kai Lin\Documents\Github\qr-microservice\backend
gradle bootRun
```

✅ Backend ready on `http://localhost:8080`

## That's It!

Backend microservice running. Test via cURL or Postman.

## Testing Endpoints

```powershell
# Generate QR Code
curl -X POST http://localhost:8080/api/qr-codes `
  -H "Content-Type: application/json" `
  -d '{
    "sublinkUrl": "https://example.com",
    "expiresInMinutes": 60,
    "createdBy": "admin"
  }'

# Get all QR codes
curl http://localhost:8080/api/qr-codes

# Get by status
curl http://localhost:8080/api/qr-codes/status/VALID
```

## Stop Services

```powershell
# Ctrl+C in backend terminal
# Stop database
docker stop postgres-qr
```

## Detailed Setup

See `SETUP.md` for full Windows setup with troubleshooting.

## Architecture

```
Backend (Spring Boot 3.2 / Java 17)
      ↓ SQL
Database (PostgreSQL 15)
```

## File Structure

```
qr-microservice/
├── backend/               # Spring Boot service
│   ├── src/main/java/    # Source code
│   ├── build.gradle.kts  # Gradle config
│   └── README.md
├── docker-compose.yml    # Database setup
├── README.md            # Full documentation
├── SETUP.md             # Detailed Windows setup
└── QUICK_START.md       # This file
```

## Common Issues

| Issue | Fix |
|-------|-----|
| Port 5432 in use | `docker stop postgres-qr` or check Services |
| Backend won't connect to DB | Check docker is running: `docker ps` |

## Next

- Modify QR expiry: `backend/src/main/resources/application.yml`
- Add authentication: See README.md Future Steps

---

**API Base:** `http://localhost:8080/api`  
**Database:** `localhost:5432`
