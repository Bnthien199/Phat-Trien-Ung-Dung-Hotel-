USE QuanLyKhachSan;
GO

IF OBJECT_ID('dbo.CK_BangGiaHeader_TrangThai', 'C') IS NOT NULL
    ALTER TABLE dbo.BangGiaHeader DROP CONSTRAINT CK_BangGiaHeader_TrangThai;
GO

UPDATE dbo.BangGiaHeader
SET trangThai = 1;
GO

ALTER TABLE dbo.BangGiaHeader
ADD CONSTRAINT CK_BangGiaHeader_TrangThai
CHECK (trangThai IN (0, 1));
GO

IF NOT EXISTS (SELECT 1 FROM dbo.BangGiaHeader WHERE maBangGia = 'BG_NT_DEF')
BEGIN
    INSERT INTO dbo.BangGiaHeader
        (maBangGia, tenBangGia, loaiBangGia, ngayBatDau, ngayKetThuc, loaiNgay, trangThai, phanTramTang)
    VALUES
        ('BG_NT_DEF', N'Giá ngày thường', N'GIA_GOC', '2000-01-01', '2099-12-31', N'NGAY_THUONG', 1, 0);
END
GO

IF NOT EXISTS (SELECT 1 FROM dbo.BangGiaHeader WHERE maBangGia = 'BG_CT_DEF')
BEGIN
    INSERT INTO dbo.BangGiaHeader
        (maBangGia, tenBangGia, loaiBangGia, ngayBatDau, ngayKetThuc, loaiNgay, trangThai, phanTramTang)
    VALUES
        ('BG_CT_DEF', N'Giá cuối tuần', N'GIA_GOC', '2000-01-01', '2099-12-31', N'CUOI_TUAN', 1, 10);
END
GO

PRINT N'SUA XONG LOI TRANG THAI BANG GIA BIT';
GO