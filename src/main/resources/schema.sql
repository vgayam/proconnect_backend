-- ProConnect Database Schema
-- Statement separator: ^^ (not ; — allows PL/pgSQL $$ blocks without splitting)
-- Spring Boot property: spring.sql.init.separator=^^

-- ============================================================
-- EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pg_trgm ^^
CREATE EXTENSION IF NOT EXISTS unaccent ^^

-- ============================================================
-- DROP EVERYTHING (reverse dependency order)
-- ============================================================
DROP TRIGGER IF EXISTS trig_professionals_fts  ON professionals ^^
DROP TRIGGER IF EXISTS trig_professionals_slug ON professionals ^^
DROP FUNCTION IF EXISTS professionals_search_vector_update() ^^
DROP FUNCTION IF EXISTS generate_professional_slug() ^^

DROP TABLE IF EXISTS contact_messages           CASCADE ^^
DROP TABLE IF EXISTS social_links               CASCADE ^^
DROP TABLE IF EXISTS services                   CASCADE ^^
DROP TABLE IF EXISTS professional_subcategories CASCADE ^^
DROP TABLE IF EXISTS professional_skills        CASCADE ^^
DROP TABLE IF EXISTS subcategories              CASCADE ^^
DROP TABLE IF EXISTS skills                     CASCADE ^^
DROP TABLE IF EXISTS professionals              CASCADE ^^

-- ============================================================
-- PROFESSIONALS
-- ============================================================
CREATE TABLE professionals (
    id              BIGSERIAL PRIMARY KEY,
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
    email           VARCHAR(100),
    phone           VARCHAR(30),
    whatsapp        VARCHAR(30),
    category        VARCHAR(100),
    search_vector   tsvector,
    created_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP             DEFAULT CURRENT_TIMESTAMP
) ^^

-- ============================================================
-- SUBCATEGORIES
-- ============================================================
CREATE TABLE subcategories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    category   VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ^^

-- ============================================================
-- JUNCTION TABLES
-- ============================================================
CREATE TABLE professional_subcategories (
    professional_id BIGINT NOT NULL,
    subcategory_id  BIGINT NOT NULL,
    PRIMARY KEY (professional_id, subcategory_id),
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    FOREIGN KEY (subcategory_id)  REFERENCES subcategories(id) ON DELETE CASCADE
) ^^

CREATE TABLE professional_skills (
    professional_id BIGINT NOT NULL,
    skill_id        BIGINT NOT NULL,
    PRIMARY KEY (professional_id, skill_id),
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id)        REFERENCES subcategories(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- SERVICES
-- ============================================================
CREATE TABLE services (
    id              BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL,
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
-- SOCIAL LINKS
-- ============================================================
CREATE TABLE social_links (
    id              BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL,
    platform        VARCHAR(50)  NOT NULL,
    url             VARCHAR(500) NOT NULL,
    label           VARCHAR(100),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE
) ^^

-- ============================================================
-- CONTACT MESSAGES
-- ============================================================
CREATE TABLE contact_messages (
    id              BIGSERIAL PRIMARY KEY,
    professional_id BIGINT NOT NULL,
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
-- INDEXES
-- ============================================================
CREATE INDEX idx_professionals_city      ON professionals(city) ^^
CREATE INDEX idx_professionals_country   ON professionals(country) ^^
CREATE INDEX idx_professionals_category  ON professionals(category) ^^
CREATE INDEX idx_professionals_available ON professionals(is_available) ^^
CREATE INDEX idx_professionals_remote    ON professionals(remote) ^^
CREATE INDEX idx_professionals_slug      ON professionals(slug) ^^
CREATE INDEX idx_subcategories_category  ON subcategories(category) ^^
CREATE INDEX idx_services_professional   ON services(professional_id) ^^
CREATE INDEX idx_contact_messages_professional ON contact_messages(professional_id) ^^
CREATE INDEX idx_contact_messages_status ON contact_messages(status) ^^

CREATE INDEX idx_professionals_fts
    ON professionals USING GIN(search_vector) ^^

CREATE INDEX idx_professionals_city_trgm
    ON professionals USING GIN(city gin_trgm_ops) ^^
CREATE INDEX idx_professionals_name_trgm
    ON professionals USING GIN((first_name || ' ' || last_name) gin_trgm_ops) ^^
CREATE INDEX idx_subcategories_name_trgm
    ON subcategories USING GIN(name gin_trgm_ops) ^^

-- ============================================================
-- FTS TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION professionals_search_vector_update() RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('english', unaccent(coalesce(NEW.first_name,'') || ' ' || coalesce(NEW.last_name,''))), 'A') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.headline,''))),                                        'A') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.category,''))),                                        'B') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.city,'') || ' ' || coalesce(NEW.state,'') || ' ' || coalesce(NEW.country,''))), 'B') ||
        setweight(to_tsvector('english', unaccent(coalesce(NEW.bio,''))),                                             'C');
    RETURN NEW;
