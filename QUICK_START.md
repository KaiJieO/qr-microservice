# Quick Start - 5 Minute Setup

## Prerequisite Check

```powershell
java -version          # Java 17+
node --version         # Node 16+
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

## Terminal 1: Start Backend

```powershell
cd C:\Users\Kai Lin\Documents\Github\qr-microservice\backend
gradle bootRun
```

✅ Backend ready on `http://localhost:8080`

## Terminal 2: Start Frontend

```powershell
cd C:\Users\Kai Lin\Documents\Github\qr-microservice\frontend
npm install
npm start
```

✅ Frontend opens on `http://localhost:3000`

## That's It!

- **Generate QR**: Fill form → click Generate
- **View History**: Auto-loads table below
- **Filter**: Click status buttons (Valid/Expired)

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
# Ctrl+C in backend and frontend terminals
# Stop database
docker stop postgres-qr
```

## Detailed Setup

See `SETUP.md` for full Windows setup with troubleshooting.

## Architecture

```
Frontend (React 18)
      ↓ HTTP
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
├── frontend/              # React app
│   ├── src/              # Source code
│   ├── package.json      # Dependencies
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
| Frontend shows "Failed to fetch" | Backend not running on 8080 |
| npm: command not found | Restart PowerShell after installing Node |

## Next

- Modify QR expiry: `backend/src/main/resources/application.yml`
- Change frontend colors: `frontend/src/App.css`
- Add authentication: See README.md Future Steps

---

**API Base:** `http://localhost:8080/api`  
**Frontend:** `http://localhost:3000`  
**Database:** `localhost:5432`
