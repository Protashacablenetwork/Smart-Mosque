-- Masjid & Society Management Database Schema for InfinityFree / MySQL

CREATE TABLE IF NOT EXISTS families (
    id VARCHAR(64) PRIMARY KEY,
    family_code VARCHAR(50) NOT NULL,
    family_name VARCHAR(150) NOT NULL,
    address VARCHAR(255) DEFAULT '',
    phone VARCHAR(50) DEFAULT '',
    email VARCHAR(150) DEFAULT '',
    monthly_due_amount DECIMAL(10,2) DEFAULT 300.00,
    is_active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS members (
    id VARCHAR(64) PRIMARY KEY,
    family_id VARCHAR(64) NOT NULL,
    member_name VARCHAR(150) NOT NULL,
    phone VARCHAR(50) DEFAULT '',
    relation VARCHAR(100) DEFAULT 'প্রধান',
    occupation VARCHAR(100) DEFAULT '',
    is_head TINYINT(1) DEFAULT 0,
    FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(64) PRIMARY KEY,
    family_id VARCHAR(64) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_date VARCHAR(20) NOT NULL, -- Format: YYYY-MM-DD
    payment_type VARCHAR(50) DEFAULT 'DUES', -- DUES, DONATION, ZAKAT
    receipt_no VARCHAR(100) DEFAULT '',
    note VARCHAR(255) DEFAULT '',
    recorded_by VARCHAR(150) DEFAULT 'Admin',
    FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qurbani_cows (
    id VARCHAR(64) PRIMARY KEY,
    year INT NOT NULL,
    cow_number VARCHAR(50) NOT NULL,
    cow_name VARCHAR(150) DEFAULT '',
    total_shares INT DEFAULT 7,
    total_cost DECIMAL(10,2) DEFAULT 0.00,
    note VARCHAR(255) DEFAULT ''
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS qurbani_distributions (
    id VARCHAR(64) PRIMARY KEY,
    year INT NOT NULL,
    family_id VARCHAR(64) NOT NULL,
    meat_kg DECIMAL(5,2) DEFAULT 5.00,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, DELIVERED
    FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notices (
    id VARCHAR(64) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    date VARCHAR(20) NOT NULL,
    author VARCHAR(150) DEFAULT 'মসজিদ কমিটি'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample seed data
INSERT IGNORE INTO families (id, family_code, family_name, address, phone, monthly_due_amount) VALUES
('FAM-001', 'FAM-001', 'মুহাম্মদ রিয়াদ পরিবার', 'দত্ত পাড়া, শেরপুর', '01711223344', 500.00),
('FAM-002', 'FAM-002', 'আব্দুল করিম সর্দার পরিবার', 'উত্তর পাড়া, শেরপুর', '01822334455', 400.00),
('FAM-003', 'FAM-003', 'হাফিজুর রহমান পরিবার', 'মধ্য বাজার, শেরপুর', '01933445566', 300.00);

INSERT IGNORE INTO members (id, family_id, member_name, phone, relation, occupation, is_head) VALUES
('MEM-101', 'FAM-001', 'মুহাম্মদ রিয়াদ', '01711223344', 'প্রধান', 'ব্যবসায়ী', 1),
('MEM-102', 'FAM-002', 'আব্দুল করিম', '01822334455', 'প্রধান', 'চাকরিজীবী', 1),
('MEM-103', 'FAM-003', 'হাফিজুর রহমান', '01933445566', 'প্রধান', 'শিক্ষক', 1);

INSERT IGNORE INTO payments (id, family_id, amount, payment_date, payment_type, receipt_no, note) VALUES
('PAY-01', 'FAM-001', 500.00, '2026-01-10', 'DUES', 'REC-001', 'জানুয়ারি চাঁদা'),
('PAY-02', 'FAM-001', 500.00, '2026-02-12', 'DUES', 'REC-002', 'ফেব্রুয়ারি চাঁদা'),
('PAY-03', 'FAM-002', 400.00, '2026-01-15', 'DUES', 'REC-003', 'জানুয়ারি চাঁদা');

INSERT IGNORE INTO notices (id, title, content, date) VALUES
('NOT-1', 'আসন্ন জুম্মার বয়ান ও বিশেষ দোয়া', 'আগামী জুম্মার নামাজে ফিলিস্তিনের মুসলিম ভাইদের জন্য বিশেষ দোয়া ও ত্রাণ তহবিল গঠন করা হবে।', '২০২৬-১১-২৪');
