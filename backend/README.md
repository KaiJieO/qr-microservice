# QR Code Microservice - Backend

Spring Boot 3.2 microservice for QR code generation and management.

## Prerequisites

- Java 17+
- PostgreSQL 12+
- Gradle 8.0+

## Setup

### 1. Database Setup

```powershell
# Connect to PostgreSQL
psql -U postgres

# Run schema
\i src/main/resources/schema.sql
```

Or manually:
```sql
CREATE DATABASE qrcode_db;
CREATE EXTENSION "uuid-ossp";

CREATE TABLE qr_codes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sublink_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at TIMESTAMP NOT NULL,
    qr_code_data BYTEA,
    created_by VARCHAR(100)
);

CREATE INDEX idx_qr_expired_at ON qr_codes(expired_at);
CREATE INDEX idx_qr_status ON qr_codes(status);
```

### 2. Run Application

```powershell
gradle bootRun
```

Server runs on `http://localhost:8080`

## API Endpoints

### Generate QR Code
```
POST /api/qr-codes
Content-Type: application/json

{
  "sublinkUrl": "https://example.com/resource",
  "expiresInMinutes": 60,
  "createdBy": "admin"
}
```

### Get All QR Codes
```
GET /api/qr-codes
```

### Get QR Code by ID
```
GET /api/qr-codes/{id}
```

### Get QR Codes by Status
```
GET /api/qr-codes/status/{status}
```

Status values: `VALID`, `EXPIRED`

### Expire Outdated QR Codes
```
POST /api/qr-codes/expire-outdated
```

**Note:** QR code expiration status updates automatically via scheduled task every 60 seconds.

## Build

```powershell
gradle build
```

## Docker (Optional)

```powershell
gradle bootBuildImage --imageName=qr-service:latest
docker run -p 8080:8080 qr-service:latest
```
