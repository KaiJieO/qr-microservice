-- Initial QR Code schema
CREATE TABLE IF NOT EXISTS qr_codes (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    sublink_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    CONSTRAINT chk_status CHECK (status IN ('VALID', 'EXPIRED')),
    CONSTRAINT chk_expired_after_created CHECK (expired_at > created_at)
);

-- Index for status queries
CREATE INDEX IF NOT EXISTS idx_qr_status_created ON qr_codes(status, created_at DESC);

-- Index for expiration queries
CREATE INDEX IF NOT EXISTS idx_qr_expired_at ON qr_codes(expired_at);
