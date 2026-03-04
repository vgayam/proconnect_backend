-- =============================================================
-- RESET SCRIPT — drops everything then recreates from schema.sql
-- Safe to run against Render PostgreSQL via the Shell tab.
--
-- Usage:
--   psql $DATABASE_URL -f src/main/resources/schema-reset.sql
--
-- ⚠️  This DESTROYS ALL DATA. Only run when you want a clean slate.
-- =============================================================

DROP TRIGGER IF EXISTS trig_professionals_fts  ON professionals ^^
DROP TRIGGER IF EXISTS trig_professionals_slug ON professionals ^^
DROP FUNCTION IF EXISTS professionals_search_vector_update() ^^
DROP FUNCTION IF EXISTS generate_professional_slug() ^^

DROP TABLE IF EXISTS contact_views              CASCADE ^^
DROP TABLE IF EXISTS contact_messages           CASCADE ^^
DROP TABLE IF EXISTS social_links               CASCADE ^^
DROP TABLE IF EXISTS services                   CASCADE ^^
DROP TABLE IF EXISTS professional_service_areas CASCADE ^^
DROP TABLE IF EXISTS professional_subcategories CASCADE ^^
DROP TABLE IF EXISTS subcategories              CASCADE ^^
DROP TABLE IF EXISTS reviews                    CASCADE ^^
DROP TABLE IF EXISTS booking_inquiries          CASCADE ^^
DROP TABLE IF EXISTS email_otps                 CASCADE ^^
DROP TABLE IF EXISTS professionals              CASCADE ^^
DROP TABLE IF EXISTS categories                 CASCADE ^^

-- =============================================================
-- RECREATE — run schema.sql to rebuild all tables, indexes,
-- triggers and constraints from scratch.
-- =============================================================
\i src/main/resources/schema.sql
