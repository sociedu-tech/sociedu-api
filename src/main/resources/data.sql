-- ==========================================
-- DEV SEED (chạy SAU khi Hibernate khởi tạo schema)
-- Phương pháp: ddl-auto=create-drop + defer-datasource-initialization=true
--   ⇒ Bảng được Hibernate tạo từ entity, file này chỉ INSERT.
-- Mọi seed phải tham chiếu đúng tên bảng / cột theo @Entity hiện tại.
-- Bảng KHÔNG có entity (vd skills, user_skills, user_stats, app_metrics, ...) bị bỏ qua.
-- Password mặc định: Password123! (hash bằng pgcrypto.crypt — extension được tạo trong schema.sql).
-- Mọi statement viết idempotent (ON CONFLICT DO NOTHING) để có thể chạy lại nhiều lần.
-- ==========================================

-- ==========================================
-- ROLES
-- ==========================================
INSERT INTO roles (id, name)
VALUES ('f93d94ef-1fe8-48c4-b240-d9b1001affb1', 'USER'),
       ('d7af84b1-731d-48dc-bcd6-cebe8e78b279', 'MENTOR'),
       ('29056341-f10b-4d2c-bb32-c6b82cf897bb', 'ADMIN')
ON CONFLICT (name) DO NOTHING;

-- ==========================================
-- USERS (10 USER + 10 MENTOR + 10 ADMIN), password = Password123!
-- ==========================================
INSERT INTO users (id, email, email_verified, phone_number, phone_verified, status)
VALUES ('11111111-1111-4111-8111-111111110001', 'user01@unishare.test', TRUE, '0900000001', TRUE, 'active'),
       ('11111111-1111-4111-8111-111111110002', 'user02@unishare.test', TRUE, '0900000002', TRUE, 'active'),
       ('11111111-1111-4111-8111-111111110003', 'user03@unishare.test', TRUE, '0900000003', TRUE, 'active'),
       ('11111111-1111-4111-8111-111111110004', 'user04@unishare.test', TRUE, '0900000004', TRUE, 'active'),
       ('11111111-1111-4111-8111-111111110005', 'user05@unishare.test', TRUE, '0900000005', TRUE, 'active'),
       ('11111111-1111-4111-8111-111111110006', 'user06@unishare.test', TRUE, '0900000006', TRUE, 'active'),
       ('11111111-1111-4111-8111-111111110007', 'user07@unishare.test', TRUE, '0900000007', TRUE, 'active'),
       ('11111111-1111-4111-8111-111111110008', 'user08@unishare.test', TRUE, '0900000008', TRUE, 'active'),
       ('11111111-1111-4111-8111-111111110009', 'user09@unishare.test', TRUE, '0900000009', TRUE, 'active'),
       ('11111111-1111-4111-8111-111111110010', 'user10@unishare.test', TRUE, '0900000010', TRUE, 'active'),
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
       ('33333333-3333-4333-8333-333333330001', 'admin01@unishare.test', TRUE, '0902000001', TRUE, 'active'),
       ('33333333-3333-4333-8333-333333330002', 'admin02@unishare.test', TRUE, '0902000002', TRUE, 'active'),
       ('33333333-3333-4333-8333-333333330003', 'admin03@unishare.test', TRUE, '0902000003', TRUE, 'active'),
       ('33333333-3333-4333-8333-333333330004', 'admin04@unishare.test', TRUE, '0902000004', TRUE, 'active'),
       ('33333333-3333-4333-8333-333333330005', 'admin05@unishare.test', TRUE, '0902000005', TRUE, 'active'),
       ('33333333-3333-4333-8333-333333330006', 'admin06@unishare.test', TRUE, '0902000006', TRUE, 'active'),
       ('33333333-3333-4333-8333-333333330007', 'admin07@unishare.test', TRUE, '0902000007', TRUE, 'active'),
       ('33333333-3333-4333-8333-333333330008', 'admin08@unishare.test', TRUE, '0902000008', TRUE, 'active'),
       ('33333333-3333-4333-8333-333333330009', 'admin09@unishare.test', TRUE, '0902000009', TRUE, 'active'),
       ('33333333-3333-4333-8333-333333330010', 'admin10@unishare.test', TRUE, '0902000010', TRUE, 'active')
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_credentials (user_id, password_hash)
SELECT id, crypt('Password123!', gen_salt('bf', 10))
FROM users
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT id, 'f93d94ef-1fe8-48c4-b240-d9b1001affb1'
FROM users
WHERE email LIKE 'user%@unishare.test'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT id, 'd7af84b1-731d-48dc-bcd6-cebe8e78b279'
FROM users
WHERE email LIKE 'mentor%@unishare.test'
ON CONFLICT DO NOTHING;

INSERT INTO user_roles (user_id, role_id)
SELECT id, '29056341-f10b-4d2c-bb32-c6b82cf897bb'
FROM users
WHERE email LIKE 'admin%@unishare.test'
ON CONFLICT DO NOTHING;

