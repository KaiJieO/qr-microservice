-- Phase 1: Remove qrCodeData blob column
-- This column stored binary QR code images
-- Now QR images are generated on-demand and cached

ALTER TABLE qr_codes DROP COLUMN IF EXISTS qr_code_data;
