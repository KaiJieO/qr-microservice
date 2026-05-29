# QR Microservice — Architecture & Recommendations

**Date:** 2026-05-28  
**Context:** Personal project for multi-platform integration  
**Priority:** Backend > Frontend

---

## Executive Summary

**Backend:** Production-ready foundation with critical gaps (error handling, validation, logging).  
**Frontend:** Test UI only, don't invest further.  
**Next Steps:** Remove blob storage, add caching, input validation (3-4 days effort).  
**Scaling Path:** Guava Cache now → Spring Cache + Redis at 1M users.

---

## Context

- **Type:** Personal project, internal use
- **Scale Target:** Microservice consumption (Order, Shipping, Inventory services)
- **Multi-platform:** Web, Desktop, Mobile apps via API
- **QR Codes:** Deterministic (encoded from URL), not random
- **Database:** SQL (PostgreSQL/MySQL) — good choice

---

## Backend Evaluation

### ✓ Strengths

- Clean architecture (controller → service → repository pattern)
- Well-documented REST API (INTEGRATION.md excellent)
- Scheduled background task for auto-expiration
- Proper DTO separation (request/response)
- Stateless design (horizontally scalable)
- UUID auto-generation
- Spring Boot + JPA solid foundation

### 🔴 Critical Gaps

#### 1. Error Handling (High Priority)
**Issue:** Generic `RuntimeException` on 404
```java
// Current (bad)
throw new RuntimeException("QR Code not found");
```

**Fix:** Custom exception + global handler
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(QrCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(QrCodeNotFoundException e) {
        return ResponseEntity.status(404)
            .body(new ErrorResponse("NOT_FOUND", e.getMessage()));
    }
}
```

#### 2. Input Validation (High Priority)
**Issue:** `QrCodeRequest` missing validation annotations
```java
// Current (bad)
private String sublinkUrl;
private int expiresInMinutes = 60;

// Fix
@NotBlank(message = "URL required")
@URL(message = "Invalid URL format")
private String sublinkUrl;

@Min(1) @Max(525600)  // 1 min to 1 year
private int expiresInMinutes = 60;
```

#### 3. Security (High Priority)
```java
// Current (bad)
@CrossOrigin(origins = "*")  // Too permissive

// Fix
@CrossOrigin(origins = {"https://internal-domain.com"})
```

Additional concerns:
- No authentication/authorization
- `createdBy` field unvalidated (injection risk)
- No rate limiting

#### 4. Logging & Observability (Medium Priority)
**Issue:** Only error log in scheduled task
```java
// Add structured logging
@Slf4j
@Service
public class QrCodeService {
    
    public QrCodeResponse generateQrCode(QrCodeRequest request) {
        log.info("Generating QR code for URL: {} by {}", 
            request.getSublinkUrl(), request.getCreatedBy());
        
        QrCodeResponse response = // ... generate
        
        log.info("QR code generated: {} with ID: {}", 
            response.getSublinkUrl(), response.getId());
        return response;
    }
}
```

#### 5. Database Design (Medium Priority)

**Problem: Blob Storage**
- Current code stores `qrCodeData` as LONGBLOB
- QR codes are **deterministic** (same URL = same image)
- Storing blob wastes space, slows queries

**Solution: Remove blob, generate on-demand**
```java
// Remove this:
@Column(columnDefinition = "LONGBLOB")
private byte[] qrCodeData;

// Add endpoint:
@GetMapping("/{id}/image")
public ResponseEntity<byte[]> getQrImage(@PathVariable String id) {
    QrCode qr = service.getQrCode(id);
    byte[] image = service.generateQrImage(qr.getSublinkUrl());
    return ResponseEntity.ok()
        .contentType(MediaType.IMAGE_PNG)
        .body(image);
}
```

**Benefit:** 80% smaller database, faster queries.

#### 6. Pagination (Medium Priority)
**Issue:** `getAllQrCodes()` unfiltered
```java
// Current (breaks at 100k+ records)
return qrCodeRepository.findAllByOrderByCreatedAtDesc();

// Fix
@GetMapping
public ResponseEntity<Page<QrCodeResponse>> getAllQrCodes(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size) {
    Page<QrCode> codes = repo.findAllByOrderByCreatedAtDesc(
        PageRequest.of(page, size));
    return ResponseEntity.ok(codes.map(this::mapToResponse));
}
```

#### 7. Testing (High Priority)
**Issue:** No tests in codebase
```java
@SpringBootTest
class QrCodeServiceTest {
    