-- ==========================================
-- USER PROFILES
-- ==========================================
INSERT INTO user_profiles (user_id, first_name, last_name, bio)
VALUES ('11111111-1111-4111-8111-111111110001', 'Minh Anh', 'Nguyễn', 'Sinh viên năm 3 CNTT, đang tìm mentor về Frontend.'),
       ('11111111-1111-4111-8111-111111110002', 'Bảo Châu', 'Trần', 'Năm cuối Kinh tế, muốn chuyển hướng làm Product.'),
       ('11111111-1111-4111-8111-111111110003', 'Quốc Hùng', 'Lê', 'Fresher backend, học Spring Boot.'),
       ('11111111-1111-4111-8111-111111110004', 'Lan Chi', 'Phạm', 'Sinh viên thiết kế, muốn build portfolio UI/UX.'),
       ('11111111-1111-4111-8111-111111110005', 'Khánh Linh', 'Hoàng', 'Quan tâm Data Engineering và Airflow.'),
       ('11111111-1111-4111-8111-111111110006', 'Tuấn Kiệt', 'Đặng', 'Fresher mobile dev, học Flutter.'),
       ('11111111-1111-4111-8111-111111110007', 'Mai Phương', 'Vũ', 'Junior DevOps, muốn tiến tới AWS Solution Architect.'),
       ('11111111-1111-4111-8111-111111110008', 'Đức Nam', 'Bùi', 'Marketing intern, cần mentor về growth.'),
       ('11111111-1111-4111-8111-111111110009', 'Thu Hà', 'Đỗ', 'Đang chuyển ngành sang AI/ML.'),
       ('11111111-1111-4111-8111-111111110010', 'Quang Vinh', 'Lý', 'QA junior muốn lên Senior Automation.'),
       ('22222222-2222-4222-8222-222222220001', 'Văn Long', 'Trần', 'Senior Frontend Engineer (6+ năm React/Next.js).'),
       ('22222222-2222-4222-8222-222222220002', 'Thị Mai', 'Lê', 'Product Manager B2B SaaS, ex-Base.vn.'),
       ('22222222-2222-4222-8222-222222220003', 'Hoàng Nam', 'Nguyễn', 'Senior Backend Java, Spring Boot, Microservices.'),
       ('22222222-2222-4222-8222-222222220004', 'Quỳnh Anh', 'Phạm', 'UI/UX Designer 5 năm, từng dẫn dắt team thiết kế.'),
       ('22222222-2222-4222-8222-222222220005', 'Đức Minh', 'Vũ', 'Data Engineer, chuyên Spark + Airflow.'),
       ('22222222-2222-4222-8222-222222220006', 'Thanh Hà', 'Bùi', 'Mobile Lead Flutter/React Native.'),
       ('22222222-2222-4222-8222-222222220007', 'Tuấn Khang', 'Đỗ', 'DevOps Engineer, AWS Solutions Architect Pro.'),
       ('22222222-2222-4222-8222-222222220008', 'Bảo Trân', 'Hoàng', 'Growth Marketing Lead, từng scale startup edtech 10x.'),
       ('22222222-2222-4222-8222-222222220009', 'Quang Huy', 'Đặng', 'AI Engineer (PyTorch, LLM, RAG).'),
       ('22222222-2222-4222-8222-222222220010', 'Ngọc Bích', 'Lý', 'QA Automation Lead, Cypress & Playwright trainer.'),
       ('33333333-3333-4333-8333-333333330001', 'Master', 'Admin', 'Super admin của UniShare.'),
       ('33333333-3333-4333-8333-333333330002', 'Ops', 'Admin', 'Vận hành nền tảng.'),
       ('33333333-3333-4333-8333-333333330003', 'Support', 'Admin', 'Hỗ trợ người dùng.'),
       ('33333333-3333-4333-8333-333333330004', 'Finance', 'Admin', 'Quản lý thanh toán.'),
       ('33333333-3333-4333-8333-333333330005', 'Moderation', 'Admin', 'Kiểm duyệt nội dung.'),
       ('33333333-3333-4333-8333-333333330006', 'Marketing', 'Admin', 'Truyền thông và sự kiện.'),
       ('33333333-3333-4333-8333-333333330007', 'Catalog', 'Admin', 'Quản lý gói dịch vụ.'),
       ('33333333-3333-4333-8333-333333330008', 'Mentor Care', 'Admin', 'Hỗ trợ mentor.'),
       ('33333333-3333-4333-8333-333333330009', 'Risk', 'Admin', 'Theo dõi rủi ro và fraud.'),
       ('33333333-3333-4333-8333-333333330010', 'Analytics', 'Admin', 'Phân tích dữ liệu hệ thống.')
ON CONFLICT (user_id) DO NOTHING;

-- ==========================================
-- UNIVERSITIES & FIELDS OF STUDY
-- ==========================================
INSERT INTO universities (id, name)
VALUES ('60000000-0000-0000-0000-000000000001', 'Đại học Bách khoa Hà Nội'),
       ('60000000-0000-0000-0000-000000000002', 'Đại học Quốc gia Hà Nội'),
       ('60000000-0000-0000-0000-000000000003', 'Đại học Bách khoa TP.HCM'),
       ('60000000-0000-0000-0000-000000000004', 'Đại học FPT'),
       ('60000000-0000-0000-0000-000000000005', 'Đại học Ngoại thương')
ON CONFLICT (id) DO NOTHING;

INSERT INTO fields_of_study (id, name)
VALUES ('61000000-0000-0000-0000-000000000001', 'Công nghệ thông tin'),
       ('61000000-0000-0000-0000-000000000002', 'Khoa học máy tính'),
       ('61000000-0000-0000-0000-000000000003', 'Thiết kế đồ họa'),
       ('61000000-0000-0000-0000-000000000004', 'Quản trị kinh doanh'),
       ('61000000-0000-0000-0000-000000000005', 'Marketing')
ON CONFLICT (id) DO NOTHING;

INSERT INTO user_educations (id, user_id, university_id, major_id)
VALUES ('62000000-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220001', '60000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000002'),
       ('62000000-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220002', '60000000-0000-0000-0000-000000000005', '61000000-0000-0000-0000-000000000004'),
       ('62000000-0000-0000-0000-000000000003', '22222222-2222-4222-8222-222222220003', '60000000-0000-0000-0000-000000000003', '61000000-0000-0000-0000-000000000002'),
       ('62000000-0000-0000-0000-000000000004', '22222222-2222-4222-8222-222222220004', '60000000-0000-0000-0000-000000000004', '61000000-0000-0000-0000-000000000003'),
       ('62000000-0000-0000-0000-000000000005', '22222222-2222-4222-8222-222222220005', '60000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000001'),
       ('62000000-0000-0000-0000-000000000006', '22222222-2222-4222-8222-222222220006', '60000000-0000-0000-0000-000000000003', '61000000-0000-0000-0000-000000000002'),
       ('62000000-0000-0000-0000-000000000007', '22222222-2222-4222-8222-222222220007', '60000000-0000-0000-0000-000000000002', '61000000-0000-0000-0000-000000000001'),
       ('62000000-0000-0000-0000-000000000008', '22222222-2222-4222-8222-222222220008', '60000000-0000-0000-0000-000000000005', '61000000-0000-0000-0000-000000000005'),
       ('62000000-0000-0000-0000-000000000009', '22222222-2222-4222-8222-222222220009', '60000000-0000-0000-0000-000000000001', '61000000-0000-0000-0000-000000000002'),
       ('62000000-0000-0000-0000-000000000010', '22222222-2222-4222-8222-222222220010', '60000000-0000-0000-0000-000000000004', '61000000-0000-0000-0000-000000000001')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- MENTOR PROFILES (đã verified)
-- ==========================================
INSERT INTO mentor_profiles (user_id, headline, expertise, base_price, rating_avg, sessions_completed, verification_status)
VALUES ('22222222-2222-4222-8222-222222220001', 'Senior Frontend Engineer · React, Next.js', 'React,Next.js,TypeScript,TailwindCSS', 1500000, 4.8, 42, 'verified'),
       ('22222222-2222-4222-8222-222222220002', 'Product Manager B2B SaaS', 'Product Discovery,Roadmap,User Research,SaaS', 2000000, 4.9, 35, 'verified'),
       ('22222222-2222-4222-8222-222222220003', 'Senior Backend Java · Spring Boot', 'Java,Spring Boot,Microservices,Kafka', 1800000, 4.7, 51, 'verified'),
       ('22222222-2222-4222-8222-222222220004', 'UI/UX Designer · Figma · Design System', 'Figma,Design System,User Research,Interaction', 1600000, 4.8, 29, 'verified'),
       ('22222222-2222-4222-8222-222222220005', 'Data Engineer · Airflow · Spark', 'Airflow,Spark,SQL,DBT', 2000000, 4.7, 24, 'verified'),
       ('22222222-2222-4222-8222-222222220006', 'Mobile Lead · Flutter · React Native', 'Flutter,React Native,iOS,Android', 1700000, 4.6, 33, 'verified'),
       ('22222222-2222-4222-8222-222222220007', 'DevOps Engineer · AWS · Kubernetes', 'AWS,Kubernetes,Terraform,CI/CD', 1900000, 4.8, 40, 'verified'),
       ('22222222-2222-4222-8222-222222220008', 'Growth Marketing Lead', 'Performance Ads,SEO,Content,Funnel', 1400000, 4.5, 18, 'verified'),
       ('22222222-2222-4222-8222-222222220009', 'AI Engineer · LLM · RAG', 'PyTorch,LLM,RAG,LangChain', 2300000, 4.9, 27, 'verified'),
       ('22222222-2222-4222-8222-222222220010', 'QA Automation Lead · Cypress · Playwright', 'Cypress,Playwright,Test Architecture,QA', 1500000, 4.7, 31, 'verified')
