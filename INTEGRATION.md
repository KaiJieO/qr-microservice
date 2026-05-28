# QR Service - Microservice Integration Guide

Integrate QR code generation into other microservices via REST API. No frontend required.

## Overview

QR Service exposes HTTP endpoints for microservice-to-microservice communication. Other services call the API directly to generate, retrieve, and manage QR codes.

```
┌──────────────────┐
│ Order Service    │──┐
└──────────────────┘  │
                      │  HTTP
┌──────────────────┐  │
│ Shipping Service │──┼──→ QR Service API
└──────────────────┘  │
                      │
┌──────────────────┐  │
│ Inventory Svc    │──┘
└──────────────────┘
```

## Base URL

```
http://localhost:8080/api
```

Change `localhost:8080` to QR service hostname/port in production.

## Generate QR Code

**Endpoint:** `POST /qr-codes`

**Request:**
```json
{
  "sublinkUrl": "https://order-service.internal/orders/ORD-12345",
  "expiresInMinutes": 1440,
  "createdBy": "order-service"
}
```

**Response:** HTTP 201
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "sublinkUrl": "https://order-service.internal/orders/ORD-12345",
  "status": "VALID",
  "createdAt": "2026-05-28T10:15:30",
  "expiredAt": "2026-05-29T10:15:30",
  "createdBy": "order-service"
}
```

**Usage Example (Java):**
```java
RestTemplate restTemplate = new RestTemplate();

Map<String, Object> request = Map.of(
  "sublinkUrl", "https://order-service/orders/ORD-123",
  "expiresInMinutes", 1440,
  "createdBy", "order-service"
);

ResponseEntity<Map> response = restTemplate.postForEntity(
  "http://qr-service:8080/api/qr-codes",
  request,
  Map.class
);

String qrId = response.getBody().get("id").toString();
// Store qrId in your database
```

**Usage Example (Node.js):**
```javascript
const axios = require('axios');

const response = await axios.post('http://qr-service:8080/api/qr-codes', {
  sublinkUrl: 'https://order-service/orders/ORD-123',
  expiresInMinutes: 1440,
  createdBy: 'order-service'
});

const qrId = response.data.id;
// Store qrId in your database
```

## Get QR Code by ID

**Endpoint:** `GET /qr-codes/{id}`

```bash
curl http://localhost:8080/api/qr-codes/550e8400-e29b-41d4-a716-446655440000
```

**Response:** HTTP 200
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "sublinkUrl": "https://order-service.internal/orders/ORD-12345",
  "status": "VALID",
  "createdAt": "2026-05-28T10:15:30",
  "expiredAt": "2026-05-29T10:15:30",
  "createdBy": "order-service"
}
```

## Get All QR Codes

**Endpoint:** `GET /qr-codes`

```bash
curl http://localhost:8080/api/qr-codes
```

**Response:** HTTP 200 (array)
```json
[
  { "id": "uuid-1", "status": "VALID", ... },
  { "id": "uuid-2", "status": "EXPIRED", ... }
]
```

## Filter by Status

**Endpoint:** `GET /qr-codes/status/{status}`

**Status values:** `VALID`, `EXPIRED`

```bash
curl http://localhost:8080/api/qr-codes/status/VALID
curl http://localhost:8080/api/qr-codes/status/EXPIRED
```

## Multi-Use QR Codes (Recommended)

QR codes remain VALID until auto-expiration. Multiple users/systems can access the same QR code.

If your use case requires tracking scan events, implement this in your own service:

```
1. Generate QR in QR Service → store QR ID
2. Multiple users access/scan same QR code
3. Your service logs each access event (optional)
4. QR Service auto-expires after expiredAt timestamp
5. QR status changes VALID → EXPIRED automatically
```

**Optional: Track access events in your service**

If needed, add to your service database:
```sql
CREATE TABLE qr_access_log (
  id UUID PRIMARY KEY,
  qr_code_id UUID NOT NULL,
  accessed_by VARCHAR(100),
  accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  access_location VARCHAR(255)
);
```

Example:
```java
// When QR code accessed, log it (optional)
accessLogService.recordAccess(qrCodeId, userId, location);
// QR Service handles expiration automatically
```

Note: QR Service tracks only generation and expiration, not access events.

## Best Practices

### 1. Store QR ID, not URL

```java
// ✅ Good
order.setQrCodeId("550e8400-e29b-41d4-a716-446655440000");

// ❌ Avoid
order.setQrCodeUrl("https://order-service/orders/ORD-123"); // duplicates data
```

Reason: If URL changes, QR ID is stable. Fetch fresh URL from QR service via ID.

### 2. Handle Expiration Gracefully

