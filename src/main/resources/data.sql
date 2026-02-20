-- ============================================================
-- ProConnect Sample Data
-- ============================================================

-- Clear existing data (order matters due to FK constraints)
DELETE FROM social_links;
DELETE FROM services;
DELETE FROM professional_skills;
DELETE FROM contact_messages;
DELETE FROM professionals;
DELETE FROM skills;

-- Reset sequences
ALTER SEQUENCE professionals_id_seq RESTART WITH 1;
ALTER SEQUENCE skills_id_seq RESTART WITH 1;
ALTER SEQUENCE services_id_seq RESTART WITH 1;
ALTER SEQUENCE social_links_id_seq RESTART WITH 1;

-- ============================================================
-- SKILLS
-- ============================================================
INSERT INTO skills (name, category) VALUES
  ('Residential Plumbing',   'Plumbing'),
  ('Commercial Plumbing',    'Plumbing'),
  ('Pipe Installation',      'Plumbing'),
  ('Drain Cleaning',         'Plumbing'),
  ('Residential Wiring',     'Electrical'),
  ('Commercial Wiring',      'Electrical'),
  ('Panel Upgrades',         'Electrical'),
  ('Lighting Installation',  'Electrical'),
  ('Custom Furniture',       'Carpentry'),
  ('Cabinet Making',         'Carpentry'),
  ('Deck Building',          'Carpentry'),
  ('Framing',                'Carpentry'),
  ('AC Installation',        'HVAC'),
  ('Heating Repair',         'HVAC'),
  ('Duct Work',              'HVAC'),
  ('Interior Painting',      'Painting'),
  ('Exterior Painting',      'Painting'),
  ('Commercial Painting',    'Painting'),
  ('Lawn Maintenance',       'Landscaping'),
  ('Garden Design',          'Landscaping'),
  ('Tree Service',           'Landscaping'),
  ('Portrait Photography',   'Photography'),
  ('Event Photography',      'Photography'),
  ('Commercial Photography', 'Photography'),
  ('Wedding Photography',    'Photography'),
  ('Brand Design',           'Graphic Design'),
  ('Logo Design',            'Graphic Design'),
  ('Web Design',             'Graphic Design'),
  ('Residential Design',     'Interior Design'),
  ('Commercial Design',      'Interior Design'),
  ('Personal Training',      'Fitness'),
  ('Yoga Instruction',       'Fitness'),
  ('Nutrition Coaching',     'Fitness'),
  ('Math Tutoring',          'Education'),
  ('Science Tutoring',       'Education'),
  ('Language Tutoring',      'Education'),
  ('House Cleaning',         'Cleaning'),
  ('Deep Cleaning',          'Cleaning'),
  ('Office Cleaning',        'Cleaning'),
  ('Appliance Repair',       'Handyman'),
  ('Furniture Assembly',     'Handyman'),
  ('General Repairs',        'Handyman');

-- ============================================================
-- PROFESSIONALS  (12 diverse profiles)
-- ============================================================
INSERT INTO professionals
  (first_name, last_name, display_name, headline, bio,
   city, state, country, remote, is_verified, is_available,
   rating, review_count, hourly_rate_min, hourly_rate_max, currency,
   email, phone, whatsapp, category)
VALUES
-- 1. Plumber – New York
('James', 'Carter', 'James Carter',
 'Master Plumber with 15 Years Experience',
 'Licensed master plumber specializing in residential and commercial plumbing. I handle everything from leaky faucets to full bathroom remodels. Available for emergency calls 24/7.',
 'New York', 'NY', 'USA', false, true, true,
 4.90, 142, 80.00, 150.00, 'USD',
 'james.carter@email.com', '+1-212-555-0101', '+1-212-555-0101', 'Plumbing'),

-- 2. Electrician – Los Angeles
('Sarah', 'Mitchell', 'Sarah Mitchell',
 'Certified Electrician | Residential & Commercial',
 'Licensed electrician with 10 years of experience. Expert in panel upgrades, smart home wiring, and EV charger installation. Serving the greater Los Angeles area.',
 'Los Angeles', 'CA', 'USA', false, true, true,
 4.80, 98, 90.00, 160.00, 'USD',
 'sarah.mitchell@email.com', '+1-310-555-0202', '+1-310-555-0202', 'Electrical'),

-- 3. Photographer – Chicago (Remote-friendly)
('Michael', 'Torres', 'Michael Torres',
 'Award-Winning Wedding & Portrait Photographer',
 'Capturing life''s most precious moments for over 12 years. Specializing in weddings, corporate events, and editorial portraits. Available nationwide for destination weddings.',
 'Chicago', 'IL', 'USA', true, true, true,
 4.95, 231, 200.00, 500.00, 'USD',
 'michael.torres@photos.com', '+1-312-555-0303', '+1-312-555-0303', 'Photography'),

