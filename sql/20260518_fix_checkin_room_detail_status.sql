USE QuanLyKhachSan;
GO

IF COL_LENGTH('dbo.ChiTietPhieuDat', 'trangThai') IS NULL
BEGIN
    ALTER TABLE dbo.ChiTietPhieuDat
    ADD trangThai NVARCHAR(30) NOT NULL
        CONSTRAINT DF_ChiTietPhieuDat_trangThai DEFAULT (N'CHO_NHAN');
END
GO

IF COL_LENGTH('dbo.ChiTietPhieuDat', 'thoiDiemNhanThucTe') IS NULL
BEGIN
    ALTER TABLE dbo.ChiTietPhieuDat
    ADD thoiDiemNhanThucTe DATETIME2 NULL;
END
GO

IF COL_LENGTH('dbo.ChiTietPhieuDat', 'thoiDiemTraThucTe') IS NULL
BEGIN
    ALTER TABLE dbo.ChiTietPhieuDat
    ADD thoiDiemTraThucTe DATETIME2 NULL;
END
GO

UPDATE ct
SET trangThai = CASE
    WHEN UPPER(ISNULL(pdp.trangThai, N'')) IN (N'DA_THANH_TOAN', N'DATHANHTOAN') THEN N'DA_TRA'
    WHEN UPPER(ISNULL(p.trangThai, N'')) IN (N'DANG_O', N'DANGO', N'DA_NHAN', N'DANHAN', N'DA_NHAN_PHONG', N'DANHANPHONG', N'ĐANG Ở', N'ĐÃ NHẬN PHÒNG') THEN N'DA_NHAN'
    WHEN UPPER(ISNULL(ct.trangThai, N'')) IN (N'DA_TRA', N'DATRA', N'DA_TRA_PHONG', N'DATRAPHONG', N'ĐÃ TRẢ', N'ĐÃ TRẢ PHÒNG', N'ĐÃ THANH TOÁN') THEN N'DA_TRA'
    WHEN UPPER(ISNULL(ct.trangThai, N'')) IN (N'DA_NHAN', N'DANHAN', N'DA_NHAN_PHONG', N'DANHANPHONG', N'DANG_O', N'DANGO', N'ĐANG Ở', N'ĐÃ NHẬN PHÒNG') THEN N'DA_NHAN'
    ELSE N'CHO_NHAN'
END
FROM dbo.ChiTietPhieuDat ct
JOIN dbo.PhieuDatPhong pdp ON pdp.maDatPhong = ct.maDatPhong
JOIN dbo.Phong p ON p.maPhong = ct.maPhong;
GO

PRINT N'FIX CHECK-IN TUNG PHONG: ChiTietPhieuDat da co trangThai rieng cho tung phong';
GO