ON CONFLICT (user_id) DO NOTHING;

-- ==========================================
-- SERVICE PACKAGES (2 gói / mentor = 20)
-- Schema mới: chỉ thông tin gói; price/duration nằm ở service_package_versions
-- ==========================================
INSERT INTO service_packages (id, mentor_id, name, description, is_active)
VALUES ('44444401-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220001', 'Lộ trình Frontend Junior 4 tuần', 'HTML/CSS/JS/React/Next.js bài bản, mentor 1:1.', TRUE),
       ('44444401-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220001', 'Mentor 1:1 Next.js & App Router 1 tháng', 'Next.js App Router, server actions, performance.', TRUE),
       ('44444402-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220002', 'Khởi đầu nghề Product Manager', 'PM mindset, OKR, discovery, roadmap.', TRUE),
       ('44444402-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220002', 'Mentor 1:1 ra mắt SaaS MVP', 'PMF, MVP, go-to-market cho SaaS B2B.', TRUE),
       ('44444403-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220003', 'Backend Java Spring Boot Bootcamp', 'Spring Boot, JPA, REST API, Security.', TRUE),
       ('44444403-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220003', 'Microservices thực chiến 1 tháng', 'DDD, Kafka, saga, observability, k8s.', TRUE),
       ('44444404-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220004', 'UI/UX Fundamentals 4 tuần', 'Design fundamentals, wireframe, design system.', TRUE),
       ('44444404-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220004', 'Portfolio Coaching cho Designer', 'Audit + build case study + interview prep.', TRUE),
       ('44444405-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220005', 'Data Engineering với Airflow', 'DAG, data modeling, pipeline production grade.', TRUE),
       ('44444405-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220005', 'Spark Big Data thực chiến', 'Spark core, tuning, streaming.', TRUE),
       ('44444406-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220006', 'Flutter App từ A-Z', 'Widget, state management, publish.', TRUE),
       ('44444406-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220006', 'React Native 1:1 Mentor', 'New architecture, native module, OTA.', TRUE),
       ('44444407-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220007', 'AWS Cloud Practitioner 30 ngày', 'IAM, EC2, S3, VPC, deploy.', TRUE),
       ('44444407-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220007', 'Kubernetes thực chiến', 'Pod, deployment, GitOps, observability.', TRUE),
       ('44444408-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220008', 'Growth Marketing 101', 'AARRR, acquisition, activation, retention.', TRUE),
       ('44444408-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220008', 'Performance Marketing Mentor', 'Google/Meta Ads, CAC, ROAS, reporting.', TRUE),
       ('44444409-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220009', 'Roadmap AI Engineer 2026', 'Math, PyTorch, MLOps cơ bản.', TRUE),
       ('44444409-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220009', 'LLM Fine-tune thực chiến', 'LoRA, RAG, deploy production.', TRUE),
       ('44444410-0000-0000-0000-000000000001', '22222222-2222-4222-8222-222222220010', 'QA Automation với Cypress', 'Test strategy, Cypress, CI/CD.', TRUE),
       ('44444410-0000-0000-0000-000000000002', '22222222-2222-4222-8222-222222220010', 'Test Architecture Coaching', 'Framework, Playwright multi-env, metrics.', TRUE)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- SERVICE PACKAGE VERSIONS (1 default version / package = 20 row)
-- ==========================================
INSERT INTO service_package_versions (id, package_id, price, duration, delivery_type, is_default)
VALUES ('45444401-0000-0000-0000-000000000001', '44444401-0000-0000-0000-000000000001', 1500000, 60, 'one_on_one', TRUE),
       ('45444401-0000-0000-0000-000000000002', '44444401-0000-0000-0000-000000000002', 2500000, 90, 'one_on_one', TRUE),
       ('45444402-0000-0000-0000-000000000001', '44444402-0000-0000-0000-000000000001', 2000000, 60, 'one_on_one', TRUE),
       ('45444402-0000-0000-0000-000000000002', '44444402-0000-0000-0000-000000000002', 3000000, 90, 'one_on_one', TRUE),
       ('45444403-0000-0000-0000-000000000001', '44444403-0000-0000-0000-000000000001', 1800000, 75, 'one_on_one', TRUE),
       ('45444403-0000-0000-0000-000000000002', '44444403-0000-0000-0000-000000000002', 2800000, 90, 'one_on_one', TRUE),
       ('45444404-0000-0000-0000-000000000001', '44444404-0000-0000-0000-000000000001', 1600000, 60, 'one_on_one', TRUE),
       ('45444404-0000-0000-0000-000000000002', '44444404-0000-0000-0000-000000000002', 2200000, 90, 'one_on_one', TRUE),
       ('45444405-0000-0000-0000-000000000001', '44444405-0000-0000-0000-000000000001', 2000000, 75, 'one_on_one', TRUE),
       ('45444405-0000-0000-0000-000000000002', '44444405-0000-0000-0000-000000000002', 2700000, 90, 'one_on_one', TRUE),
       ('45444406-0000-0000-0000-000000000001', '44444406-0000-0000-0000-000000000001', 1700000, 60, 'one_on_one', TRUE),
       ('45444406-0000-0000-0000-000000000002', '44444406-0000-0000-0000-000000000002', 2400000, 90, 'one_on_one', TRUE),
       ('45444407-0000-0000-0000-000000000001', '44444407-0000-0000-0000-000000000001', 1900000, 60, 'one_on_one', TRUE),
       ('45444407-0000-0000-0000-000000000002', '44444407-0000-0000-0000-000000000002', 2600000, 90, 'one_on_one', TRUE),
       ('45444408-0000-0000-0000-000000000001', '44444408-0000-0000-0000-000000000001', 1400000, 60, 'one_on_one', TRUE),
       ('45444408-0000-0000-0000-000000000002', '44444408-0000-0000-0000-000000000002', 2100000, 90, 'one_on_one', TRUE),
       ('45444409-0000-0000-0000-000000000001', '44444409-0000-0000-0000-000000000001', 2300000, 75, 'one_on_one', TRUE),
       ('45444409-0000-0000-0000-000000000002', '44444409-0000-0000-0000-000000000002', 3200000, 90, 'one_on_one', TRUE),
       ('45444410-0000-0000-0000-000000000001', '44444410-0000-0000-0000-000000000001', 1500000, 60, 'one_on_one', TRUE),
       ('45444410-0000-0000-0000-000000000002', '44444410-0000-0000-0000-000000000002', 2300000, 90, 'one_on_one', TRUE)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- PACKAGE CURRICULUMS (3 buổi / version = 60 row) — FK trỏ về package_version_id