-- 4. Carpenter – Austin
('Linda', 'Hayes', 'Linda Hayes',
 'Custom Furniture & Cabinetry Specialist',
 'Passionate woodworker with a fine eye for detail. I design and build bespoke furniture, kitchen cabinets, and outdoor decks that stand the test of time. Free in-home consultations.',
 'Austin', 'TX', 'USA', false, false, true,
 4.70, 67, 75.00, 130.00, 'USD',
 'linda.hayes@woodcraft.com', '+1-512-555-0404', null, 'Carpentry'),

-- 5. Graphic Designer – Remote (San Francisco based)
('David', 'Kim', 'David Kim',
 'Full-Stack Brand & Web Designer',
 'Creative director and designer with experience at top-tier agencies. I help startups and established brands build compelling visual identities, websites, and marketing materials.',
 'San Francisco', 'CA', 'USA', true, true, true,
 4.85, 189, 100.00, 200.00, 'USD',
 'david.kim@designstudio.com', '+1-415-555-0505', '+1-415-555-0505', 'Graphic Design'),

-- 6. Personal Trainer – Miami
('Priya', 'Nair', 'Priya Nair',
 'NASM-Certified Personal Trainer & Nutrition Coach',
 'Helping clients transform their lives through evidence-based fitness and nutrition programs. Online and in-person sessions available. Specialties: weight loss, strength training, and prenatal fitness.',
 'Miami', 'FL', 'USA', true, true, true,
 4.92, 305, 60.00, 100.00, 'USD',
 'priya.nair@fitlife.com', '+1-305-555-0606', '+1-305-555-0606', 'Fitness'),

-- 7. Interior Designer – Remote (New York based)
('Omar', 'Hassan', 'Omar Hassan',
 'Interior Designer | Modern & Minimalist Spaces',
 'Award-winning interior designer with over 8 years of experience transforming residential and commercial spaces. I work remotely with clients worldwide and offer 3D visualizations before any work begins.',
 'New York', 'NY', 'USA', true, true, true,
 4.75, 54, 120.00, 250.00, 'USD',
 'omar.hassan@interiorco.com', '+1-212-555-0707', null, 'Interior Design'),

-- 8. HVAC Technician – Houston
('Karen', 'Brooks', 'Karen Brooks',
 'HVAC Technician | AC Installation & Repair',
 'EPA-certified HVAC technician with 8 years of field experience. I service all major brands, install new systems, and provide seasonal maintenance contracts for homes and businesses.',
 'Houston', 'TX', 'USA', false, false, true,
 4.65, 112, 70.00, 120.00, 'USD',
 'karen.brooks@hvacpro.com', '+1-713-555-0808', '+1-713-555-0808', 'HVAC'),

-- 9. Tutor – Remote (Boston)
('Ethan', 'Park', 'Ethan Park',
 'PhD Tutor | Math, Physics & SAT Prep',
 'Physics PhD candidate at MIT with 6 years of tutoring experience. I break down complex concepts into simple, understandable lessons. Specializing in SAT/ACT prep, calculus, and AP sciences. 100% online sessions.',
 'Boston', 'MA', 'USA', true, true, true,
 4.98, 412, 50.00, 80.00, 'USD',
 'ethan.park@tutorpro.com', '+1-617-555-0909', null, 'Education'),

-- 10. House Cleaner – Seattle
('Maria', 'Gonzalez', 'Maria Gonzalez',
 'Professional House & Office Cleaning Services',
 'Reliable, eco-friendly cleaning services for homes and offices in the Seattle area. Fully insured and background checked. Flexible scheduling including weekends and recurring plans.',
 'Seattle', 'WA', 'USA', false, true, true,
 4.88, 276, 40.00, 70.00, 'USD',
 'maria.g@cleanpro.com', '+1-206-555-1010', '+1-206-555-1010', 'Cleaning'),

-- 11. Handyman – Denver
('Robert', 'Patel', 'Robert Patel',
 'Handyman | Repairs, Assembly & Home Maintenance',
 'Experienced handyman serving the Denver metro area. No job too small — furniture assembly, appliance repair, drywall patching, painting touch-ups, and general home maintenance. Same-day availability often possible.',
 'Denver', 'CO', 'USA', false, false, true,
 4.60, 88, 45.00, 85.00, 'USD',
 'robert.patel@handymandenver.com', '+1-720-555-1111', '+1-720-555-1111', 'Handyman'),

-- 12. Landscaper – Portland
('Aisha', 'Johnson', 'Aisha Johnson',
 'Landscape Designer & Garden Care Specialist',
 'Passionate about creating beautiful outdoor spaces. I offer garden design, lawn maintenance, seasonal planting, and tree service. Sustainable and native plant options available.',
 'Portland', 'OR', 'USA', false, true, false,
 4.72, 93, 55.00, 95.00, 'USD',
 'aisha.j@greenscapes.com', '+1-503-555-1212', null, 'Landscaping');

