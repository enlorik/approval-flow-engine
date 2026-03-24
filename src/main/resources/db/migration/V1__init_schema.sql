-- Create roles table
CREATE TABLE roles (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Create users table
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create user_roles join table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create approval_requests table
CREATE TABLE approval_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    request_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    requester_id BIGINT NOT NULL,
    current_step_order INT DEFAULT 0,
    due_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES users(id)
);

-- Create approval_steps table
CREATE TABLE approval_steps (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    approval_request_id BIGINT NOT NULL,
    step_order INT NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    due_date DATE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id) ON DELETE CASCADE
);

-- Create approver_assignments table
CREATE TABLE approver_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    approval_step_id BIGINT NOT NULL,
    assigned_user_id BIGINT,
    assigned_role_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (approval_step_id) REFERENCES approval_steps(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Create approval_decisions table
CREATE TABLE approval_decisions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    approval_step_id BIGINT NOT NULL,
    decided_by BIGINT NOT NULL,
    decision_type VARCHAR(50) NOT NULL,
    comment VARCHAR(2000),
    decided_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (approval_step_id) REFERENCES approval_steps(id) ON DELETE CASCADE,
    FOREIGN KEY (decided_by) REFERENCES users(id)
);

-- Create comments table
CREATE TABLE comments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    approval_request_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (approval_request_id) REFERENCES approval_requests(id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(id)
);

-- Create notification_logs table
CREATE TABLE notification_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    subject VARCHAR(500),
    body VARCHAR(2000),
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recipient_id) REFERENCES users(id)
);

-- Create audit_logs table
CREATE TABLE audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    performed_by BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    details VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (performed_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Insert default roles
INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_ADMIN', 'System administrator'),
(2, 'ROLE_MANAGER', 'Team manager'),
(3, 'ROLE_APPROVER', 'Request approver'),
(4, 'ROLE_USER', 'Regular user');

-- Create indexes for better query performance
CREATE INDEX idx_approval_requests_status ON approval_requests(status);
CREATE INDEX idx_approval_requests_requester ON approval_requests(requester_id);
CREATE INDEX idx_approval_requests_type ON approval_requests(request_type);
CREATE INDEX idx_approval_steps_status ON approval_steps(status);
CREATE INDEX idx_approval_steps_due_date ON approval_steps(due_date);
CREATE INDEX idx_approval_steps_request ON approval_steps(approval_request_id);
CREATE INDEX idx_approver_assignments_user ON approver_assignments(assigned_user_id);
CREATE INDEX idx_approver_assignments_role ON approver_assignments(assigned_role_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_comments_request ON comments(approval_request_id);
