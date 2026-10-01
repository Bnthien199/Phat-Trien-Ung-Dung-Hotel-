USE QuanLyKhachSan;
GO

/* XÓA FK nếu đã tạo dở */
IF OBJECT_ID('dbo.FK_PhieuDatPhong_NhanVienNhanCoc', 'F') IS NOT NULL
    ALTER TABLE dbo.PhieuDatPhong DROP CONSTRAINT FK_PhieuDatPhong_NhanVienNhanCoc;
GO

/* XÓA bảng tạo dở */
IF OBJECT_ID('dbo.LichSuThanhToanCoc', 'U') IS NOT NULL
    DROP TABLE dbo.LichSuThanhToanCoc;
GO

IF OBJECT_ID('dbo.HuyDatPhong', 'U') IS NOT NULL
    DROP TABLE dbo.HuyDatPhong;
GO

/* SỬA độ dài cột maNhanVienNhanCoc cho giống NhanVien.maNhanVien */
DECLARE @lenNhanVien INT;

SELECT @lenNhanVien = CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'NhanVien'
  AND COLUMN_NAME = 'maNhanVien';

DECLARE @sql NVARCHAR(MAX);

IF COL_LENGTH('dbo.PhieuDatPhong', 'maNhanVienNhanCoc') IS NULL
BEGIN
    SET @sql = N'ALTER TABLE dbo.PhieuDatPhong ADD maNhanVienNhanCoc VARCHAR(' + CAST(@lenNhanVien AS VARCHAR(10)) + N') NULL;';
    EXEC sp_executesql @sql;
END
ELSE
BEGIN
    SET @sql = N'ALTER TABLE dbo.PhieuDatPhong ALTER COLUMN maNhanVienNhanCoc VARCHAR(' + CAST(@lenNhanVien AS VARCHAR(10)) + N') NULL;';
    EXEC sp_executesql @sql;
END
GO

/* Tạo lại FK nhân viên nhận cọc */
IF OBJECT_ID('dbo.FK_PhieuDatPhong_NhanVienNhanCoc', 'F') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuDatPhong
    ADD CONSTRAINT FK_PhieuDatPhong_NhanVienNhanCoc
    FOREIGN KEY (maNhanVienNhanCoc) REFERENCES dbo.NhanVien(maNhanVien);
END;
GO

/* Lấy độ dài thật của các khóa */
DECLARE @lenDatPhong INT;
DECLARE @lenNhanVien INT;
DECLARE @lenKhachHang INT;

SELECT @lenDatPhong = CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'PhieuDatPhong'
  AND COLUMN_NAME = 'maDatPhong';

SELECT @lenNhanVien = CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'NhanVien'
  AND COLUMN_NAME = 'maNhanVien';

SELECT @lenKhachHang = CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'KhachHang'
  AND COLUMN_NAME = 'maKhachHang';

/* Tạo bảng LichSuThanhToanCoc đúng độ dài */
DECLARE @sql2 NVARCHAR(MAX);

SET @sql2 = N'
CREATE TABLE dbo.LichSuThanhToanCoc (
    maGiaoDich INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maDatPhong VARCHAR(' + CAST(@lenDatPhong AS VARCHAR(10)) + N') NOT NULL,
    loaiGiaoDich NVARCHAR(30) NOT NULL,
    soTien DECIMAL(18,2) NOT NULL,
    hinhThuc NVARCHAR(50) NULL,
    thoiGian DATETIME2 NOT NULL CONSTRAINT DF_LichSuThanhToanCoc_thoiGian DEFAULT (SYSDATETIME()),
    maNhanVien VARCHAR(' + CAST(@lenNhanVien AS VARCHAR(10)) + N') NULL,
    ghiChu NVARCHAR(500) NULL,

    CONSTRAINT CK_LichSuThanhToanCoc_soTien CHECK (soTien >= 0),

    CONSTRAINT FK_LichSuThanhToanCoc_PhieuDatPhong
        FOREIGN KEY (maDatPhong) REFERENCES dbo.PhieuDatPhong(maDatPhong),

    CONSTRAINT FK_LichSuThanhToanCoc_NhanVien
        FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien)
);';

EXEC sp_executesql @sql2;
GO

/* Tạo bảng HuyDatPhong đúng độ dài */
DECLARE @lenDatPhong INT;
DECLARE @lenNhanVien INT;
DECLARE @lenKhachHang INT;

SELECT @lenDatPhong = CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'PhieuDatPhong'
  AND COLUMN_NAME = 'maDatPhong';

SELECT @lenNhanVien = CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'NhanVien'
  AND COLUMN_NAME = 'maNhanVien';

SELECT @lenKhachHang = CHARACTER_MAXIMUM_LENGTH
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'KhachHang'
  AND COLUMN_NAME = 'maKhachHang';

DECLARE @sql3 NVARCHAR(MAX);

SET @sql3 = N'
CREATE TABLE dbo.HuyDatPhong (
    maHuy INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maDatPhong VARCHAR(' + CAST(@lenDatPhong AS VARCHAR(10)) + N') NOT NULL,
    maKhachHang VARCHAR(' + CAST(@lenKhachHang AS VARCHAR(10)) + N') NOT NULL,
    ngayDatCoc DATETIME2 NULL,
    ngayHuy DATETIME2 NOT NULL,
    soNgaySauDatCoc INT NOT NULL CONSTRAINT DF_HuyDatPhong_soNgaySauDatCoc DEFAULT (0),
    tienCocBanDau DECIMAL(18,2) NOT NULL CONSTRAINT DF_HuyDatPhong_tienCocBanDau DEFAULT (0),
    tyLeHoanCoc DECIMAL(5,2) NOT NULL CONSTRAINT DF_HuyDatPhong_tyLeHoanCoc DEFAULT (0),
    tienHoanLai DECIMAL(18,2) NOT NULL CONSTRAINT DF_HuyDatPhong_tienHoanLai DEFAULT (0),
    tienKhachSanGiu DECIMAL(18,2) NOT NULL CONSTRAINT DF_HuyDatPhong_tienKhachSanGiu DEFAULT (0),
    lyDoHuy NVARCHAR(500) NOT NULL,
    hinhThucHoanTien NVARCHAR(50) NULL,
    maNhanVienXuLy VARCHAR(' + CAST(@lenNhanVien AS VARCHAR(10)) + N') NULL,
    thoiGianXuLy DATETIME2 NOT NULL CONSTRAINT DF_HuyDatPhong_thoiGianXuLy DEFAULT (SYSDATETIME()),

    CONSTRAINT FK_HuyDatPhong_PhieuDatPhong
        FOREIGN KEY (maDatPhong) REFERENCES dbo.PhieuDatPhong(maDatPhong),

    CONSTRAINT FK_HuyDatPhong_KhachHang
        FOREIGN KEY (maKhachHang) REFERENCES dbo.KhachHang(maKhachHang),

    CONSTRAINT FK_HuyDatPhong_NhanVien
        FOREIGN KEY (maNhanVienXuLy) REFERENCES dbo.NhanVien(maNhanVien),

    CONSTRAINT CK_HuyDatPhong_Tien CHECK (
        tienCocBanDau >= 0
        AND tienHoanLai >= 0
        AND tienKhachSanGiu >= 0
        AND tyLeHoanCoc >= 0
        AND tyLeHoanCoc <= 100
    )
);';

EXEC sp_executesql @sql3;
GO

PRINT N'SUA XONG LOI DO DAI KHOA NGOAI';
GO