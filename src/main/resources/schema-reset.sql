-- =============================================================
-- RESET SCRIPT — for local development ONLY
-- Drops everything and re-runs the full schema from scratch.
--
-- Usage (local dev):
--   psql -U proconnect -d proconnect -f src/main/resources/schema-reset.sql
--
-- DO NOT run this against production. It will destroy all data.
-- =============================================================

DROP TRIGGER IF EXISTS trig_professionals_fts  ON professionals;
DROP TRIGGER IF EXISTS trig_professionals_slug ON professionals;
DROP FUNCTION IF EXISTS professionals_search_vector_update();
DROP FUNCTION IF EXISTS generate_professional_slug();

DROP TABLE IF EXISTS contact_messages           CASCADE;
DROP TABLE IF EXISTS social_links               CASCADE;
DROP TABLE IF EXISTS services                   CASCADE;
DROP TABLE IF EXISTS professional_service_areas CASCADE;
DROP TABLE IF EXISTS professional_subcategories CASCADE;
DROP TABLE IF EXISTS professional_skills        CASCADE;
DROP TABLE IF EXISTS subcategories              CASCADE;
DROP TABLE IF EXISTS skills                     CASCADE;
DROP TABLE IF EXISTS reviews                    CASCADE;
DROP TABLE IF EXISTS booking_inquiries          CASCADE;
DROP TABLE IF EXISTS email_otps                 CASCADE;
DROP TABLE IF EXISTS professionals              CASCADE;
DROP TABLE IF EXISTS categories                 CASCADE;

-- Now run the main schema to rebuild everything
\i schema.sql
