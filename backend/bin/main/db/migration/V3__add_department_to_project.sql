ALTER TABLE project ADD COLUMN department_id BIGINT REFERENCES department(id);
