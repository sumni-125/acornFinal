USE ocean_db;

-- 테스트 사용자 데이터
INSERT INTO users (user_code, user_id, user_name, nickname, email, provider, is_profile_complete) VALUES
('USR001', 'testuser1', '김철수', '철수', 'test1@ocean.com', 'google', true),
('USR002', 'testuser2', '이영희', '영희', 'test2@ocean.com', 'kakao', true),
('USR003', 'testuser3', '박민수', '민수', 'test3@ocean.com', 'google', false);

-- 테스트 워크스페이스
INSERT INTO workspaces (workspace_code, workspace_name, workspace_description, created_by, invite_code) VALUES
('WS001', '개발팀', '개발팀 협업 공간입니다.', 'USR001', 'DEV2025'),
('WS002', '마케팅팀', '마케팅팀 협업 공간입니다.', 'USR002', 'MKT2025');

-- 워크스페이스 멤버
INSERT INTO workspace_members (workspace_code, user_code, user_nickname, user_role) VALUES
('WS001', 'USR001', '철수', 'OWNER'),
('WS001', 'USR002', '영희', 'MEMBER'),
('WS002', 'USR002', '영희', 'OWNER'),
('WS002', 'USR003', '민수', 'MEMBER');