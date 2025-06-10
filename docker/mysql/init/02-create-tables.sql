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

-- 데이터베이스 사용
USE ocean_db;

CREATE TABLE `users` (
  `user_cd` varchar(100) PRIMARY KEY,
  `user_id` varchar(100) UNIQUE NOT NULL COMMENT '소셜 로그인 ID',
  `user_nm` varchar(100) NOT NULL COMMENT '실명',
  `nickname` varchar(50) COMMENT '서비스 내 닉네임',
  `email` varchar(255) COMMENT '이메일',
  `user_pf_img` varchar(500) COMMENT '프로필 이미지',
  `provider` varchar(20) NOT NULL COMMENT 'google, kakao',
  `ph_num` varchar(20) COMMENT '휴대폰 번호',
  `department` varchar(100) COMMENT '부서(소속)',
  `position` varchar(100) COMMENT '직급',
  `is_active` boolean DEFAULT true COMMENT '계정의 상태(탈퇴/비활성화/정지 등등..',
  `last_login_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `user_tokens` (
  `user_cd` varchar(100) PRIMARY KEY,
  `access_token` varchar(500),
  `refresh_token` varchar(500),
  `token_expires_at` timestamp NULL DEFAULT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `refresh_tokens` (
  `token_id` varchar(100) PRIMARY KEY,
  `user_cd` varchar(100) NOT NULL,
  `refresh_token` varchar(500) UNIQUE NOT NULL,
  `expires_at` timestamp NOT NULL COMMENT 'Ocean 서비스의 리프레쉬 토큰 만료시간',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `workspaces` (
  `workspace_cd` varchar(100) PRIMARY KEY,
  `workspace_name` varchar(200) NOT NULL,
  `workspace_img` varchar(500),
  `invite_cd` varchar(50) UNIQUE NOT NULL COMMENT '사용자가 초대 요청을 보낼 수 있는 코드',
  `max_members` int DEFAULT 50,
  `is_active` boolean DEFAULT true,
  `created_by` varchar(100) NOT NULL COMMENT '누가 만들었는지(방장)',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '언제 만들었는지'
);

CREATE TABLE `workspace_members` (
  `workspace_cd` varchar(100) COMMENT '관리용 워크스페이스 코드',
  `user_cd` varchar(100) COMMENT '외래키',
  `user_nickname` varchar(50) COMMENT '워크스페이스별 닉네임',
  `user_role` enum('OWNER','ADMIN','MEMBER') DEFAULT 'MEMBER',
  `status_message` varchar(200) COMMENT '멤버 상태 메시지',
  `is_active` boolean DEFAULT true,
  `joined_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '최초 접속 시간',
  `last_activity_at` timestamp NULL DEFAULT NULL COMMENT '마지막으로 접속한 시간',
  PRIMARY KEY (`workspace_cd`, `user_cd`)
);

CREATE TABLE `workspace_invitations` (
  `invite_id` varchar(100) PRIMARY KEY COMMENT '관리용 초대 코드',
  `workspace_cd` varchar(100) NOT NULL COMMENT '어느 워크스페이스에 초대 됐는지',
  `invite_type` enum('EMAIL_INVITE','CODE_REQUEST') NOT NULL COMMENT '초대 유형',
  `invited_by` varchar(100) COMMENT '사용자가 초대 코드로 요청한다 : null , 관리자가 초대한다 :getUserCode()',
  `invited_email` varchar(255),
  `status` enum('PENDING','ACCEPTED','REJECTED','EXPIRED') DEFAULT 'PENDING',
  `expired_at` timestamp NULL DEFAULT NULL COMMENT '요청 유효 기간',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '언제 초대 했는지',
  `used_at` timestamp NULL DEFAULT NULL
);

