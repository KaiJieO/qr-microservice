# QR Code Microservice

**Backend-only microservice for QR code generation and management.**

This is a Spring Boot REST API designed to be consumed by other microservices, web apps, desktop apps, and mobile apps. No frontend included.

## Quick Start

- **Setup:** See `QUICK_START.md`
- **Docker:** See `DOCKER.md`
- **Testing:** See `TEST_VALIDATION.md`
- **API Integration:** See `INTEGRATION.md`

## Architecture

Backend microservice that:
- Generates QR codes from URLs
- Stores metadata (expiry, creator, status)
- Provides REST API for CRUD operations
- Auto-expires QR codes on schedule
- Returns PNG images on demand

**No frontend UI.** Designed for API-first consumption.

