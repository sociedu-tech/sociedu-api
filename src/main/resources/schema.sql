-- Chạy SAU khi Hibernate đã tạo schema (vì spring.jpa.defer-datasource-initialization=true).
-- Chỉ chứa thứ Hibernate không tự sinh: extension Postgres.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
