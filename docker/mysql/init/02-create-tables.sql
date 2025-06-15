-- 파일 최상단에 추가
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- Ocean MVP 데이터베이스 스키마
-- Docker-compose MySQL 초기화 스크립트

CREATE DATABASE IF NOT EXISTS ocean_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ocean_db;

-- ========================================
-- 1. 사용자 관리 도메인
-- ========================================

CREATE TABLE `users` (
  `user_cd` VARCHAR(100) PRIMARY KEY COMMENT '사용자 고유 코드',
  `user_id` VARCHAR(100) UNIQUE NOT NULL COMMENT '소셜 로그인 ID',
  `user_nm` VARCHAR(100) NOT NULL COMMENT '사용자 실명',
  `email` VARCHAR(255) COMMENT '이메일 주소',
  `user_pf_img` VARCHAR(500) COMMENT '프로필 이미지 URL',
  `provider` VARCHAR(20) NOT NULL COMMENT 'OAuth 제공자 (google, kakao)',
  `ph_num` VARCHAR(20) COMMENT '휴대폰 번호',
  `department` VARCHAR(100) COMMENT '부서(소속)',
  `position` VARCHAR(100) COMMENT '직급',
  `timezone` VARCHAR(50) DEFAULT 'Asia/Seoul' COMMENT '사용자 시간대',
  `language_preference` VARCHAR(10) DEFAULT 'ko' COMMENT '언어 설정',
  `notification_settings` JSON COMMENT '알림 설정 JSON',
  `is_active` BOOLEAN DEFAULT TRUE COMMENT '계정 활성 상태',
  `last_login_at` TIMESTAMP NULL COMMENT '마지막 로그인 시간',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '계정 생성일',
  INDEX `idx_provider_user` (`provider`, `user_id`),
  INDEX `idx_created` (`created_at`),
  INDEX `idx_email` (`email`)
) COMMENT = '사용자 기본 정보 테이블';

