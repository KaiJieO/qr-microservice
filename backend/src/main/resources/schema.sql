-- Create database
CREATE DATABASE IF NOT EXISTS qrcode_db;
USE qrcode_db;

-- Create qr_codes table
CREATE TABLE IF NOT EXISTS qr_codes (
    id CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    sublink_url VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expired_at TIMESTAMP NOT NULL,
    qr_code_data LONGBLOB,
    created_by VARCHAR(100),
    CONSTRAINT chk_status CHECK (status IN ('VALID', 'EXPIRED'))
);

-- Create indexes
CREATE INDEX idx_qr_expired_at ON qr_codes(expired_at);
CREATE INDEX idx_qr_status ON qr_codes(status);
CREATE INDEX idx_qr_created_at ON qr_codes(created_at DESC);
