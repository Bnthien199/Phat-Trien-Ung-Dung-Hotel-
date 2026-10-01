USE QuanLyKhachSan;
GO

IF OBJECT_ID('dbo.LoaiNgay', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.LoaiNgay (
        MaLoaiNgay      VARCHAR(30)     NOT NULL,
        TenLoaiNgay     NVARCHAR(100)   NOT NULL,
        TrangThai       BIT             NOT NULL CONSTRAINT DF_LoaiNgay_TrangThai DEFAULT (1),
        CONSTRAINT PK_LoaiNgay PRIMARY KEY (MaLoaiNgay),
        CONSTRAINT UQ_LoaiNgay_TenLoaiNgay UNIQUE (TenLoaiNgay)
    );
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.LoaiNgay WHERE TenLoaiNgay = N'Ngày thường')
BEGIN
    INSERT INTO dbo.LoaiNgay(MaLoaiNgay, TenLoaiNgay, TrangThai)
    VALUES ('NGAY_THUONG', N'Ngày thường', 1);
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.LoaiNgay WHERE TenLoaiNgay = N'Cuối tuần')
BEGIN
    INSERT INTO dbo.LoaiNgay(MaLoaiNgay, TenLoaiNgay, TrangThai)
    VALUES ('CUOI_TUAN', N'Cuối tuần', 1);
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.LoaiNgay WHERE TenLoaiNgay = N'Ngày lễ')
BEGIN
    INSERT INTO dbo.LoaiNgay(MaLoaiNgay, TenLoaiNgay, TrangThai)
    VALUES ('NGAY_LE', N'Ngày lễ', 1);
END
GO

/* Xóa FK cũ nếu có */
IF OBJECT_ID('dbo.FK_PhieuDatPhong_TrangThai', 'F') IS NOT NULL
    ALTER TABLE dbo.PhieuDatPhong DROP CONSTRAINT FK_PhieuDatPhong_TrangThai;
GO

/* Xóa PK cũ của bảng lookup */
DECLARE @pkName SYSNAME;
SELECT @pkName = kc.name
FROM sys.key_constraints kc
WHERE kc.parent_object_id = OBJECT_ID('dbo.TrangThaiPhieuDat')
  AND kc.type = 'PK';

IF @pkName IS NOT NULL
BEGIN
    DECLARE @sql NVARCHAR(MAX);
    SET @sql = N'ALTER TABLE dbo.TrangThaiPhieuDat DROP CONSTRAINT ' + QUOTENAME(@pkName);
    EXEC sp_executesql @sql;
END
GO

/* Sửa 2 cột cho cùng độ dài NVARCHAR(40) */
ALTER TABLE dbo.PhieuDatPhong ALTER COLUMN trangThai NVARCHAR(40) NOT NULL;
GO

ALTER TABLE dbo.TrangThaiPhieuDat ALTER COLUMN maCode NVARCHAR(40) NOT NULL;
GO

ALTER TABLE dbo.TrangThaiPhieuDat
ADD CONSTRAINT PK_TrangThaiPhieuDat PRIMARY KEY (maCode);
GO

/* Chuẩn hóa dữ liệu */
UPDATE dbo.PhieuDatPhong
SET trangThai = CASE
    WHEN trangThai IS NULL OR LTRIM(RTRIM(trangThai)) = N'' THEN N'CHO_XAC_NHAN'
    WHEN UPPER(LTRIM(RTRIM(trangThai))) IN (N'CHOCOC', N'CHO_COC', N'CHỜ CỌC') THEN N'CHO_COC'
    WHEN UPPER(LTRIM(RTRIM(trangThai))) IN (N'DADATCOC', N'DA_DAT_COC', N'ĐÃ ĐẶT CỌC') THEN N'DA_DAT_COC'
    WHEN UPPER(LTRIM(RTRIM(trangThai))) IN (N'CHOXACNHAN', N'CHO_XAC_NHAN', N'CHỜ XÁC NHẬN') THEN N'CHO_XAC_NHAN'
    WHEN UPPER(LTRIM(RTRIM(trangThai))) IN (N'DAXACNHAN', N'DA_XAC_NHAN', N'ĐÃ XÁC NHẬN') THEN N'DA_XAC_NHAN'
    WHEN UPPER(LTRIM(RTRIM(trangThai))) IN (N'DANHANPHONG', N'DA_NHAN_PHONG', N'ĐÃ NHẬN PHÒNG', N'DANGO', N'DANG_O', N'ĐANG Ở') THEN N'DA_NHAN_PHONG'
    WHEN UPPER(LTRIM(RTRIM(trangThai))) IN (N'DATHANHTOAN', N'DA_THANH_TOAN', N'ĐÃ THANH TOÁN', N'DATRAPHONG', N'ĐÃ TRẢ PHÒNG') THEN N'DA_THANH_TOAN'
    WHEN UPPER(LTRIM(RTRIM(trangThai))) IN (N'DAHUY', N'DA_HUY', N'ĐÃ HỦY') THEN N'DA_HUY'
    ELSE N'CHO_XAC_NHAN'
END;
GO

/* Thêm đủ mã trạng thái */
MERGE dbo.TrangThaiPhieuDat AS target
USING (VALUES
    (N'CHO_COC', N'Chờ cọc', N'Phiếu chờ đặt cọc', 1),
    (N'DA_DAT_COC', N'Đã đặt cọc', N'Phiếu đã đặt cọc', 2),
    (N'CHO_XAC_NHAN', N'Chờ xác nhận', N'Phiếu chờ xác nhận', 3),
    (N'DA_XAC_NHAN', N'Đã xác nhận', N'Phiếu đã xác nhận', 4),
    (N'DA_NHAN_PHONG', N'Đã nhận phòng', N'Khách đã nhận phòng', 5),
    (N'DA_THANH_TOAN', N'Đã thanh toán', N'Khách đã thanh toán', 6),
    (N'DA_HUY', N'Đã hủy', N'Phiếu đã hủy', 7)
) AS source(maCode, tenHienThi, moTa, thuTu)
ON target.maCode = source.maCode
WHEN MATCHED THEN 
    UPDATE SET tenHienThi = source.tenHienThi, moTa = source.moTa, thuTu = source.thuTu
WHEN NOT MATCHED THEN
    INSERT (maCode, tenHienThi, moTa, thuTu)
    VALUES (source.maCode, source.tenHienThi, source.moTa, source.thuTu);
GO

/* Tạo lại FK */
ALTER TABLE dbo.PhieuDatPhong
ADD CONSTRAINT FK_PhieuDatPhong_TrangThai
FOREIGN KEY (trangThai) REFERENCES dbo.TrangThaiPhieuDat(maCode);
GO

PRINT N'SUA FK TRANG THAI PHIEU DAT THANH CONG';
GO
