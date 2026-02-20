-- ProConnect Database Schema

-- Create professionals table
CREATE TABLE IF NOT EXISTS professionals (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(200),
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
    category VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create skills table
CREATE TABLE IF NOT EXISTS skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create professional_skills junction table
CREATE TABLE IF NOT EXISTS professional_skills (
    professional_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (professional_id, skill_id),
    FOREIGN KEY (professional_id) REFERENCES professionals(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_id) REFERENCES skills(id) ON DELETE CASCADE
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

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_professionals_city ON professionals(city);
CREATE INDEX IF NOT EXISTS idx_professionals_available ON professionals(is_available);
CREATE INDEX IF NOT EXISTS idx_skills_category ON skills(category);
CREATE INDEX IF NOT EXISTS idx_services_professional ON services(professional_id);
CREATE INDEX IF NOT EXISTS idx_contact_messages_professional ON contact_messages(professional_id);
CREATE INDEX IF NOT EXISTS idx_contact_messages_status ON contact_messages(status);

-- Insert sample skills
INSERT INTO skills (name, category) VALUES
    ('Residential Plumbing', 'Plumbing'),
    ('Commercial Plumbing', 'Plumbing'),
    ('Pipe Installation', 'Plumbing'),
    ('Drain Cleaning', 'Plumbing'),
    ('Residential Wiring', 'Electrical'),
    ('Commercial Wiring', 'Electrical'),
    ('Panel Upgrades', 'Electrical'),
    ('Lighting Installation', 'Electrical'),
    ('Custom Furniture', 'Carpentry'),
    ('Cabinet Making', 'Carpentry'),
    ('Deck Building', 'Carpentry'),
    ('Framing', 'Carpentry'),
    ('AC Installation', 'HVAC'),
    ('Heating Repair', 'HVAC'),
    ('Duct Work', 'HVAC'),
    ('Interior Painting', 'Painting'),
    ('Exterior Painting', 'Painting'),
    ('Commercial Painting', 'Painting'),
    ('Lawn Maintenance', 'Landscaping'),
    ('Garden Design', 'Landscaping'),
    ('Tree Service', 'Landscaping'),
    ('Portrait Photography', 'Photography'),
    ('Event Photography', 'Photography'),
    ('Commercial Photography', 'Photography'),
    ('Wedding Photography', 'Photography'),
    ('Brand Design', 'Graphic Design'),
    ('Logo Design', 'Graphic Design'),
    ('Web Design', 'Graphic Design'),
    ('Residential Design', 'Interior Design'),
    ('Commercial Design', 'Interior Design'),
    ('Personal Training', 'Fitness'),
    ('Math Tutoring', 'Education'),
    ('Science Tutoring', 'Education')
ON CONFLICT (name) DO NOTHING;

-- Migration: Add contact info and category columns (safe to run on existing DB)
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS email VARCHAR(100);
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS whatsapp VARCHAR(30);
ALTER TABLE professionals ADD COLUMN IF NOT EXISTS category VARCHAR(100);