CREATE TABLE `user_tokens` (
  `user_cd` VARCHAR(100) PRIMARY KEY COMMENT '사용자 코드',
  `access_token` VARCHAR(500) COMMENT 'OAuth 액세스 토큰',
  `refresh_token` VARCHAR(500) COMMENT 'OAuth 리프레시 토큰',
  `token_expires_at` TIMESTAMP NULL COMMENT '토큰 만료 시간',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT = 'OAuth 제공자로부터 받은 초기 토큰 저장';

-- ========================================
-- 2. 워크스페이스 도메인
-- ========================================

CREATE TABLE `workspaces` (
  `workspace_cd` VARCHAR(100) PRIMARY KEY COMMENT '워크스페이스 고유 코드',
  `workspace_name` VARCHAR(200) NOT NULL COMMENT '워크스페이스 이름',
  `workspace_img` VARCHAR(500) COMMENT '워크스페이스 대표 이미지',
  `invite_cd` VARCHAR(50) UNIQUE NOT NULL COMMENT '초대 코드 (사용자 공유용)',
  `max_members` INT DEFAULT 50 COMMENT '최대 멤버 수',
  `current_members` INT DEFAULT 0 COMMENT '현재 멤버 수 (트리거로 자동 업데이트)',
  `storage_limit` BIGINT DEFAULT 10737418240 COMMENT '저장 공간 제한 (기본 10GB)',
  `storage_used` BIGINT DEFAULT 0 COMMENT '사용된 저장 공간',
  `subscription_type` ENUM('FREE','BASIC','PREMIUM','ENTERPRISE') DEFAULT 'FREE' COMMENT '구독 플랜',
  `is_active` BOOLEAN DEFAULT TRUE COMMENT '워크스페이스 활성 상태',
  `created_by` VARCHAR(100) NOT NULL COMMENT '생성자 (방장)',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `end_date` TIMESTAMP NULL COMMENT '워크스페이스 종료일',
  INDEX `idx_invite_code` (`invite_cd`),
  INDEX `idx_created` (`created_at`),
  INDEX `idx_fk_created_by` (`created_by`)
) COMMENT = '워크스페이스 기본 정보';

CREATE TABLE `workspace_members` (
  `workspace_cd` VARCHAR(100) COMMENT '워크스페이스 코드',
  `user_cd` VARCHAR(100) COMMENT '사용자 코드',
  `user_nickname` VARCHAR(50) COMMENT '워크스페이스 내 닉네임',
  `user_role` ENUM('OWNER','MEMBER') DEFAULT 'MEMBER' COMMENT '멤버 권한',
  `status_message` VARCHAR(200) COMMENT '상태 메시지',
  `profile_status` ENUM('PENDING','IN_PROGRESS','COMPLETE','NEEDS_UPDATE') DEFAULT 'PENDING' COMMENT '프로필 완성도',
  `is_active` BOOLEAN DEFAULT TRUE,
  `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '가입일',
  `last_activity_at` TIMESTAMP NULL COMMENT '마지막 활동 시간',
  PRIMARY KEY (`workspace_cd`, `user_cd`),
  INDEX `idx_user` (`user_cd`),
  INDEX `idx_role` (`user_role`),
  INDEX `idx_joined` (`joined_at`),
  INDEX `idx_user_workspace` (`user_cd`, `workspace_cd`, `is_active`)
) COMMENT = '워크스페이스 멤버 관리';

CREATE TABLE `workspace_invitations` (
  `invite_id` VARCHAR(100) PRIMARY KEY COMMENT '초대 고유 ID',
  `workspace_cd` VARCHAR(100) NOT NULL,
  `invite_type` ENUM('EMAIL_INVITE','CODE_REQUEST') NOT NULL COMMENT '초대 유형',
  `invited_by` VARCHAR(100) COMMENT 'null=코드요청, value=관리자초대',
  `invited_email` VARCHAR(255) COMMENT '초대받은 이메일',
  `status` ENUM('PENDING','ACCEPTED','REJECTED','EXPIRED') DEFAULT 'PENDING',
  `expired_at` TIMESTAMP NULL COMMENT '초대 만료 시간',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `used_at` TIMESTAMP NULL COMMENT '초대 수락 시간',
  INDEX `idx_status` (`status`),
  INDEX `idx_expired` (`expired_at`)
) COMMENT = '워크스페이스 초대 관리';

-- ========================================
-- 3. 캘린더 도메인
-- ========================================

CREATE TABLE `calendar_events` (
  `event_id` VARCHAR(100) PRIMARY KEY,
  `user_cd` VARCHAR(100) NOT NULL COMMENT '일정 생성자',
  `workspace_cd` VARCHAR(100) COMMENT 'null=개인일정',
  `title` VARCHAR(300) NOT NULL COMMENT '일정 제목',
  `description` TEXT COMMENT '일정 설명',
  `start_datetime` DATETIME NOT NULL COMMENT '시작 일시',
  `end_datetime` DATETIME NOT NULL COMMENT '종료 일시',
  `location_cd` VARCHAR(500) COMMENT '장소 코드 또는 텍스트',
  `color` ENUM('RED','ORANGE','YELLOW','GREEN','BLUE','GRAY') DEFAULT 'BLUE',
  `is_all_day` BOOLEAN DEFAULT FALSE COMMENT '종일 일정 여부',
  `is_private` BOOLEAN DEFAULT FALSE COMMENT '비공개 일정',
  `status` ENUM('BEFORE','IN_PROGRESS','DONE') NOT NULL COMMENT '일정 진행 상태',
  `importance` ENUM('LOW','NORMAL','HIGH') DEFAULT 'NORMAL',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_user_date` (`user_cd`, `start_datetime`),
  INDEX `idx_workspace_date` (`workspace_cd`, `start_datetime`),
  INDEX `idx_type_status` (`is_private`, `status`),
  INDEX `idx_event_datetime` (`start_datetime`, `end_datetime`, `status`),
  CONSTRAINT `chk_event_time` CHECK (`end_datetime` > `start_datetime`)
) COMMENT = '캘린더 일정 관리';

CREATE TABLE `event_attendees` (
  `event_id` VARCHAR(100),
  `user_cd` VARCHAR(100),
  PRIMARY KEY (`event_id`, `user_cd`)
) COMMENT = '일정 참석자 관리 (다대다 관계)';

CREATE TABLE `event_notifications` (
  `notification_id` VARCHAR(100) PRIMARY KEY,
  `event_id` VARCHAR(100) NOT NULL,
  `notify_before_minutes` INT NOT NULL COMMENT '알림 시간 (분 단위)',
  INDEX `idx_event_notify` (`event_id`)
) COMMENT = '일정 알림 설정 (10분전, 30분전 등)';

-- ========================================
-- 4. 회의 도메인
-- ========================================

CREATE TABLE `meeting_rooms` (
  `room_cd` VARCHAR(100) PRIMARY KEY,
  `room_name` VARCHAR(200) NOT NULL COMMENT '회의 제목',
  `description` TEXT COMMENT '회의 설명',
  `workspace_cd` VARCHAR(100) NOT NULL,
  `host_user_cd` VARCHAR(100) NOT NULL COMMENT '회의 주최자',
  `status` ENUM('WAITING','IN_PROGRESS','ENDED') DEFAULT 'WAITING',
  `max_participants` INT DEFAULT 10 COMMENT '최대 참가자 수',
  `is_recording_enabled` BOOLEAN DEFAULT FALSE,
  `recording_storage_path` VARCHAR(500),
  `meeting_password` VARCHAR(100),
  `waiting_room_enabled` BOOLEAN DEFAULT FALSE,
  `scheduled_start_time` TIMESTAMP NULL,
  `scheduled_end_time` TIMESTAMP NULL,
  `actual_start_time` TIMESTAMP NULL,
  `actual_end_time` TIMESTAMP NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_workspace_status` (`workspace_cd`, `status`),
  INDEX `idx_fk_host_user` (`host_user_cd`),
  INDEX `idx_meeting_time` (`created_at`, `status`),
  CONSTRAINT `chk_max_participants` CHECK (`max_participants` BETWEEN 2 AND 100)
) COMMENT = '화상회의 룸 관리';

CREATE TABLE `meeting_participants` (
  `room_cd` VARCHAR(100),
  `user_cd` VARCHAR(100),
  `joined_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `left_at` TIMESTAMP NULL,
  `is_active` BOOLEAN DEFAULT TRUE,
  `role` ENUM('HOST','PARTICIPANT') DEFAULT 'PARTICIPANT',
  `is_video_on` BOOLEAN DEFAULT TRUE,
  `is_audio_on` BOOLEAN DEFAULT TRUE,
  `is_screen_sharing` BOOLEAN DEFAULT FALSE,
  `connection_quality` ENUM('POOR','FAIR','GOOD','EXCELLENT'),
  PRIMARY KEY (`room_cd`, `user_cd`),
  INDEX `idx_active` (`room_cd`, `is_active`)
) COMMENT = '회의 참가자 상태 관리';

CREATE TABLE `meeting_documents` (
  `document_id` VARCHAR(100) PRIMARY KEY,
  `room_cd` VARCHAR(100) NOT NULL,
  `file_name` VARCHAR(300) NOT NULL,
  `file_type` ENUM('PDF','PPT','PPTX') NOT NULL,
  `file_path` VARCHAR(1000) NOT NULL,
  `file_size` BIGINT NOT NULL,
  `total_pages` INT,
  `conversion_status` ENUM('PENDING','PROCESSING','COMPLETED','FAILED') DEFAULT 'PENDING',
  `uploaded_by` VARCHAR(100) NOT NULL,
  `uploaded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `converted_at` TIMESTAMP NULL,
  `is_active` BOOLEAN DEFAULT TRUE,
  INDEX `idx_room_active` (`room_cd`, `is_active`),
  INDEX `idx_conversion` (`conversion_status`)
) COMMENT = '회의 문서 관리 (PPT, PDF)';

CREATE TABLE `document_pages` (
  `page_id` VARCHAR(100) PRIMARY KEY,
  `document_id` VARCHAR(100) NOT NULL,
  `page_number` INT NOT NULL,
  `image_path` VARCHAR(1000) NOT NULL,
  `thumbnail_path` VARCHAR(1000),
  `width` INT,
  `height` INT,
  INDEX `idx_document` (`document_id`)
) COMMENT = 'PPT/PDF를 이미지로 변환한 페이지';

CREATE TABLE `document_annotations` (
  `annotation_id` VARCHAR(100) PRIMARY KEY,
  `document_id` VARCHAR(100) NOT NULL,
  `page_number` INT NOT NULL,
  `annotation_data` JSON NOT NULL,
  `annotation_type` ENUM('DRAWING','TEXT','HIGHLIGHT','SHAPE') NOT NULL,
  `created_by` VARCHAR(100) NOT NULL,
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `is_visible` BOOLEAN DEFAULT TRUE,
  INDEX `idx_doc_page` (`document_id`, `page_number`),
  INDEX `idx_createdby` (`created_by`)
) COMMENT = '문서 위에 그리기/주석 기능';

CREATE TABLE `chat_messages` (
  `message_id` VARCHAR(100) PRIMARY KEY,
  `room_cd` VARCHAR(100) NOT NULL,
  `sender_cd` VARCHAR(100) NOT NULL,
  `message_type` ENUM('TEXT','FILE') DEFAULT 'TEXT' COMMENT '메시지 타입',
  `message_content` TEXT COMMENT '메시지 내용',
  `file_path` VARCHAR(1000) COMMENT '파일 공유 시 경로',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '전송 시간',
  INDEX `idx_room_created` (`room_cd`, `created_at`)
) COMMENT = '회의 단체 채팅 메시지';

-- ========================================
-- 5. 외래키 제약조건
-- ========================================

-- 사용자 도메인
ALTER TABLE `user_tokens` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`) ON DELETE CASCADE;

-- 워크스페이스 도메인
ALTER TABLE `workspaces` ADD FOREIGN KEY (`created_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `workspace_members` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`) ON DELETE CASCADE;
ALTER TABLE `workspace_members` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`) ON DELETE CASCADE;
ALTER TABLE `workspace_invitations` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`) ON DELETE CASCADE;
ALTER TABLE `workspace_invitations` ADD FOREIGN KEY (`invited_by`) REFERENCES `users` (`user_cd`);

