USE ocean_db;

-- 테스트 사용자 데이터
INSERT INTO users (user_cd, user_id, user_nm, nickname, email, provider, department, position) VALUES
('USR001', 'testuser1', '김철수', '철수', 'test1@ocean.com', 'google', '개발팀', '팀장'),
('USR002', 'testuser2', '이영희', '영희', 'test2@ocean.com', 'kakao', '마케팅팀', '팀원'),
('USR003', 'testuser3', '박민수', '민수', 'test3@ocean.com', 'google', '개발팀', '팀원');

-- 테스트 워크스페이스
INSERT INTO workspaces (workspace_cd, workspace_name, created_by, invite_cd) VALUES
('WS001', '개발팀', 'USR001', 'DEV2025'),
('WS002', '마케팅팀', 'USR002', 'MKT2025');

-- 워크스페이스 멤버
INSERT INTO workspace_members (workspace_cd, user_cd, user_nickname, user_role) VALUES
('WS001', 'USR001', '철수', 'OWNER'),
('WS001', 'USR002', '영희', 'MEMBER'),
('WS002', 'USR002', '영희', 'OWNER'),
('WS002', 'USR003', '민수', 'MEMBER');

-- 테스트 캘린더 이벤트
INSERT INTO calendar_events (event_id, user_cd, workspace_cd, title, description, start_datetime, end_datetime, event_type) VALUES
('EVT001', 'USR001', 'WS001', '스프린트 회의', '2주차 스프린트 리뷰', '2025-06-15 10:00:00', '2025-06-15 11:00:00', 'WORKSPACE'),
('EVT002', 'USR002', NULL, '개인 일정', '병원 방문', '2025-06-16 14:00:00', '2025-06-16 15:00:00', 'PERSONAL');

-- 테스트 미팅룸
INSERT INTO meeting_rooms (room_cd, room_name, description, workspace_cd, host_user_cd) VALUES
('MTG001', '일일 스탠드업', '매일 아침 진행하는 스탠드업 미팅', 'WS001', 'USR001');