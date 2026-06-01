-- Chạy SAU Hibernate (spring.jpa.defer-datasource-initialization=true).
-- Bổ sung extension + bảng roles khi DB remote thiếu bảng mới dù schema cũ chưa đồng bộ.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS roles
(
    id   UUID         NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

