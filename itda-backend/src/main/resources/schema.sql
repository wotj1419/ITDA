-- itda-backend MVP 데이터베이스 스키마
-- MySQL 8.0+

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS itda
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE itda_dev;

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(500),
    role ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 향후 확장 테이블 (스키마 확정 후 추가 예정)
-- CREATE TABLE projects (...);
-- CREATE TABLE scenes (...);
-- CREATE TABLE nodes (...);
-- CREATE TABLE objects (...);
-- CREATE TABLE timelines (...);