-- ==========================================
-- Cột id không có DEFAULT (entity dùng UUID generator phía Java). Tự sinh bằng gen_random_uuid().
INSERT INTO package_curriculums (id, package_version_id, title, description, order_index, duration)
VALUES (gen_random_uuid(), '45444401-0000-0000-0000-000000000001', 'Tuần 1: HTML/CSS nâng cao + Tailwind', 'Layout phức tạp, responsive, animation cơ bản.', 1, 60),
       (gen_random_uuid(), '45444401-0000-0000-0000-000000000001', 'Tuần 2: JavaScript ES2024 + TypeScript', 'Type system, generics, async, fetch.', 2, 60),
       (gen_random_uuid(), '45444401-0000-0000-0000-000000000001', 'Tuần 3: React + Next.js App Router', 'Hooks, server component, routing, data fetching.', 3, 60),
       (gen_random_uuid(), '45444401-0000-0000-0000-000000000002', 'Kiến trúc Next.js App Router production', 'Layout, server actions, caching, edge runtime.', 1, 90),
       (gen_random_uuid(), '45444401-0000-0000-0000-000000000002', 'Tối ưu performance & SEO', 'RSC, ISR, Web Vitals, Lighthouse.', 2, 90),
       (gen_random_uuid(), '45444401-0000-0000-0000-000000000002', 'Mini-project & code review 1:1', 'Build feature thật, review chi tiết.', 3, 90),
       (gen_random_uuid(), '45444402-0000-0000-0000-000000000001', 'Vai trò Product Manager', 'Trách nhiệm, OKR, KPI, làm việc với cross-team.', 1, 60),
       (gen_random_uuid(), '45444402-0000-0000-0000-000000000001', 'Discovery & Roadmap', 'Customer interview, JTBD, prioritization.', 2, 60),
       (gen_random_uuid(), '45444402-0000-0000-0000-000000000001', 'Spec & Delivery', 'PRD, ticket, release plan, metrics.', 3, 60),
       (gen_random_uuid(), '45444402-0000-0000-0000-000000000002', 'Tìm Product Market Fit', 'Define ICP, problem-solution fit.', 1, 90),
       (gen_random_uuid(), '45444402-0000-0000-0000-000000000002', 'Xây MVP và đo lường', 'Build vs buy, north star metric.', 2, 90),
       (gen_random_uuid(), '45444402-0000-0000-0000-000000000002', 'Go-to-market cho SaaS B2B', 'Pricing, sales funnel, retention loop.', 3, 90),
       (gen_random_uuid(), '45444403-0000-0000-0000-000000000001', 'Spring Boot cơ bản & cấu trúc dự án', 'Bean, DI, profile, layered architecture.', 1, 75),
       (gen_random_uuid(), '45444403-0000-0000-0000-000000000001', 'JPA, REST API và validation', 'Entity, repository, DTO mapping, exception.', 2, 75),
       (gen_random_uuid(), '45444403-0000-0000-0000-000000000001', 'Bảo mật & deploy', 'JWT, Spring Security, Docker compose.', 3, 75),
       (gen_random_uuid(), '45444403-0000-0000-0000-000000000002', 'Tách monolith thành microservices', 'Bounded context, contract, event-driven.', 1, 90),
       (gen_random_uuid(), '45444403-0000-0000-0000-000000000002', 'Kafka & saga pattern', 'Producer, consumer, saga, outbox.', 2, 90),
       (gen_random_uuid(), '45444403-0000-0000-0000-000000000002', 'Observability & resilience', 'Tracing, retry, circuit breaker, k8s deploy.', 3, 90),
       (gen_random_uuid(), '45444404-0000-0000-0000-000000000001', 'Design fundamentals', 'Typography, color, layout, grid.', 1, 60),
       (gen_random_uuid(), '45444404-0000-0000-0000-000000000001', 'Wireframe & user flow', 'Sketch, prototyping với Figma.', 2, 60),
       (gen_random_uuid(), '45444404-0000-0000-0000-000000000001', 'Design system căn bản', 'Token, component, variant.', 3, 60),
       (gen_random_uuid(), '45444404-0000-0000-0000-000000000002', 'Phân tích portfolio hiện tại', 'Strengths, gap, ngách phù hợp.', 1, 90),
       (gen_random_uuid(), '45444404-0000-0000-0000-000000000002', 'Build case study chuẩn', 'Cấu trúc, storytelling, visual.', 2, 90),
       (gen_random_uuid(), '45444404-0000-0000-0000-000000000002', 'Phỏng vấn & gửi hồ sơ', 'Pitch, deck portfolio, mock interview.', 3, 90),
       (gen_random_uuid(), '45444405-0000-0000-0000-000000000001', 'Airflow basics & DAG patterns', 'Sensor, operator, scheduling.', 1, 75),
       (gen_random_uuid(), '45444405-0000-0000-0000-000000000001', 'Data modeling cho warehouse', 'Dim/fact, slowly changing dimension.', 2, 75),
       (gen_random_uuid(), '45444405-0000-0000-0000-000000000001', 'Pipeline production grade', 'Monitoring, retry, alert, SLA.', 3, 75),
       (gen_random_uuid(), '45444405-0000-0000-0000-000000000002', 'Spark fundamentals', 'RDD vs DataFrame, partitioning.', 1, 90),
       (gen_random_uuid(), '45444405-0000-0000-0000-000000000002', 'Performance tuning Spark', 'Skew, shuffle, broadcast.', 2, 90),
       (gen_random_uuid(), '45444405-0000-0000-0000-000000000002', 'Streaming với Spark Structured', 'Watermark, state, sink Kafka.', 3, 90),
       (gen_random_uuid(), '45444406-0000-0000-0000-000000000001', 'Flutter setup & widget cơ bản', 'Stateless/Stateful, hot reload, layout.', 1, 60),
       (gen_random_uuid(), '45444406-0000-0000-0000-000000000001', 'State management Bloc/Riverpod', 'Reactive, dependency injection.', 2, 60),
       (gen_random_uuid(), '45444406-0000-0000-0000-000000000001', 'Build & publish app store', 'Sign, CI, release iOS/Android.', 3, 60),
       (gen_random_uuid(), '45444406-0000-0000-0000-000000000002', 'React Native architecture', 'Expo vs CLI, new architecture, fabric.', 1, 90),
       (gen_random_uuid(), '45444406-0000-0000-0000-000000000002', 'Performance & native module', 'Bridging, profiling.', 2, 90),
       (gen_random_uuid(), '45444406-0000-0000-0000-000000000002', 'Publish & maintain', 'OTA update, crash report, scale.', 3, 90),
       (gen_random_uuid(), '45444407-0000-0000-0000-000000000001', 'AWS core services overview', 'IAM, EC2, S3, RDS, VPC.', 1, 60),
       (gen_random_uuid(), '45444407-0000-0000-0000-000000000001', 'Networking & security', 'VPC, security group, route table.', 2, 60),
       (gen_random_uuid(), '45444407-0000-0000-0000-000000000001', 'Deploy app lên AWS', 'ECS Fargate, RDS, ALB.', 3, 60),
       (gen_random_uuid(), '45444407-0000-0000-0000-000000000002', 'Kubernetes fundamentals', 'Pod, deployment, service, ingress.', 1, 90),
       (gen_random_uuid(), '45444407-0000-0000-0000-000000000002', 'Helm & GitOps', 'Helm chart, ArgoCD.', 2, 90),
       (gen_random_uuid(), '45444407-0000-0000-0000-000000000002', 'Observability & scaling', 'Prometheus, HPA, network policy.', 3, 90),
       (gen_random_uuid(), '45444408-0000-0000-0000-000000000001', 'Funnel & metric', 'AARRR, north star, dashboard.', 1, 60),
       (gen_random_uuid(), '45444408-0000-0000-0000-000000000001', 'Acquisition channels', 'SEO, Ads, content, social.', 2, 60),
       (gen_random_uuid(), '45444408-0000-0000-0000-000000000001', 'Activation & retention', 'Onboarding, lifecycle, email.', 3, 60),
       (gen_random_uuid(), '45444408-0000-0000-0000-000000000002', 'Cấu trúc campaign Google/Meta', 'Audience, creative, bidding.', 1, 90),
       (gen_random_uuid(), '45444408-0000-0000-0000-000000000002', 'Tối ưu CAC & ROAS', 'A/B test creative, audit funnel.', 2, 90),
       (gen_random_uuid(), '45444408-0000-0000-0000-000000000002', 'Tự động hoá báo cáo', 'Looker Studio, GA4, attribution.', 3, 90),
       (gen_random_uuid(), '45444409-0000-0000-0000-000000000001', 'Toán & nền tảng ML', 'Tuyến tính, xác suất, gradient.', 1, 75),
       (gen_random_uuid(), '45444409-0000-0000-0000-000000000001', 'Deep learning với PyTorch', 'Tensor, autograd, training loop.', 2, 75),
       (gen_random_uuid(), '45444409-0000-0000-0000-000000000001', 'MLOps cơ bản', 'Tracking, deploy, monitoring.', 3, 75),
       (gen_random_uuid(), '45444409-0000-0000-0000-000000000002', 'LoRA & PEFT cho LLM', 'Fine-tune nhẹ, dataset chuẩn.', 1, 90),
       (gen_random_uuid(), '45444409-0000-0000-0000-000000000002', 'RAG pipeline production', 'Vector DB, retriever, eval.', 2, 90),
       (gen_random_uuid(), '45444409-0000-0000-0000-000000000002', 'Deploy & cost optimization', 'Quantization, batching, GPU sizing.', 3, 90),
       (gen_random_uuid(), '45444410-0000-0000-0000-000000000001', 'Test pyramid & strategy', 'Unit/Integration/E2E balance.', 1, 60),
       (gen_random_uuid(), '45444410-0000-0000-0000-000000000001', 'Cypress E2E thực chiến', 'Selector, fixture, network stub.', 2, 60),
       (gen_random_uuid(), '45444410-0000-0000-0000-000000000001', 'CI/CD cho automation', 'GitHub Actions, parallel, report.', 3, 60),
       (gen_random_uuid(), '45444410-0000-0000-0000-000000000002', 'Playwright multi-env', 'Device, project, fixture pattern.', 2, 90),
       (gen_random_uuid(), '45444410-0000-0000-0000-000000000002', 'Quality metrics & coaching', 'Flakiness, mean-time-to-detect.', 3, 90);

