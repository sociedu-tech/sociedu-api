-- ==========================================
-- INIT DATA
-- ==========================================

INSERT INTO roles (id, name)
VALUES ('f93d94ef-1fe8-48c4-b240-d9b1001affb1', 'USER'),
       ('d7af84b1-731d-48dc-bcd6-cebe8e78b279', 'MENTOR'),
       ('29056341-f10b-4d2c-bb32-c6b82cf897bb', 'ADMIN');

INSERT INTO capabilities (name)
VALUES

-- USER
('UPDATE_PROFILE'),
('VIEW_PROFILE'),
('BOOK_SESSION'),
('CANCEL_BOOKING'),
('WRITE_REVIEW'),

-- MENTOR
('CREATE_SERVICE_PACKAGE'),
('UPDATE_OWN_SERVICE_PACKAGE'),
('DELETE_OWN_SERVICE_PACKAGE'),
('MANAGE_PACKAGE_CURRICULUM'),
('VIEW_OWN_BOOKINGS'),
('MANAGE_OWN_BOOKINGS'),
('MANAGE_SESSIONS'),
('VIEW_EARNINGS'),

-- BOOKING
('VIEW_BOOKING'),
('JOIN_SESSION'),
('START_SESSION'),
('COMPLETE_SESSION'),

-- PAYMENT
('CREATE_PAYMENT'),
('VIEW_PAYMENT'),
('REQUEST_PAYOUT'),
('VIEW_PAYOUT'),
('REFUND_REQUEST'),

-- CHAT
('SEND_MESSAGE'),
('VIEW_CONVERSATION'),
('UPLOAD_ATTACHMENT'),

-- REPORT / DISPUTE
('CREATE_REPORT'),
('VIEW_OWN_REPORT'),
('CREATE_DISPUTE'),
('VIEW_OWN_DISPUTE'),

-- ADMIN
('MANAGE_USERS'),
('MANAGE_MENTORS'),
('MANAGE_ALL_BOOKINGS'),
('MANAGE_PAYMENTS'),
('RESOLVE_REPORT'),
('RESOLVE_DISPUTE'),
('VIEW_SYSTEM_METRICS'),
('MANAGE_ALL');

INSERT INTO role_capabilities (role_id, capability_id)
SELECT (SELECT id FROM roles WHERE name = 'USER'),
       id
FROM capabilities
WHERE name IN (
               'UPDATE_PROFILE',
               'VIEW_PROFILE',
               'BOOK_SESSION',
               'CANCEL_BOOKING',
               'WRITE_REVIEW',
               'VIEW_BOOKING',
               'JOIN_SESSION',
               'CREATE_PAYMENT',
               'VIEW_PAYMENT',
               'SEND_MESSAGE',
               'VIEW_CONVERSATION',
               'UPLOAD_ATTACHMENT',
               'CREATE_REPORT',
               'VIEW_OWN_REPORT',
               'CREATE_DISPUTE',
               'VIEW_OWN_DISPUTE'
    );

INSERT INTO role_capabilities (role_id, capability_id)
SELECT (SELECT id FROM roles WHERE name = 'MENTOR'),
       id
FROM capabilities
WHERE name IN (
    -- USER basic
               'UPDATE_PROFILE',
               'VIEW_PROFILE',
               'BOOK_SESSION',
               'CANCEL_BOOKING',
               'WRITE_REVIEW',

    -- mentor core
               'CREATE_SERVICE_PACKAGE',
               'UPDATE_OWN_SERVICE_PACKAGE',
               'DELETE_OWN_SERVICE_PACKAGE',
               'MANAGE_PACKAGE_CURRICULUM',
               'VIEW_OWN_BOOKINGS',
               'MANAGE_OWN_BOOKINGS',
               'MANAGE_SESSIONS',
               'VIEW_EARNINGS',

    -- booking/session
               'VIEW_BOOKING',
               'JOIN_SESSION',
               'START_SESSION',
               'COMPLETE_SESSION',

    -- payment
               'VIEW_PAYMENT',
               'REQUEST_PAYOUT',
               'VIEW_PAYOUT',

    -- chat
               'SEND_MESSAGE',
               'VIEW_CONVERSATION',
               'UPLOAD_ATTACHMENT'
    );

INSERT INTO role_capabilities (role_id, capability_id)
SELECT (SELECT id FROM roles WHERE name = 'ADMIN'),
       id
FROM capabilities;

