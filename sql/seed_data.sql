
/*
    seed_data.sql
    Sample seed data for QuanLyKhachSan
*/

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

-- Clear in child -> parent order
DELETE FROM dbo.ChiTietHoaDon;
DELETE FROM dbo.HoaDon;
DELETE FROM dbo.ChiTietPhieuDat;
DELETE FROM dbo.PhieuDatPhong;
DELETE FROM dbo.ChiTietBangGia;
DELETE FROM dbo.BangGiaHeader;
DELETE FROM dbo.DichVu;
DELETE FROM dbo.Phong;
DELETE FROM dbo.LoaiPhong;
DELETE FROM dbo.KhachHang;
DELETE FROM dbo.NhanVien;
GO

INSERT INTO dbo.NhanVien(maNhanVien, tenNhanVien, chucVu, luongNhanVien, phaiNhanVien, soDienThoai, tenDangNhap, matKhau, vaiTro) VALUES
('NV001', N'Quản trị hệ thống', N'Quản lý', 25000000, 1, '0909000001', 'admin', '1234567', N'ADMIN'),
('NV002', N'Lê Minh Tâm', N'Lễ tân', 12000000, 1, '0909000002', 'letan01', '123456', N'LE_TAN'),
('NV003', N'Nguyễn Thu Hà', N'Kế toán', 15000000, 0, '0909000003', 'ketoan01', '123456', N'KE_TOAN');
GO

INSERT INTO dbo.KhachHang(maKhachHang, tenKhachHang, diaChi, gioiTinh, soDienThoai, loaiGiayTo, soGiayTo) VALUES
('KH001', N'Nguyễn Văn An', N'Quận 1, TP.HCM', 1, '0911111111', N'CCCD', '079201000001'),
('KH002', N'Trần Thị Bình', N'Thủ Đức, TP.HCM', 0, '0911111112', N'CCCD', '079201000002'),
('KH003', N'Phạm Gia Huy', N'Biên Hòa, Đồng Nai', 1, '0911111113', N'Hộ chiếu', 'B12345678'),
('KH004', N'Lê Mỹ Linh', N'Đà Nẵng', 0, '0911111114', N'CCCD', '079201000004');
GO

INSERT INTO dbo.LoaiPhong(maLoai, loaiPhong) VALUES
('LP01', N'STANDARD'),
('LP02', N'DELUXE'),
('LP03', N'SUITE'),
('FML', N'FAMILY');
GO

INSERT INTO dbo.Phong(maPhong, loaiPhong, soNguoi, tang, trangThai, giaPhong, maLoai) VALUES
('P101', N'STANDARD', 2, 1, N'TRONG', 350000, 'LP01'),
('P102', N'STANDARD', 2, 1, N'TRONG', 350000, 'LP01'),
('P201', N'DELUXE',   3, 2, N'TRONG', 550000, 'LP02'),
('P202', N'DELUXE',   3, 2, N'CHO_XAC_NHAN', 550000, 'LP02'),
('P301', N'SUITE',    4, 3, N'DANG_O', 900000, 'LP03'),
('P302', N'SUITE',    4, 3, N'SUA_CHUA', 900000, 'LP03');
GO

INSERT INTO dbo.DichVu(maDichVu, tenDichVu, donGia, trangThai) VALUES
('DV001', N'Nước suối', 10000, N'Đang kinh doanh'),
('DV002', N'Mì ly', 25000, N'Đang kinh doanh'),
('DV003', N'Giặt ủi', 50000, N'Đang kinh doanh'),
('DV004', N'Bia lon', 30000, N'Ngừng kinh doanh');
GO

INSERT INTO dbo.BangGiaHeader(maBangGia, tenBangGia, loaiBangGia, loaiNgay, ngayBatDau, ngayKetThuc, trangThai) VALUES
('BG001', N'Bảng giá ngày thường 2026', N'THEO_NGAY', 'NGAY_THUONG', '2026-01-01', NULL, 1),
('BG002', N'Bảng giá cuối tuần 2026', N'THEO_NGAY', 'CUOI_TUAN', '2026-01-01', NULL, 1),
('BG003', N'Bảng giá lễ 2026', N'THEO_NGAY', 'NGAY_LE', '2026-01-01', NULL, 1);
GO

INSERT INTO dbo.ChiTietBangGia(maBangGia, maLoai, giaTheoGio, giaQuaDem, giaTheoNgay, giaCuoiTuan, giaLe, phuThu) VALUES
('BG001', 'LP01', 80000, 250000, 350000, 420000, 500000, 30000),
('BG001', 'LP02', 120000, 380000, 550000, 650000, 750000, 50000),
('BG001', 'LP03', 200000, 650000, 900000, 1050000, 1200000, 80000),
('BG002', 'LP01', 90000, 300000, 420000, 420000, 500000, 30000),
('BG002', 'LP02', 140000, 450000, 650000, 650000, 750000, 50000),
('BG002', 'LP03', 230000, 760000, 1050000, 1050000, 1200000, 80000),
('BG003', 'LP01', 110000, 350000, 500000, 500000, 500000, 50000),
('BG003', 'LP02', 160000, 520000, 750000, 750000, 750000, 70000),
('BG003', 'LP03', 260000, 880000, 1200000, 1200000, 1200000, 100000);
GO

INSERT INTO dbo.PhieuDatPhong(maDatPhong, ngayDat, trangThai, maNhanVien, maKhachHang) VALUES
('DP001', '2026-04-18T09:00:00', N'CHO_XAC_NHAN', 'NV002', 'KH001'),
('DP002', '2026-04-18T10:30:00', N'DA_XAC_NHAN', 'NV002', 'KH002'),
('DP003', '2026-04-19T14:00:00', N'DA_NHAN_PHONG', 'NV001', 'KH003');
GO

INSERT INTO dbo.ChiTietPhieuDat(maDatPhong, maPhong, giaThuePhong, ngayNhan, ngayTra) VALUES
('DP001', 'P102', 350000, '2026-04-21T14:00:00', '2026-04-22T12:00:00'),
('DP002', 'P202', 650000, '2026-04-20T14:00:00', '2026-04-21T12:00:00'),
('DP003', 'P301', 900000, '2026-04-19T14:00:00', '2026-04-21T12:00:00');
GO

INSERT INTO dbo.HoaDon(maHoaDon, ngayLap, thue, tongTienPhong, tongTienDichVu, maDatPhong, maNhanVien) VALUES
('HD00001', '2026-04-20T08:30:00', 8, 1800000, 70000, 'DP003', 'NV001');
GO

INSERT INTO dbo.ChiTietHoaDon(maHoaDon, maDichVu, maPhong, soLuong, donGiaLuuTru) VALUES
('HD00001', 'DV001', 'P301', 2, 10000),
('HD00001', 'DV003', 'P301', 1, 50000);
GO

PRINT N'=== XONG. Database đã sẵn sàng. ===';
PRINT N'Tài khoản đăng nhập app: admin / 1234567';
GO