-- ==========================================
-- ORDERS (ĐƠN HÀNG)
-- ==========================================
INSERT INTO orders (id, buyer_id, service_id, status, total_amount, paid_at, created_at)
VALUES 
    -- Order 1: user01 mua gói 1 của mentor01 (paid)
    ('88888888-8888-4888-a888-888888880001', '11111111-1111-4111-8111-111111110001', '45444401-0000-0000-0000-000000000001', 'paid', 1500000.00, '2026-05-20 09:00:00', '2026-05-20 08:30:00'),
    -- Order 2: user02 mua gói 2 của mentor01 (paid)
    ('88888888-8888-4888-a888-888888880002', '11111111-1111-4111-8111-111111110002', '45444401-0000-0000-0000-000000000002', 'paid', 2500000.00, '2026-05-21 14:00:00', '2026-05-21 13:15:00'),
    -- Order 3: user03 mua gói 1 của mentor02 (paid)
    ('88888888-8888-4888-a888-888888880003', '11111111-1111-4111-8111-111111110003', '45444402-0000-0000-0000-000000000001', 'paid', 2000000.00, '2026-05-18 10:30:00', '2026-05-18 10:00:00'),
    -- Order 4: user04 mua gói 2 của mentor02 (pending_payment)
    ('88888888-8888-4888-a888-888888880004', '11111111-1111-4111-8111-111111110004', '45444402-0000-0000-0000-000000000002', 'pending_payment', 3000000.00, NULL, '2026-05-24 10:00:00'),
    -- Order 5: user05 mua gói 1 của mentor03 (paid)
    ('88888888-8888-4888-a888-888888880005', '11111111-1111-4111-8111-111111110005', '45444403-0000-0000-0000-000000000001', 'paid', 1800000.00, '2026-05-22 16:15:00', '2026-05-22 15:45:00'),
    -- Order 6: user06 mua gói 2 của mentor03 (failed)
    ('88888888-8888-4888-a888-888888880006', '11111111-1111-4111-8111-111111110006', '45444403-0000-0000-0000-000000000002', 'failed', 2800000.00, NULL, '2026-05-22 15:00:00'),
    -- Order 7: user07 mua gói 1 của mentor04 (paid)
    ('88888888-8888-4888-a888-888888880007', '11111111-1111-4111-8111-111111110007', '45444404-0000-0000-0000-000000000001', 'paid', 1600000.00, '2026-05-15 08:45:00', '2026-05-15 08:00:00'),
    -- Order 8: user08 mua gói 2 của mentor05 (canceled)
    ('88888888-8888-4888-a888-888888880008', '11111111-1111-4111-8111-111111110008', '45444405-0000-0000-0000-000000000002', 'canceled', 2700000.00, NULL, '2026-05-20 10:00:00')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- PAYMENT TRANSACTIONS (GIAO DỊCH THANH TOÁN)