-- ==========================================
-- SEED USERS (10 USER + 10 MENTOR + 10 ADMIN)
-- Mật khẩu mặc định cho TẤT CẢ tài khoản seed: Password123!
-- Hash sinh runtime bằng pgcrypto: crypt('Password123!', gen_salt('bf', 10))
-- ==========================================
INSERT INTO users (id, email, email_verified, phone_number, phone_verified, status)
VALUES
    -- USER (10)
    ('11111111-1111-4111-8111-111111110001', 'user01@unishare.test',  TRUE, '0900000001', TRUE, 'active'),
    ('11111111-1111-4111-8111-111111110002', 'user02@unishare.test',  TRUE, '0900000002', TRUE, 'active'),
    ('11111111-1111-4111-8111-111111110003', 'user03@unishare.test',  TRUE, '0900000003', TRUE, 'active'),
    ('11111111-1111-4111-8111-111111110004', 'user04@unishare.test',  TRUE, '0900000004', TRUE, 'active'),
    ('11111111-1111-4111-8111-111111110005', 'user05@unishare.test',  TRUE, '0900000005', TRUE, 'active'),
    ('11111111-1111-4111-8111-111111110006', 'user06@unishare.test',  TRUE, '0900000006', TRUE, 'active'),
    ('11111111-1111-4111-8111-111111110007', 'user07@unishare.test',  TRUE, '0900000007', TRUE, 'active'),
    ('11111111-1111-4111-8111-111111110008', 'user08@unishare.test',  TRUE, '0900000008', TRUE, 'active'),
    ('11111111-1111-4111-8111-111111110009', 'user09@unishare.test',  TRUE, '0900000009', TRUE, 'active'),
    ('11111111-1111-4111-8111-111111110010', 'user10@unishare.test',  TRUE, '0900000010', TRUE, 'active'),
    -- MENTOR (10)
    ('22222222-2222-4222-8222-222222220001', 'mentor01@unishare.test', TRUE, '0901000001', TRUE, 'active'),
    ('22222222-2222-4222-8222-222222220002', 'mentor02@unishare.test', TRUE, '0901000002', TRUE, 'active'),
    ('22222222-2222-4222-8222-222222220003', 'mentor03@unishare.test', TRUE, '0901000003', TRUE, 'active'),
    ('22222222-2222-4222-8222-222222220004', 'mentor04@unishare.test', TRUE, '0901000004', TRUE, 'active'),
    ('22222222-2222-4222-8222-222222220005', 'mentor05@unishare.test', TRUE, '0901000005', TRUE, 'active'),
    ('22222222-2222-4222-8222-222222220006', 'mentor06@unishare.test', TRUE, '0901000006', TRUE, 'active'),
    ('22222222-2222-4222-8222-222222220007', 'mentor07@unishare.test', TRUE, '0901000007', TRUE, 'active'),
    ('22222222-2222-4222-8222-222222220008', 'mentor08@unishare.test', TRUE, '0901000008', TRUE, 'active'),
    ('22222222-2222-4222-8222-222222220009', 'mentor09@unishare.test', TRUE, '0901000009', TRUE, 'active'),
    ('22222222-2222-4222-8222-222222220010', 'mentor10@unishare.test', TRUE, '0901000010', TRUE, 'active'),
    -- ADMIN (10)
    ('33333333-3333-4333-8333-333333330001', 'admin01@unishare.test',  TRUE, '0902000001', TRUE, 'active'),
    ('33333333-3333-4333-8333-333333330002', 'admin02@unishare.test',  TRUE, '0902000002', TRUE, 'active'),
    ('33333333-3333-4333-8333-333333330003', 'admin03@unishare.test',  TRUE, '0902000003', TRUE, 'active'),
    ('33333333-3333-4333-8333-333333330004', 'admin04@unishare.test',  TRUE, '0902000004', TRUE, 'active'),
    ('33333333-3333-4333-8333-333333330005', 'admin05@unishare.test',  TRUE, '0902000005', TRUE, 'active'),
    ('33333333-3333-4333-8333-333333330006', 'admin06@unishare.test',  TRUE, '0902000006', TRUE, 'active'),
    ('33333333-3333-4333-8333-333333330007', 'admin07@unishare.test',  TRUE, '0902000007', TRUE, 'active'),
    ('33333333-3333-4333-8333-333333330008', 'admin08@unishare.test',  TRUE, '0902000008', TRUE, 'active'),
    ('33333333-3333-4333-8333-333333330009', 'admin09@unishare.test',  TRUE, '0902000009', TRUE, 'active'),
    ('33333333-3333-4333-8333-333333330010', 'admin10@unishare.test',  TRUE, '0902000010', TRUE, 'active');

