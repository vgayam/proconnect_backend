-- ProConnect Database Schema

-- ============================================================
-- EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pg_trgm;    -- fuzzy/similarity matching
CREATE EXTENSION IF NOT EXISTS unaccent;   -- accent-insensitive search

-- ============================================================
-- PROFESSIONALS
-- ============================================================
CREATE TABLE IF NOT EXISTS professionals (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200),
    slug VARCHAR(300) UNIQUE,              -- e.g. james-carter-plumber-new-york
    headline VARCHAR(255) NOT NULL,
    bio TEXT,
    avatar_url VARCHAR(500),
    cover_image_url VARCHAR(500),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    remote BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    is_available BOOLEAN DEFAULT TRUE,
    rating DECIMAL(3, 2),
    review_count INTEGER DEFAULT 0,
    hourly_rate_min DECIMAL(10, 2),
    hourly_rate_max DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    email VARCHAR(100),
    phone VARCHAR(30),
    whatsapp VARCHAR(30),
    category VARCHAR(100),                 -- primary category (e.g. "Plumbing")
    search_vector tsvector,                -- weighted FTS vector (auto-updated by trigger)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- SUBCATEGORIES  (formerly "skills" — category + subcategory model)
-- e.g. category="Plumbing", name="Drain Cleaning"
-- ============================================================
CREATE TABLE IF NOT EXISTS subcategories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,     -- subcategory name  e.g. "Drain Cleaning"
    category VARCHAR(100) NOT NULL,        -- parent category   e.g. "Plumbing"
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- PROFESSIONAL ↔ SUBCATEGORY junction
-- ============================================================
CREATE TABLE IF NOT EXISTS professional_subcategories (
    professional_id BIGINT NOT NULL,
    subcategory_id  BIGINT NOT NULL,
    PRIMARY KEY (professional_id, subcategory_id),
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    FOREIGN KEY (subcategory_id)  REFERENCES subcategories(id) ON DELETE CASCADE
);

-- Keep old name as alias so existing data/queries still work during migration
CREATE TABLE IF NOT EXISTS professional_skills (
    professional_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (professional_id, skill_id),
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES subcategories(id) ON DELETE CASCADE
);

-- Create services table
CREATE TABLE IF NOT EXISTS services (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    price_min DECIMAL(10, 2),
    price_max DECIMAL(10, 2),
    currency VARCHAR(3) DEFAULT 'USD',
    price_unit VARCHAR(50),
    duration VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
);

-- Create social_links table
CREATE TABLE IF NOT EXISTS social_links (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    platform VARCHAR(50) NOT NULL,
    url VARCHAR(500) NOT NULL,
    label VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
);

-- Create contact_messages table
CREATE TABLE IF NOT EXISTS contact_messages (
    id BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    sender_name VARCHAR(200) NOT NULL,
    sender_email VARCHAR(200) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    service_id BIGINT,
    status VARCHAR(20) DEFAULT 'NEW',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
);

-- ============================================================
-- INDEXES
-- ============================================================

-- Standard indexes
CREATE INDEX IF NOT EXISTS idx_professionals_city      ON professionals(city);
CREATE INDEX IF NOT EXISTS idx_professionals_country   ON professionals(country);
CREATE INDEX IF NOT EXISTS idx_professionals_category  ON professionals(category);
CREATE INDEX IF NOT EXISTS idx_professionals_available ON professionals(is_available);
CREATE INDEX IF NOT EXISTS idx_professionals_remote    ON professionals(remote);
CREATE INDEX IF NOT EXISTS idx_professionals_slug      ON professionals(slug);
CREATE INDEX IF NOT EXISTS idx_subcategories_category  ON subcategories(category);
CREATE INDEX IF NOT EXISTS idx_services_professional   ON services(professional_id);
CREATE INDEX IF NOT EXISTS idx_contact_messages_professional ON contact_messages(professional_id);
CREATE INDEX IF NOT EXISTS idx_contact_messages_status ON contact_messages(status);

-- GIN index for full-text search vector (fast @@ queries)
CREATE INDEX IF NOT EXISTS idx_professionals_fts
    ON professionals USING GIN(search_vector);

