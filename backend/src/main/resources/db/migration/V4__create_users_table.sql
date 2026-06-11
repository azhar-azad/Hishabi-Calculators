-- V4: users table for Phase 5 auth.
-- email stored lowercase (normalised in User.@PrePersist); UNIQUE enforces case-insensitivity.
-- password_hash stores BCrypt output (always 60 chars; min-length CHECK added with BCrypt in 5.2).
CREATE TABLE users (
                       id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       email         VARCHAR(255)             NOT NULL,
                       password_hash VARCHAR(255)             NOT NULL,
                       created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                       CONSTRAINT uq_users_email UNIQUE (email)
);