INSERT INTO user_credentials (user_id, password_hash)
SELECT id, crypt('Password123!', gen_salt('bf', 10))
FROM users;

-- Gán role theo email prefix
INSERT INTO user_roles (user_id, role_id)
SELECT id, 'f93d94ef-1fe8-48c4-b240-d9b1001affb1'
FROM users
WHERE email LIKE 'user%@unishare.test';

INSERT INTO user_roles (user_id, role_id)
SELECT id, 'd7af84b1-731d-48dc-bcd6-cebe8e78b279'
FROM users
WHERE email LIKE 'mentor%@unishare.test';

INSERT INTO user_roles (user_id, role_id)
SELECT id, '29056341-f10b-4d2c-bb32-c6b82cf897bb'
FROM users
WHERE email LIKE 'admin%@unishare.test';

-- ==========================================
-- USER PROFILES (30)
-- ==========================================
INSERT INTO user_profiles (user_id, first_name, last_name, bio)
VALUES
    -- USER
    ('11111111-1111-4111-8111-111111110001', 'Minh Anh',   'Nguyễn', 'Sinh viên năm 3 CNTT, đang tìm mentor về Frontend.'),
    ('11111111-1111-4111-8111-111111110002', 'Bảo Châu',   'Trần',   'Năm cuối Kinh tế, muốn chuyển hướng làm Product.'),
    ('11111111-1111-4111-8111-111111110003', 'Quốc Hùng',  'Lê',     'Fresher backend, học Spring Boot.'),
    ('11111111-1111-4111-8111-111111110004', 'Lan Chi',    'Phạm',   'Sinh viên thiết kế, muốn build portfolio UI/UX.'),
    ('11111111-1111-4111-8111-111111110005', 'Khánh Linh', 'Hoàng',  'Quan tâm Data Engineering và Airflow.'),
    ('11111111-1111-4111-8111-111111110006', 'Tuấn Kiệt',  'Đặng',   'Fresher mobile dev, học Flutter.'),
    ('11111111-1111-4111-8111-111111110007', 'Mai Phương', 'Vũ',     'Junior DevOps, muốn tiến tới AWS Solution Architect.'),
    ('11111111-1111-4111-8111-111111110008', 'Đức Nam',    'Bùi',    'Marketing intern, cần mentor về growth.'),
    ('11111111-1111-4111-8111-111111110009', 'Thu Hà',     'Đỗ',     'Đang chuyển ngành sang AI/ML.'),
    ('11111111-1111-4111-8111-111111110010', 'Quang Vinh', 'Lý',     'QA junior muốn lên Senior Automation.'),
    -- MENTOR
    ('22222222-2222-4222-8222-222222220001', 'Văn Long',    'Trần',   'Senior Frontend Engineer (6+ năm React/Next.js).'),
    ('22222222-2222-4222-8222-222222220002', 'Thị Mai',     'Lê',     'Product Manager B2B SaaS, ex-Base.vn.'),
    ('22222222-2222-4222-8222-222222220003', 'Hoàng Nam',   'Nguyễn', 'Senior Backend Java, Spring Boot, Microservices.'),
    ('22222222-2222-4222-8222-222222220004', 'Quỳnh Anh',   'Phạm',   'UI/UX Designer 5 năm, từng dẫn dắt team thiết kế.'),
    ('22222222-2222-4222-8222-222222220005', 'Đức Minh',    'Vũ',     'Data Engineer, chuyên Spark + Airflow.'),
    ('22222222-2222-4222-8222-222222220006', 'Thanh Hà',    'Bùi',    'Mobile Lead Flutter/React Native.'),
    ('22222222-2222-4222-8222-222222220007', 'Tuấn Khang',  'Đỗ',     'DevOps Engineer, AWS Solutions Architect Pro.'),
    ('22222222-2222-4222-8222-222222220008', 'Bảo Trân',    'Hoàng',  'Growth Marketing Lead, từng scale startup edtech 10x.'),
    ('22222222-2222-4222-8222-222222220009', 'Quang Huy',   'Đặng',   'AI Engineer (PyTorch, LLM, RAG).'),
    ('22222222-2222-4222-8222-222222220010', 'Ngọc Bích',   'Lý',     'QA Automation Lead, Cypress & Playwright trainer.'),
    -- ADMIN
    ('33333333-3333-4333-8333-333333330001', 'Master',      'Admin',  'Super admin của UniShare.'),
    ('33333333-3333-4333-8333-333333330002', 'Ops',         'Admin',  'Vận hành nền tảng.'),
    ('33333333-3333-4333-8333-333333330003', 'Support',     'Admin',  'Hỗ trợ người dùng.'),
    ('33333333-3333-4333-8333-333333330004', 'Finance',     'Admin',  'Quản lý thanh toán.'),
    ('33333333-3333-4333-8333-333333330005', 'Moderation',  'Admin',  'Kiểm duyệt nội dung.'),
    ('33333333-3333-4333-8333-333333330006', 'Marketing',   'Admin',  'Truyền thông và sự kiện.'),
    ('33333333-3333-4333-8333-333333330007', 'Catalog',     'Admin',  'Quản lý gói dịch vụ.'),
    ('33333333-3333-4333-8333-333333330008', 'Mentor Care', 'Admin',  'Hỗ trợ mentor.'),
    ('33333333-3333-4333-8333-333333330009', 'Risk',        'Admin',  'Theo dõi rủi ro và fraud.'),
    ('33333333-3333-4333-8333-333333330010', 'Analytics',   'Admin',  'Phân tích dữ liệu hệ thống.');

