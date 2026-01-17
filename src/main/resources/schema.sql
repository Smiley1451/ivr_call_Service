-- ============================================
-- LABOUR CONNECT DATABASE SCHEMA
-- College Project Demo Version
-- ============================================

-- Drop existing tables if any
DROP TABLE IF EXISTS call_logs CASCADE;
DROP TABLE IF EXISTS work CASCADE;
DROP TABLE IF EXISTS labour CASCADE;

-- ============================================
-- 1. LABOUR TABLE (Job Seekers / Workers)
-- ============================================
CREATE TABLE labour (
    labour_id SERIAL PRIMARY KEY,
    phone_no VARCHAR(15) NOT NULL,  -- No UNIQUE constraint (allows re-registration)
    name VARCHAR(100),
    experience INTEGER,
    work_expertise VARCHAR(200),
    location VARCHAR(100),
    preferred_wage INTEGER,  -- Minimum expected wage per day
    bio TEXT,
    language_preference VARCHAR(10) DEFAULT 'en',  -- 'en', 'hi', 'kn'
    registration_date TIMESTAMP DEFAULT NOW(),
    last_updated TIMESTAMP DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_experience CHECK (experience >= 0 AND experience <= 50),
    CONSTRAINT chk_wage CHECK (preferred_wage >= 0),
    CONSTRAINT chk_language CHECK (language_preference IN ('en', 'hi', 'kn'))
);

-- Indexes for faster searching
CREATE INDEX idx_labour_expertise ON labour(work_expertise);
CREATE INDEX idx_labour_phone ON labour(phone_no);
CREATE INDEX idx_labour_location ON labour(location);
CREATE INDEX idx_labour_reg_date ON labour(registration_date DESC);

-- ============================================
-- 2. WORK TABLE (Job Postings by Employers)
-- ============================================
CREATE TABLE work (
    work_id SERIAL PRIMARY KEY,
    phone_no VARCHAR(15) NOT NULL,  -- Employer's contact
    type_of_work VARCHAR(150) NOT NULL,
    location VARCHAR(100) NOT NULL,
    wages_offered INTEGER,  -- Per day wage
    organisation_name VARCHAR(150),
    description TEXT,
    language_preference VARCHAR(10) DEFAULT 'en',
    posted_date TIMESTAMP DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_wages CHECK (wages_offered >= 0),
    CONSTRAINT chk_work_language CHECK (language_preference IN ('en', 'hi', 'kn'))
);

-- Indexes for faster searching
CREATE INDEX idx_work_type ON work(type_of_work);
CREATE INDEX idx_work_location ON work(location);
CREATE INDEX idx_work_phone ON work(phone_no);
CREATE INDEX idx_work_posted_date ON work(posted_date DESC);

-- ============================================
-- 3. CALL LOGS TABLE (Track IVR interactions)
-- ============================================
CREATE TABLE call_logs (
    call_id SERIAL PRIMARY KEY,
    phone_no VARCHAR(15),
    call_purpose VARCHAR(50),  -- 'job_seeker' or 'employer'
    language_selected VARCHAR(10),
    call_duration INTEGER,  -- in seconds
    call_timestamp TIMESTAMP DEFAULT NOW(),
    status VARCHAR(20) DEFAULT 'completed',  -- 'completed', 'dropped', 'failed'

    -- Constraints
    CONSTRAINT chk_purpose CHECK (call_purpose IN ('job_seeker', 'employer')),
    CONSTRAINT chk_status CHECK (status IN ('completed', 'dropped', 'failed'))
);

-- Index for analytics
CREATE INDEX idx_call_logs_timestamp ON call_logs(call_timestamp DESC);
CREATE INDEX idx_call_logs_phone ON call_logs(phone_no);

-- ============================================
-- SAMPLE DATA FOR DEMO
-- ============================================