-- GIN trigram indexes for fuzzy/ILIKE matching on city & name
CREATE INDEX IF NOT EXISTS idx_professionals_city_trgm
    ON professionals USING GIN(city gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_professionals_name_trgm
    ON professionals USING GIN((first_name || ' ' || last_name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_subcategories_name_trgm
    ON subcategories USING GIN(name gin_trgm_ops);

-- ============================================================
-- FTS TRIGGER — keeps search_vector in sync on every upsert
-- Weights: A=headline/name, B=category, C=bio/subcategory names
-- ============================================================
CREATE OR REPLACE FUNCTION professionals_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', unaccent(coalesce(NEW.first_name,'')  || ' ' || coalesce(NEW.last_name,''))), 'A') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.headline,''))),  'A') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.category,''))),  'B') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.city,'')     || ' ' || coalesce(NEW.state,'') || ' ' || coalesce(NEW.country,''))), 'B') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.bio,''))),       'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trig_professionals_fts ON professionals;
CREATE TRIGGER trig_professionals_fts
    BEFORE INSERT OR UPDATE ON professionals
    FOR EACH ROW EXECUTE FUNCTION professionals_search_vector_update();

-- ============================================================
-- SLUG GENERATOR — auto-creates slug on insert if not provided
-- ============================================================
CREATE OR REPLACE FUNCTION generate_professional_slug() RETURNS trigger AS $$
DECLARE
    base_slug TEXT;
    final_slug TEXT;
    counter   INT := 0;
BEGIN
    IF NEW.slug IS NULL OR NEW.slug = '' THEN
        base_slug := lower(
            regexp_replace(
                unaccent(coalesce(NEW.first_name,'') || '-' || coalesce(NEW.last_name,'') || '-' || coalesce(NEW.category,'') || '-' || coalesce(NEW.city,'')),
                '[^a-z0-9]+', '-', 'g'
            )
        );
        base_slug := trim(both '-' from base_slug);
        final_slug := base_slug;
        WHILE EXISTS (SELECT 1 FROM professionals WHERE slug = final_slug AND id != coalesce(NEW.id, -1)) LOOP
            counter := counter + 1;
            final_slug := base_slug || '-' || counter;
        END LOOP;
        NEW.slug := final_slug;
    END IF;
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trig_professionals_slug ON professionals;
CREATE TRIGGER trig_professionals_slug
    BEFORE INSERT OR UPDATE ON professionals
    FOR EACH ROW EXECUTE FUNCTION generate_professional_slug();

-- ============================================================
-- MIGRATION GUARDS — safe to run on existing DB
-- ============================================================
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS email       VARCHAR(100);
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS phone       VARCHAR(30);
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS whatsapp    VARCHAR(30);
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS category    VARCHAR(100);
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS slug        VARCHAR(300);
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Migrate data: copy skills → subcategories if subcategories table is empty
INSERT INTO subcategories (name, category, created_at)
SELECT name, category, created_at FROM skills
WHERE NOT EXISTS (SELECT 1 FROM subcategories LIMIT 1)
ON CONFLICT (name) DO NOTHING;

-- Backfill search_vector for existing rows
UPDATE professionals SET updated_at = updated_at WHERE search_vector IS NULL;

-- Backfill slugs for existing rows  
UPDATE professionals SET slug = NULL WHERE slug IS NOT NULL AND slug = '';

-- ============================================================
-- SEED SUBCATEGORIES
-- ============================================================
INSERT INTO subcategories (name, category) VALUES
    ('Residential Plumbing', 'Plumbing'), ('Commercial Plumbing', 'Plumbing'),
    ('Pipe Installation', 'Plumbing'),    ('Drain Cleaning', 'Plumbing'),
    ('Residential Wiring', 'Electrical'), ('Commercial Wiring', 'Electrical'),
    ('Panel Upgrades', 'Electrical'),     ('Lighting Installation', 'Electrical'),
    ('Custom Furniture', 'Carpentry'),    ('Cabinet Making', 'Carpentry'),
    ('Deck Building', 'Carpentry'),       ('Framing', 'Carpentry'),
    ('AC Installation', 'HVAC'),          ('Heating Repair', 'HVAC'),
    ('Duct Work', 'HVAC'),
    ('Interior Painting', 'Painting'),    ('Exterior Painting', 'Painting'),
    ('Commercial Painting', 'Painting'),
    ('Lawn Maintenance', 'Landscaping'),  ('Garden Design', 'Landscaping'),
    ('Tree Service', 'Landscaping'),
    ('Portrait Photography', 'Photography'), ('Event Photography', 'Photography'),
    ('Commercial Photography', 'Photography'), ('Wedding Photography', 'Photography'),
    ('Brand Design', 'Graphic Design'),   ('Logo Design', 'Graphic Design'),
    ('Web Design', 'Graphic Design'),     ('UI/UX Design', 'Graphic Design'),
    ('Figma', 'Graphic Design'),          ('Prototyping', 'Graphic Design'),
    ('Residential Design', 'Interior Design'), ('Commercial Design', 'Interior Design'),
    ('3D Visualization', 'Interior Design'), ('Space Planning', 'Interior Design'),
    ('Personal Training', 'Fitness'),     ('Yoga Instruction', 'Fitness'),
    ('Nutrition Coaching', 'Fitness'),    ('Meditation Coaching', 'Fitness'),
    ('Math Tutoring', 'Education'),       ('Science Tutoring', 'Education'),
    ('Language Tutoring', 'Education'),
    ('House Cleaning', 'Cleaning'),       ('Deep Cleaning', 'Cleaning'),
    ('Office Cleaning', 'Cleaning'),
    ('Appliance Repair', 'Handyman'),     ('Furniture Assembly', 'Handyman'),
    ('General Repairs', 'Handyman'),
    ('React Development', 'Technology'),  ('Spring Boot', 'Technology'),
    ('Node.js', 'Technology'),            ('AWS Cloud', 'Technology'),
    ('Solar Panel Installation', 'Electrical'), ('Inverter Setup', 'Electrical'),
    ('User Research', 'Graphic Design')
ON CONFLICT (name) DO NOTHING;