-- ==========================================
-- UNIVERSITIES & FIELDS OF STUDY
-- ==========================================
INSERT INTO universities (id, name)
VALUES
    ('60000000-0000-0000-0000-000000000001', 'Đại học Bách khoa Hà Nội'),
    ('60000000-0000-0000-0000-000000000002', 'Đại học Quốc gia Hà Nội'),
    ('60000000-0000-0000-0000-000000000003', 'Đại học Bách khoa TP.HCM'),
    ('60000000-0000-0000-0000-000000000004', 'Đại học FPT'),
    ('60000000-0000-0000-0000-000000000005', 'Đại học Ngoại thương');

INSERT INTO fields_of_study (id, name)
VALUES
    ('61000000-0000-0000-0000-000000000001', 'Công nghệ thông tin'),
    ('61000000-0000-0000-0000-000000000002', 'Khoa học máy tính'),
    ('61000000-0000-0000-0000-000000000003', 'Thiết kế đồ họa'),
    ('61000000-0000-0000-0000-000000000004', 'Quản trị kinh doanh'),
    ('61000000-0000-0000-0000-000000000005', 'Marketing');

-- Mỗi mentor có 1 record học vấn để marketplace hiển thị
INSERT INTO user_educations (user_id, university_id, major_id)
VALUES
    ('22222222-2222-4222-8222-222222220001', '60000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000002'),
    ('22222222-2222-4222-8222-222222220002', '60000000-0000-0000-0000-000000000005', '61000000-0000-0000-0000-000000000004'),
    ('22222222-2222-4222-8222-222222220003', '60000000-0000-0000-0000-000000000003', '61000000-0000-0000-0000-000000000002'),
    ('22222222-2222-4222-8222-222222220004', '60000000-0000-0000-0000-000000000004', '61000000-0000-0000-0000-000000000003'),
    ('22222222-2222-4222-8222-222222220005', '60000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000001'),
    ('22222222-2222-4222-8222-222222220006', '60000000-0000-0000-0000-000000000003', '61000000-0000-0000-0000-000000000002'),
    ('22222222-2222-4222-8222-222222220007', '60000000-0000-0000-0000-000000000002', '61000000-0000-0000-0000-000000000001'),
    ('22222222-2222-4222-8222-222222220008', '60000000-0000-0000-0000-000000000005', '61000000-0000-0000-0000-000000000005'),
    ('22222222-2222-4222-8222-222222220009', '60000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000002'),
    ('22222222-2222-4222-8222-222222220010', '60000000-0000-0000-0000-000000000004', '61000000-0000-0000-0000-000000000001');

-- ==========================================
-- SKILLS + USER_SKILLS (gắn cho 10 mentor)
-- ==========================================
INSERT INTO skills (id, name)
VALUES
    ('70000000-0000-0000-0000-000000000001', 'React'),
    ('70000000-0000-0000-0000-000000000002', 'Next.js'),
    ('70000000-0000-0000-0000-000000000003', 'Spring Boot'),
    ('70000000-0000-0000-0000-000000000004', 'Microservices'),
    ('70000000-0000-0000-0000-000000000005', 'Figma'),
    ('70000000-0000-0000-0000-000000000006', 'Flutter'),
    ('70000000-0000-0000-0000-000000000007', 'AWS'),
    ('70000000-0000-0000-0000-000000000008', 'Kubernetes'),
    ('70000000-0000-0000-0000-000000000009', 'Airflow'),
    ('70000000-0000-0000-0000-000000000010', 'PyTorch'),
    ('70000000-0000-0000-0000-000000000011', 'Cypress'),
    ('70000000-0000-0000-0000-000000000012', 'Playwright'),
    ('70000000-0000-0000-0000-000000000013', 'Growth Marketing');