-- ==========================================
INSERT INTO payment_transactions (id, order_id, provider, provider_transaction_id, amount, status, created_at)
VALUES
    -- Giao dịch cho Order 1
    ('99999999-9999-4999-b999-999999990001', '88888888-8888-4888-a888-888888880001', 'vnpay', 'VNPAY202605200001', 1500000.00, 'success', '2026-05-20 09:00:00'),
    -- Giao dịch cho Order 2
    ('99999999-9999-4999-b999-999999990002', '88888888-8888-4888-a888-888888880002', 'vnpay', 'VNPAY202605210002', 2500000.00, 'success', '2026-05-21 14:00:00'),
    -- Giao dịch cho Order 3
    ('99999999-9999-4999-b999-999999990003', '88888888-8888-4888-a888-888888880003', 'vnpay', 'VNPAY202605180003', 2000000.00, 'success', '2026-05-18 10:30:00'),
    -- Giao dịch cho Order 5
    ('99999999-9999-4999-b999-999999990005', '88888888-8888-4888-a888-888888880005', 'vnpay', 'VNPAY202605220005', 1800000.00, 'success', '2026-05-22 16:15:00'),
    -- Giao dịch cho Order 6 (thất bại)
    ('99999999-9999-4999-b999-999999990006', '88888888-8888-4888-a888-888888880006', 'vnpay', 'VNPAY202605220006', 2800000.00, 'failed', '2026-05-22 15:05:00'),
    -- Giao dịch cho Order 7
    ('99999999-9999-4999-b999-999999990007', '88888888-8888-4888-a888-888888880007', 'vnpay', 'VNPAY202605150007', 1600000.00, 'success', '2026-05-15 08:45:00')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- BOOKINGS (ĐẶT LỊCH)
-- ==========================================
INSERT INTO bookings (id, order_id, buyer_id, mentor_id, package_id, status, created_at, version)
VALUES
    -- Booking 1: user01 học mentor01 (completed)
    ('aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0001', '88888888-8888-4888-a888-888888880001', '11111111-1111-4111-8111-111111110001', '22222222-2222-4222-8222-222222220001', '44444401-0000-0000-0000-000000000001', 'completed', '2026-05-20 09:00:00', 1),
    -- Booking 2: user02 học mentor01 (scheduled)
    ('aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0002', '88888888-8888-4888-a888-888888880002', '11111111-1111-4111-8111-111111110002', '22222222-2222-4222-8222-222222220001', '44444401-0000-0000-0000-000000000002', 'scheduled', '2026-05-21 14:00:00', 1),
    -- Booking 3: user03 học mentor02 (completed)
    ('aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0003', '88888888-8888-4888-a888-888888880003', '11111111-1111-4111-8111-111111110003', '22222222-2222-4222-8222-222222220002', '44444402-0000-0000-0000-000000000001', 'completed', '2026-05-18 10:30:00', 1),
    -- Booking 5: user05 học mentor03 (in_progress)
    ('aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0005', '88888888-8888-4888-a888-888888880005', '11111111-1111-4111-8111-111111110005', '22222222-2222-4222-8222-222222220003', '44444403-0000-0000-0000-000000000001', 'in_progress', '2026-05-22 16:15:00', 1),
    -- Booking 7: user07 học mentor04 (completed)
    ('aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0007', '88888888-8888-4888-a888-888888880007', '11111111-1111-4111-8111-111111110007', '22222222-2222-4222-8222-222222220004', '44444404-0000-0000-0000-000000000001', 'completed', '2026-05-15 08:45:00', 1)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- BOOKING SESSIONS (CHI TIẾT BUỔI HỌC)
-- ==========================================
INSERT INTO booking_sessions (id, booking_id, curriculum_id, title, status, scheduled_at, completed_at, meeting_url, created_at, actual_started_at, actual_ended_at, version)
VALUES
    -- Booking 1 (completed) - 3 buổi hoàn thành
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0101', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0001', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444401-0000-0000-0000-000000000001' AND order_index = 1), 
        'HTML/CSS nâng cao + Tailwind', 'completed', '2026-05-21 09:00:00', '2026-05-21 10:00:00', 'https://meet.google.com/abc-defg-hij', '2026-05-20 09:05:00', '2026-05-21 08:58:00', '2026-05-21 10:02:00', 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0102', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0001', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444401-0000-0000-0000-000000000001' AND order_index = 2), 
        'JavaScript ES2024 + TypeScript', 'completed', '2026-05-22 09:00:00', '2026-05-22 10:00:00', 'https://meet.google.com/abc-defg-hij', '2026-05-20 09:05:00', '2026-05-22 09:00:00', '2026-05-22 10:00:00', 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0103', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0001', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444401-0000-0000-0000-000000000001' AND order_index = 3), 
        'React + Next.js App Router', 'completed', '2026-05-23 09:00:00', '2026-05-23 10:00:00', 'https://meet.google.com/abc-defg-hij', '2026-05-20 09:05:00', '2026-05-23 08:59:00', '2026-05-23 10:01:00', 1),

    -- Booking 2 (scheduled) - 1 buổi completed, 2 buổi scheduled
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0201', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0002', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444401-0000-0000-0000-000000000002' AND order_index = 1), 
        'Kiến trúc Next.js App Router production', 'completed', '2026-05-24 14:00:00', '2026-05-24 15:30:00', 'https://meet.google.com/xyz-uvwx-yza', '2026-05-21 14:05:00', '2026-05-24 14:00:00', '2026-05-24 15:30:00', 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0202', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0002', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444401-0000-0000-0000-000000000002' AND order_index = 2), 
        'Tối ưu performance & SEO', 'scheduled', '2026-05-26 14:00:00', NULL, 'https://meet.google.com/xyz-uvwx-yza', '2026-05-21 14:05:00', NULL, NULL, 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0203', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0002', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444401-0000-0000-0000-000000000002' AND order_index = 3), 
        'Mini-project & code review 1:1', 'scheduled', '2026-05-28 14:00:00', NULL, 'https://meet.google.com/xyz-uvwx-yza', '2026-05-21 14:05:00', NULL, NULL, 1),

    -- Booking 3 (completed) - 3 buổi hoàn thành
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0301', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0003', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444402-0000-0000-0000-000000000001' AND order_index = 1), 
        'Vai trò Product Manager', 'completed', '2026-05-19 10:00:00', '2026-05-19 11:00:00', 'https://meet.google.com/pqr-stuv-wxy', '2026-05-18 10:35:00', '2026-05-19 10:00:00', '2026-05-19 11:00:00', 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0302', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0003', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444402-0000-0000-0000-000000000001' AND order_index = 2), 
        'Discovery & Roadmap', 'completed', '2026-05-20 10:00:00', '2026-05-20 11:00:00', 'https://meet.google.com/pqr-stuv-wxy', '2026-05-18 10:35:00', '2026-05-20 10:00:00', '2026-05-20 11:00:00', 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0303', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0003', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444402-0000-0000-0000-000000000001' AND order_index = 3), 
        'Spec & Delivery', 'completed', '2026-05-21 10:00:00', '2026-05-21 11:00:00', 'https://meet.google.com/pqr-stuv-wxy', '2026-05-18 10:35:00', '2026-05-21 10:00:00', '2026-05-21 11:00:00', 1),

    -- Booking 5 (in_progress) - 1 buổi completed, 2 buổi scheduled
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0501', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0005', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444403-0000-0000-0000-000000000001' AND order_index = 1), 
        'Spring Boot cơ bản & cấu trúc dự án', 'completed', '2026-05-23 16:00:00', '2026-05-23 17:15:00', 'https://meet.google.com/mno-pqrs-tuv', '2026-05-22 16:20:00', '2026-05-23 16:00:00', '2026-05-23 17:15:00', 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0502', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0005', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444403-0000-0000-0000-000000000001' AND order_index = 2), 
        'JPA, REST API và validation', 'scheduled', '2026-05-26 16:00:00', NULL, 'https://meet.google.com/mno-pqrs-tuv', '2026-05-22 16:20:00', NULL, NULL, 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0503', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0005', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444403-0000-0000-0000-000000000001' AND order_index = 3), 
        'Bảo mật & deploy', 'scheduled', '2026-05-29 16:00:00', NULL, 'https://meet.google.com/mno-pqrs-tuv', '2026-05-22 16:20:00', NULL, NULL, 1),

    -- Booking 7 (completed) - 3 buổi hoàn thành
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0701', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0007', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444404-0000-0000-0000-000000000001' AND order_index = 1), 
        'Design fundamentals', 'completed', '2026-05-16 09:00:00', '2026-05-16 10:00:00', 'https://meet.google.com/ghi-jklm-nop', '2026-05-15 08:50:00', '2026-05-16 09:00:00', '2026-05-16 10:00:00', 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0702', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0007', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444404-0000-0000-0000-000000000001' AND order_index = 2), 
        'Wireframe & user flow', 'completed', '2026-05-17 09:00:00', '2026-05-17 10:00:00', 'https://meet.google.com/ghi-jklm-nop', '2026-05-15 08:50:00', '2026-05-17 09:00:00', '2026-05-17 10:00:00', 1),
    ('bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0703', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0007', 
        (SELECT id FROM package_curriculums WHERE package_version_id = '45444404-0000-0000-0000-000000000001' AND order_index = 3), 
        'Design system căn bản', 'completed', '2026-05-18 09:00:00', '2026-05-18 10:00:00', 'https://meet.google.com/ghi-jklm-nop', '2026-05-15 08:50:00', '2026-05-18 09:00:00', '2026-05-18 10:00:00', 1)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- PAYOUT RECORDS (GHI NHẬN DOANH THU MENTOR)
