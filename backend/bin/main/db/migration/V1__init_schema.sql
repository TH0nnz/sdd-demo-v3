-- Department
CREATE TABLE department (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- User (users to avoid reserved word)
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    password_changed BOOLEAN NOT NULL DEFAULT false,
    department_id BIGINT NOT NULL REFERENCES department(id),
    active BOOLEAN NOT NULL DEFAULT true,
    failed_login_count INT NOT NULL DEFAULT 0,
    last_failed_login TIMESTAMP,
    locked_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id)
);
CREATE UNIQUE INDEX idx_user_email ON users(email);

-- User Roles
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- Project
CREATE TABLE project (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    total_budget_hours DECIMAL(10,1) NOT NULL,
    consumed_hours DECIMAL(10,1) NOT NULL DEFAULT 0,
    pm_id BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    closed_at TIMESTAMP,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0
);

-- Task
CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    project_id BIGINT NOT NULL REFERENCES project(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    budget_hours DECIMAL(10,1) NOT NULL,
    consumed_hours DECIMAL(10,1) NOT NULL DEFAULT 0,
    assignee_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_task_project_status ON task(project_id, status);

-- Work Entry
CREATE TABLE work_entry (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    task_id BIGINT NOT NULL REFERENCES task(id),
    work_date DATE NOT NULL,
    hours DECIMAL(3,1) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_workentry_task_date ON work_entry(task_id, work_date);
CREATE INDEX idx_workentry_user_date ON work_entry(user_id, work_date);

-- Hours Request
CREATE TABLE hours_request (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(id),
    requester_id BIGINT NOT NULL REFERENCES users(id),
    requested_hours DECIMAL(10,1) NOT NULL,
    description TEXT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_task_id BIGINT REFERENCES task(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT REFERENCES users(id),
    review_comment TEXT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_hoursreq_project_status ON hours_request(project_id, status);

-- Audit Log (append-only)
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    action_type VARCHAR(50) NOT NULL,
    actor_id BIGINT NOT NULL REFERENCES users(id),
    target_entity VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_auditlog_created_at ON audit_log(created_at);

-- Notification
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_notification_user_read ON notification(user_id, is_read);
