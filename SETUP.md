# Local Setup Guide - Windows

**Backend microservice only. No frontend included.**

Complete step-by-step setup for running QR Code Microservice locally on Windows.

## Prerequisites

### Required
- **Java 17 LTS** - [Download](https://www.oracle.com/java/technologies/downloads/#java17)
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

## Step 5: Test the Backend API

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

## Troubleshooting

### Backend won't start

**Error: "Connection refused"**
- PostgreSQL not running
- Check Docker: `docker ps | findstr postgres-qr`
- Check local: Services → PostgreSQL → Running?

**Error: "gradle not found"**
- Run from `backend` directory
- Delete `.gradle` folder and retry

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

## Stopping Services

```powershell
# Backend: Ctrl+C in backend PowerShell window

# Docker containers
docker stop postgres-qr

# Restart later
docker start postgres-qr
```

## Next: Production Deployment

See README.md for Docker Compose and cloud deployment options.