-- ==========================================
INSERT INTO payout_records (id, booking_id, mentor_id, source_event_id, amount, status, created_at, updated_at, version)
VALUES
    -- Record cho Booking 1 của mentor01 (1500000)
    ('dddddddd-dddd-4ddd-fddd-dddddddd0001', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0001', '22222222-2222-4222-8222-222222220001', 'cccccccc-cccc-4ccc-eccc-cccccccc0001', 1500000.00, 'SUCCESS', '2026-05-23 10:00:00', '2026-05-23 10:00:00', 1),
    -- Record cho Booking 3 của mentor02 (2000000)
    ('dddddddd-dddd-4ddd-fddd-dddddddd0003', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0003', '22222222-2222-4222-8222-222222220002', 'cccccccc-cccc-4ccc-eccc-cccccccc0003', 2000000.00, 'SUCCESS', '2026-05-21 11:30:00', '2026-05-21 11:30:00', 1),
    -- Record cho Booking 7 của mentor04 (1600000)
    ('dddddddd-dddd-4ddd-fddd-dddddddd0007', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0007', '22222222-2222-4222-8222-222222220004', 'cccccccc-cccc-4ccc-eccc-cccccccc0007', 1600000.00, 'SUCCESS', '2026-05-18 10:30:00', '2026-05-18 10:30:00', 1)
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- PAYOUT REQUESTS (YÊU CẦU RÚT TIỀN MENTOR)
-- ==========================================
INSERT INTO payout_requests (id, mentor_id, gross_amount, platform_fee_rate, net_amount, status, bank_name, account_number, account_holder, reject_reason, failure_reason, transaction_reference, processed_by, version, created_at, updated_at, processed_at)
VALUES
    -- Request 1 của mentor01 (PAID) - gross=1000000, fee=10.00, net=900000
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeee0001', '22222222-2222-4222-8222-222222220001', 1000000.00, 10.00, 900000.00, 'PAID', 'Vietcombank', '9876543210', 'TRAN VAN LONG', NULL, NULL, 'TX-REF-0001', '33333333-3333-4333-8333-333333330004', 1, '2026-05-22 11:00:00', '2026-05-22 14:00:00', '2026-05-22 14:00:00'),
    -- Request 2 của mentor01 (PENDING) - gross=500000, fee=10.00, net=450000
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeee0002', '22222222-2222-4222-8222-222222220001', 500000.00, 10.00, 450000.00, 'PENDING', 'Vietcombank', '9876543210', 'TRAN VAN LONG', NULL, NULL, NULL, NULL, 0, '2026-05-24 10:00:00', '2026-05-24 10:00:00', NULL),
    -- Request 3 của mentor02 (PENDING) - gross=1500000, fee=10.00, net=1350000
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeee0003', '22222222-2222-4222-8222-222222220002', 1500000.00, 10.00, 1350000.00, 'PENDING', 'Techcombank', '1234567890', 'LE THI MAI', NULL, NULL, NULL, NULL, 0, '2026-05-24 15:30:00', '2026-05-24 15:30:00', NULL),
    -- Request 4 của mentor03 (REJECTED)
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeee0004', '22222222-2222-4222-8222-222222220003', 1000000.00, 10.00, 900000.00, 'REJECTED', 'MB Bank', '111222333', 'NGUYEN HOANG NAM', 'Sai tên chủ tài khoản thụ hưởng', NULL, NULL, '33333333-3333-4333-8333-333333330004', 1, '2026-05-24 08:00:00', '2026-05-24 12:00:00', '2026-05-24 12:00:00'),
    -- Request 5 của mentor04 (APPROVED)
    ('eeeeeeee-eeee-4eee-8eee-eeeeeeee0005', '22222222-2222-4222-8222-222222220004', 1200000.00, 10.00, 1080000.00, 'APPROVED', 'BIDV', '444555666', 'PHAM QUYNH ANH', NULL, NULL, NULL, '33333333-3333-4333-8333-333333330004', 1, '2026-05-23 15:30:00', '2026-05-24 09:00:00', '2026-05-24 09:00:00')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- BOOKING REVIEWS (ĐÁNH GIÁ MENTOR)
-- ==========================================
INSERT INTO booking_reviews (id, booking_id, mentor_id, package_id, reviewer_id, rating, comment, version, created_at, updated_at)
VALUES
    -- Đánh giá Booking 1
    ('ffffffff-ffff-4fff-9fff-ffffffff0001', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0001', '22222222-2222-4222-8222-222222220001', '44444401-0000-0000-0000-000000000001', '11111111-1111-4111-8111-111111110001', 5, 'Mentor cực kỳ nhiệt tình, hướng dẫn Next.js App Router rất dễ hiểu và thực chiến!', 1, '2026-05-23 10:15:00', '2026-05-23 10:15:00'),
    -- Đánh giá Booking 3
    ('ffffffff-ffff-4fff-9fff-ffffffff0002', 'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0003', '22222222-2222-4222-8222-222222220002', '44444402-0000-0000-0000-000000000001', '11111111-1111-4111-8111-111111110003', 4, 'Gói học có lộ trình rõ ràng, mentor giúp mình hiểu sâu về PM.', 1, '2026-05-21 12:00:00', '2026-05-21 12:00:00')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- PRE-COMPUTED STATS (THỐNG KÊ SẴN)
-- ==========================================
INSERT INTO user_stats (user_id, total_orders, total_spent, total_sessions, completed_sessions, updated_at)
VALUES
    ('11111111-1111-4111-8111-111111110001', 1, 1500000.00, 3, 3, NOW()),
    ('11111111-1111-4111-8111-111111110002', 1, 2500000.00, 3, 1, NOW()),
    ('11111111-1111-4111-8111-111111110003', 1, 2000000.00, 3, 3, NOW())
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO mentor_stats (mentor_id, total_students, total_sessions, total_revenue, rating_avg, updated_at)
VALUES
    ('22222222-2222-4222-8222-222222220001', 2, 6, 4000000.00, 4.8, NOW()),
    ('22222222-2222-4222-8222-222222220002', 1, 3, 2000000.00, 4.9, NOW())
ON CONFLICT (mentor_id) DO NOTHING;

INSERT INTO service_stats (package_id, total_orders, total_revenue, updated_at)
VALUES
    ('44444401-0000-0000-0000-000000000001', 1, 1500000.00, NOW()),
    ('44444401-0000-0000-0000-000000000002', 1, 2500000.00, NOW())
ON CONFLICT (package_id) DO NOTHING;

INSERT INTO system_stats (date, new_users, active_users, total_orders, successful_orders, failed_orders, revenue, total_sessions, completed_sessions, new_mentors, created_at)
VALUES
    ('2026-05-25', 30, 8, 8, 5, 1, 9400000.00, 15, 11, 10, NOW())
ON CONFLICT (date) DO NOTHING;

-- ==========================================
-- MENTOR APPLICATIONS (đơn đăng ký mentor)
-- ==========================================
INSERT INTO mentor_requests (id, user_id, status, headline, bio, expertise, years_of_experience, hourly_rate, portfolio_urls, certificates, reason, note, reviewed_by, reviewed_at, resubmit_count, created_at, updated_at)
VALUES
    ('dddddddd-dddd-4ddd-addd-dddddddd0001', '11111111-1111-4111-8111-111111110006', 'SUBMITTED',
     'Fresher Flutter muốn mentor mobile',
     'Đã hoàn thành 2 app Flutter cá nhân, muốn hướng dẫn sinh viên.',
     '["Flutter","Dart","Mobile"]', 2, 800000.00, '["https://github.com/tuankiet-demo"]', '[]',
     NULL, NULL, NULL, NULL, 0, '2026-05-24 08:00:00', '2026-05-24 08:00:00'),
    ('dddddddd-dddd-4ddd-addd-dddddddd0002', '11111111-1111-4111-8111-111111110009', 'SUBMITTED',
     'AI/ML intern chuyển sang mentor',
     'Từng thực tập ML tại lab đại học, có kinh nghiệm PyTorch cơ bản.',
     '["PyTorch","Python","Machine Learning"]', 1, 900000.00, '[]', '[]',
     NULL, NULL, NULL, NULL, 0, '2026-05-25 10:00:00', '2026-05-25 10:00:00'),
    ('dddddddd-dddd-4ddd-addd-dddddddd0003', '11111111-1111-4111-8111-111111110004', 'REJECTED',
     'Designer portfolio chưa đủ',
     'Portfolio còn mỏng, cần bổ sung case study.',
     '["Figma","UI/UX"]', 1, 700000.00, '[]', '[]',
     'Hồ sơ chưa đủ minh chứng năng lực mentor.', 'Vui lòng bổ sung 3 case study trước khi nộp lại.',
     '33333333-3333-4333-8333-333333330005', '2026-05-22 14:00:00', 1, '2026-05-20 09:00:00', '2026-05-22 14:00:00')
ON CONFLICT (id) DO NOTHING;

-- ==========================================
-- MODERATION REPORTS & DISPUTES
-- ==========================================
INSERT INTO reports (id, reporter_id, reported_user_id, type, entity_id, reason, description, status, created_at)
VALUES
    ('99999999-9999-4999-a999-999999990001', '11111111-1111-4111-8111-111111110002', '22222222-2222-4222-8222-222222220001',
     'mentor', '22222222-2222-4222-8222-222222220001', 'Hành vi không phù hợp',
     'Mentor trả lời chậm và thiếu chuyên nghiệp trong buổi học thử.', 'open', '2026-05-23 11:00:00'),
    ('99999999-9999-4999-a999-999999990002', '11111111-1111-4111-8111-111111110005', NULL,
     'review', 'ffffffff-ffff-4fff-9fff-ffffffff0001', 'Spam / nội dung không liên quan',
     'Đánh giá có dấu hiệu spam link ngoài.', 'under_review', '2026-05-24 09:30:00'),
    ('99999999-9999-4999-a999-999999990003', '11111111-1111-4111-8111-111111110001', '22222222-2222-4222-8222-222222220003',
     'session', 'bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0501', 'Tranh chấp sau buổi học',
     'Học viên cho rằng mentor kết thúc sớm 15 phút so với lịch.', 'open', '2026-05-25 16:00:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO disputes (id, report_id, booking_id, session_id, raised_by, reason, description, status, created_at)
VALUES
    ('88888888-8888-4888-b888-888888880099', '99999999-9999-4999-a999-999999990003',
     'aaaaaaaa-aaaa-4aaa-caaa-aaaaaaaa0005', 'bbbbbbbb-bbbb-4bbb-dbbb-bbbbbbbb0501',
     '11111111-1111-4111-8111-111111110001', 'Kết thúc buổi sớm',
     'Buổi học kết thúc lúc 16:45 trong khi lịch đến 17:00.', 'open', '2026-05-25 16:05:00')
ON CONFLICT (id) DO NOTHING;