END
$$ LANGUAGE plpgsql ^^

CREATE TRIGGER trig_professionals_fts
    BEFORE INSERT OR UPDATE ON professionals
    FOR EACH ROW EXECUTE FUNCTION professionals_search_vector_update() ^^

-- ============================================================
-- SLUG TRIGGER
-- ============================================================
CREATE OR REPLACE FUNCTION generate_professional_slug() RETURNS trigger AS $$
DECLARE
    base_slug  TEXT;
    final_slug TEXT;
    counter    INT := 0;
BEGIN
    IF NEW.slug IS NULL OR NEW.slug = '' THEN
        base_slug := lower(
            regexp_replace(
                unaccent(
                    coalesce(NEW.first_name,'') || '-' || coalesce(NEW.last_name,'') ||
                    '-' || coalesce(NEW.category,'') || '-' || coalesce(NEW.city,'')
                ),
                '[^a-z0-9]+', '-', 'g'
            )
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

CREATE TRIGGER trig_professionals_slug
    BEFORE INSERT OR UPDATE ON professionals
    FOR EACH ROW EXECUTE FUNCTION generate_professional_slug() ^^

-- ============================================================
-- SEED SUBCATEGORIES
-- ============================================================
INSERT INTO subcategories (name, category) VALUES
    ('Residential Plumbing', 'Plumbing'), ('Commercial Plumbing', 'Plumbing'),
    ('Pipe Installation', 'Plumbing'),    ('Drain Cleaning', 'Plumbing'),
    ('Tap & Fixture Repair', 'Plumbing'), ('Overhead Tank Cleaning', 'Plumbing'),
    ('Residential Wiring', 'Electrical'), ('Commercial Wiring', 'Electrical'),
    ('Panel Upgrades', 'Electrical'),     ('Lighting Installation', 'Electrical'),
    ('Solar Panel Installation', 'Electrical'), ('Inverter Setup', 'Electrical'),
    ('MCB & Fuse Box Repair', 'Electrical'),
    ('Custom Furniture', 'Carpentry'),    ('Cabinet Making', 'Carpentry'),
    ('Deck Building', 'Carpentry'),       ('Framing', 'Carpentry'),
    ('Modular Kitchen', 'Carpentry'),     ('Wardrobe Installation', 'Carpentry'),
    ('AC Installation', 'HVAC'),          ('Heating Repair', 'HVAC'),
    ('Duct Work', 'HVAC'),                ('AC Service & Cleaning', 'HVAC'),
    ('Interior Painting', 'Painting'),    ('Exterior Painting', 'Painting'),
    ('Commercial Painting', 'Painting'),  ('Waterproofing', 'Painting'),
    ('Lawn Maintenance', 'Landscaping'),  ('Garden Design', 'Landscaping'),
    ('Tree Service', 'Landscaping'),
    ('Portrait Photography', 'Photography'), ('Event Photography', 'Photography'),
    ('Commercial Photography', 'Photography'), ('Wedding Photography', 'Photography'),
    ('Brand Design', 'Graphic Design'),   ('Logo Design', 'Graphic Design'),
    ('Web Design', 'Graphic Design'),     ('UI/UX Design', 'Graphic Design'),
    ('Figma', 'Graphic Design'),          ('Prototyping', 'Graphic Design'),
    ('User Research', 'Graphic Design'),
    ('Residential Design', 'Interior Design'), ('Commercial Design', 'Interior Design'),
    ('3D Visualization', 'Interior Design'),   ('Space Planning', 'Interior Design'),
    ('Personal Training', 'Fitness'),     ('Yoga Instruction', 'Fitness'),
    ('Nutrition Coaching', 'Fitness'),    ('Meditation Coaching', 'Fitness'),
    ('Math Tutoring', 'Education'),       ('Science Tutoring', 'Education'),
    ('Language Tutoring', 'Education'),   ('Coding for Kids', 'Education'),
    ('House Cleaning', 'Cleaning'),       ('Deep Cleaning', 'Cleaning'),
    ('Office Cleaning', 'Cleaning'),      ('Sofa & Carpet Cleaning', 'Cleaning'),
    ('Appliance Repair', 'Handyman'),     ('Furniture Assembly', 'Handyman'),
    ('General Repairs', 'Handyman'),      ('Tile & Flooring', 'Handyman'),
    ('React Development', 'Technology'),  ('Spring Boot', 'Technology'),
    ('Node.js', 'Technology'),            ('AWS Cloud', 'Technology'),
    ('Flutter Development', 'Technology'),('DevOps & CI/CD', 'Technology'),
    ('Data Engineering', 'Technology'),   ('Machine Learning', 'Technology'),
    ('Cockroach Treatment', 'Pest Control'), ('Termite Treatment', 'Pest Control'),
    ('Rodent Control', 'Pest Control'),
    ('Haircut & Styling', 'Beauty & Wellness'), ('Bridal Makeup', 'Beauty & Wellness'),
    ('Spa & Massage', 'Beauty & Wellness'),     ('Mehendi', 'Beauty & Wellness'),
    ('Bike Service', 'Vehicle'), ('Car Repair', 'Vehicle'), ('Denting & Painting', 'Vehicle')
ON CONFLICT (name) DO NOTHING ^^

-- ============================================================
-- SEED PROFESSIONALS — Bengaluru, India
-- ============================================================
INSERT INTO professionals
    (first_name, last_name, display_name, headline, bio,
     city, state, country, remote, is_verified, is_available,
     rating, review_count, hourly_rate_min, hourly_rate_max, currency,
     email, phone, category)
VALUES
('Arjun',   'Sharma',   'Arjun Sharma',
 'Senior Full-Stack Developer | React + Spring Boot',
 'Ex-Flipkart engineer with 8 years building scalable web apps. Specialises in React, Spring Boot, and AWS. Open to freelance projects and consulting.',
 'Bengaluru', 'Karnataka', 'India', true, true, true,
 4.9, 134, 1500, 3000, 'INR', 'arjun.sharma@example.com', '+919900011111', 'Technology'),
('Ravi',    'Kumar',    'Ravi Kumar',
 'Certified Electrician | Domestic & Commercial Wiring',
 'Licensed electrician with 12 years of experience in Bengaluru. Expert in solar panel installation, inverter setup, and MCB repairs. Available on weekends.',
 'Bengaluru', 'Karnataka', 'India', false, true, true,
 4.7, 98, 300, 700, 'INR', 'ravi.kumar@example.com', '+919900022222', 'Electrical'),
('Priya',   'Nair',     'Priya Nair',
 'Interior Designer | Modular Kitchens & Living Spaces',
 'Award-winning interior designer based in Whitefield. 10+ years transforming homes and offices. Specialist in Scandinavian and Indo-contemporary styles.',
 'Bengaluru', 'Karnataka', 'India', true, true, true,
 4.8, 76, 2000, 5000, 'INR', 'priya.nair@example.com', '+919900033333', 'Interior Design'),
('Meena',   'Iyer',     'Meena Iyer',
 'Certified Yoga & Meditation Instructor | 500-hr RYT',
 'Hatha and Vinyasa yoga teacher with 7 years of experience. Conducts group and personal sessions from Indiranagar studio. Online sessions also available.',
 'Bengaluru', 'Karnataka', 'India', true, true, true,
 4.9, 210, 400, 800, 'INR', 'meena.iyer@example.com', '+919900044444', 'Fitness'),
('Suresh',  'Babu',     'Suresh Babu',
 'Expert Plumber | Residential & Commercial',
 'Experienced plumber covering Koramangala, HSR Layout, and BTM. Handles pipe leaks, drain cleaning, tap replacement, and overhead tank cleaning.',
 'Bengaluru', 'Karnataka', 'India', false, true, true,
 4.6, 87, 200, 500, 'INR', 'suresh.babu@example.com', '+919900055555', 'Plumbing'),
('Deepika', 'Rao',      'Deepika Rao',
 'Wedding & Portrait Photographer | Candid Specialist',
 'Capturing stories one frame at a time. 6 years shooting weddings, pre-weddings, and corporate events across Bengaluru and Mysuru.',
 'Bengaluru', 'Karnataka', 'India', false, true, true,
 4.8, 163, 5000, 20000, 'INR', 'deepika.rao@example.com', '+919900066666', 'Photography'),
('Mohan',   'Das',      'Mohan Das',
 'AC & HVAC Technician | All Brands Serviced',
 'Certified HVAC technician with experience on Daikin, Voltas, LG, and Samsung units. Same-day service available in south and east Bengaluru.',
 'Bengaluru', 'Karnataka', 'India', false, true, true,
 4.5, 142, 350, 800, 'INR', 'mohan.das@example.com', '+919900077777', 'HVAC'),
('Ananya',  'Krishnan', 'Ananya Krishnan',
 'UI/UX & Brand Designer | Figma Expert',
 'Freelance designer with 5 years at product startups. Proficient in Figma, Adobe XD, and Illustrator. Helped 30+ Bengaluru startups launch their visual identity.',
 'Bengaluru', 'Karnataka', 'India', true, true, true,
 4.9, 91, 800, 2000, 'INR', 'ananya.k@example.com', '+919900088888', 'Graphic Design'),
('Vikram',  'Hegde',    'Vikram Hegde',
 'Math & Science Tutor | CBSE / ICSE / PUC',
 'Post-graduate in Mathematics, 9 years tutoring students from Grade 6 to PUC. Excellent track record with board-exam results. Available in Jayanagar area.',
 'Bengaluru', 'Karnataka', 'India', true, true, true,
 4.8, 54, 300, 600, 'INR', 'vikram.hegde@example.com', '+919900099999', 'Education'),
('Ramesh',  'Gowda',    'Ramesh Gowda',
 'Master Carpenter | Modular Kitchens & Wardrobes',
 'Skilled carpenter with 15 years of experience. Expert in modular kitchen fabrication, wardrobe installations, and custom furniture. Works across north Bengaluru.',
 'Bengaluru', 'Karnataka', 'India', false, true, true,
 4.7, 119, 400, 900, 'INR', 'ramesh.gowda@example.com', '+919900010101', 'Carpentry'),
('Kavitha', 'Reddy',    'Kavitha Reddy',
 'Professional Home & Deep Cleaning Services',
 'Running a trusted home cleaning service in Bengaluru since 2015. Team of 8 trained staff. Specialises in deep cleaning, sofa/carpet cleaning, and move-out cleans.',
 'Bengaluru', 'Karnataka', 'India', false, true, true,
 4.6, 207, 500, 1500, 'INR', 'kavitha.reddy@example.com', '+919900010202', 'Cleaning'),
('Nikhil',  'Patel',    'Nikhil Patel',
 'Flutter Developer & DevOps Engineer | GCP / AWS',
 'Full-stack mobile developer with 6 years building Flutter apps on Play Store and App Store. Also handles CI/CD pipelines and cloud deployments.',
 'Bengaluru', 'Karnataka', 'India', true, true, true,
 4.8, 67, 1200, 2500, 'INR', 'nikhil.patel@example.com', '+919900010303', 'Technology') ^^

-- ============================================================
-- LINK SUBCATEGORIES TO PROFESSIONALS
-- ============================================================
INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'arjun.sharma@example.com'
  AND s.name IN ('React Development','Spring Boot','AWS Cloud','Node.js') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'ravi.kumar@example.com'
  AND s.name IN ('Residential Wiring','Solar Panel Installation','Inverter Setup','MCB & Fuse Box Repair') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'priya.nair@example.com'
  AND s.name IN ('Residential Design','Space Planning','3D Visualization') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'meena.iyer@example.com'
  AND s.name IN ('Yoga Instruction','Meditation Coaching','Personal Training') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'suresh.babu@example.com'
  AND s.name IN ('Drain Cleaning','Pipe Installation','Tap & Fixture Repair','Overhead Tank Cleaning','Residential Plumbing') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'deepika.rao@example.com'
  AND s.name IN ('Wedding Photography','Portrait Photography','Event Photography') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'mohan.das@example.com'
  AND s.name IN ('AC Installation','AC Service & Cleaning','Heating Repair','Duct Work') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'ananya.k@example.com'
  AND s.name IN ('UI/UX Design','Figma','Brand Design','Logo Design','Prototyping','User Research') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'vikram.hegde@example.com'
  AND s.name IN ('Math Tutoring','Science Tutoring','Coding for Kids') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'ramesh.gowda@example.com'
  AND s.name IN ('Modular Kitchen','Wardrobe Installation','Custom Furniture','Cabinet Making') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'kavitha.reddy@example.com'
  AND s.name IN ('House Cleaning','Deep Cleaning','Sofa & Carpet Cleaning','Office Cleaning') ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p, subcategories s
WHERE p.email = 'nikhil.patel@example.com'
  AND s.name IN ('Flutter Development','DevOps & CI/CD','AWS Cloud','React Development') ^^
