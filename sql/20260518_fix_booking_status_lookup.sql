USE QuanLyKhachSan;
GO

DECLARE @maCodeDangInsert NVARCHAR(40) = N'DA_DAT_COC';

SELECT @maCodeDangInsert AS maCodeDangInsert;

SELECT maCode, tenHienThi, moTa, thuTu
FROM dbo.TrangThaiPhieuDat
WHERE maCode = @maCodeDangInsert;
GO

IF NOT EXISTS (
    SELECT 1
    FROM dbo.TrangThaiPhieuDat
    WHERE maCode = N'DA_DAT_COC'
)
BEGIN
    INSERT INTO dbo.TrangThaiPhieuDat (maCode, tenHienThi, moTa, thuTu)
    VALUES (N'DA_DAT_COC', N'Đã đặt cọc', N'Phiếu đã đặt cọc', 2);
END;
GO

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
    UPDATE SET tenHienThi = source.tenHienThi,
               moTa = source.moTa,
               thuTu = source.thuTu
WHEN NOT MATCHED THEN
    INSERT (maCode, tenHienThi, moTa, thuTu)
    VALUES (source.maCode, source.tenHienThi, source.moTa, source.thuTu);
GO

SELECT maCode, tenHienThi, moTa, thuTu
FROM dbo.TrangThaiPhieuDat
WHERE maCode IN (
    N'CHO_COC',
    N'DA_DAT_COC',
    N'CHO_XAC_NHAN',
    N'DA_XAC_NHAN',
    N'DA_NHAN_PHONG',
    N'DA_THANH_TOAN',
    N'DA_HUY'
)
ORDER BY thuTu;
GO
