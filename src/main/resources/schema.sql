-- ProConnect Database Schema — DDL only
-- Statement separator: ^^ (allows PL/pgSQL $$ blocks without splitting on ;)
-- Spring Boot: spring.sql.init.schema-locations=classpath:schema.sql
--              spring.sql.init.separator=^^
-- Strategy: CREATE TABLE IF NOT EXISTS — safe to re-run, never drops existing data.
-- Seed data lives in seed.sql (spring.sql.init.data-locations=classpath:seed.sql)

-- ============================================================
-- EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pg_trgm ^^
CREATE EXTENSION IF NOT EXISTS unaccent ^^

-- ============================================================
-- CATEGORIES
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    emoji       VARCHAR(10),
    description VARCHAR(500),
    sort_order  INTEGER      NOT NULL DEFAULT 0,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ^^

-- ============================================================
-- PROFESSIONALS
-- ============================================================
CREATE TABLE IF NOT EXISTS professionals (
    id              BIGSERIAL    PRIMARY KEY,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    display_name    VARCHAR(200),
    slug            VARCHAR(300) UNIQUE,
    headline        VARCHAR(255) NOT NULL,
    bio             TEXT,
    avatar_url      VARCHAR(500),
    cover_image_url VARCHAR(500),
    city            VARCHAR(100) NOT NULL,
    state           VARCHAR(100) NOT NULL,
    country         VARCHAR(100) NOT NULL,
    remote          BOOLEAN      NOT NULL DEFAULT FALSE,
    is_verified     BOOLEAN               DEFAULT FALSE,
    is_available    BOOLEAN               DEFAULT TRUE,
    rating          DECIMAL(3,2),
    review_count    INTEGER               DEFAULT 0,
    hourly_rate_min DECIMAL(10,2),
    hourly_rate_max DECIMAL(10,2),
    currency        VARCHAR(3)            DEFAULT 'USD',
    email           VARCHAR(100) UNIQUE,
    phone           VARCHAR(30)  UNIQUE,
    whatsapp        VARCHAR(30),
    category_id     BIGINT,
    search_vector   tsvector,
    latitude        DECIMAL(9,6),
    longitude       DECIMAL(9,6),
    created_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
) ^^