INSERT INTO user_skills (user_id, skill_id, level)
VALUES
    ('22222222-2222-4222-8222-222222220001', '70000000-0000-0000-0000-000000000001', 5),
    ('22222222-2222-4222-8222-222222220001', '70000000-0000-0000-0000-000000000002', 5),
    ('22222222-2222-4222-8222-222222220003', '70000000-0000-0000-0000-000000000003', 5),
    ('22222222-2222-4222-8222-222222220003', '70000000-0000-0000-0000-000000000004', 4),
    ('22222222-2222-4222-8222-222222220004', '70000000-0000-0000-0000-000000000005', 5),
    ('22222222-2222-4222-8222-222222220005', '70000000-0000-0000-0000-000000000009', 5),
    ('22222222-2222-4222-8222-222222220006', '70000000-0000-0000-0000-000000000006', 5),
    ('22222222-2222-4222-8222-222222220007', '70000000-0000-0000-0000-000000000007', 5),
    ('22222222-2222-4222-8222-222222220007', '70000000-0000-0000-0000-000000000008', 4),
    ('22222222-2222-4222-8222-222222220008', '70000000-0000-0000-0000-000000000013', 5),
    ('22222222-2222-4222-8222-222222220009', '70000000-0000-0000-0000-000000000010', 5),
    ('22222222-2222-4222-8222-222222220010', '70000000-0000-0000-0000-000000000011', 5),
    ('22222222-2222-4222-8222-222222220010', '70000000-0000-0000-0000-000000000012', 5);

-- ==========================================
-- MENTOR PROFILES (10) - đã verified, sẵn sàng nhận booking
-- ==========================================
INSERT INTO mentor_profiles (user_id, headline, expertise, base_price, rating_avg, sessions_completed, verification_status)
VALUES
    ('22222222-2222-4222-8222-222222220001', 'Senior Frontend Engineer · React, Next.js',  'React,Next.js,TypeScript,TailwindCSS',         1500000, 4.8, 42, 'verified'),
    ('22222222-2222-4222-8222-222222220002', 'Product Manager B2B SaaS',                   'Product Discovery,Roadmap,User Research,SaaS', 2000000, 4.9, 35, 'verified'),
    ('22222222-2222-4222-8222-222222220003', 'Senior Backend Java · Spring Boot',          'Java,Spring Boot,Microservices,Kafka',         1800000, 4.7, 51, 'verified'),
    ('22222222-2222-4222-8222-222222220004', 'UI/UX Designer · Figma · Design System',     'Figma,Design System,User Research,Interaction',1600000, 4.8, 29, 'verified'),
    ('22222222-2222-4222-8222-222222220005', 'Data Engineer · Airflow · Spark',            'Airflow,Spark,SQL,DBT',                        2000000, 4.7, 24, 'verified'),
    ('22222222-2222-4222-8222-222222220006', 'Mobile Lead · Flutter · React Native',       'Flutter,React Native,iOS,Android',             1700000, 4.6, 33, 'verified'),
    ('22222222-2222-4222-8222-222222220007', 'DevOps Engineer · AWS · Kubernetes',         'AWS,Kubernetes,Terraform,CI/CD',               1900000, 4.8, 40, 'verified'),
    ('22222222-2222-4222-8222-222222220008', 'Growth Marketing Lead',                      'Performance Ads,SEO,Content,Funnel',           1400000, 4.5, 18, 'verified'),
    ('22222222-2222-4222-8222-222222220009', 'AI Engineer · LLM · RAG',                    'PyTorch,LLM,RAG,LangChain',                    2300000, 4.9, 27, 'verified'),
    ('22222222-2222-4222-8222-222222220010', 'QA Automation Lead · Cypress · Playwright',  'Cypress,Playwright,Test Architecture,QA',      1500000, 4.7, 31, 'verified');

