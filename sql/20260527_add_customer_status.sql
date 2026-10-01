IF COL_LENGTH('dbo.KhachHang', 'TrangThai') IS NULL
BEGIN
    ALTER TABLE dbo.KhachHang
    ADD TrangThai NVARCHAR(50) NOT NULL
        CONSTRAINT DF_KhachHang_TrangThai DEFAULT (N'Đang hoạt động');
END;
GO

UPDATE dbo.KhachHang
SET TrangThai = CASE
    WHEN TrangThai IS NULL OR LTRIM(RTRIM(TrangThai)) = N'' THEN N'Đang hoạt động'
    WHEN UPPER(LTRIM(RTRIM(TrangThai))) IN (N'VO_HIEU_HOA', N'VOHIEUHOA', N'VÔ HIỆU HÓA') THEN N'Vô hiệu hóa'
    ELSE N'Đang hoạt động'
END;
GO