-- ============================================================
-- SUBCATEGORIES
-- ============================================================
CREATE TABLE IF NOT EXISTS subcategories (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    category_id BIGINT       NOT NULL,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- PROFESSIONAL_SUBCATEGORIES (junction)
-- ============================================================
CREATE TABLE IF NOT EXISTS professional_subcategories (
    professional_id BIGINT NOT NULL,
    subcategory_id  BIGINT NOT NULL,
    PRIMARY KEY (professional_id, subcategory_id),
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    FOREIGN KEY (subcategory_id)  REFERENCES subcategories(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- SERVICES
-- ============================================================
CREATE TABLE IF NOT EXISTS services (
    id              BIGSERIAL    PRIMARY KEY,
    professional_id BIGINT       NOT NULL,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    price_min       DECIMAL(10,2),
    price_max       DECIMAL(10,2),
    currency        VARCHAR(3)   DEFAULT 'USD',
    price_unit      VARCHAR(50),
    duration        VARCHAR(50),
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- PROFESSIONAL_SERVICE_AREAS
-- ============================================================
CREATE TABLE IF NOT EXISTS professional_service_areas (
    id              BIGSERIAL    PRIMARY KEY,
    professional_id BIGINT       NOT NULL,
    area_name       VARCHAR(200) NOT NULL,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- SOCIAL_LINKS
-- ============================================================
CREATE TABLE IF NOT EXISTS social_links (
    id              BIGSERIAL    PRIMARY KEY,
    professional_id BIGINT       NOT NULL,
    platform        VARCHAR(50)  NOT NULL,
    url             VARCHAR(500) NOT NULL,
    label           VARCHAR(100),
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- CONTACT_MESSAGES
-- ============================================================
CREATE TABLE IF NOT EXISTS contact_messages (
    id              BIGSERIAL    PRIMARY KEY,
    professional_id BIGINT       NOT NULL,
    sender_name     VARCHAR(200) NOT NULL,
    sender_email    VARCHAR(200) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    message         TEXT         NOT NULL,
    service_id      BIGINT,
    status          VARCHAR(20)  DEFAULT 'NEW',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- EMAIL_OTPS
-- ============================================================
CREATE TABLE IF NOT EXISTS email_otps (
    id         BIGSERIAL    PRIMARY KEY,
    email      VARCHAR(200) NOT NULL,
    otp_code   VARCHAR(6)   NOT NULL,
    verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMP    NOT NULL,
    created_at TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ^^

-- ============================================================
-- BOOKING_INQUIRIES
-- ============================================================
CREATE TABLE IF NOT EXISTS booking_inquiries (
    id               BIGSERIAL    PRIMARY KEY,
    professional_id  BIGINT       NOT NULL,
    customer_name    VARCHAR(200) NOT NULL,
    customer_email   VARCHAR(200),
    customer_phone   VARCHAR(30),
    preferred_date   VARCHAR(20),
    preferred_time   VARCHAR(10),
    note             VARCHAR(1000),
    review_token     VARCHAR(64)  NOT NULL UNIQUE,
    token_used       BOOLEAN      NOT NULL DEFAULT FALSE,
    token_expires_at TIMESTAMP    NOT NULL,
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- REVIEWS
-- ============================================================
CREATE TABLE IF NOT EXISTS reviews (
    id              BIGSERIAL PRIMARY KEY,
    professional_id BIGINT    NOT NULL,
    inquiry_id      BIGINT    NOT NULL UNIQUE,
    customer_name   VARCHAR(200) NOT NULL,
    rating          SMALLINT  NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment         TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    FOREIGN KEY (inquiry_id)      REFERENCES booking_inquiries(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- CONTACT_VIEWS  (rate-limit tracking for contact reveals)
-- ============================================================
CREATE TABLE IF NOT EXISTS contact_views (
    id              BIGSERIAL    PRIMARY KEY,
    professional_id BIGINT       NOT NULL,
    viewer_email    TEXT,
    viewer_ip       TEXT,
    viewed_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- INDEXES
-- ============================================================
CREATE INDEX IF NOT EXISTS idx_professionals_city          ON professionals(city) ^^
CREATE INDEX IF NOT EXISTS idx_professionals_country       ON professionals(country) ^^
CREATE INDEX IF NOT EXISTS idx_professionals_category      ON professionals(category_id) ^^
CREATE INDEX IF NOT EXISTS idx_professionals_available     ON professionals(is_available) ^^
CREATE INDEX IF NOT EXISTS idx_professionals_remote        ON professionals(remote) ^^
CREATE INDEX IF NOT EXISTS idx_professionals_slug          ON professionals(slug) ^^
CREATE INDEX IF NOT EXISTS idx_professionals_fts           ON professionals USING GIN(search_vector) ^^
CREATE INDEX IF NOT EXISTS idx_professionals_city_trgm     ON professionals USING GIN(city gin_trgm_ops) ^^
CREATE INDEX IF NOT EXISTS idx_professionals_name_trgm     ON professionals USING GIN((first_name || ' ' || last_name) gin_trgm_ops) ^^
CREATE INDEX IF NOT EXISTS idx_subcategories_category      ON subcategories(category_id) ^^
CREATE INDEX IF NOT EXISTS idx_subcategories_name_trgm     ON subcategories USING GIN(name gin_trgm_ops) ^^
CREATE INDEX IF NOT EXISTS idx_services_professional       ON services(professional_id) ^^
CREATE INDEX IF NOT EXISTS idx_service_areas_professional  ON professional_service_areas(professional_id) ^^
CREATE INDEX IF NOT EXISTS idx_service_areas_name_trgm     ON professional_service_areas USING GIN(area_name gin_trgm_ops) ^^
CREATE INDEX IF NOT EXISTS idx_contact_messages_pro        ON contact_messages(professional_id) ^^
CREATE INDEX IF NOT EXISTS idx_contact_messages_status     ON contact_messages(status) ^^
CREATE INDEX IF NOT EXISTS idx_booking_inquiries_pro       ON booking_inquiries(professional_id) ^^
CREATE INDEX IF NOT EXISTS idx_booking_inquiries_token     ON booking_inquiries(review_token) ^^
CREATE INDEX IF NOT EXISTS idx_reviews_professional        ON reviews(professional_id) ^^
CREATE INDEX IF NOT EXISTS idx_email_otps_email            ON email_otps(email) ^^
CREATE INDEX IF NOT EXISTS idx_contact_views_email_date    ON contact_views (viewer_email, viewed_at) ^^
CREATE INDEX IF NOT EXISTS idx_contact_views_ip_date       ON contact_views (viewer_ip,    viewed_at) ^^

-- ============================================================
-- FTS TRIGGER  (CREATE OR REPLACE — always idempotent)
-- ============================================================
CREATE OR REPLACE FUNCTION professionals_search_vector_update() RETURNS trigger AS $$
DECLARE
    v_category_name TEXT;
BEGIN
    SELECT name INTO v_category_name FROM categories WHERE id = NEW.category_id;
    NEW.search_vector :=
        setweight(to_tsvector('english', unaccent(coalesce(NEW.first_name,'') || ' ' || coalesce(NEW.last_name,''))), 'A') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.headline,''))),                                        'A') ||
        setweight(to_tsvector('english', unaccent(coalesce(v_category_name,''))),                                     'B') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.city,'') || ' ' || coalesce(NEW.state,'') || ' ' || coalesce(NEW.country,''))), 'B') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.bio,''))),                                             'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql ^^

DROP TRIGGER IF EXISTS trig_professionals_fts ON professionals ^^
CREATE TRIGGER trig_professionals_fts
    BEFORE INSERT OR UPDATE ON professionals
    FOR EACH ROW EXECUTE FUNCTION professionals_search_vector_update() ^^

-- ============================================================
-- SLUG TRIGGER  (CREATE OR REPLACE — always idempotent)
-- ============================================================
CREATE OR REPLACE FUNCTION generate_professional_slug() RETURNS trigger AS $$
DECLARE
    base_slug       TEXT;
    final_slug      TEXT;
    counter         INT := 0;
    v_category_name TEXT;
BEGIN
    IF NEW.slug IS NULL OR NEW.slug = '' THEN
        SELECT name INTO v_category_name FROM categories WHERE id = NEW.category_id;
        base_slug := regexp_replace(
            lower(unaccent(
                coalesce(NEW.first_name,'') || '-' || coalesce(NEW.last_name,'') ||
                '-' || coalesce(v_category_name,'') || '-' || coalesce(NEW.city,'')
            )),
            '[^a-z0-9]+', '-', 'g'
        );
        base_slug  := trim(both '-' from base_slug);
        final_slug := base_slug;
        WHILE EXISTS (
            SELECT 1 FROM professionals
            WHERE slug = final_slug AND id IS DISTINCT FROM NEW.id
        ) LOOP
            counter    := counter + 1;
            final_slug := base_slug || '-' || counter;
        END LOOP;
        NEW.slug := final_slug;
    END IF;
    RETURN NEW;
END
$$ LANGUAGE plpgsql ^^

DROP TRIGGER IF EXISTS trig_professionals_slug ON professionals ^^
CREATE TRIGGER trig_professionals_slug
    BEFORE INSERT OR UPDATE ON professionals
    FOR EACH ROW EXECUTE FUNCTION generate_professional_slug() ^^