-- ==========================================
-- SERVICE PACKAGES (2 gói / mentor = 20)
-- package_group_id = id của bản version 1 (về sau version mới sẽ giữ chung group)
-- ==========================================
INSERT INTO service_packages (id, package_group_id, mentor_id, name, version, is_active, price, duration)
VALUES
    -- M01 Frontend
    ('44444401-0000-0000-0000-000000000001', '44444401-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220001', 'Lộ trình Frontend Junior 4 tuần',           1, TRUE, 1500000, 60),
    ('44444401-0000-0000-0000-000000000002', '44444401-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220001', 'Mentor 1:1 Next.js & App Router 1 tháng',   1, TRUE, 2500000, 90),
    -- M02 Product
    ('44444402-0000-0000-0000-000000000001', '44444402-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220002', 'Khởi đầu nghề Product Manager',             1, TRUE, 2000000, 60),
    ('44444402-0000-0000-0000-000000000002', '44444402-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220002', 'Mentor 1:1 ra mắt SaaS MVP',                1, TRUE, 3000000, 90),
    -- M03 Backend
    ('44444403-0000-0000-0000-000000000001', '44444403-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220003', 'Backend Java Spring Boot Bootcamp',         1, TRUE, 1800000, 75),
    ('44444403-0000-0000-0000-000000000002', '44444403-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220003', 'Microservices thực chiến 1 tháng',          1, TRUE, 2800000, 90),
    -- M04 UI/UX
    ('44444404-0000-0000-0000-000000000001', '44444404-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220004', 'UI/UX Fundamentals 4 tuần',                 1, TRUE, 1600000, 60),
    ('44444404-0000-0000-0000-000000000002', '44444404-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220004', 'Portfolio Coaching cho Designer',           1, TRUE, 2200000, 90),
    -- M05 Data
    ('44444405-0000-0000-0000-000000000001', '44444405-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220005', 'Data Engineering với Airflow',              1, TRUE, 2000000, 75),
    ('44444405-0000-0000-0000-000000000002', '44444405-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220005', 'Spark Big Data thực chiến',                 1, TRUE, 2700000, 90),
    -- M06 Mobile
    ('44444406-0000-0000-0000-000000000001', '44444406-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220006', 'Flutter App từ A-Z',                        1, TRUE, 1700000, 60),
    ('44444406-0000-0000-0000-000000000002', '44444406-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220006', 'React Native 1:1 Mentor',                   1, TRUE, 2400000, 90),
    -- M07 DevOps
    ('44444407-0000-0000-0000-000000000001', '44444407-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220007', 'AWS Cloud Practitioner 30 ngày',            1, TRUE, 1900000, 60),
    ('44444407-0000-0000-0000-000000000002', '44444407-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220007', 'Kubernetes thực chiến',                     1, TRUE, 2600000, 90),
    -- M08 Marketing
    ('44444408-0000-0000-0000-000000000001', '44444408-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220008', 'Growth Marketing 101',                      1, TRUE, 1400000, 60),
    ('44444408-0000-0000-0000-000000000002', '44444408-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220008', 'Performance Marketing Mentor',              1, TRUE, 2100000, 90),
    -- M09 AI/ML
    ('44444409-0000-0000-0000-000000000001', '44444409-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220009', 'Roadmap AI Engineer 2026',                  1, TRUE, 2300000, 75),
    ('44444409-0000-0000-0000-000000000002', '44444409-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220009', 'LLM Fine-tune thực chiến',                  1, TRUE, 3200000, 90),
    -- M10 QA
    ('44444410-0000-0000-0000-000000000001', '44444410-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220010', 'QA Automation với Cypress',                 1, TRUE, 1500000, 60),
    ('44444410-0000-0000-0000-000000000002', '44444410-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220010', 'Test Architecture Coaching',                1, TRUE, 2300000, 90);

