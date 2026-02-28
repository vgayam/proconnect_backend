-- ProConnect Seed Data
-- Statement separator: ^^ (matches spring.sql.init.separator=^^)
-- All statements use ON CONFLICT DO NOTHING — safe to re-run on any DB state.
-- Loaded via: spring.sql.init.data-locations=classpath:seed.sql

-- ============================================================
-- CATEGORIES
-- ============================================================
INSERT INTO categories (name, emoji, description, sort_order) VALUES
    ('Plumbing',          '🔧', 'Pipe installation, drain cleaning, and fixture repair',          1),
    ('Electrical',        '⚡', 'Wiring, panel upgrades, solar installation, and lighting',       2),
    ('Carpentry',         '🪚', 'Custom furniture, modular kitchens, and wardrobes',              3),
    ('Cleaning',          '🧹', 'Home deep cleaning, office cleaning, and sofa/carpet cleaning',  4),
    ('Photography',       '📷', 'Wedding, portrait, event, and commercial photography',           5),
    ('Interior Design',   '🛋️', 'Residential and commercial interior design and space planning', 6),
    ('Painting',          '🎨', 'Interior/exterior painting, texture finishes, waterproofing',    7),
    ('HVAC',              '❄️', 'AC installation, service, and heating repair',                  8),
    ('Landscaping',       '🌿', 'Lawn maintenance, garden design, and tree service',              9),
    ('Graphic Design',    '✏️', 'Logo, brand, UI/UX design, and prototyping',                   10),
    ('Fitness',           '💪', 'Personal training, yoga, nutrition coaching, and meditation',    11),
    ('Education',         '📚', 'Math, science, language tutoring, and coding for kids',         12),
    ('Technology',        '💻', 'Full-stack dev, mobile apps, cloud, and DevOps',                13),
    ('Pest Control',      '🐛', 'Cockroach, termite, and rodent control',                        14),
    ('Beauty & Wellness', '💆', 'Bridal makeup, mehendi, spa, and haircut',                      15),
    ('Handyman',          '🔨', 'Tile fixing, appliance repair, furniture assembly',             16),
    ('Vehicle',           '🚗', 'Bike service, car repair, and denting & painting',              17)
ON CONFLICT (name) DO NOTHING ^^

