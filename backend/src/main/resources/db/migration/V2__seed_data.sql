-- Seed Departments
INSERT INTO department (name) VALUES ('研發部');
INSERT INTO department (name) VALUES ('產品部');
INSERT INTO department (name) VALUES ('人力資源部');

-- Seed Users (password: Welcome123)
-- BCrypt hash: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (name, email, password_hash, password_changed, department_id, active)
VALUES ('管理層Admin', 'admin@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', false, 1, true);

INSERT INTO users (name, email, password_hash, password_changed, department_id, active)
VALUES ('專案經理PM', 'pm@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', false, 1, true);

INSERT INTO users (name, email, password_hash, password_changed, department_id, active)
VALUES ('部門主管Manager', 'manager@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', false, 1, true);

INSERT INTO users (name, email, password_hash, password_changed, department_id, active)
VALUES ('執行人員Executor', 'executor@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', false, 1, true);

INSERT INTO users (name, email, password_hash, password_changed, department_id, active)
VALUES ('人資HR', 'hr@company.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', false, 3, true);

-- Seed User Roles
INSERT INTO user_roles (user_id, role) VALUES (1, 'ADMIN');
INSERT INTO user_roles (user_id, role) VALUES (2, 'PM');
INSERT INTO user_roles (user_id, role) VALUES (3, 'DEPT_MANAGER');
INSERT INTO user_roles (user_id, role) VALUES (4, 'EXECUTOR');
INSERT INTO user_roles (user_id, role) VALUES (5, 'HR');