    @Test
    void shouldGenerateQrCode() {
        QrCodeRequest request = QrCodeRequest.builder()
            .sublinkUrl("https://test.com")
            .expiresInMinutes(60)
            .createdBy("test-user")
            .build();
        
        QrCodeResponse response = service.generateQrCode(request);
        
        assertNotNull(response.getId());
        assertEquals("VALID", response.getStatus());
        assertTrue(response.getExpiredAt().isAfter(LocalDateTime.now()));
    }
}
```

### Performance Issues

| Issue | Impact | Fix | Effort |
|-------|--------|-----|--------|
| No blob storage | OOM at 10k records | Remove qrCodeData field | 2 hours |
| No pagination | Timeout at 100k records | Add Page<QrCode> | 2 hours |
| No indexes | Slow status queries | Add DB index (status, createdAt) | 1 hour |
| Scheduled task no jitter | Thundering herd | Add @Scheduled(fixedRate + jitter) | 1 hour |

---

## Frontend Evaluation

### What It Does ✓
- Simple QR generation form
- History table with filtering
- Responsive UI
- Real-time display

### For Testing Only (Acceptable)
- Hardcoded `createdBy: 'admin'` (test-only, OK)
- No error boundaries (fine for test UI)
- Basic CSS (intentional, not production)
- No auth (internal testing)

### Recommendation
**Keep as-is for testing. Don't invest further.**

When real UI needed (web/mobile/desktop):
- Rebuild with TypeScript
- Add state management
- Implement error boundaries
- Use service abstraction

---

## QR Code Reality Check

### ❌ NOT Random Generated

Current implementation encodes URL into QR:
```java
QRCode.from(request.getSublinkUrl())  // URL = deterministic input
    .writeTo(out);
```

**Same URL → Same QR image always**  
Different URL → Different QR image

**Implication:** Without deduplication, same URL generates duplicate blobs.

### ✓ Deterministic = Good for On-Demand Generation
No need to store blob if regenerating is cheap (<5ms).

---

## Storage Options: Detailed Comparison

### Option A: Keep Blob in DB (Current)
```java
@Column(columnDefinition = "LONGBLOB")
private byte[] qrCodeData;
```

| Aspect | Rating |
|--------|--------|
| Space | ❌ High (duplicates) |
| Speed | ✓ Fast (direct retrieval) |
| Simplicity | ✓ Easy (no generation) |
| Scalability | ❌ Poor (blob locking) |

### Option B: Generate On-Demand (Recommended)
```java
// Remove qrCodeData field
// GET /qr-codes/{id}/image → regenerate fresh
```

| Aspect | Rating |
|--------|--------|
| Space | ✓ Minimal |
| Speed | ✓ <5ms generation |
| Simplicity | ✓ Simple |
| Scalability | ✓ Excellent |

### Option C: Deduplicate + Cache (Optimal)
```java
// Store URL hash
@Column
private String urlHash = SHA256(sublinkUrl);

// Reuse QR for same URL
List<QrCode> existing = repo.findByUrlHash(urlHash);
```

| Aspect | Rating |
|--------|--------|
| Space | ✓ One blob per unique URL |
| Speed | ✓ Fast (deduplicated) |
| Simplicity | ⚠️ Medium |
| Scalability | ✓ Good |

### Decision: **Option B (Recommended)**
- Remove blob from database
- Add GET `/qr-codes/{id}/image` endpoint
- Generate fresh from `sublinkUrl`
- Cache with TTL (see below)

---

## Caching Strategy

### Current Situation (No Cache)
```
Each request → DB query (5ms) + QR generation (3ms) = 8ms
```

### Option 1: Guava Cache (In-Memory Library)

**Best for personal project.**

```java
@Configuration
public class CacheConfig {
    @Bean
    public Cache<String, byte[]> qrImageCache() {
        return CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000)  // Limit memory
            .build();
    }
}

@Service
@RequiredArgsConstructor
public class QrCodeService {
    private final Cache<String, byte[]> imageCache;
    
    @GetMapping("/{id}/image")
    public byte[] getQrImage(String id) {
        return imageCache.get(id, () -> {
            QrCode qr = repo.findById(id)
                .orElseThrow(() -> new NotFoundException(...));
            return generateQrCodeData(qr.getSublinkUrl());
        });
    }
}
```

| Aspect | Rating |
|--------|--------|
| Setup | ✓ 1 dependency, 10 lines |
| Speed | ✓ <1ms hit |
| External Services | ✓ None (in-process) |
| Scaling | ❌ Single instance only |
| Multi-Server | ❌ No (each has own copy) |

### Option 2: Spring Cache (Framework Abstraction)

**Better for scaling later.**

```java
@Configuration
@EnableCaching
public class CacheConfig { }