-- ============================================================
-- SUBCATEGORIES
-- ============================================================
INSERT INTO subcategories (name, category_id)
SELECT v.name, c.id FROM (VALUES
    ('Residential Plumbing',    'Plumbing'),
    ('Commercial Plumbing',     'Plumbing'),
    ('Pipe Installation',       'Plumbing'),
    ('Drain Cleaning',          'Plumbing'),
    ('Tap & Fixture Repair',    'Plumbing'),
    ('Overhead Tank Cleaning',  'Plumbing'),
    ('Residential Wiring',      'Electrical'),
    ('Commercial Wiring',       'Electrical'),
    ('Panel Upgrades',          'Electrical'),
    ('Lighting Installation',   'Electrical'),
    ('Solar Panel Installation','Electrical'),
    ('Inverter Setup',          'Electrical'),
    ('MCB & Fuse Box Repair',   'Electrical'),
    ('Custom Furniture',        'Carpentry'),
    ('Cabinet Making',          'Carpentry'),
    ('Deck Building',           'Carpentry'),
    ('Framing',                 'Carpentry'),
    ('Modular Kitchen',         'Carpentry'),
    ('Wardrobe Installation',   'Carpentry'),
    ('AC Installation',         'HVAC'),
    ('Heating Repair',          'HVAC'),
    ('Duct Work',               'HVAC'),
    ('AC Service & Cleaning',   'HVAC'),
    ('Interior Painting',       'Painting'),
    ('Exterior Painting',       'Painting'),
    ('Commercial Painting',     'Painting'),
    ('Waterproofing',           'Painting'),
    ('Lawn Maintenance',        'Landscaping'),
    ('Garden Design',           'Landscaping'),
    ('Tree Service',            'Landscaping'),
    ('Portrait Photography',    'Photography'),
    ('Event Photography',       'Photography'),
    ('Commercial Photography',  'Photography'),
    ('Wedding Photography',     'Photography'),
    ('Brand Design',            'Graphic Design'),
    ('Logo Design',             'Graphic Design'),
    ('Web Design',              'Graphic Design'),
    ('UI/UX Design',            'Graphic Design'),
    ('Figma',                   'Graphic Design'),
    ('Prototyping',             'Graphic Design'),
    ('User Research',           'Graphic Design'),
    ('Residential Design',      'Interior Design'),
    ('Commercial Design',       'Interior Design'),
    ('3D Visualization',        'Interior Design'),
    ('Space Planning',          'Interior Design'),
    ('Personal Training',       'Fitness'),
    ('Yoga Instruction',        'Fitness'),
    ('Nutrition Coaching',      'Fitness'),
    ('Meditation Coaching',     'Fitness'),
    ('Math Tutoring',           'Education'),
    ('Science Tutoring',        'Education'),
    ('Language Tutoring',       'Education'),
    ('Coding for Kids',         'Education'),
    ('House Cleaning',          'Cleaning'),
    ('Deep Cleaning',           'Cleaning'),
    ('Office Cleaning',         'Cleaning'),
    ('Sofa & Carpet Cleaning',  'Cleaning'),
    ('Appliance Repair',        'Handyman'),
    ('Furniture Assembly',      'Handyman'),
    ('General Repairs',         'Handyman'),
    ('Tile & Flooring',         'Handyman'),
    ('React Development',       'Technology'),
    ('Spring Boot',             'Technology'),
    ('Node.js',                 'Technology'),
    ('AWS Cloud',               'Technology'),
    ('Flutter Development',     'Technology'),
    ('DevOps & CI/CD',          'Technology'),
    ('Data Engineering',        'Technology'),
    ('Machine Learning',        'Technology'),
    ('Cockroach Treatment',     'Pest Control'),
    ('Termite Treatment',       'Pest Control'),
    ('Rodent Control',          'Pest Control'),
    ('Haircut & Styling',       'Beauty & Wellness'),
    ('Bridal Makeup',           'Beauty & Wellness'),
    ('Spa & Massage',           'Beauty & Wellness'),
    ('Mehendi',                 'Beauty & Wellness'),
    ('Bike Service',            'Vehicle'),
    ('Car Repair',              'Vehicle'),
    ('Denting & Painting',      'Vehicle')
) AS v(name, cat_name)
JOIN categories c ON c.name = v.cat_name
ON CONFLICT (name) DO NOTHING ^^

