-- Ocean 프로젝트 데이터베이스 구현
-- 작성일: 2024-01-20
-- 데이터베이스: MySQL 8.0

-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS ocean_db
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE ocean_db;

-- =====================================================
-- 1. 사용자 관리 테이블
-- =====================================================

-- 사용자 기본 정보 테이블
CREATE TABLE users (
    user_code VARCHAR(100) PRIMARY KEY,
    user_id VARCHAR(100) UNIQUE NOT NULL COMMENT '소셜 로그인 ID',
    user_name VARCHAR(100) NOT NULL COMMENT '실명',
    nickname VARCHAR(50) COMMENT '서비스 내 닉네임',
    email VARCHAR(255),
    user_profile_img VARCHAR(500),
    provider VARCHAR(20) NOT NULL COMMENT 'google, kakao',
    phone_number VARCHAR(20),
    department VARCHAR(100),
    position VARCHAR(100),
    is_profile_complete BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_provider_user (provider, user_id),
    INDEX idx_nickname (nickname),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- OAuth2 토큰 관리
CREATE TABLE user_tokens (
    user_code VARCHAR(100) PRIMARY KEY,
    access_token VARCHAR(500),
    refresh_token VARCHAR(500),
    token_expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_code) REFERENCES users(user_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Refresh 토큰 관리 (JWT용)
CREATE TABLE refresh_tokens (
    token_id VARCHAR(100) PRIMARY KEY,
    user_code VARCHAR(100) NOT NULL,
    refresh_token VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_code) REFERENCES users(user_code) ON DELETE CASCADE,
    INDEX idx_token (refresh_token),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 2. 워크스페이스 관리 테이블
-- =====================================================

-- 워크스페이스 테이블
CREATE TABLE workspaces (
    workspace_code VARCHAR(100) PRIMARY KEY,
    workspace_name VARCHAR(200) NOT NULL,
    workspace_description TEXT,
    workspace_img VARCHAR(500),
    workspace_color VARCHAR(20),
    invite_code VARCHAR(50) UNIQUE,
    max_members INT DEFAULT 50,
    is_active BOOLEAN DEFAULT TRUE,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(user_code),
    INDEX idx_invite_code (invite_code),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 워크스페이스 참여자 테이블
CREATE TABLE workspace_members (
    workspace_code VARCHAR(100),
    user_code VARCHAR(100),
    user_nickname VARCHAR(50) COMMENT '워크스페이스별 닉네임',
    user_role ENUM('OWNER', 'ADMIN', 'MEMBER') DEFAULT 'MEMBER',
    status_message VARCHAR(200),
    is_active BOOLEAN DEFAULT TRUE,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_activity_at TIMESTAMP NULL,
    PRIMARY KEY (workspace_code, user_code),
    FOREIGN KEY (workspace_code) REFERENCES workspaces(workspace_code) ON DELETE CASCADE,
    FOREIGN KEY (user_code) REFERENCES users(user_code) ON DELETE CASCADE,
    INDEX idx_user (user_code),
    INDEX idx_role (user_role),
    INDEX idx_joined (joined_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 워크스페이스 초대 테이블
CREATE TABLE workspace_invitations (
    invite_id VARCHAR(100) PRIMARY KEY,
    workspace_code VARCHAR(100) NOT NULL,
    invite_code VARCHAR(50) NOT NULL,
    invited_by VARCHAR(100) NOT NULL,
    invited_email VARCHAR(255),
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED') DEFAULT 'PENDING',
    expired_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    FOREIGN KEY (workspace_code) REFERENCES workspaces(workspace_code) ON DELETE CASCADE,
    FOREIGN KEY (invited_by) REFERENCES users(user_code),
    INDEX idx_invite_code (invite_code),
    INDEX idx_status (status),
    INDEX idx_expired (expired_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 3. 캘린더 및 일정 관리 테이블
-- =====================================================

-- 캘린더 이벤트 테이블
CREATE TABLE calendar_events (
    event_id VARCHAR(100) PRIMARY KEY,
    user_code VARCHAR(100) NOT NULL,
    workspace_code VARCHAR(100),
    title VARCHAR(300) NOT NULL,
    description TEXT,
    start_datetime DATETIME NOT NULL,
    end_datetime DATETIME NOT NULL,
    location VARCHAR(500),
    color VARCHAR(20),
    is_all_day BOOLEAN DEFAULT FALSE,
    is_shared BOOLEAN DEFAULT FALSE,
    shared_workspace_code VARCHAR(100),
    google_event_id VARCHAR(255),
    original_event_id VARCHAR(100),
    event_type ENUM('PERSONAL', 'WORKSPACE', 'SHARED') NOT NULL,
    status ENUM('CONFIRMED', 'TENTATIVE', 'CANCELLED') DEFAULT 'CONFIRMED',
    recurrence_rule JSON,
    reminder_minutes INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_code) REFERENCES users(user_code),
    FOREIGN KEY (workspace_code) REFERENCES workspaces(workspace_code),
    FOREIGN KEY (shared_workspace_code) REFERENCES workspaces(workspace_code),
    INDEX idx_user_date (user_code, start_datetime),
    INDEX idx_workspace_date (workspace_code, start_datetime),
    INDEX idx_google_event (google_event_id),
    INDEX idx_type_status (event_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 이벤트 참석자 테이블
CREATE TABLE event_attendees (
    event_id VARCHAR(100),
    user_code VARCHAR(100),
    response_status ENUM('ACCEPTED', 'DECLINED', 'TENTATIVE', 'NEEDS_ACTION') DEFAULT 'NEEDS_ACTION',
    is_organizer BOOLEAN DEFAULT FALSE,
    is_optional BOOLEAN DEFAULT FALSE,
    responded_at TIMESTAMP NULL,
    PRIMARY KEY (event_id, user_code),
    FOREIGN KEY (event_id) REFERENCES calendar_events(event_id) ON DELETE CASCADE,
    FOREIGN KEY (user_code) REFERENCES users(user_code),
    INDEX idx_user_response (user_code, response_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 구글 캘린더 동기화 정보
CREATE TABLE user_calendar_sync (
    user_code VARCHAR(100) PRIMARY KEY,
    google_calendar_id VARCHAR(255),
    google_refresh_token VARCHAR(500),
    sync_enabled BOOLEAN DEFAULT TRUE,
    last_sync_at TIMESTAMP NULL,
    sync_error_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_code) REFERENCES users(user_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 4. 회의 관리 테이블
-- =====================================================

-- 회의실 테이블
CREATE TABLE meeting_rooms (
    room_code VARCHAR(100) PRIMARY KEY,
    room_name VARCHAR(200) NOT NULL,
    description TEXT,
    workspace_code VARCHAR(100) NOT NULL,
    host_user_code VARCHAR(100) NOT NULL,
    status ENUM('WAITING', 'IN_PROGRESS', 'ENDED') DEFAULT 'WAITING',
    max_participants INT DEFAULT 10,
    is_recording_enabled BOOLEAN DEFAULT FALSE,
    scheduled_start_time TIMESTAMP NULL,
    scheduled_end_time TIMESTAMP NULL,
    actual_start_time TIMESTAMP NULL,
    actual_end_time TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (workspace_code) REFERENCES workspaces(workspace_code),
    FOREIGN KEY (host_user_code) REFERENCES users(user_code),
    INDEX idx_workspace_status (workspace_code, status),
    INDEX idx_scheduled (scheduled_start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 회의 참여자 테이블
CREATE TABLE meeting_participants (
    room_code VARCHAR(100),
    user_code VARCHAR(100),
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    role ENUM('HOST', 'PRESENTER', 'PARTICIPANT') DEFAULT 'PARTICIPANT',
    PRIMARY KEY (room_code, user_code),
    FOREIGN KEY (room_code) REFERENCES meeting_rooms(room_code) ON DELETE CASCADE,
    FOREIGN KEY (user_code) REFERENCES users(user_code),
    INDEX idx_active (room_code, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 회의 문서 테이블
CREATE TABLE meeting_documents (
    document_id VARCHAR(100) PRIMARY KEY,
    room_code VARCHAR(100) NOT NULL,
    file_name VARCHAR(300) NOT NULL,
    file_type ENUM('PDF', 'PPT', 'PPTX') NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    total_pages INT,
    conversion_status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    uploaded_by VARCHAR(100) NOT NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    converted_at TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (room_code) REFERENCES meeting_rooms(room_code) ON DELETE CASCADE,
    FOREIGN KEY (uploaded_by) REFERENCES users(user_code),
    INDEX idx_room_active (room_code, is_active),
    INDEX idx_conversion (conversion_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 문서 페이지 테이블
CREATE TABLE document_pages (
    page_id VARCHAR(100) PRIMARY KEY,
    document_id VARCHAR(100) NOT NULL,
    page_number INT NOT NULL,
    image_path VARCHAR(1000) NOT NULL,
    thumbnail_path VARCHAR(1000),
    width INT,
    height INT,
    UNIQUE KEY unique_doc_page (document_id, page_number),
    FOREIGN KEY (document_id) REFERENCES meeting_documents(document_id) ON DELETE CASCADE,
    INDEX idx_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 스케치 저장 테이블
CREATE TABLE meeting_sketches (
    sketch_id VARCHAR(100) PRIMARY KEY,
    room_code VARCHAR(100) NOT NULL,
    save_name VARCHAR(200),
    description TEXT,
    sketch_data JSON,
    thumbnail_path VARCHAR(1000),
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_code) REFERENCES meeting_rooms(room_code),
    FOREIGN KEY (created_by) REFERENCES users(user_code),
    INDEX idx_room (room_code),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 5. 파일 관리 테이블
-- =====================================================

-- 파일 정보 테이블
CREATE TABLE file_info (
    file_code VARCHAR(100) PRIMARY KEY,
    file_name VARCHAR(300) NOT NULL,
    original_file_name VARCHAR(300) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100),
    file_extension VARCHAR(20),
    event_id VARCHAR(100),
    upload_type ENUM('CALENDAR_PERSONAL', 'CALENDAR_WORKSPACE', 'BOARD', 'TASK', 'MEETING') NOT NULL,
    uploader_code VARCHAR(100) NOT NULL,
    download_count INT DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP NULL,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP NULL,
    FOREIGN KEY (event_id) REFERENCES calendar_events(event_id) ON DELETE SET NULL,
    FOREIGN KEY (uploader_code) REFERENCES users(user_code),
    INDEX idx_event (event_id),
    INDEX idx_upload_type (upload_type),
    INDEX idx_uploader (uploader_code),
    INDEX idx_uploaded (uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 파일 미리보기 정보 테이블
CREATE TABLE file_previews (
    file_code VARCHAR(100) PRIMARY KEY,
    thumbnail_path VARCHAR(1000),
    preview_path VARCHAR(1000),
    width INT,
    height INT,
    page_count INT,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (file_code) REFERENCES file_info(file_code) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 6. 지도 및 약속 장소 테이블
-- =====================================================

-- 장소 정보 테이블
CREATE TABLE places (
    place_code VARCHAR(100) PRIMARY KEY,
    place_name VARCHAR(200) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    address VARCHAR(500),
    road_address VARCHAR(500),
    kakao_place_id VARCHAR(50),
    category VARCHAR(100),
    phone VARCHAR(20),
    place_url VARCHAR(500),
    place_type ENUM('CUSTOM', 'KAKAO') DEFAULT 'CUSTOM',
    workspace_code VARCHAR(100),
    is_public BOOLEAN DEFAULT FALSE,
    tags JSON,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (workspace_code) REFERENCES workspaces(workspace_code),
    FOREIGN KEY (created_by) REFERENCES users(user_code),
    INDEX idx_kakao_place (kakao_place_id),
    INDEX idx_workspace (workspace_code),
    INDEX idx_coordinates (latitude, longitude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 약속 테이블
CREATE TABLE appointments (
    appointment_id VARCHAR(100) PRIMARY KEY,
    appointment_name VARCHAR(200) NOT NULL,
    place_code VARCHAR(100),
    workspace_code VARCHAR(100),
    event_id VARCHAR(100),
    appointment_time DATETIME NOT NULL,
    description TEXT,
    status ENUM('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED') DEFAULT 'SCHEDULED',
    reminder_minutes INT DEFAULT 30,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (place_code) REFERENCES places(place_code),
    FOREIGN KEY (workspace_code) REFERENCES workspaces(workspace_code),
    FOREIGN KEY (event_id) REFERENCES calendar_events(event_id),
    FOREIGN KEY (created_by) REFERENCES users(user_code),
    INDEX idx_appointment_time (appointment_time),
    INDEX idx_status (status),
    INDEX idx_workspace (workspace_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 약속 참여자 테이블
CREATE TABLE appointment_participants (
    appointment_id VARCHAR(100),
    user_code VARCHAR(100),
    participation_status ENUM('INVITED', 'ACCEPTED', 'DECLINED', 'MAYBE') DEFAULT 'INVITED',
    current_latitude DOUBLE,
    current_longitude DOUBLE,
    location_updated_at TIMESTAMP NULL,
    estimated_arrival_time DATETIME,
    transportation_mode VARCHAR(50),
    is_arrived BOOLEAN DEFAULT FALSE,
    arrived_at TIMESTAMP NULL,
    PRIMARY KEY (appointment_id, user_code),
    FOREIGN KEY (appointment_id) REFERENCES appointments(appointment_id) ON DELETE CASCADE,
    FOREIGN KEY (user_code) REFERENCES users(user_code),
    INDEX idx_status (participation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 사용자 위치 기록 테이블
CREATE TABLE user_locations (
    location_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_code VARCHAR(100) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    accuracy DOUBLE,
    address VARCHAR(500),
    location_type ENUM('CURRENT', 'HOME', 'WORK', 'FAVORITE') DEFAULT 'CURRENT',
    location_name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_code) REFERENCES users(user_code),
    INDEX idx_user_type (user_code, location_type),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 7. 주요 뷰(View) 생성
-- =====================================================

-- 활성 워크스페이스 멤버 뷰
CREATE VIEW v_active_workspace_members AS
SELECT
    wm.workspace_code,
    wm.user_code,
    u.user_name,
    u.user_profile_img,
    wm.user_nickname,
    wm.user_role,
    wm.status_message,
    wm.joined_at,
    wm.last_activity_at
FROM workspace_members wm
JOIN users u ON wm.user_code = u.user_code
WHERE wm.is_active = TRUE AND u.is_active = TRUE;

-- 이번 주 일정 뷰
CREATE VIEW v_this_week_events AS
SELECT
    ce.event_id,
    ce.user_code,
    ce.workspace_code,
    ce.title,
    ce.start_datetime,
    ce.end_datetime,
    ce.location,
    ce.event_type,
    u.user_name,
    u.user_profile_img
FROM calendar_events ce
JOIN users u ON ce.user_code = u.user_code
WHERE ce.status = 'CONFIRMED'
    AND ce.start_datetime >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
    AND ce.start_datetime < DATE_ADD(DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY), INTERVAL 7 DAY);

-- =====================================================
-- 8. 주요 쿼리 예시
-- =====================================================

-- 사용자의 워크스페이스 목록 조회
DELIMITER $$
CREATE PROCEDURE sp_get_user_workspaces(IN p_user_code VARCHAR(100))
BEGIN
    SELECT
        w.workspace_code,
        w.workspace_name,
        w.workspace_description,
        w.workspace_img,
        w.workspace_color,
        wm.user_role,
        wm.joined_at,
        (SELECT COUNT(*) FROM workspace_members WHERE workspace_code = w.workspace_code AND is_active = TRUE) as member_count,
        (SELECT MAX(created_at) FROM calendar_events WHERE workspace_code = w.workspace_code) as last_activity
    FROM workspaces w
    JOIN workspace_members wm ON w.workspace_code = wm.workspace_code
    WHERE wm.user_code = p_user_code
        AND wm.is_active = TRUE
        AND w.is_active = TRUE
    ORDER BY wm.joined_at DESC;
END$$
DELIMITER ;

-- 개인 일정 목록 조회
DELIMITER $$
CREATE PROCEDURE sp_get_personal_events(
    IN p_user_code VARCHAR(100),
    IN p_start_date DATE,
    IN p_end_date DATE
)
BEGIN
    SELECT
        ce.event_id,
        ce.title,
        ce.description,
        ce.start_datetime,
        ce.end_datetime,
        ce.location,
        ce.color,
        ce.is_all_day,
        ce.is_shared,
        ce.shared_workspace_code,
        ce.google_event_id,
        ce.status,
        (SELECT COUNT(*) FROM file_info WHERE event_id = ce.event_id AND is_deleted = FALSE) as file_count
    FROM calendar_events ce
    WHERE ce.user_code = p_user_code
        AND ce.event_type = 'PERSONAL'
        AND ce.status != 'CANCELLED'
        AND DATE(ce.start_datetime) <= p_end_date
        AND DATE(ce.end_datetime) >= p_start_date
    ORDER BY ce.start_datetime ASC;
END$$
DELIMITER ;

-- 워크스페이스 일정 목록 조회
DELIMITER $$
CREATE PROCEDURE sp_get_workspace_events(
    IN p_workspace_code VARCHAR(100),
    IN p_start_date DATE,
    IN p_end_date DATE,
    IN p_user_code VARCHAR(100)
)
BEGIN
    SELECT
        ce.event_id,
        ce.title,
        ce.description,
        ce.start_datetime,
        ce.end_datetime,
        ce.location,
        ce.color,
        ce.is_all_day,
        ce.event_type,
        ce.status,
        u.user_code as creator_code,
        u.user_name as creator_name,
        u.user_profile_img as creator_img,
        ea.response_status as my_response,
        (SELECT COUNT(*) FROM event_attendees WHERE event_id = ce.event_id) as attendee_count,
        (SELECT COUNT(*) FROM file_info WHERE event_id = ce.event_id AND is_deleted = FALSE) as file_count
    FROM calendar_events ce
    JOIN users u ON ce.user_code = u.user_code
    LEFT JOIN event_attendees ea ON ce.event_id = ea.event_id AND ea.user_code = p_user_code
    WHERE ce.workspace_code = p_workspace_code
        AND ce.status != 'CANCELLED'
        AND DATE(ce.start_datetime) <= p_end_date
        AND DATE(ce.end_datetime) >= p_start_date
    ORDER BY ce.start_datetime ASC;
END$$
DELIMITER ;

-- 회의실 생성
DELIMITER $$
CREATE PROCEDURE sp_create_meeting_room(
    IN p_room_name VARCHAR(200),
    IN p_description TEXT,
    IN p_workspace_code VARCHAR(100),
    IN p_host_user_code VARCHAR(100),
    IN p_max_participants INT,
    IN p_scheduled_start TIMESTAMP,
    IN p_scheduled_end TIMESTAMP,
    OUT p_room_code VARCHAR(100)
)
BEGIN
    SET p_room_code = CONCAT('ROOM_', UUID());

    INSERT INTO meeting_rooms (
        room_code, room_name, description, workspace_code,
        host_user_code, max_participants, scheduled_start_time, scheduled_end_time
    ) VALUES (
        p_room_code, p_room_name, p_description, p_workspace_code,
        p_host_user_code, p_max_participants, p_scheduled_start, p_scheduled_end
    );

    -- 호스트를 참여자로 추가
    INSERT INTO meeting_participants (room_code, user_code, role)
    VALUES (p_room_code, p_host_user_code, 'HOST');
END$$
DELIMITER ;

-- 중간 지점 계산을 위한 참여자 위치 조회
DELIMITER $$
CREATE PROCEDURE sp_get_participant_locations(IN p_appointment_id VARCHAR(100))
BEGIN
    SELECT
        ap.user_code,
        u.user_name,
        ap.current_latitude,
        ap.current_longitude,
        ap.location_updated_at,
        ap.transportation_mode,
        ap.estimated_arrival_time
    FROM appointment_participants ap
    JOIN users u ON ap.user_code = u.user_code
    WHERE ap.appointment_id = p_appointment_id
        AND ap.participation_status IN ('ACCEPTED', 'MAYBE')
        AND ap.current_latitude IS NOT NULL
        AND ap.current_longitude IS NOT NULL
    ORDER BY ap.location_updated_at DESC;
END$$
DELIMITER ;

-- =====================================================
-- 9. 인덱스 최적화
-- =====================================================

-- 자주 사용되는 조인 및 검색을 위한 추가 인덱스
CREATE INDEX idx_events_user_start ON calendar_events(user_code, start_datetime);
CREATE INDEX idx_events_workspace_start ON calendar_events(workspace_code, start_datetime);
CREATE INDEX idx_members_workspace_active ON workspace_members(workspace_code, is_active);
CREATE INDEX idx_files_event_active ON file_info(event_id, is_deleted);
CREATE INDEX idx_rooms_workspace_status ON meeting_rooms(workspace_code, status);

-- =====================================================
-- 10. 초기 데이터 삽입
-- =====================================================

-- 시스템 사용자 (선택사항)
INSERT INTO users (user_code, user_id, user_name, provider)
VALUES ('SYSTEM', 'system', 'System', 'system');

-- =====================================================
-- 11. 권한 설정
-- =====================================================

-- 애플리케이션 사용자 생성 및 권한 부여
-- CREATE USER 'ocean_app'@'%' IDENTIFIED BY 'your_secure_password';
-- GRANT SELECT, INSERT, UPDATE, DELETE, EXECUTE ON ocean_db.* TO 'ocean_app'@'%';
-- FLUSH PRIVILEGES;