-- ==========================================
-- PACKAGE CURRICULUMS (3 buổi / gói = 60 row)
-- ==========================================
INSERT INTO package_curriculums (package_id, title, description, order_index, duration)
VALUES
    -- M01 A
    ('44444401-0000-0000-0000-000000000001', 'Tuần 1: HTML/CSS nâng cao + Tailwind',    'Layout phức tạp, responsive, animation cơ bản.',         1, 60),
    ('44444401-0000-0000-0000-000000000001', 'Tuần 2: JavaScript ES2024 + TypeScript',  'Type system, generics, async, fetch.',                   2, 60),
    ('44444401-0000-0000-0000-000000000001', 'Tuần 3: React + Next.js App Router',      'Hooks, server component, routing, data fetching.',       3, 60),
    -- M01 B
    ('44444401-0000-0000-0000-000000000002', 'Kiến trúc Next.js App Router production', 'Layout, server actions, caching, edge runtime.',         1, 90),
    ('44444401-0000-0000-0000-000000000002', 'Tối ưu performance & SEO',                'RSC, ISR, Web Vitals, Lighthouse.',                      2, 90),
    ('44444401-0000-0000-0000-000000000002', 'Mini-project & code review 1:1',          'Build feature thật, review chi tiết.',                   3, 90),
    -- M02 A
    ('44444402-0000-0000-0000-000000000001', 'Vai trò Product Manager',                 'Trách nhiệm, OKR, KPI, làm việc với cross-team.',        1, 60),
    ('44444402-0000-0000-0000-000000000001', 'Discovery & Roadmap',                     'Customer interview, JTBD, prioritization.',              2, 60),
    ('44444402-0000-0000-0000-000000000001', 'Spec & Delivery',                         'PRD, ticket, release plan, metrics.',                    3, 60),
    -- M02 B
    ('44444402-0000-0000-0000-000000000002', 'Tìm Product Market Fit',                  'Define ICP, problem-solution fit.',                      1, 90),
    ('44444402-0000-0000-0000-000000000002', 'Xây MVP và đo lường',                     'Build vs buy, north star metric.',                       2, 90),
    ('44444402-0000-0000-0000-000000000002', 'Go-to-market cho SaaS B2B',               'Pricing, sales funnel, retention loop.',                 3, 90),
    -- M03 A
    ('44444403-0000-0000-0000-000000000001', 'Spring Boot cơ bản & cấu trúc dự án',     'Bean, DI, profile, layered architecture.',               1, 75),
    ('44444403-0000-0000-0000-000000000001', 'JPA, REST API và validation',             'Entity, repository, DTO mapping, exception.',            2, 75),
    ('44444403-0000-0000-0000-000000000001', 'Bảo mật & deploy',                        'JWT, Spring Security, Docker compose.',                  3, 75),
    -- M03 B
    ('44444403-0000-0000-0000-000000000002', 'Tách monolith thành microservices',       'Bounded context, contract, event-driven.',               1, 90),
    ('44444403-0000-0000-0000-000000000002', 'Kafka & saga pattern',                    'Producer, consumer, saga, outbox.',                      2, 90),
    ('44444403-0000-0000-0000-000000000002', 'Observability & resilience',              'Tracing, retry, circuit breaker, k8s deploy.',           3, 90),
    -- M04 A
    ('44444404-0000-0000-0000-000000000001', 'Design fundamentals',                     'Typography, color, layout, grid.',                       1, 60),
    ('44444404-0000-0000-0000-000000000001', 'Wireframe & user flow',                   'Sketch, prototyping với Figma.',                         2, 60),
    ('44444404-0000-0000-0000-000000000001', 'Design system căn bản',                   'Token, component, variant.',                             3, 60),
    -- M04 B
    ('44444404-0000-0000-0000-000000000002', 'Phân tích portfolio hiện tại',            'Strengths, gap, ngách phù hợp.',                         1, 90),
    ('44444404-0000-0000-0000-000000000002', 'Build case study chuẩn',                  'Cấu trúc, storytelling, visual.',                        2, 90),
    ('44444404-0000-0000-0000-000000000002', 'Phỏng vấn & gửi hồ sơ',                   'Pitch, deck portfolio, mock interview.',                 3, 90),
    -- M05 A
    ('44444405-0000-0000-0000-000000000001', 'Airflow basics & DAG patterns',           'Sensor, operator, scheduling.',                          1, 75),
    ('44444405-0000-0000-0000-000000000001', 'Data modeling cho warehouse',             'Dim/fact, slowly changing dimension.',                   2, 75),
    ('44444405-0000-0000-0000-000000000001', 'Pipeline production grade',               'Monitoring, retry, alert, SLA.',                         3, 75),
    -- M05 B
    ('44444405-0000-0000-0000-000000000002', 'Spark fundamentals',                      'RDD vs DataFrame, partitioning.',                        1, 90),
    ('44444405-0000-0000-0000-000000000002', 'Performance tuning Spark',                'Skew, shuffle, broadcast.',                              2, 90),
    ('44444405-0000-0000-0000-000000000002', 'Streaming với Spark Structured',          'Watermark, state, sink Kafka.',                          3, 90),
    -- M06 A
    ('44444406-0000-0000-0000-000000000001', 'Flutter setup & widget cơ bản',           'Stateless/Stateful, hot reload, layout.',                1, 60),
    ('44444406-0000-0000-0000-000000000001', 'State management Bloc/Riverpod',          'Reactive, dependency injection.',                        2, 60),
    ('44444406-0000-0000-0000-000000000001', 'Build & publish app store',               'Sign, CI, release iOS/Android.',                         3, 60),
    -- M06 B
    ('44444406-0000-0000-0000-000000000002', 'React Native architecture',               'Expo vs CLI, new architecture, fabric.',                 1, 90),
    ('44444406-0000-0000-0000-000000000002', 'Performance & native module',             'Bridging, profiling.',                                   2, 90),
    ('44444406-0000-0000-0000-000000000002', 'Publish & maintain',                      'OTA update, crash report, scale.',                       3, 90),
    -- M07 A
    ('44444407-0000-0000-0000-000000000001', 'AWS core services overview',              'IAM, EC2, S3, RDS, VPC.',                                1, 60),
    ('44444407-0000-0000-0000-000000000001', 'Networking & security',                   'VPC, security group, route table.',                      2, 60),
    ('44444407-0000-0000-0000-000000000001', 'Deploy app lên AWS',                      'ECS Fargate, RDS, ALB.',                                 3, 60),
    -- M07 B
    ('44444407-0000-0000-0000-000000000002', 'Kubernetes fundamentals',                 'Pod, deployment, service, ingress.',                     1, 90),
    ('44444407-0000-0000-0000-000000000002', 'Helm & GitOps',                           'Helm chart, ArgoCD.',                                    2, 90),
    ('44444407-0000-0000-0000-000000000002', 'Observability & scaling',                 'Prometheus, HPA, network policy.',                       3, 90),
    -- M08 A
    ('44444408-0000-0000-0000-000000000001', 'Funnel & metric',                         'AARRR, north star, dashboard.',                          1, 60),
    ('44444408-0000-0000-0000-000000000001', 'Acquisition channels',                    'SEO, Ads, content, social.',                             2, 60),
    ('44444408-0000-0000-0000-000000000001', 'Activation & retention',                  'Onboarding, lifecycle, email.',                          3, 60),
    -- M08 B
    ('44444408-0000-0000-0000-000000000002', 'Cấu trúc campaign Google/Meta',           'Audience, creative, bidding.',                           1, 90),
    ('44444408-0000-0000-0000-000000000002', 'Tối ưu CAC & ROAS',                       'A/B test creative, audit funnel.',                       2, 90),
    ('44444408-0000-0000-0000-000000000002', 'Tự động hoá báo cáo',                     'Looker Studio, GA4, attribution.',                       3, 90),
    -- M09 A
    ('44444409-0000-0000-0000-000000000001', 'Toán & nền tảng ML',                      'Tuyến tính, xác suất, gradient.',                        1, 75),
    ('44444409-0000-0000-0000-000000000001', 'Deep learning với PyTorch',               'Tensor, autograd, training loop.',                       2, 75),
    ('44444409-0000-0000-0000-000000000001', 'MLOps cơ bản',                            'Tracking, deploy, monitoring.',                          3, 75),
    -- M09 B
    ('44444409-0000-0000-0000-000000000002', 'LoRA & PEFT cho LLM',                     'Fine-tune nhẹ, dataset chuẩn.',                          1, 90),
    ('44444409-0000-0000-0000-000000000002', 'RAG pipeline production',                 'Vector DB, retriever, eval.',                            2, 90),
    ('44444409-0000-0000-0000-000000000002', 'Deploy & cost optimization',              'Quantization, batching, GPU sizing.',                    3, 90),
    -- M10 A
    ('44444410-0000-0000-0000-000000000001', 'Test pyramid & strategy',                 'Unit/Integration/E2E balance.',                          1, 60),
    ('44444410-0000-0000-0000-000000000001', 'Cypress E2E thực chiến',                  'Selector, fixture, network stub.',                       2, 60),
    ('44444410-0000-0000-0000-000000000001', 'CI/CD cho automation',                    'GitHub Actions, parallel, report.',                      3, 60),
    -- M10 B
    ('44444410-0000-0000-0000-000000000002', 'Architect test framework',                'Page object, custom command.',                           1, 90),
    ('44444410-0000-0000-0000-000000000002', 'Playwright multi-env',                    'Device, project, fixture pattern.',                      2, 90),
    ('44444410-0000-0000-0000-000000000002', 'Quality metrics & coaching',              'Flakiness, mean-time-to-detect.',                        3, 90);