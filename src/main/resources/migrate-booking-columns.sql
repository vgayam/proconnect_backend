-- Migration: add booking slot columns to booking_inquiries
-- Run once against production DB:
--   psql $DATABASE_URL -f migrate-booking-columns.sql
-- Safe to re-run (IF NOT EXISTS).

ALTER TABLE booking_inquiries ADD COLUMN IF NOT EXISTS preferred_date VARCHAR(20);
ALTER TABLE booking_inquiries ADD COLUMN IF NOT EXISTS preferred_time VARCHAR(10);
ALTER TABLE booking_inquiries ADD COLUMN IF NOT EXISTS note           VARCHAR(1000);