@Service
public class QrCodeService {
    
    @Cacheable(value = "qrImages", key = "#id")
    public byte[] getQrImage(String id) {
        QrCode qr = repo.findById(id)
            .orElseThrow(() -> new NotFoundException(...));
        return generateQrCodeData(qr.getSublinkUrl());
    }
}

// Config
spring.cache.type=simple  // Or redis
```

| Aspect | Rating |
|--------|--------|
| Setup | ✓ Annotation + config |
| Speed | ✓ <1ms hit (slight overhead) |
| External Services | ⚠️ Optional (configurable) |
| Scaling | ✓ Can swap to Redis |
| Multi-Server | ✓ Yes (if Redis) |

### Recommendation: **Guava Cache (Now)**

**Why:**
- Personal project (no multi-instance)
- Simple, explicit code
- Zero external dependencies
- Easy to debug
- Can migrate to Spring Cache + Redis later (50 min)

**Implementation:**
```xml
<!-- Add dependency -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
    <version>32.1.3-jre</version>
</dependency>
```

```java
// Create cache bean
@Bean
public Cache<String, byte[]> qrImageCache() {
    return CacheBuilder.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .maximumSize(10000)
        .build();
}

// Use in service
public byte[] getQrImage(String id) {
    return imageCache.get(id, () -> generateQrCodeData(id));
}
```

**Performance Impact:**
```
Without cache:
GET /image (1000x/sec) → 8ms each = 8 sec total latency

With cache (90% hit rate):
GET /image (1000x/sec) → ~100 misses (8ms) + 900 hits (1ms) = ~1.7 sec
```

---

## Scaling Path: 1M Users

### Problem: Guava Cache Doesn't Scale
```
Single instance cache:
- Each server has own copy
- Cache miss = DB hit
- At 3+ servers: hit rate drops to 30%
→ Database overloaded
```

### Migration Strategy (When Needed)

**Triggers:**
- Traffic spikes to 100 req/sec+
- Cache hit rate drops below 80%
- Database query time > 50ms
- Multiple instances deployed

**Migration (50 minutes):**

1. Add Redis dependency (2 min)
2. Remove Guava bean (5 min)
3. Change to Spring Cache annotations (10 min)
4. Add Redis config (5 min)
5. Test (15 min)
6. Deploy (10 min)

**No code logic changes.** Just config.

```java
// Step 1: Add dependency
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

// Step 2: Remove Guava bean

// Step 3: Add Spring Cache
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        return RedisCacheManager.create(factory);
    }
}

// Step 4: Refactor to annotation
@Cacheable(value = "qrImages", key = "#id")
public byte[] getQrImage(String id) {
    return generateQrCodeData(id);
}

// Step 5: Config
spring.cache.type=redis
spring.redis.host=redis.internal
spring.redis.port=6379
spring.cache.redis.time-to-live=300000
```

---

## Recommended Implementation Plan

### Phase 1: Foundation (3-4 days)

**Priority: HIGH**

- [ ] Remove qrCodeData blob field from entity
- [ ] Add `GET /qr-codes/{id}/image` endpoint
- [ ] Add input validation (URL format, expiry bounds)
- [ ] Create custom exceptions + global exception handler
- [ ] Add structured logging to service
- [ ] Unit tests for service layer (>80% coverage)
- [ ] Update documentation

**Effort:** 3-4 days solo

**Code changes:**
```
QrCode.java:        -1 field (qrCodeData)
QrCodeRequest.java: +5 validation annotations
QrCodeService.java: +10 lines (logging), refactor generation
QrCodeController.java: +1 endpoint (/image), -1 field in response
Exception classes:  +3 custom exceptions
ExceptionHandler:   +1 new class
Tests:              +5 test classes
```

### Phase 2: Cache & Performance (2 days)

**Priority: MEDIUM**

- [ ] Add Guava Cache bean
- [ ] Implement caching in getQrImage()
- [ ] Add DB index on (status, createdAt)
- [ ] Add pagination to getAllQrCodes()
- [ ] Database migration framework (Flyway)

**Effort:** 2 days

### Phase 3: Scale (Later, When Needed)

**Priority: LOW** (only when traffic demands)

- [ ] Migrate to Spring Cache + Redis
- [ ] Add load balancer (Nginx)
- [ ] Database read replicas
- [ ] Prometheus metrics
- [ ] Helm charts

---

## Go-Live Checklist (Personal)

```
Backend
  ✓ Remove blob storage
  ✓ Add image endpoint
  ✓ Input validation (all fields)
  ✓ Exception handling (404, 400, 500)
  ✓ Structured logging
  ✓ Unit tests (>80% coverage)
  ✓ Pagination (getAllQrCodes)
  ✓ Guava Cache configured
  ✓ Database migrations
  ✓ README updated

