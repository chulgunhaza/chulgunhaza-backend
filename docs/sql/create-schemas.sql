-- #99: MySQL 인스턴스는 하나로 유지하되 스키마를 user/attendance/chatting으로 나눈다.
-- 기존 "chulgunhaza" 스키마는 이 스크립트로 지우지 않는다 — chulgunhaza_user로
-- 새로 만들고, 필요하면 옛 스키마는 나중에 수동으로 정리한다.
--
-- 스키마 생성 + 권한 부여 둘 다 root 계정이 필요하다(.env의 DATABASE_USER 계정은
-- 기존 "chulgunhaza" 스키마에만 권한이 있음). 로컬 실행 예:
--   docker exec -i chulgunhaza-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < docs/sql/create-schemas.sql
-- (MYSQL_ROOT_PASSWORD는 컨테이너를 처음 띄울 때 지정한 값 — .env의
-- DATABASE_PASSWORD와는 다른 값일 수 있다)

CREATE DATABASE IF NOT EXISTS chulgunhaza_user
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS chulgunhaza_attendance
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS chulgunhaza_chatting
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- .env의 DATABASE_USER 계정(기본값 chulgunhaza)에 새 스키마 권한을 부여한다.
-- 계정명이 다르면 아래 'chulgunhaza'를 실제 계정명으로 바꿔서 실행할 것.
GRANT ALL PRIVILEGES ON chulgunhaza_user.* TO 'chulgunhaza'@'%';
GRANT ALL PRIVILEGES ON chulgunhaza_attendance.* TO 'chulgunhaza'@'%';
GRANT ALL PRIVILEGES ON chulgunhaza_chatting.* TO 'chulgunhaza'@'%';
FLUSH PRIVILEGES;
