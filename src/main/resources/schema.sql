-- Chạy SAU Hibernate (spring.jpa.defer-datasource-initialization=true).
-- Bổ sung extension + bảng RBAC khi DB remote thiếu bảng mới (vd capabilities) dù roles đã có từ schema cũ.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS roles
(
    id   UUID         NOT NULL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uk_roles_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS capabilities
(
    id   UUID         NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_capabilities_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS role_capabilities
(
    role_id       UUID NOT NULL,
    capability_id UUID NOT NULL,
    PRIMARY KEY (role_id, capability_id),
    CONSTRAINT fk_role_capabilities_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_capabilities_capability FOREIGN KEY (capability_id) REFERENCES capabilities (id) ON DELETE CASCADE
);