Frontend
  ✓ Keep test UI as-is
  ✓ Document as "testing only"
  ✓ No further investment
```

---

## Database Schema Changes

### Remove Blob
```sql
-- Before
ALTER TABLE qr_codes ADD COLUMN qr_code_data LONGBLOB;

-- After
ALTER TABLE qr_codes DROP COLUMN qr_code_data;
```

### Add Index
```sql
CREATE INDEX idx_qr_status_created 
ON qr_codes(status, created_at DESC);
```

---

## API Changes Summary

### Remove from Response
```json
// Before
{
  "id": "...",
  "sublinkUrl": "...",
  "qrCodeData": "base64..."  // ❌ Remove
}

// After
{
  "id": "...",
  "sublinkUrl": "...",
  "status": "...",
  "createdAt": "...",
  "expiredAt": "...",
  "createdBy": "..."
}
```

### Add New Endpoint
```
GET /qr-codes/{id}/image
Response: PNG binary (image/png)
Cache: 5-minute TTL (Guava Cache)
```

---

## Technology Decisions

| Decision | Choice | Reason |
|----------|--------|--------|
| Storage | SQL (PostgreSQL) | Good fit, no change needed |
| QR Blob | Remove (generate on-demand) | Deterministic, cacheable |
| Cache Strategy | Guava Cache (in-memory) | Personal project, simple |
| Scale Path | Guava → Spring Cache + Redis | Easy migration (50 min) |
| Frontend | Keep test UI | Disposable, don't invest |
| Error Handling | Custom exceptions | Production quality |
| Logging | Structured (SLF4J) | Debugging, multi-service |
| Testing | Unit + integration | >80% coverage |
| Validation | Jakarta (Hibernate) | Standard, built-in |

---

## Security Considerations

**For Personal Use:**
- CORS open to localhost (OK for testing)
- No authentication required (internal)
- No rate limiting needed yet

**When Shared Externally:**
- Add API key authentication
- Restrict CORS to internal domains
- Implement rate limiting (50 req/min/IP)
- Validate all inputs
- Log access attempts

---

## Monitoring & Observability

### Metrics to Track
- QR generation time (should be <10ms with cache)
- Cache hit rate (should be >85%)
- DB query time (should be <5ms indexed)
- Error rate (should be <0.1%)

### Logging Pattern
```
[timestamp] [level] [service] [trace-id] message
2026-05-28 10:15:30 INFO QrCodeService abc-123-def Generating QR: https://example.com by user1
```

---

## Effort Estimates

| Task | Days | Notes |
|------|------|-------|
| Remove blob + add image endpoint | 0.5 | Simple refactor |
| Input validation | 0.5 | Add annotations |
| Exception handling | 0.5 | Custom classes |
| Logging | 0.5 | SLF4J setup |
| Unit tests | 1.0 | Service layer |
| Integration tests | 0.5 | API endpoints |
| Caching (Guava) | 0.5 | Bean + code |
| Pagination | 0.5 | Repository change |
| DB migration | 0.5 | Flyway setup |
| Documentation | 0.5 | README + comments |
| **Total** | **5.5 days** | **Solo effort** |

---

## Summary

**Start with Phase 1 (Foundation).** Gets you to production-quality in 3-4 days.

**Add Phase 2 (Cache) after Phase 1.** Improves performance, adds caching foundation.

**Phase 3 (Scale) only when traffic demands.** Don't build for 1M users today.

**Frontend:** Keep test UI, don't invest further.

**Scaling:** Guava Cache now, Spring Cache + Redis later (50 min migration).

---

## References

- INTEGRATION.md — API documentation (excellent)
- Spring Boot docs — Error handling, caching
- Guava Cache — In-memory caching library
- Flyway — Database migrations

---

**Last Updated:** 2026-05-28  
**Author:** Architecture Review  
**Status:** Recommendations Ready for Implementation
