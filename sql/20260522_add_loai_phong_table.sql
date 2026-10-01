USE QuanLyKhachSan;
GO

SET NOCOUNT ON;
GO

IF OBJECT_ID('dbo.LoaiPhong', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoaiPhong (
        maLoai          VARCHAR(10)     NOT NULL,
        loaiPhong       NVARCHAR(50)    NOT NULL,
        ngayTao         DATETIME2       NOT NULL CONSTRAINT DF_LoaiPhong_NgayTao DEFAULT (SYSUTCDATETIME()),
        ngayCapNhat     DATETIME2       NULL,
        CONSTRAINT PK_LoaiPhong PRIMARY KEY (maLoai),
        CONSTRAINT UQ_LoaiPhong_Ten UNIQUE (loaiPhong)
    );
END
GO

IF OBJECT_ID('dbo.Phong', 'U') IS NOT NULL
BEGIN
    INSERT INTO dbo.LoaiPhong(maLoai, loaiPhong)
    SELECT src.maLoai, src.loaiPhong
    FROM (
        SELECT DISTINCT
            UPPER(LTRIM(RTRIM(maLoai))) AS maLoai,
            LTRIM(RTRIM(loaiPhong)) AS loaiPhong
        FROM dbo.Phong
        WHERE maLoai IS NOT NULL AND LTRIM(RTRIM(maLoai)) <> ''
          AND loaiPhong IS NOT NULL AND LTRIM(RTRIM(loaiPhong)) <> ''
    ) src
    WHERE NOT EXISTS (
        SELECT 1
        FROM dbo.LoaiPhong lp
        WHERE UPPER(LTRIM(RTRIM(lp.maLoai))) = src.maLoai
           OR UPPER(LTRIM(RTRIM(lp.loaiPhong))) = UPPER(src.loaiPhong)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.LoaiPhong WHERE maLoai = 'LP01' OR loaiPhong = N'STANDARD')
    INSERT INTO dbo.LoaiPhong(maLoai, loaiPhong) VALUES ('LP01', N'STANDARD');
IF NOT EXISTS (SELECT 1 FROM dbo.LoaiPhong WHERE maLoai = 'LP02' OR loaiPhong = N'DELUXE')
    INSERT INTO dbo.LoaiPhong(maLoai, loaiPhong) VALUES ('LP02', N'DELUXE');
IF NOT EXISTS (SELECT 1 FROM dbo.LoaiPhong WHERE maLoai = 'LP03' OR loaiPhong = N'SUITE')
    INSERT INTO dbo.LoaiPhong(maLoai, loaiPhong) VALUES ('LP03', N'SUITE');
IF NOT EXISTS (SELECT 1 FROM dbo.LoaiPhong WHERE maLoai = 'FML' OR loaiPhong = N'FAMILY')
    INSERT INTO dbo.LoaiPhong(maLoai, loaiPhong) VALUES ('FML', N'FAMILY');
GO

PRINT N'Da san sang bang dbo.LoaiPhong.';
GO