-- ============================================================
-- PROFESSIONALS  (ON CONFLICT (email) DO NOTHING — safe to re-run)
-- ============================================================
INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Arjun',     'Sharma',        'Arjun Sharma',        'Senior Full-Stack Developer | React + Spring Boot',        'Ex-Flipkart engineer with 8 years building scalable web apps. Specialises in React, Spring Boot, and AWS. Open to freelance projects and consulting.',                                                                                 'Bengaluru','Karnataka','India', true,  true, true, 4.9, 134,  1500,  3000, 'INR', 'arjun.sharma@example.com',    '+919900011111', (SELECT id FROM categories WHERE name='Technology'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Ravi',      'Kumar',         'Ravi Kumar',          'Certified Electrician | Domestic & Commercial Wiring',     'Licensed electrician with 12 years of experience in Bengaluru. Expert in solar panel installation, inverter setup, and MCB repairs. Available on weekends.',                                                                           'Bengaluru','Karnataka','India', false, true, true, 4.7,  98,   300,   700, 'INR', 'ravi.kumar@example.com',      '+919900022222', (SELECT id FROM categories WHERE name='Electrical'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Priya',     'Nair',          'Priya Nair',          'Interior Designer | Modular Kitchens & Living Spaces',     'Award-winning interior designer based in Whitefield. 10+ years transforming homes and offices. Specialist in Scandinavian and Indo-contemporary styles.',                                                                             'Bengaluru','Karnataka','India', true,  true, true, 4.8,  76,  2000,  5000, 'INR', 'priya.nair@example.com',      '+919900033333', (SELECT id FROM categories WHERE name='Interior Design'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Meena',     'Iyer',          'Meena Iyer',          'Certified Yoga & Meditation Instructor | 500-hr RYT',      'Hatha and Vinyasa yoga teacher with 7 years of experience. Conducts group and personal sessions from Indiranagar studio. Online sessions also available.',                                                                           'Bengaluru','Karnataka','India', true,  true, true, 4.9, 210,   400,   800, 'INR', 'meena.iyer@example.com',      '+919900044444', (SELECT id FROM categories WHERE name='Fitness'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Suresh',    'Babu',          'Suresh Babu',         'Expert Plumber | Residential & Commercial',                'Experienced plumber covering Koramangala, HSR Layout, and BTM. Handles pipe leaks, drain cleaning, tap replacement, and overhead tank cleaning.',                                                                                    'Bengaluru','Karnataka','India', false, true, true, 4.6,  87,   200,   500, 'INR', 'suresh.babu@example.com',     '+919900055555', (SELECT id FROM categories WHERE name='Plumbing'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Deepika',   'Rao',           'Deepika Rao',         'Wedding & Portrait Photographer | Candid Specialist',      'Capturing stories one frame at a time. 6 years shooting weddings, pre-weddings, and corporate events across Bengaluru and Mysuru.',                                                                                                   'Bengaluru','Karnataka','India', false, true, true, 4.8, 163,  5000, 20000, 'INR', 'deepika.rao@example.com',     '+919900066666', (SELECT id FROM categories WHERE name='Photography'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Mohan',     'Das',           'Mohan Das',           'AC & HVAC Technician | All Brands Serviced',               'Certified HVAC technician with experience on Daikin, Voltas, LG, and Samsung units. Same-day service available in south and east Bengaluru.',                                                                                        'Bengaluru','Karnataka','India', false, true, true, 4.5, 142,   350,   800, 'INR', 'mohan.das@example.com',       '+919900077777', (SELECT id FROM categories WHERE name='HVAC'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Ananya',    'Krishnan',      'Ananya Krishnan',     'UI/UX & Brand Designer | Figma Expert',                    'Freelance designer with 5 years at product startups. Proficient in Figma, Adobe XD, and Illustrator. Helped 30+ Bengaluru startups launch their visual identity.',                                                                   'Bengaluru','Karnataka','India', true,  true, true, 4.9,  91,   800,  2000, 'INR', 'ananya.k@example.com',        '+919900088888', (SELECT id FROM categories WHERE name='Graphic Design'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Vikram',    'Hegde',         'Vikram Hegde',        'Math & Science Tutor | CBSE / ICSE / PUC',                 'Post-graduate in Mathematics, 9 years tutoring students from Grade 6 to PUC. Excellent track record with board-exam results. Available in Jayanagar area.',                                                                           'Bengaluru','Karnataka','India', true,  true, true, 4.8,  54,   300,   600, 'INR', 'vikram.hegde@example.com',    '+919900099999', (SELECT id FROM categories WHERE name='Education'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Ramesh',    'Gowda',         'Ramesh Gowda',        'Master Carpenter | Modular Kitchens & Wardrobes',          'Skilled carpenter with 15 years of experience. Expert in modular kitchen fabrication, wardrobe installations, and custom furniture. Works across north Bengaluru.',                                                                    'Bengaluru','Karnataka','India', false, true, true, 4.7, 119,   400,   900, 'INR', 'ramesh.gowda@example.com',    '+919900010101', (SELECT id FROM categories WHERE name='Carpentry'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Kavitha',   'Reddy',         'Kavitha Reddy',       'Professional Home & Deep Cleaning Services',               'Running a trusted home cleaning service in Bengaluru since 2015. Team of 8 trained staff. Specialises in deep cleaning, sofa/carpet cleaning, and move-out cleans.',                                                                   'Bengaluru','Karnataka','India', false, true, true, 4.6, 207,   500,  1500, 'INR', 'kavitha.reddy@example.com',   '+919900010202', (SELECT id FROM categories WHERE name='Cleaning'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Nikhil',    'Patel',         'Nikhil Patel',        'Flutter Developer & DevOps Engineer | GCP / AWS',          'Full-stack mobile developer with 6 years building Flutter apps on Play Store and App Store. Also handles CI/CD pipelines and cloud deployments.',                                                                                       'Bengaluru','Karnataka','India', true,  true, true, 4.8,  67,  1200,  2500, 'INR', 'nikhil.patel@example.com',    '+919900010303', (SELECT id FROM categories WHERE name='Technology'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Sanjay',    'Verma',         'Sanjay Verma',        'Pest Control Specialist | Residential & Commercial',       'Certified pest control expert serving Bengaluru since 2012. Provides eco-friendly treatment for cockroaches, termites, rodents, and bed bugs.',                                                                                         'Bengaluru','Karnataka','India', false, true, true, 4.7, 188,   500,  1800, 'INR', 'sanjay.verma@example.com',    '+919900010404', (SELECT id FROM categories WHERE name='Pest Control'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Lakshmi',   'Subramaniam',   'Lakshmi Subramaniam', 'Bridal Makeup Artist & Mehendi Designer',                  'Professional makeup artist and mehendi designer with 9 years of experience. Specialises in South Indian bridal looks and Tamil Nadu-style mehendi.',                                                                                    'Bengaluru','Karnataka','India', false, true, true, 4.9, 312,  3000, 15000, 'INR', 'lakshmi.subra@example.com',   '+919900010505', (SELECT id FROM categories WHERE name='Beauty & Wellness'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Arun',      'Nayak',         'Arun Nayak',          'Exterior & Waterproofing Painter | 10+ Years',             'Skilled painter specialising in exterior weather-shield painting and terrace waterproofing. Trusted by housing societies across Bengaluru.',                                                                                              'Bengaluru','Karnataka','India', false, true, true, 4.6, 143,   250,   600, 'INR', 'arun.nayak@example.com',      '+919900010606', (SELECT id FROM categories WHERE name='Painting'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Pooja',     'Menon',         'Pooja Menon',         'Certified Nutritionist & Diet Coach | Weight Management',  'MSc Nutrition and Dietetics. Helps clients achieve weight loss, sports performance, and disease-specific goals.',                                                                                                                        'Bengaluru','Karnataka','India', true,  true, true, 4.8,  97,   600,  1500, 'INR', 'pooja.menon@example.com',     '+919900010707', (SELECT id FROM categories WHERE name='Fitness'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Girish',    'Shetty',        'Girish Shetty',       'Two-Wheeler Mechanic | All Makes & Models',                'Expert two-wheeler mechanic with 13 years in Bengaluru. Proficient with Honda, Bajaj, Royal Enfield, and Hero bikes. Doorstep service available.',                                                                                     'Bengaluru','Karnataka','India', false, true, true, 4.7, 229,   200,   600, 'INR', 'girish.shetty@example.com',   '+919900010808', (SELECT id FROM categories WHERE name='Vehicle'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Sneha',     'Pillai',        'Sneha Pillai',        'Landscape Designer | Terrace Gardens & Lawns',             'Landscape architect with 8 years creating beautiful outdoor spaces in Bengaluru villas and apartment complexes.',                                                                                                                       'Bengaluru','Karnataka','India', false, true, true, 4.8,  61,  1500,  4000, 'INR', 'sneha.pillai@example.com',    '+919900010909', (SELECT id FROM categories WHERE name='Landscaping'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Kiran',     'Bhat',          'Kiran Bhat',          'Handyman & Home Repair Expert | Tile, Flooring & More',    'All-round handyman serving north and central Bengaluru. Handles tile fixing, false ceiling, furniture assembly, and appliance repair.',                                                                                                'Bengaluru','Karnataka','India', false, true, true, 4.5, 174,   300,   700, 'INR', 'kiran.bhat@example.com',      '+919900011010', (SELECT id FROM categories WHERE name='Handyman'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Divya',     'Kamath',        'Divya Kamath',        'Commercial Photographer | Product & Corporate',            'Freelance commercial photographer with 7 years shooting for e-commerce, food brands, and corporate clients. Professional studio in Koramangala.',                                                                                       'Bengaluru','Karnataka','India', false, true, true, 4.8,  88,  4000, 15000, 'INR', 'divya.kamath@example.com',    '+919900011112', (SELECT id FROM categories WHERE name='Photography'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Naveen',    'Kulkarni',      'Naveen Kulkarni',     'Data Engineer & ML Engineer | Python & GCP',               'Senior data engineer with 7 years at Bengaluru product companies. Expert in data pipelines, BigQuery, and TensorFlow.',                                                                                                                'Bengaluru','Karnataka','India', true,  true, true, 4.9,  73,  1800,  3500, 'INR', 'naveen.kulkarni@example.com', '+919900011212', (SELECT id FROM categories WHERE name='Technology'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Rohit',     'Joshi',         'Rohit Joshi',         'Interior Painter | Texture & Decorative Finishes',         'Specialist in interior painting, texture finishes, and wall murals. Uses low-VOC eco-friendly paints. Completed 200+ projects across Sarjapur, HSR, and Bellandur.',                                                                   'Bengaluru','Karnataka','India', false, true, true, 4.6, 116,   200,   500, 'INR', 'rohit.joshi@example.com',     '+919900011313', (SELECT id FROM categories WHERE name='Painting'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Sangeetha', 'Murthy',        'Sangeetha Murthy',    'Spoken English & Kannada Language Tutor',                  'MA in English Literature with 11 years teaching. Helps students improve spoken English and communication skills. Also offers Kannada for non-native speakers.',                                                                          'Bengaluru','Karnataka','India', true,  true, true, 4.8, 142,   250,   500, 'INR', 'sangeetha.murthy@example.com','+919900011414', (SELECT id FROM categories WHERE name='Education'))
ON CONFLICT (email) DO NOTHING ^^

INSERT INTO professionals (first_name, last_name, display_name, headline, bio, city, state, country, remote, is_verified, is_available, rating, review_count, hourly_rate_min, hourly_rate_max, currency, email, phone, category_id) VALUES
('Harish',    'Naidu',         'Harish Naidu',        'Car Repair & Denting Specialist | Multi-Brand',            'Experienced automobile technician with expertise in denting, painting, and general car repair for Maruti, Hyundai, Toyota, and Honda vehicles.',                                                                                       'Bengaluru','Karnataka','India', false, true, true, 4.6, 198,   500,  2500, 'INR', 'harish.naidu@example.com',    '+919900011515', (SELECT id FROM categories WHERE name='Vehicle'))
ON CONFLICT (email) DO NOTHING ^^

-- ============================================================
-- PROFESSIONAL_SUBCATEGORIES
-- ============================================================
INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('React Development','Spring Boot','AWS Cloud','Node.js')
WHERE p.email = 'arjun.sharma@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Residential Wiring','Solar Panel Installation','Inverter Setup','MCB & Fuse Box Repair')
WHERE p.email = 'ravi.kumar@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Residential Design','Space Planning','3D Visualization')
WHERE p.email = 'priya.nair@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Yoga Instruction','Meditation Coaching','Personal Training')
WHERE p.email = 'meena.iyer@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Drain Cleaning','Pipe Installation','Tap & Fixture Repair','Overhead Tank Cleaning','Residential Plumbing')
WHERE p.email = 'suresh.babu@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Wedding Photography','Portrait Photography','Event Photography')
WHERE p.email = 'deepika.rao@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('AC Installation','AC Service & Cleaning','Heating Repair','Duct Work')
WHERE p.email = 'mohan.das@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('UI/UX Design','Figma','Brand Design','Logo Design','Prototyping','User Research')
WHERE p.email = 'ananya.k@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Math Tutoring','Science Tutoring','Coding for Kids')
WHERE p.email = 'vikram.hegde@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Modular Kitchen','Wardrobe Installation','Custom Furniture','Cabinet Making')
WHERE p.email = 'ramesh.gowda@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('House Cleaning','Deep Cleaning','Sofa & Carpet Cleaning','Office Cleaning')
WHERE p.email = 'kavitha.reddy@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Flutter Development','DevOps & CI/CD','AWS Cloud','React Development')
WHERE p.email = 'nikhil.patel@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Cockroach Treatment','Termite Treatment','Rodent Control')
WHERE p.email = 'sanjay.verma@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Bridal Makeup','Mehendi','Haircut & Styling','Spa & Massage')
WHERE p.email = 'lakshmi.subra@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Exterior Painting','Waterproofing','Interior Painting')
WHERE p.email = 'arun.nayak@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Nutrition Coaching','Personal Training','Meditation Coaching')
WHERE p.email = 'pooja.menon@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Bike Service')
WHERE p.email = 'girish.shetty@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Garden Design','Lawn Maintenance','Tree Service')
WHERE p.email = 'sneha.pillai@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Tile & Flooring','Furniture Assembly','Appliance Repair','General Repairs')
WHERE p.email = 'kiran.bhat@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Commercial Photography','Portrait Photography','Event Photography')
WHERE p.email = 'divya.kamath@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Data Engineering','Machine Learning','AWS Cloud')
WHERE p.email = 'naveen.kulkarni@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Interior Painting','Commercial Painting','Waterproofing')
WHERE p.email = 'rohit.joshi@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Language Tutoring','Math Tutoring')
WHERE p.email = 'sangeetha.murthy@example.com' ON CONFLICT DO NOTHING ^^

INSERT INTO professional_subcategories (professional_id, subcategory_id)
SELECT p.id, s.id FROM professionals p JOIN subcategories s ON s.name IN ('Car Repair','Denting & Painting')
WHERE p.email = 'harish.naidu@example.com' ON CONFLICT DO NOTHING ^^
