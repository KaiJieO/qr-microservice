# Local Setup Guide - Windows

Complete step-by-step setup for running QR Code Microservice locally on Windows.

## Prerequisites

### Required
- **Java 17 LTS** - [Download](https://www.oracle.com/java/technologies/downloads/#java17)
- **Node.js 16+** - [Download](https://nodejs.org)
- **PostgreSQL 12+** - [Download](https://www.postgresql.org/download/windows/) OR Docker

### Recommended
- **Git** - [Download](https://git-scm.com/download/win)
- **VS Code** - [Download](https://code.visualstudio.com)
- **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop)

## Step 1: Verify Installations

Open PowerShell and verify:

```powershell
# Java
java -version
# Should show: java 17.x.x

# Node
node --version
# Should show: v16.x.x or higher

# npm
npm --version
# Should show: 8.x.x or higher
```

## Step 2: Database Setup

### Option A: Docker (Recommended)

```powershell
# Pull and run PostgreSQL container
docker run --name postgres-qr `
  -e POSTGRES_PASSWORD=postgres `
  -e POSTGRES_DB=qrcode_db `
  -p 5432:5432 `
  -d postgres:15

# Verify
docker ps
```

### Option B: Local PostgreSQL

1. Install PostgreSQL from [here](https://www.postgresql.org/download/windows/)
2. Run installer with default settings
3. Note the password you set for `postgres` user
4. Create database:

```powershell
# Open PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE qrcode_db;

# Verify
\l

# Exit
\q
```

## Step 3: Backend Setup

```powershell
# Navigate to backend
cd C:\Users\Kai Lin\Documents\Github\qr-microservice\backend

# Build (downloads Gradle automatically)
gradle build

# Run
gradle bootRun
```

✅ Backend runs on `http://localhost:8080`

Check startup with:
```powershell
# Another PowerShell window
curl http://localhost:8080/api/qr-codes
# Should return: []
```

## Step 4: Frontend Setup

```powershell
# Open new PowerShell
# Navigate to frontend
cd C:\Users\Kai Lin\Documents\Github\qr-microservice\frontend

# Install dependencies (use --legacy-peer-deps for qrcode.react)
npm install --legacy-peer-deps

# Start dev server
npm start
```

✅ Frontend opens on `http://localhost:3000`

## Step 5: Test the System

### Backend API Test

```powershell
# Generate QR code
$body = @{
    sublinkUrl = "https://example.com/test"
    expiresInMinutes = 60
    createdBy = "test-user"
} | ConvertTo-Json

Invoke-WebRequest -Uri "http://localhost:8080/api/qr-codes" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body

# Should return QR code JSON with ID
```

### Frontend Test

1. Open `http://localhost:3000` in browser
2. Enter URL in "Generate QR Code" form
3. Click "Generate QR Code"
4. See visual QR code displayed in result card
5. Verify QR code appears in history table below

## Step 6: Database Management (Optional)

Use pgAdmin to manage database:

```powershell
docker run --name pgadmin `
  -e PGADMIN_DEFAULT_EMAIL=admin@example.com `
  -e PGADMIN_DEFAULT_PASSWORD=admin `
  -p 5050:80 `
  -d dpage/pgadmin4
```

Access at: `http://localhost:5050`
- Email: `admin@example.com`
- Password: `admin`

Register server:
- Hostname: `postgres-qr` (or `host.docker.internal` on Windows)
- Port: `5432`
- Username: `postgres`
- Password: `postgres`

## Troubleshooting

### Frontend npm install fails

**Error: "ERESOLVE unable to resolve dependency tree"**
- qrcode.react@1.0.1 doesn't support React 18
- Solution: `npm install --legacy-peer-deps`
- Or update qrcode.react: `npm install qrcode.react@latest --legacy-peer-deps`

### Backend won't start

**Error: "Connection refused"**
- PostgreSQL not running
- Check Docker: `docker ps | findstr postgres-qr`
- Check local: Services → PostgreSQL → Running?

**Error: "gradle not found"**
- Run from `backend` directory
- Delete `.gradle` folder and retry

### Frontend won't load data

**Error: "Failed to fetch"**
- Backend not running on port 8080
- Check CORS: ensure `@CrossOrigin(origins = "*")` in controller
- Update API base in `src/services/api.js` if needed

**Port already in use**
```powershell
# Find process using port 3000
netstat -ano | findstr :3000

# Kill process (replace PID)
taskkill /PID <PID> /F
```

### Database issues

**"database does not exist"**
- Create via PostgreSQL or Docker init script

**"permission denied"**
- Check `application.yml` credentials match PostgreSQL user

## Development Workflow

### Backend Changes

1. Edit Java files in `backend/src`
2. `gradle bootRun` auto-reloads on changes (with plugin)
3. Or restart manually: Ctrl+C, then re-run

### Frontend Changes

1. Edit React files in `frontend/src`
2. Browser auto-refreshes (hot reload)
3. Check console for errors

## Stopping Services

```powershell
# Backend: Ctrl+C in backend PowerShell window
# Frontend: Ctrl+C in frontend PowerShell window

# Docker containers
docker stop postgres-qr pgadmin

# Restart later
docker start postgres-qr pgadmin
```

## Next: Production Deployment

See README.md for Docker Compose and cloud deployment options.