-- ============================================================
-- PROFESSIONAL  ↔  SKILLS  links
-- ============================================================
INSERT INTO professional_skills (professional_id, skill_id)
SELECT p.id, s.id FROM professionals p, skills s WHERE
  (p.last_name = 'Carter'   AND s.name IN ('Residential Plumbing','Commercial Plumbing','Pipe Installation','Drain Cleaning'))
  OR (p.last_name = 'Mitchell' AND s.name IN ('Residential Wiring','Commercial Wiring','Panel Upgrades','Lighting Installation'))
  OR (p.last_name = 'Torres'   AND s.name IN ('Portrait Photography','Event Photography','Commercial Photography','Wedding Photography'))
  OR (p.last_name = 'Hayes'    AND s.name IN ('Custom Furniture','Cabinet Making','Deck Building','Framing'))
  OR (p.last_name = 'Kim'      AND s.name IN ('Brand Design','Logo Design','Web Design'))
  OR (p.last_name = 'Nair'     AND s.name IN ('Personal Training','Yoga Instruction','Nutrition Coaching'))
  OR (p.last_name = 'Hassan'   AND s.name IN ('Residential Design','Commercial Design'))
  OR (p.last_name = 'Brooks'   AND s.name IN ('AC Installation','Heating Repair','Duct Work'))
  OR (p.last_name = 'Park'     AND s.name IN ('Math Tutoring','Science Tutoring'))
  OR (p.last_name = 'Gonzalez' AND s.name IN ('House Cleaning','Deep Cleaning','Office Cleaning'))
  OR (p.last_name = 'Patel'    AND s.name IN ('Appliance Repair','Furniture Assembly','General Repairs'))
  OR (p.last_name = 'Johnson'  AND s.name IN ('Lawn Maintenance','Garden Design','Tree Service'));

