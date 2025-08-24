- Complete Database Schema for Planty Application
-- MySQL Workbench에서 실행

USE Centralthon;

-- 1. User 테이블 (Spring Boot 엔티티와 일치)
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL UNIQUE,
    point INT NOT NULL DEFAULT 0,
    profile_img VARCHAR(500),
    block_user_id INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. BlockUser 테이블
CREATE TABLE block_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    block_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (block_id) REFERENCES users(id),
    UNIQUE KEY unique_block (user_id, block_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Crop 테이블 (추가 컬럼 포함, Spring Boot 엔티티와 일치)
CREATE TABLE crop (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    name VARCHAR(100) NOT NULL,
    crop_img VARCHAR(500), -- NULL 허용
    start_at DATE NOT NULL,
    end_at DATE NOT NULL,
    environment VARCHAR(500), -- NULL 허용
    temperature VARCHAR(100), -- NULL 허용
    height VARCHAR(100), -- NULL 허용
    how_to TEXT, -- NULL 허용
    analysis_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 추가된 컬럼
    is_registered BOOLEAN NOT NULL DEFAULT FALSE, -- 추가된 컬럼
    harvest BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. CropCategory 테이블
CREATE TABLE crop_categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    crop_id INT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (crop_id) REFERENCES crop(id),
    UNIQUE KEY unique_crop_category (crop_id, category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Diary 테이블
CREATE TABLE diary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    crop_id INT NOT NULL,
    analysis TEXT,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (crop_id) REFERENCES crop(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. DiaryImage 테이블
CREATE TABLE diary_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    diary_id INT NOT NULL,
    diary_img VARCHAR(500) NOT NULL,
    created_at DATE,
    thumbnail BOOLEAN,
    FOREIGN KEY (diary_id) REFERENCES diary(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Chat 테이블
CREATE TABLE chat (
    id INT AUTO_INCREMENT PRIMARY KEY,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. ChatUser 테이블
CREATE TABLE chat_users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    chat_id INT NOT NULL,
    user_id INT NOT NULL,
    FOREIGN KEY (chat_id) REFERENCES chat(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE KEY unique_chat_user (chat_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9. ChatMessage 테이블
CREATE TABLE chat_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    chat_id INT NOT NULL,
    sender_id INT NOT NULL,
    content VARCHAR(1000),
    chat_img VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (chat_id) REFERENCES chat(id),
    FOREIGN KEY (sender_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 10. Board 테이블
CREATE TABLE board (
    id INT AUTO_INCREMENT PRIMARY KEY,
    crop_id INT NOT NULL,
    seller_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    price INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sell BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (crop_id) REFERENCES crop(id),
    FOREIGN KEY (seller_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 11. BoardImage 테이블
CREATE TABLE board_images (
    id INT AUTO_INCREMENT PRIMARY KEY,
    board_id INT NOT NULL,
    board_img VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    thumbnail BOOLEAN,
    FOREIGN KEY (board_id) REFERENCES board(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 12. AiChat 테이블
CREATE TABLE ai_chats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 13. AiMessage 테이블
CREATE TABLE ai_messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ai_chat_id INT NOT NULL,
    content VARCHAR(2000) NOT NULL,
    sender VARCHAR(10) NOT NULL COMMENT 'ENUM: user, ai',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ai_image VARCHAR(500),
    FOREIGN KEY (ai_chat_id) REFERENCES ai_chats(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 14. AiCategory 테이블
CREATE TABLE ai_categories (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ai_message_id INT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    FOREIGN KEY (ai_message_id) REFERENCES ai_messages(id),
    UNIQUE KEY unique_ai_category (ai_message_id, category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 15. Users 테이블의 block_user_id 외래키 제약 추가
ALTER TABLE users ADD CONSTRAINT fk_users_blockusers 
FOREIGN KEY (block_user_id) REFERENCES block_users(id);

-- 16. 테이블 구조 확인
SHOW TABLES;