QR codes auto-expire after expiredAt timestamp. Check status before use:

```java
// Check if QR is expired
QrCodeResponse qr = qrService.getQrCode(qrId);
if ("EXPIRED".equals(qr.getStatus())) {
  // Generate new QR or notify user
  qrId = qrService.generateQrCode(...).getId();
} else if ("VALID".equals(qr.getStatus())) {
  // Safe to use
}
```

### 3. Set Appropriate Expiration

```
expiresInMinutes = context-dependent

60 minutes     → One-time checkout links
1440 minutes   → Multi-day order shipments
10080 minutes  → Weekly asset tags (7 days)
525600 minutes → Yearly product codes
```

### 4. Track Creator Context

```json
{
  "sublinkUrl": "...",
  "expiresInMinutes": 60,
  "createdBy": "order-service:checkout-flow"
}
```

Helps debugging and audit trails.

### 5. Retry on Failure

```java
int maxRetries = 3;
for (int i = 0; i < maxRetries; i++) {
  try {
    QrCodeResponse qr = qrService.generateQrCode(request);
    return qr.getId();
  } catch (Exception e) {
    if (i == maxRetries - 1) throw e;
    Thread.sleep(1000); // wait before retry
  }
}
```

## Error Handling

### 404 Not Found
```
GET /qr-codes/invalid-id
→ HTTP 404: "QR Code not found"
```

Handle: Generate new QR or return error to user.

### 500 Internal Error
```
POST /qr-codes (QR Service down)
→ HTTP 500
```

Handle: Retry with exponential backoff. Fall back to cached QR if needed.

### Connection Timeout

```
QR Service unreachable
```

Handle: Use circuit breaker pattern or cache generated QRs locally.

## Security

### For Development
- No authentication required (localhost testing)
- CORS enabled for all origins

### For Production
- Implement API key authentication
- Use mTLS between services
- Restrict CORS to internal services only
- Encrypt QR data in transit

Example (add to QrCodeController):
```java
@PostMapping
@PreAuthorize("hasRole('MICROSERVICE')")
public ResponseEntity<QrCodeResponse> generateQrCode(@RequestBody QrCodeRequest request) {
  // ...
}
```

## Monitoring

Log QR generation/usage in your service:

```
[ORDER-SERVICE] Generated QR: 550e8400... for order ORD-12345
[SHIPPING-SERVICE] Accessed QR: 550e8400... at location warehouse-2
[INVENTORY-SERVICE] QR expired: 550e8400... (24h past expiry)
```

Check QR Service logs for auto-expiration:
```
[QR-SERVICE] Expired 5 outdated QR codes (scheduled task)
```

## Example: Order Service Integration

```java
@Service
public class OrderService {
  
  @Autowired
  private RestTemplate restTemplate;
  
  private static final String QR_SERVICE_URL = "http://qr-service:8080/api/qr-codes";
  
  public void createOrder(OrderRequest request) {
    // Create order
    Order order = new Order();
    order.setId(UUID.randomUUID().toString());
    order.setStatus("PENDING");
    
    // Generate QR code
    try {
      QrCodeResponse qr = generateQrCode(order.getId());
      order.setQrCodeId(qr.getId());
      order.setQrUrl(qr.getSublinkUrl());
    } catch (Exception e) {
      log.warn("Failed to generate QR code: {}", e.getMessage());
      // Continue without QR (graceful degradation)
    }
    
    // Save order
    orderRepository.save(order);
  }
  
  private QrCodeResponse generateQrCode(String orderId) {
    Map<String, Object> request = Map.of(
      "sublinkUrl", "https://order-service/orders/" + orderId,
      "expiresInMinutes", 1440,
      "createdBy", "order-service"
    );
    
    ResponseEntity<QrCodeResponse> response = restTemplate.postForEntity(
      QR_SERVICE_URL,
      request,
      QrCodeResponse.class
    );
    
    return response.getBody();
  }
}
```

## Troubleshooting

### QR Service connection refused
```
Check QR Service is running: curl http://localhost:8080/api/qr-codes
Check firewall/network policies allow access
Check service URL in your config matches QR Service hostname:port
```

### QR codes not expiring
```
Check QR Service logs for scheduled task errors
Check database has correct expired_at timestamps
Manually trigger: POST /qr-codes/expire-outdated
```


## API Response Codes

| Code | Meaning | Action |
|------|---------|--------|
| 200 | OK | Success |
| 201 | Created | QR generated |
| 400 | Bad Request | Invalid input (check request JSON) |
| 404 | Not Found | QR doesn't exist (wrong ID) |
| 500 | Server Error | QR Service down (retry later) |

---

**Support:** Check backend README.md for detailed API documentation.
