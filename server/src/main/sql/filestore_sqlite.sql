--
-- ���� ������������ � ������� SQLiteStudio v3.2.1 � �� ��� 17 19:49:57 2019
--
-- �������������� ��������� ������: System
--
PRAGMA foreign_keys = off;
BEGIN TRANSACTION;

-- �������: files
DROP TABLE IF EXISTS files;
CREATE TABLE files (
    id       INTEGER     NOT NULL
                          UNIQUE
                          PRIMARY KEY AUTOINCREMENT,
    md5      VARCHAR (32) NOT NULL,
    length   BIGINT (20)  NOT NULL
                          DEFAULT '0',
    position BIGINT (20)  NOT NULL
                          DEFAULT '0'
);

-- �������: user_files
DROP TABLE IF EXISTS user_files;
CREATE TABLE `user_files` (
  `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  `user_id` INT(11) NOT NULL,
  `file_id` INT(11) NOT NULL,
  `filename` VARCHAR(90) NULL DEFAULT NULL,
   FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE RESTRICT,
   FOREIGN KEY(file_id) REFERENCES files(id) ON DELETE RESTRICT
);

-- �������: users
DROP TABLE IF EXISTS users;
CREATE TABLE `users` (
  `id` INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,
  `login` VARCHAR(45) NULL DEFAULT NULL,
  `password` VARCHAR(45) NULL DEFAULT NULL
);
INSERT INTO users (id, login, password) VALUES (1, 'login1', 'pass1');
INSERT INTO users (id, login, password) VALUES (2, 'login2', 'pass2');

-- ������: file_idx
DROP INDEX IF EXISTS file_idx;
CREATE INDEX file_idx ON user_files (
    file_id
);

-- ������: user_idx
DROP INDEX IF EXISTS user_idx;
CREATE INDEX user_idx ON user_files (
    user_id
);

-- �������������: files_of_user_view
DROP VIEW IF EXISTS files_of_user_view;
CREATE VIEW `files_of_user_view` AS
SELECT 
  user_files.user_id AS user_id,
  user_files.filename AS fileName,
  files.md5 AS md5,
  files.length AS fullLength,
  files.position AS currentLength
FROM user_files, users, files 
WHERE user_files.user_id=users.id AND user_files.file_id=files.id;

COMMIT TRANSACTION;
PRAGMA foreign_keys = on;