-- ============================================================
-- SERVICES
-- ============================================================
INSERT INTO services (professional_id, title, description, price_min, price_max, currency, price_unit, duration)
SELECT p.id, v.title, v.description, v.price_min, v.price_max, v.currency, v.price_unit, v.duration
FROM professionals p
JOIN (VALUES
  -- James Carter – Plumber
  ('Carter', 'Drain Cleaning',           'Full drain inspection and professional cleaning',           100.00,  200.00, 'USD', 'per job',     '1-2 hours'),
  ('Carter', 'Pipe Installation',        'New pipe installation or full replacement',                 300.00,  800.00, 'USD', 'per job',     '2-5 hours'),
  ('Carter', 'Bathroom Plumbing Remodel','Complete plumbing remodel for bathroom renovations',       1200.00, 3000.00, 'USD', 'per project', '1-3 days'),
  -- Sarah Mitchell – Electrician
  ('Mitchell', 'Panel Upgrade',          'Upgrade electrical panel to 200-amp service',              500.00, 1500.00, 'USD', 'per job',     '4-6 hours'),
  ('Mitchell', 'EV Charger Installation','Level 2 EV charger installation at home or office',        400.00,  900.00, 'USD', 'per job',     '2-4 hours'),
  ('Mitchell', 'Smart Home Wiring',      'Whole-home smart lighting and outlet wiring',             1000.00, 4000.00, 'USD', 'per project', '1-3 days'),
  -- Michael Torres – Photographer
  ('Torres', 'Wedding Photography',      'Full-day wedding coverage with edited gallery',           1800.00, 4000.00, 'USD', 'per day',     'Full day'),
  ('Torres', 'Portrait Session',         '1-hour portrait session, 30 edited digital images',        250.00,  500.00, 'USD', 'per session', '1-2 hours'),
  ('Torres', 'Corporate Event Coverage', 'Half or full-day corporate event photography',             600.00, 1500.00, 'USD', 'per day',     '4-8 hours'),
  -- Linda Hayes – Carpenter
  ('Hayes', 'Custom Bookshelf',          'Handcrafted built-in or freestanding bookshelves',         400.00, 1200.00, 'USD', 'per project', '2-4 days'),
  ('Hayes', 'Kitchen Cabinet Refacing',  'Sand, prime and refinish existing kitchen cabinets',       800.00, 2500.00, 'USD', 'per project', '3-5 days'),
  -- David Kim – Designer
  ('Kim', 'Brand Identity Package',     'Logo, color palette, typography and brand guidelines',      800.00, 2500.00, 'USD', 'per project', '1-2 weeks'),
  ('Kim', 'Website Design (5 pages)',   'Custom web design mockups in Figma with handoff',           600.00, 1800.00, 'USD', 'per project', '1-3 weeks'),
  -- Priya Nair – Trainer
  ('Nair', 'Online Personal Training',  'Weekly 1-on-1 video sessions + custom workout plan',         60.00,  100.00, 'USD', 'per session', '1 hour'),
  ('Nair', '12-Week Transformation',    'Nutrition plan + 3 training sessions per week for 12 weeks', 900.00, 1500.00, 'USD', 'per package', '12 weeks'),
  -- Omar Hassan – Interior Designer
  ('Hassan', 'Room Design Consultation','Virtual consultation with mood board and layout plan',       150.00,  400.00, 'USD', 'per session', '1-2 hours'),
  ('Hassan', 'Full Room Redesign',      'Complete redesign: 3D render, sourcing, and styling guide', 800.00, 2000.00, 'USD', 'per room',    '1-2 weeks'),
  -- Karen Brooks – HVAC
  ('Brooks', 'AC Service & Tune-Up',    'Annual air conditioning inspection and maintenance',          80.00,  150.00, 'USD', 'per visit',   '1-2 hours'),
  ('Brooks', 'New AC Installation',     'Supply and install new central AC system',                 2500.00, 6000.00, 'USD', 'per project', '1-2 days'),
  -- Ethan Park – Tutor
  ('Park', 'SAT Math Prep',             '1-on-1 online SAT Math tutoring sessions',                   50.00,   75.00, 'USD', 'per hour',    '1 hour'),
  ('Park', 'AP Calculus Coaching',      'AP Calculus AB/BC exam preparation sessions',               55.00,   80.00, 'USD', 'per hour',    '1 hour'),
  -- Maria Gonzalez – Cleaner
  ('Gonzalez', 'Standard Home Cleaning','Regular cleaning: vacuum, mop, kitchen, and bathrooms',     100.00,  180.00, 'USD', 'per visit',   '2-3 hours'),
  ('Gonzalez', 'Deep Cleaning',         'Thorough deep clean including appliances and baseboards',   180.00,  350.00, 'USD', 'per visit',   '4-6 hours'),
  -- Robert Patel – Handyman
  ('Patel', 'Furniture Assembly',       'IKEA and flat-pack furniture assembly',                      45.00,   90.00, 'USD', 'per hour',    '1-3 hours'),
  ('Patel', 'General Home Repairs',     'Minor repairs: drywall, doors, fixtures, and more',          50.00,   85.00, 'USD', 'per hour',    '1-4 hours'),
  -- Aisha Johnson – Landscaper
  ('Johnson', 'Lawn Maintenance Plan',  'Bi-weekly mow, edge, and blow service',                      60.00,   90.00, 'USD', 'per visit',   '1-2 hours'),
  ('Johnson', 'Garden Design',          'Custom garden plan with seasonal plant selection',           300.00,  900.00, 'USD', 'per project', '3-7 days')
) AS v(last_name, title, description, price_min, price_max, currency, price_unit, duration)
ON p.last_name = v.last_name;

-- ============================================================
-- SOCIAL LINKS
-- ============================================================
INSERT INTO social_links (professional_id, platform, url, label)
SELECT p.id, v.platform, v.url, v.label
FROM professionals p
JOIN (VALUES
  ('Carter',   'linkedin',  'https://linkedin.com/in/jamescarter-plumbing',   'LinkedIn'),
  ('Mitchell', 'linkedin',  'https://linkedin.com/in/sarahmitchell-elec',     'LinkedIn'),
  ('Mitchell', 'instagram', 'https://instagram.com/sarah_electrician',        'Instagram'),
  ('Torres',   'instagram', 'https://instagram.com/michaeltorresphotos',      'Instagram'),
  ('Torres',   'website',   'https://michaeltorresphotography.com',           'Portfolio'),
  ('Kim',      'website',   'https://davidkimdesign.com',                     'Portfolio'),
  ('Kim',      'linkedin',  'https://linkedin.com/in/davidkimdesigner',       'LinkedIn'),
  ('Kim',      'instagram', 'https://instagram.com/davidkimdesign',           'Instagram'),
  ('Nair',     'instagram', 'https://instagram.com/priya_fitcoach',           'Instagram'),
  ('Nair',     'website',   'https://priyafitlife.com',                       'Website'),
  ('Hassan',   'website',   'https://omarhassan.design',                      'Portfolio'),
  ('Hassan',   'instagram', 'https://instagram.com/omarhassan_interiors',     'Instagram'),
  ('Park',     'linkedin',  'https://linkedin.com/in/ethanpark-tutor',        'LinkedIn'),
  ('Gonzalez', 'facebook',  'https://facebook.com/mariagcleanpro',            'Facebook'),
  ('Johnson',  'instagram', 'https://instagram.com/aisha_greenscapes',        'Instagram')
) AS v(last_name, platform, url, label)
ON p.last_name = v.last_name;