-- Insert Sample Workers
INSERT INTO labour (phone_no, name, experience, work_expertise, location, preferred_wage, bio, language_preference) VALUES
('+919876543210', 'Raju Kumar', 5, 'Electrician', 'Bangalore', 800, 'Expert in house wiring and industrial electrical work', 'en'),
('+919876543211', 'Suresh Reddy', 3, 'Plumber', 'Bangalore', 600, 'Residential and commercial plumbing specialist', 'kn'),
('+919876543212', 'Vijay Singh', 8, 'Carpenter', 'Mysore', 1000, 'Furniture making and woodwork expert', 'hi'),
('+919876543213', 'Kumar Das', 2, 'Helper', 'Bangalore', 400, 'General helper for construction and loading work', 'kn'),
('+919876543214', 'Manoj Sharma', 6, 'Mason', 'Hubli', 900, 'Bricklaying and plastering work', 'hi'),
('+919876543215', 'Rajesh Nair', 4, 'Painter', 'Bangalore', 700, 'Interior and exterior painting', 'en'),
('+919876543216', 'Ashok Patil', 7, 'Welder', 'Mangalore', 950, 'Arc welding and metal fabrication', 'kn'),
('+919876543217', 'Dinesh Gowda', 3, 'Electrician', 'Bangalore', 650, 'Electrical repairs and maintenance', 'kn'),
('+919876543218', 'Prakash Joshi', 5, 'Plumber', 'Mysore', 750, 'Pipeline installation and repairs', 'hi'),
('+919876543219', 'Ramesh Yadav', 10, 'Carpenter', 'Bangalore', 1200, 'Expert furniture designer and carpenter', 'en');

-- Insert Sample Job Postings
INSERT INTO work (phone_no, type_of_work, location, wages_offered, organisation_name, description, language_preference) VALUES
('+919123456780', 'Electrician', 'Bangalore', 850, 'ABC Industries', 'Need electrician for factory wiring work', 'en'),
('+919123456781', 'Plumber', 'Bangalore', 650, 'Residents Welfare Association', 'Plumbing work in apartment complex', 'kn'),
('+919123456782', 'Helper', 'Mysore', 450, 'Construction Site', 'Loading and unloading materials', 'hi'),
('+919123456783', 'Carpenter', 'Bangalore', 1100, 'Furniture Shop', 'Custom furniture making required', 'en'),
('+919123456784', 'Mason', 'Hubli', 950, 'Building Contractor', 'House construction masonry work', 'kn'),
('+919123456785', 'Painter', 'Bangalore', 700, 'Home Owner', 'House painting - interior and exterior', 'en'),
('+919123456786', 'Electrician', 'Bangalore', 800, 'IT Office', 'Office electrical maintenance work', 'en'),
('+919123456787', 'Welder', 'Mangalore', 1000, 'Metal Works Ltd', 'Metal gate and grille fabrication', 'kn');

-- Insert Sample Call Logs
INSERT INTO call_logs (phone_no, call_purpose, language_selected, call_duration, call_timestamp, status) VALUES
('+919876543210', 'job_seeker', 'en', 180, NOW() - INTERVAL '2 days', 'completed'),
('+919123456780', 'employer', 'en', 150, NOW() - INTERVAL '1 day', 'completed'),
('+919876543211', 'job_seeker', 'kn', 200, NOW() - INTERVAL '3 hours', 'completed'),
('+919999999999', 'job_seeker', 'hi', 45, NOW() - INTERVAL '1 hour', 'dropped');

-- ============================================
-- USEFUL QUERIES FOR DEMO
-- ============================================

-- View all workers with their details
-- SELECT * FROM labour ORDER BY registration_date DESC;

-- View all job postings
-- SELECT * FROM work ORDER BY posted_date DESC;

-- View recent call logs
-- SELECT * FROM call_logs ORDER BY call_timestamp DESC LIMIT 10;

-- Count workers by expertise
-- SELECT work_expertise, COUNT(*) as worker_count
-- FROM labour
-- GROUP BY work_expertise
-- ORDER BY worker_count DESC;

-- Count jobs by type
-- SELECT type_of_work, COUNT(*) as job_count
-- FROM work
-- GROUP BY type_of_work
-- ORDER BY job_count DESC;

-- Find workers by skill and location
-- SELECT * FROM labour
-- WHERE work_expertise ILIKE '%electrician%'
-- AND location ILIKE '%bangalore%'
-- ORDER BY experience DESC;

-- ============================================
-- END OF SCHEMA
-- ============================================