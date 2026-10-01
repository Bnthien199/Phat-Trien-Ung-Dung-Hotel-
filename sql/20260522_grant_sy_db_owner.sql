USE master;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.sql_logins WHERE name = 'sy'
)
BEGIN
    CREATE LOGIN sy WITH PASSWORD = '123';
END
GO

USE QuanLyKhachSan;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.database_principals WHERE name = 'sy'
)
BEGIN
    CREATE USER sy FOR LOGIN sy;
END
GO

ALTER ROLE db_owner ADD MEMBER sy;
GO

PRINT N'Da cap quyen db_owner cho user sy trong database QuanLyKhachSan.';
GO