-- 캘린더 도메인
ALTER TABLE `calendar_events` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `calendar_events` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`);
ALTER TABLE `event_attendees` ADD FOREIGN KEY (`event_id`) REFERENCES `calendar_events` (`event_id`) ON DELETE CASCADE;
ALTER TABLE `event_attendees` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `event_notifications` ADD FOREIGN KEY (`event_id`) REFERENCES `calendar_events` (`event_id`) ON DELETE CASCADE;

-- 회의 도메인
ALTER TABLE `meeting_rooms` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`);
ALTER TABLE `meeting_rooms` ADD FOREIGN KEY (`host_user_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `meeting_participants` ADD FOREIGN KEY (`room_cd`) REFERENCES `meeting_rooms` (`room_cd`) ON DELETE CASCADE;
ALTER TABLE `meeting_participants` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `meeting_documents` ADD FOREIGN KEY (`room_cd`) REFERENCES `meeting_rooms` (`room_cd`);
ALTER TABLE `meeting_documents` ADD FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `document_pages` ADD FOREIGN KEY (`document_id`) REFERENCES `meeting_documents` (`document_id`) ON DELETE CASCADE;
ALTER TABLE `document_annotations` ADD FOREIGN KEY (`document_id`) REFERENCES `meeting_documents` (`document_id`) ON DELETE CASCADE;
ALTER TABLE `document_annotations` ADD FOREIGN KEY (`created_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `chat_messages` ADD FOREIGN KEY (`room_cd`) REFERENCES `meeting_rooms` (`room_cd`);
ALTER TABLE `chat_messages` ADD FOREIGN KEY (`sender_cd`) REFERENCES `users` (`user_cd`);

-- ========================================
-- 6. 트리거 (워크스페이스 멤버 수 자동 업데이트)
-- ========================================

DELIMITER $$

-- 멤버 추가 시
CREATE TRIGGER update_workspace_member_count_after_insert
AFTER INSERT ON workspace_members
FOR EACH ROW
BEGIN
    UPDATE workspaces
    SET current_members = (
        SELECT COUNT(*) FROM workspace_members
        WHERE workspace_cd = NEW.workspace_cd AND is_active = TRUE
    )
    WHERE workspace_cd = NEW.workspace_cd;
END$$

-- 멤버 상태 변경 시
CREATE TRIGGER update_workspace_member_count_after_update
AFTER UPDATE ON workspace_members
FOR EACH ROW
BEGIN
    IF NEW.is_active != OLD.is_active THEN
        UPDATE workspaces
        SET current_members = (
            SELECT COUNT(*) FROM workspace_members
            WHERE workspace_cd = NEW.workspace_cd AND is_active = TRUE
        )
        WHERE workspace_cd = NEW.workspace_cd;
    END IF;
END$$

-- 멤버 삭제 시
CREATE TRIGGER update_workspace_member_count_after_delete
AFTER DELETE ON workspace_members
FOR EACH ROW
BEGIN
    UPDATE workspaces
    SET current_members = (
        SELECT COUNT(*) FROM workspace_members
        WHERE workspace_cd = OLD.workspace_cd AND is_active = TRUE
    )
    WHERE workspace_cd = OLD.workspace_cd;
END$$

DELIMITER ;

-- ========================================
-- ========================================

-- 파일 최하단에 추가
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;