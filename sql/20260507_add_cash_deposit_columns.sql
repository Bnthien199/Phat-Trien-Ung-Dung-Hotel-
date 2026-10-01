USE QuanLyKhachSan;
GO

IF COL_LENGTH('dbo.PhieuDatPhong', 'soTienKhachDua') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuDatPhong
    ADD soTienKhachDua DECIMAL(18,2) DEFAULT 0;
END
GO

IF COL_LENGTH('dbo.PhieuDatPhong', 'soTienThucNhan') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuDatPhong
    ADD soTienThucNhan DECIMAL(18,2) DEFAULT 0;
END
GO

IF COL_LENGTH('dbo.PhieuDatPhong', 'tienThuaTraKhach') IS NULL
BEGIN
    ALTER TABLE dbo.PhieuDatPhong
    ADD tienThuaTraKhach DECIMAL(18,2) DEFAULT 0;
END
GO