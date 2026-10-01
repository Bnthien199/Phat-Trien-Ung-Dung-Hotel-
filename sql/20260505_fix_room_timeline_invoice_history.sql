/*
    Fix room timeline and invoice history.
    Safe to run multiple times on SQL Server.
*/

/* 1) Store booking room snapshots so invoices do not depend on current Room rows. */
IF COL_LENGTH('dbo.ChiTietPhieuDat', 'tenPhongSnapshot') IS NULL
BEGIN
    ALTER TABLE dbo.ChiTietPhieuDat ADD tenPhongSnapshot NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH('dbo.ChiTietPhieuDat', 'loaiPhongSnapshot') IS NULL
BEGIN
    ALTER TABLE dbo.ChiTietPhieuDat ADD loaiPhongSnapshot NVARCHAR(50) NULL;
END;
GO

IF COL_LENGTH('dbo.ChiTietPhieuDat', 'donGiaPhongSnapshot') IS NULL
BEGIN
    ALTER TABLE dbo.ChiTietPhieuDat ADD donGiaPhongSnapshot DECIMAL(18,2) NULL;
END;
GO

UPDATE ct
SET tenPhongSnapshot = COALESCE(ct.tenPhongSnapshot, p.maPhong),
    loaiPhongSnapshot = COALESCE(ct.loaiPhongSnapshot, p.loaiPhong),
    donGiaPhongSnapshot = COALESCE(ct.donGiaPhongSnapshot, ct.giaThuePhong, p.giaPhong)
FROM dbo.ChiTietPhieuDat ct
LEFT JOIN dbo.Phong p ON p.maPhong = ct.maPhong
WHERE ct.tenPhongSnapshot IS NULL
   OR ct.loaiPhongSnapshot IS NULL
   OR ct.donGiaPhongSnapshot IS NULL;
GO

/* 2) Optional maintenance timeline. Room.trangThai remains current-state cache only. */
IF OBJECT_ID('dbo.LichSuTrangThaiPhong', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.LichSuTrangThaiPhong (
        id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        maPhong VARCHAR(10) NOT NULL,
        trangThai NVARCHAR(30) NOT NULL,
        tuNgay DATETIME2 NOT NULL,
        denNgay DATETIME2 NULL,
        ghiChu NVARCHAR(255) NULL,
        CONSTRAINT FK_LSTTP_Phong FOREIGN KEY (maPhong) REFERENCES dbo.Phong(maPhong),
        CONSTRAINT CK_LSTTP_Range CHECK (denNgay IS NULL OR denNgay > tuNgay)
    );
END;
GO

/* 3) Timeline query indexes. */
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_CTPD_Timeline'
      AND object_id = OBJECT_ID('dbo.ChiTietPhieuDat')
)
BEGIN
    CREATE INDEX IX_CTPD_Timeline
    ON dbo.ChiTietPhieuDat(maPhong, ngayNhan, ngayTra)
    INCLUDE (maDatPhong, giaThuePhong);
END;
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'IX_LSTTP_Timeline'
      AND object_id = OBJECT_ID('dbo.LichSuTrangThaiPhong')
)
BEGIN
    CREATE INDEX IX_LSTTP_Timeline
    ON dbo.LichSuTrangThaiPhong(maPhong, trangThai, tuNgay, denNgay);
END;
GO