CREATE TABLE `calendar_events` (
  `event_id` varchar(100) PRIMARY KEY,
  `user_cd` varchar(100) NOT NULL COMMENT '(fk)유저 코드',
  `workspace_cd` varchar(100) COMMENT '(fk) 워크스페이스 코드',
  `title` varchar(300) NOT NULL COMMENT '제목',
  `description` text COMMENT '내용',
  `start_datetime` datetime NOT NULL COMMENT '시작일시',
  `end_datetime` datetime NOT NULL COMMENT '종료일시',
  `location` varchar(500) COMMENT '장소',
  `is_all_day` boolean DEFAULT false,
  `is_shared` boolean DEFAULT false,
  `shared_workspace_cd` varchar(100) COMMENT '(fk) 공유된 워크스페이스 코드',
  `google_event_id` varchar(255),
  `original_event_id` varchar(100),
  `event_type` enum('PERSONAL','WORKSPACE','SHARED') NOT NULL COMMENT '개인일정 , 단체일정 ,개인일정을  워크스페이스에 공유',
  `status` enum('CONFIRMED','TENTATIVE','CANCELLED') DEFAULT 'CONFIRMED' COMMENT '완료,미정,취소',
  `recurrence_rule` json COMMENT '캘린더 사용 시 필요되는 반복적인 룰',
  `reminder_minutes` int COMMENT '다양한 알림을 제공',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `event_attendees` (
  `event_id` varchar(100),
  `user_cd` varchar(100) COMMENT '(FK)유저 코드',
  `response_status` enum('ACCEPTED','DECLINED','TENTATIVE','NEEDS_ACTION') DEFAULT 'NEEDS_ACTION' COMMENT '참석,불참,미정',
  `is_organizer` boolean DEFAULT false COMMENT 'TRUE = 주최자 , false = 참석자',
  `is_optional` boolean DEFAULT false COMMENT 'true = 필수 참석자 , false = 선택적 참석자',
  `responded_at` timestamp NULL DEFAULT NULL COMMENT '참석 여부에 응답한 시간, 알림기능에 사용 가능',
  PRIMARY KEY (`event_id`, `user_cd`)
);

CREATE TABLE `user_calendar_sync` (
  `user_cd` varchar(100) PRIMARY KEY COMMENT '(fk) 유저 코드',
  `google_calendar_id` varchar(255),
  `google_refresh_token` varchar(500) COMMENT '구글 api와 통신할 리프레쉬 토큰 저장',
  `sync_enabled` boolean DEFAULT true COMMENT '동기화 On/Off',
  `last_sync_at` timestamp NULL DEFAULT NULL COMMENT '마지막 동기화',
  `sync_error_count` int DEFAULT 0 COMMENT '일정 횟수 초과 자동 비활성화',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `meeting_rooms` (
  `room_cd` varchar(100) PRIMARY KEY,
  `room_name` varchar(200) NOT NULL COMMENT '회의 제목',
  `description` text COMMENT '내용',
  `workspace_cd` varchar(100) NOT NULL COMMENT 'FK',
  `host_user_cd` varchar(100) NOT NULL COMMENT '(FK) 회의 주체자',
  `status` enum('WAITING','IN_PROGRESS','ENDED') DEFAULT 'WAITING',
  `max_participants` int DEFAULT 10 COMMENT '최대 참여자',
  `is_recording_enabled` boolean DEFAULT false COMMENT '녹화 기능 활성화 여부',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `meeting_participants` (
  `room_cd` varchar(100) COMMENT 'FK',
  `user_cd` varchar(100) COMMENT 'FK',
  `joined_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '회의 접속시간',
  `left_at` timestamp NULL DEFAULT NULL COMMENT '나간 시간',
  `is_active` boolean DEFAULT true,
  `role` enum('HOST','PARTICIPANT') DEFAULT 'PARTICIPANT' COMMENT '방장,참석자',
  PRIMARY KEY (`room_cd`, `user_cd`)
);

CREATE TABLE `meeting_documents` (
  `document_id` varchar(100) PRIMARY KEY,
  `room_cd` varchar(100) NOT NULL COMMENT '어느 회의의 문서인지',
  `file_name` varchar(300) NOT NULL COMMENT '원본 파일명(발표자료.pptx)',
  `file_type` enum('PDF','PPT','PPTX') NOT NULL COMMENT '파일유형',
  `file_path` varchar(1000) NOT NULL COMMENT '서버에 저장된 원본 파일 경로',
  `file_size` bigint NOT NULL COMMENT '업로드 제한 체크용',
  `total_pages` int COMMENT '페이지 수(발표 진행률 표시)',
  `conversion_status` enum('PENDING','PROCESSING','COMPLETED','FAILED') DEFAULT 'PENDING' COMMENT '이미지 변환 진행 상황',
  `uploaded_by` varchar(100) NOT NULL COMMENT 'FK',
  `uploaded_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `converted_at` timestamp NULL DEFAULT NULL COMMENT '변환 완료 시간',
  `is_active` boolean DEFAULT true COMMENT '활성 상태 삭제 시 false'
);

CREATE TABLE `document_annotations` (
  `annotation_id` VARCHAR(100) PRIMARY KEY,
  `document_id` VARCHAR(100) NOT NULL COMMENT 'FK',
  `page_number` INT NOT NULL COMMENT '페이지 번호',
  `annotation_data` JSON NOT NULL COMMENT '주석 데이터',
  `annotation_type` enum('DRAWING','TEXT','HIGHLIGHT','SHAPE') NOT NULL COMMENT '주석 데이터(그림, 텍스트, 하이라이트)',
  `created_by` VARCHAR(100) NOT NULL COMMENT 'FK',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '그려진 시간',
  `is_visible` BOOLEAN DEFAULT TRUE COMMENT '표시 여부(삭제하기 기능)'
);

CREATE TABLE `document_pages` (
  `page_id` varchar(100) PRIMARY KEY,
  `document_id` varchar(100) NOT NULL COMMENT 'FK',
  `page_number` int NOT NULL COMMENT '페이지 번호 (1부터 시작)',
  `image_path` varchar(1000) NOT NULL COMMENT '변환된 이미지 경로(고해상도)',
  `thumbnail_path` varchar(1000) COMMENT '썸네일 이미지 경로 (미리보기 용)',
  `width` int COMMENT '이미지 너비',
  `height` int COMMENT '이미지 높이'
);

CREATE TABLE `meeting_sketches` (
  `sketch_id` varchar(100) PRIMARY KEY,
  `room_cd` varchar(100) NOT NULL COMMENT 'FK',
  `save_name` varchar(200) COMMENT '저장 이름',
  `description` text COMMENT '설명',
  `sketch_data` json COMMENT '스케치 데이터(JSON 형태로 도형 , 선 , 텍스트)',
  `thumbnail_path` varchar(1000) COMMENT '썸네일 이미지 경로',
  `created_by` varchar(100) NOT NULL COMMENT 'FK',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `file_info` (
  `file_cd` varchar(100) PRIMARY KEY,
  `file_name` varchar(300) NOT NULL COMMENT 'Base64로 인코딩된 파일 이름',
  `original_file_name` varchar(300) NOT NULL COMMENT '실제 파일 이름',
  `file_path` varchar(1000) NOT NULL,
  `file_size` bigint NOT NULL COMMENT '파일 용량',
  `file_type` varchar(100) COMMENT '파일 타입',
  `file_extension` varchar(20) COMMENT '파일 확장자',
  `event_id` varchar(100) COMMENT 'FK',
  `upload_type` enum('CALENDAR_PERSONAL','CALENDAR_WORKSPACE','BOARD','TASK','MEETING') NOT NULL COMMENT '개인일정,단체일정,게시판,태스크,회의',
  `uploader_cd` varchar(100) NOT NULL COMMENT 'FK',
  `download_count` int DEFAULT 0 COMMENT '다운로드 카운트',
  `is_deleted` boolean DEFAULT false COMMENT '삭제 여부',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '삭제 시간',
  `uploaded_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '업로드 시간',
  `last_accessed_at` timestamp NULL DEFAULT NULL
);

CREATE TABLE `file_previews` (
  `file_cd` varchar(100) PRIMARY KEY,
  `thumbnail_path` varchar(1000) COMMENT '작은 썸네일 해상도',
  `preview_path` varchar(1000) COMMENT '상세보기 해상도',
  `width` int,
  `height` int,
  `page_count` int COMMENT '쪽 수',
  `generated_at` timestamp DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `places` (
  `place_cd` varchar(100) PRIMARY KEY,
  `place_name` varchar(200) NOT NULL,
  `latitude` double NOT NULL COMMENT '위도 : ex) 스타벅스 강남점의 위도',
  `longitude` double NOT NULL COMMENT '경도 : ex) 스타벅스 강남점의 경도',
  `address` varchar(500) COMMENT '지번 주소',
  `road_address` varchar(500) COMMENT '도로명 주소',
  `kakao_place_id` VARCHAR(50) COMMENT '카카오 장소 ID',
  `category_name` VARCHAR(100) COMMENT '카카오 카테고리명 (ex: 카페 > 커피전문점)',
  `place_url` VARCHAR(500) COMMENT '카카오맵 상세페이지 URL',
  `phone` varchar(20) COMMENT '해당 위치의 가게,건물 전화번호',
  `workspace_cd` varchar(100) COMMENT '(FK) 워크스페이스별 즐겨찾기 장소인 경우',
  `tags` json,
  `created_by` varchar(100) NOT NULL COMMENT 'FK',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `appointments` (
  `appointment_id` varchar(100) PRIMARY KEY,
  `appointment_nm` varchar(200) NOT NULL COMMENT '약속 제목',
  `place_cd` varchar(100) COMMENT '(FK) 장소 코드',
  `workspace_cd` varchar(100) COMMENT 'FK',
  `event_id` varchar(100) COMMENT 'FK',
  `appointment_time` datetime NOT NULL COMMENT '약속시간',
  `description` text COMMENT '한줄 설명',
  `status` enum('SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED') DEFAULT 'SCHEDULED',
  `reminder_minutes` int DEFAULT 30 COMMENT '약속 시간( 기본값 30분 )',
  `created_by` varchar(100) NOT NULL COMMENT '(FK) 누가 만들었는가?',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE `appointment_participants` (
  `appointment_id` varchar(100),
  `user_cd` varchar(100) COMMENT 'FK',
  `participation_status` enum('INVITED','ACCEPTED','DECLINED','MAYBE') DEFAULT 'INVITED' COMMENT '초대됨, 받음, 거절, 보류',
  `current_latitude` double COMMENT '현재 위도',
  `current_longitude` double COMMENT '현재 경도',
  `location_updated_at` timestamp NULL DEFAULT NULL COMMENT '사용자 위치 업데이트',
  `estimated_arrival_time` datetime COMMENT '도착 예정 시간',
  `transportation_mode` varchar(50) COMMENT '교통 종류',
  `is_arrived` boolean DEFAULT false COMMENT '도착여부',
  `arrived_at` timestamp NULL DEFAULT NULL COMMENT '도착한 시간',
  PRIMARY KEY (`appointment_id`, `user_cd`)
);

CREATE TABLE `user_locations` (
  `location_id` bigint PRIMARY KEY AUTO_INCREMENT,
  `user_cd` varchar(100) NOT NULL COMMENT 'FK',
  `latitude` double NOT NULL COMMENT '위도 : ex) 자주가는 위치 (집,회사)',
  `longitude` double NOT NULL COMMENT '경도 : ex) 자주가는 위치',
  `accuracy` double COMMENT '위치 정확성',
  `address` varchar(500) COMMENT '주소',
  `location_type` enum('CURRENT','HOME','WORK','FAVORITE') DEFAULT 'CURRENT' COMMENT '현재 , 집 ,일 , 즐겨찾기',
  `location_name` varchar(100) COMMENT '장소명',
  `is_active` boolean DEFAULT true,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX `idx_provider_user` ON `users` (`provider`, `user_id`);
CREATE INDEX `idx_nickname` ON `users` (`nickname`);
CREATE INDEX `idx_users_created` ON `users` (`created_at`);
CREATE INDEX `idx_token` ON `refresh_tokens` (`refresh_token`);
CREATE INDEX `idx_expires` ON `refresh_tokens` (`expires_at`);
CREATE INDEX `idx_invite_code` ON `workspaces` (`invite_cd`);
CREATE INDEX `idx_workspaces_created` ON `workspaces` (`created_at`);
CREATE INDEX `idx_user` ON `workspace_members` (`user_cd`);
CREATE INDEX `idx_role` ON `workspace_members` (`user_role`);
CREATE INDEX `idx_joined` ON `workspace_members` (`joined_at`);
CREATE INDEX `idx_status` ON `workspace_invitations` (`status`);
CREATE INDEX `idx_expired` ON `workspace_invitations` (`expired_at`);
CREATE INDEX `idx_user_date` ON `calendar_events` (`user_cd`, `start_datetime`);
CREATE INDEX `idx_workspace_date` ON `calendar_events` (`workspace_cd`, `start_datetime`);
CREATE INDEX `idx_google_event` ON `calendar_events` (`google_event_id`);
CREATE INDEX `idx_type_status` ON `calendar_events` (`event_type`, `status`);
CREATE INDEX `idx_user_response` ON `event_attendees` (`user_cd`, `response_status`);
CREATE INDEX `idx_workspace_status` ON `meeting_rooms` (`workspace_cd`, `status`);
CREATE INDEX `idx_active` ON `meeting_participants` (`room_cd`, `is_active`);
CREATE INDEX `idx_room_active` ON `meeting_documents` (`room_cd`, `is_active`);
CREATE INDEX `idx_conversion` ON `meeting_documents` (`conversion_status`);
CREATE INDEX `idx_doc_page` ON `document_annotations` (`document_id`, `page_number`);
CREATE INDEX `idx_createdby` ON `document_annotations` (`created_by`);
CREATE UNIQUE INDEX `unique_doc_page` ON `document_pages` (`document_id`, `page_number`);
CREATE INDEX `idx_document` ON `document_pages` (`document_id`);
CREATE INDEX `idx_room` ON `meeting_sketches` (`room_cd`);
CREATE INDEX `idx_sketches_created` ON `meeting_sketches` (`created_at`);
CREATE INDEX `idx_event` ON `file_info` (`event_id`);
CREATE INDEX `idx_upload_type` ON `file_info` (`upload_type`);
CREATE INDEX `idx_uploader` ON `file_info` (`uploader_cd`);
CREATE INDEX `idx_uploaded` ON `file_info` (`uploaded_at`);
CREATE INDEX `idx_kakao_place` ON `places` (`kakao_place_id`);
CREATE INDEX `idx_places_workspace` ON `places` (`workspace_cd`);
CREATE INDEX `idx_coordinates` ON `places` (`latitude`, `longitude`);
CREATE INDEX `idx_appointment_time` ON `appointments` (`appointment_time`);
CREATE INDEX `idx_appointments_status` ON `appointments` (`status`);
CREATE INDEX `idx_appointments_workspace` ON `appointments` (`workspace_cd`);
CREATE INDEX `idx_participants_status` ON `appointment_participants` (`participation_status`);
CREATE INDEX `idx_user_type` ON `user_locations` (`user_cd`, `location_type`);
CREATE INDEX `idx_locations_created` ON `user_locations` (`created_at`);

-- 테이블 코멘트
ALTER TABLE `users` COMMENT = '사용자 정보';
ALTER TABLE `user_tokens` COMMENT = '초기에 발급 되는 엑세스 토큰';
ALTER TABLE `refresh_tokens` COMMENT = '우리 웹 자체의 리프레쉬 토큰';
ALTER TABLE `workspaces` COMMENT = '워크스페이스';
ALTER TABLE `workspace_members` COMMENT = '워크스페이스 멤버';
ALTER TABLE `workspace_invitations` COMMENT = '워크스페이스 초대';
ALTER TABLE `calendar_events` COMMENT = '캘린더';
ALTER TABLE `event_attendees` COMMENT = '캘린더의 일정 참석자';
ALTER TABLE `user_calendar_sync` COMMENT = 'Ocean과 구글 캘린더가 양방향으로 동기화 하기 위한 테이블';
ALTER TABLE `meeting_rooms` COMMENT = '회의 테이블';
ALTER TABLE `meeting_participants` COMMENT = '회의 참석자';
ALTER TABLE `meeting_documents` COMMENT = '업로드 된 문서의 기본 정보 관리';
ALTER TABLE `document_annotations` COMMENT = '발표 자료에 스케치를 할 수 있는 기능';
ALTER TABLE `document_pages` COMMENT = '변환된 페이지 이미지';
ALTER TABLE `meeting_sketches` COMMENT = '회의 스케치보드';
ALTER TABLE `file_info` COMMENT = '파일의 정보 테이블';
ALTER TABLE `file_previews` COMMENT = '렌더링 최소화 하여 최적화 , 미리보기 제공';
ALTER TABLE `places` COMMENT = '고정된 정보에 대한 장소 테이블';
ALTER TABLE `appointments` COMMENT = '약속(회의) 잡기 테이블';
ALTER TABLE `appointment_participants` COMMENT = '실시간 사용자 위치를 제공하기 위한 테이블';
ALTER TABLE `user_locations` COMMENT = '사용자가 자주 가는 위치';

-- 외래키 설정
ALTER TABLE `user_tokens` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`) ON DELETE CASCADE;
ALTER TABLE `refresh_tokens` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`) ON DELETE CASCADE;
ALTER TABLE `workspaces` ADD FOREIGN KEY (`created_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `workspace_members` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`) ON DELETE CASCADE;
ALTER TABLE `workspace_members` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`) ON DELETE CASCADE;
ALTER TABLE `workspace_invitations` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`) ON DELETE CASCADE;
ALTER TABLE `workspace_invitations` ADD FOREIGN KEY (`invited_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `calendar_events` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `calendar_events` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`);
ALTER TABLE `calendar_events` ADD FOREIGN KEY (`shared_workspace_cd`) REFERENCES `workspaces` (`workspace_cd`);
ALTER TABLE `event_attendees` ADD FOREIGN KEY (`event_id`) REFERENCES `calendar_events` (`event_id`) ON DELETE CASCADE;
ALTER TABLE `event_attendees` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `user_calendar_sync` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`) ON DELETE CASCADE;
ALTER TABLE `meeting_rooms` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`);
ALTER TABLE `meeting_rooms` ADD FOREIGN KEY (`host_user_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `meeting_participants` ADD FOREIGN KEY (`room_cd`) REFERENCES `meeting_rooms` (`room_cd`) ON DELETE CASCADE;
ALTER TABLE `meeting_participants` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `meeting_documents` ADD FOREIGN KEY (`room_cd`) REFERENCES `meeting_rooms` (`room_cd`) ON DELETE CASCADE;
ALTER TABLE `meeting_documents` ADD FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `document_pages` ADD FOREIGN KEY (`document_id`) REFERENCES `meeting_documents` (`document_id`) ON DELETE CASCADE;
ALTER TABLE `meeting_sketches` ADD FOREIGN KEY (`room_cd`) REFERENCES `meeting_rooms` (`room_cd`);
ALTER TABLE `meeting_sketches` ADD FOREIGN KEY (`created_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `document_annotations` ADD FOREIGN KEY (`document_id`) REFERENCES `meeting_documents` (`document_id`);
ALTER TABLE `document_annotations` ADD FOREIGN KEY (`created_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `file_info` ADD FOREIGN KEY (`event_id`) REFERENCES `calendar_events` (`event_id`) ON DELETE SET NULL;
ALTER TABLE `file_info` ADD FOREIGN KEY (`uploader_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `file_previews` ADD FOREIGN KEY (`file_cd`) REFERENCES `file_info` (`file_cd`) ON DELETE CASCADE;
ALTER TABLE `places` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`);
ALTER TABLE `places` ADD FOREIGN KEY (`created_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `appointments` ADD FOREIGN KEY (`place_cd`) REFERENCES `places` (`place_cd`);
ALTER TABLE `appointments` ADD FOREIGN KEY (`workspace_cd`) REFERENCES `workspaces` (`workspace_cd`);
ALTER TABLE `appointments` ADD FOREIGN KEY (`event_id`) REFERENCES `calendar_events` (`event_id`);
ALTER TABLE `appointments` ADD FOREIGN KEY (`created_by`) REFERENCES `users` (`user_cd`);
ALTER TABLE `appointment_participants` ADD FOREIGN KEY (`appointment_id`) REFERENCES `appointments` (`appointment_id`) ON DELETE CASCADE;
ALTER TABLE `appointment_participants` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`);
ALTER TABLE `user_locations` ADD FOREIGN KEY (`user_cd`) REFERENCES `users` (`user_cd`);

-- 파일 최하단에 추가
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;