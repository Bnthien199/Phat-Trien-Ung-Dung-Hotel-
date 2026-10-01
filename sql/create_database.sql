USE master;
GO

IF DB_ID(N'QuanLyKhachSan') IS NOT NULL
BEGIN
    ALTER DATABASE QuanLyKhachSan SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE QuanLyKhachSan;
END
GO

CREATE DATABASE QuanLyKhachSan;
GO

USE QuanLyKhachSan;
GO

CREATE TABLE dbo.NhanVien (
    maNhanVien      VARCHAR(10)     NOT NULL,
    tenNhanVien     NVARCHAR(100)   NOT NULL,
    chucVu          NVARCHAR(100)   NULL,
    luongNhanVien   DECIMAL(18,2)   NOT NULL CONSTRAINT DF_NhanVien_Luong DEFAULT (0),
    phaiNhanVien    BIT             NOT NULL CONSTRAINT DF_NhanVien_Phai DEFAULT (1),
    soDienThoai     VARCHAR(15)     NULL,
    tenDangNhap     VARCHAR(50)     NOT NULL,
    matKhau         VARCHAR(255)    NOT NULL,
    vaiTro          NVARCHAR(50)    NOT NULL,
    CONSTRAINT PK_NhanVien PRIMARY KEY (maNhanVien),
    CONSTRAINT UQ_NhanVien_TenDangNhap UNIQUE (tenDangNhap),
    CONSTRAINT UQ_NhanVien_SDT UNIQUE (soDienThoai)
);
GO

CREATE TABLE dbo.KhachHang (
    maKhachHang     VARCHAR(10)     NOT NULL,
    tenKhachHang    NVARCHAR(100)   NOT NULL,
    diaChi          NVARCHAR(255)   NULL,
    gioiTinh        BIT             NOT NULL CONSTRAINT DF_KhachHang_GioiTinh DEFAULT (1),
    soDienThoai     VARCHAR(15)     NULL,
    loaiGiayTo      NVARCHAR(50)    NULL,
    soGiayTo        VARCHAR(30)     NULL,
    TrangThai       NVARCHAR(50)    NOT NULL CONSTRAINT DF_KhachHang_TrangThai DEFAULT (N'Đang hoạt động'),
    CONSTRAINT PK_KhachHang PRIMARY KEY (maKhachHang),
    CONSTRAINT UQ_KhachHang_SDT UNIQUE (soDienThoai),
    CONSTRAINT UQ_KhachHang_GiayTo UNIQUE (soGiayTo)
);
GO

CREATE TABLE dbo.LoaiPhong (
    maLoai          VARCHAR(10)     NOT NULL,
    loaiPhong       NVARCHAR(50)    NOT NULL,
    ngayTao         DATETIME2       NOT NULL CONSTRAINT DF_LoaiPhong_NgayTao DEFAULT (SYSUTCDATETIME()),
    ngayCapNhat     DATETIME2       NULL,
    CONSTRAINT PK_LoaiPhong PRIMARY KEY (maLoai),
    CONSTRAINT UQ_LoaiPhong_Ten UNIQUE (loaiPhong)
);
GO

CREATE TABLE dbo.Phong (
    maPhong         VARCHAR(10)     NOT NULL,
    loaiPhong       NVARCHAR(50)    NOT NULL,
    soNguoi         INT             NOT NULL CONSTRAINT DF_Phong_SoNguoi DEFAULT (1),
    tang            INT             NOT NULL CONSTRAINT DF_Phong_Tang DEFAULT (1),
    trangThai       NVARCHAR(30)    NOT NULL,
    giaPhong        DECIMAL(18,2)   NOT NULL CONSTRAINT DF_Phong_Gia DEFAULT (0),
    maLoai          VARCHAR(10)     NOT NULL,
    CONSTRAINT PK_Phong PRIMARY KEY (maPhong),
    CONSTRAINT CK_Phong_SoNguoi CHECK (soNguoi > 0),
    CONSTRAINT CK_Phong_Tang CHECK (tang > 0),
    CONSTRAINT CK_Phong_Gia CHECK (giaPhong >= 0),
    CONSTRAINT FK_Phong_LoaiPhong FOREIGN KEY (maLoai) REFERENCES dbo.LoaiPhong(maLoai)
);
GO

CREATE TABLE dbo.DichVu (
    maDichVu        VARCHAR(10)     NOT NULL,
    tenDichVu       NVARCHAR(100)   NOT NULL,
    donGia          DECIMAL(18,2)   NOT NULL CONSTRAINT DF_DichVu_DonGia DEFAULT (0),
    trangThai       NVARCHAR(50)    NOT NULL,
    CONSTRAINT PK_DichVu PRIMARY KEY (maDichVu),
    CONSTRAINT CK_DichVu_DonGia CHECK (donGia >= 0)
);
GO

CREATE TABLE dbo.LoaiNgay (
    MaLoaiNgay      VARCHAR(30)     NOT NULL,
    TenLoaiNgay     NVARCHAR(100)   NOT NULL,
    TrangThai       BIT             NOT NULL CONSTRAINT DF_LoaiNgay_TrangThai DEFAULT (1),
    CONSTRAINT PK_LoaiNgay PRIMARY KEY (MaLoaiNgay),
    CONSTRAINT UQ_LoaiNgay_TenLoaiNgay UNIQUE (TenLoaiNgay)
);
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

CREATE TABLE dbo.BangGiaHeader (
    maBangGia       VARCHAR(10)     NOT NULL,
    tenBangGia      NVARCHAR(100)   NOT NULL,
    loaiBangGia     NVARCHAR(50)    NOT NULL,
    loaiNgay        VARCHAR(20)     NOT NULL,
    ngayBatDau      DATE            NULL,
    ngayKetThuc     DATE            NULL,
    trangThai       BIT             NOT NULL CONSTRAINT DF_BangGiaHeader_TrangThai DEFAULT (1),
    phanTramTang    DECIMAL(5,2)    NOT NULL CONSTRAINT DF_BangGiaHeader_phanTramTang DEFAULT (0),
    CONSTRAINT PK_BangGiaHeader PRIMARY KEY (maBangGia),
    CONSTRAINT CK_BangGiaHeader_LoaiNgay CHECK (loaiNgay IN ('NGAY_THUONG', 'CUOI_TUAN', 'NGAY_LE')),
    CONSTRAINT CK_BangGiaHeader_Ngay CHECK (ngayKetThuc IS NULL OR ngayBatDau IS NULL OR ngayKetThuc >= ngayBatDau),
    CONSTRAINT CK_BangGiaHeader_TrangThai CHECK (trangThai IN (0, 1)),
    CONSTRAINT CK_BangGiaHeader_PhanTramTang CHECK (phanTramTang BETWEEN 0 AND 100)
);
GO

CREATE TABLE dbo.ChiTietBangGia (
    maBangGia       VARCHAR(10)     NOT NULL,
    maLoai          VARCHAR(10)     NOT NULL,
    giaTheoGio      DECIMAL(18,2)   NOT NULL CONSTRAINT DF_CTBG_Gio DEFAULT (0),
    giaQuaDem       DECIMAL(18,2)   NOT NULL CONSTRAINT DF_CTBG_QuaDem DEFAULT (0),
    giaTheoNgay     DECIMAL(18,2)   NOT NULL CONSTRAINT DF_CTBG_Ngay DEFAULT (0),
    giaCuoiTuan     DECIMAL(18,2)   NOT NULL CONSTRAINT DF_CTBG_CuoiTuan DEFAULT (0),
    giaLe           DECIMAL(18,2)   NOT NULL CONSTRAINT DF_CTBG_Le DEFAULT (0),
    phuThu          DECIMAL(18,2)   NOT NULL CONSTRAINT DF_CTBG_PhuThu DEFAULT (0),
    CONSTRAINT PK_ChiTietBangGia PRIMARY KEY (maBangGia, maLoai),
    CONSTRAINT FK_CTBG_BangGia FOREIGN KEY (maBangGia) REFERENCES dbo.BangGiaHeader(maBangGia),
    CONSTRAINT FK_CTBG_LoaiPhong FOREIGN KEY (maLoai) REFERENCES dbo.LoaiPhong(maLoai),
    CONSTRAINT CK_CTBG_Gia CHECK (
        giaTheoGio >= 0 AND giaQuaDem >= 0 AND giaTheoNgay >= 0 AND
        giaCuoiTuan >= 0 AND giaLe >= 0 AND phuThu >= 0
    )
);
GO

CREATE TABLE dbo.PhieuDatPhong (
    maDatPhong      VARCHAR(10)     NOT NULL,
    ngayDat         DATETIME2       NOT NULL,
    trangThai       NVARCHAR(30)    NOT NULL,
    maNhanVien      VARCHAR(10)     NOT NULL,
    maKhachHang     VARCHAR(10)     NOT NULL,

    soDemLuuTru     INT             NOT NULL CONSTRAINT DF_PhieuDatPhong_soDemLuuTru DEFAULT (0),
    tongTienPhong   DECIMAL(18,2)   NOT NULL CONSTRAINT DF_PhieuDatPhong_tongTienPhong DEFAULT (0),
    tienDatCoc      DECIMAL(18,2)   NOT NULL CONSTRAINT DF_PhieuDatPhong_tienDatCoc DEFAULT (0),
    tienConLai      DECIMAL(18,2)   NOT NULL CONSTRAINT DF_PhieuDatPhong_tienConLai DEFAULT (0),
    tyLeDatCoc      DECIMAL(5,2)    NOT NULL CONSTRAINT DF_PhieuDatPhong_tyLeDatCoc DEFAULT (50),
    hinhThucDatCoc  NVARCHAR(50)    NULL,
    ngayDatCoc      DATETIME2       NULL,
    maNhanVienNhanCoc VARCHAR(10)   NULL,
    ghiChuDatCoc    NVARCHAR(500)   NULL,

    soTienKhachDua  DECIMAL(18,2)   NOT NULL CONSTRAINT DF_PhieuDatPhong_soTienKhachDua DEFAULT (0),
    soTienThucNhan  DECIMAL(18,2)   NOT NULL CONSTRAINT DF_PhieuDatPhong_soTienThucNhan DEFAULT (0),
    tienThuaTraKhach DECIMAL(18,2)  NOT NULL CONSTRAINT DF_PhieuDatPhong_tienThuaTraKhach DEFAULT (0),

    CONSTRAINT PK_PhieuDatPhong PRIMARY KEY (maDatPhong),
    CONSTRAINT FK_PDP_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT FK_PDP_KhachHang FOREIGN KEY (maKhachHang) REFERENCES dbo.KhachHang(maKhachHang),
    CONSTRAINT FK_PhieuDatPhong_NhanVienNhanCoc FOREIGN KEY (maNhanVienNhanCoc) REFERENCES dbo.NhanVien(maNhanVien)
);
GO

CREATE TABLE dbo.ChiTietPhieuDat (
    maDatPhong      VARCHAR(10)     NOT NULL,
    maPhong         VARCHAR(10)     NOT NULL,
    giaThuePhong    DECIMAL(18,2)   NOT NULL CONSTRAINT DF_CTPD_Gia DEFAULT (0),
    ngayNhan        DATETIME2       NOT NULL,
    ngayTra         DATETIME2       NOT NULL,
    trangThai       NVARCHAR(30)    NOT NULL CONSTRAINT DF_CTPD_TrangThai DEFAULT (N'CHO_NHAN'),
    thoiDiemNhanThucTe DATETIME2    NULL,
    thoiDiemTraThucTe  DATETIME2    NULL,
    CONSTRAINT PK_ChiTietPhieuDat PRIMARY KEY (maDatPhong, maPhong),
    CONSTRAINT FK_CTPD_PDP FOREIGN KEY (maDatPhong) REFERENCES dbo.PhieuDatPhong(maDatPhong),
    CONSTRAINT FK_CTPD_Phong FOREIGN KEY (maPhong) REFERENCES dbo.Phong(maPhong),
    CONSTRAINT CK_CTPD_Gia CHECK (giaThuePhong >= 0),
    CONSTRAINT CK_CTPD_Ngay CHECK (ngayTra >= ngayNhan)
);
GO

CREATE TABLE dbo.HoaDon (
    maHoaDon        VARCHAR(10)     NOT NULL,
    ngayLap         DATETIME2       NOT NULL,
    thue            DECIMAL(5,2)    NOT NULL CONSTRAINT DF_HoaDon_Thue DEFAULT (0),
    tongTienPhong   DECIMAL(18,2)   NOT NULL CONSTRAINT DF_HoaDon_TongPhong DEFAULT (0),
    tongTienDichVu  DECIMAL(18,2)   NOT NULL CONSTRAINT DF_HoaDon_TongDV DEFAULT (0),
    maDatPhong      VARCHAR(10)     NOT NULL,
    maNhanVien      VARCHAR(10)     NOT NULL,
    CONSTRAINT PK_HoaDon PRIMARY KEY (maHoaDon),
    CONSTRAINT FK_HoaDon_PDP FOREIGN KEY (maDatPhong) REFERENCES dbo.PhieuDatPhong(maDatPhong),
    CONSTRAINT FK_HoaDon_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT UQ_HoaDon_MaDatPhong UNIQUE (maDatPhong),
    CONSTRAINT CK_HoaDon_Thue CHECK (thue >= 0),
    CONSTRAINT CK_HoaDon_Tong CHECK (tongTienPhong >= 0 AND tongTienDichVu >= 0)
);
GO

CREATE TABLE dbo.ChiTietHoaDon (
    maHoaDon        VARCHAR(10)     NOT NULL,
    maDichVu        VARCHAR(10)     NOT NULL,
    maPhong         VARCHAR(10)     NOT NULL,
    soLuong         INT             NOT NULL CONSTRAINT DF_CTHD_SoLuong DEFAULT (1),
    donGiaLuuTru    DECIMAL(18,2)   NOT NULL CONSTRAINT DF_CTHD_DonGia DEFAULT (0),
    CONSTRAINT PK_ChiTietHoaDon PRIMARY KEY (maHoaDon, maPhong, maDichVu),
    CONSTRAINT FK_CTHD_HoaDon FOREIGN KEY (maHoaDon) REFERENCES dbo.HoaDon(maHoaDon),
    CONSTRAINT FK_CTHD_DichVu FOREIGN KEY (maDichVu) REFERENCES dbo.DichVu(maDichVu),
    CONSTRAINT FK_CTHD_Phong FOREIGN KEY (maPhong) REFERENCES dbo.Phong(maPhong),
    CONSTRAINT CK_CTHD_SoLuong CHECK (soLuong > 0),
    CONSTRAINT CK_CTHD_DonGia CHECK (donGiaLuuTru >= 0)
);
GO

CREATE TABLE dbo.TrangThaiPhieuDat (
    maCode NVARCHAR(40) NOT NULL PRIMARY KEY,
    tenHienThi NVARCHAR(100) NOT NULL,
    moTa NVARCHAR(255) NULL,
    thuTu INT NOT NULL DEFAULT 0
);
GO

CREATE TABLE dbo.TrangThaiPhong (
    maCode NVARCHAR(40) NOT NULL PRIMARY KEY,
    tenHienThi NVARCHAR(100) NOT NULL,
    moTa NVARCHAR(255) NULL,
    thuTu INT NOT NULL DEFAULT 0
);
GO

CREATE TABLE dbo.LichSuThanhToanCoc (
    maGiaoDich INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maDatPhong VARCHAR(10) NOT NULL,
    loaiGiaoDich NVARCHAR(30) NOT NULL,
    soTien DECIMAL(18,2) NOT NULL,
    hinhThuc NVARCHAR(50) NULL,
    thoiGian DATETIME2 NOT NULL CONSTRAINT DF_LichSuThanhToanCoc_thoiGian DEFAULT (SYSDATETIME()),
    maNhanVien VARCHAR(10) NULL,
    ghiChu NVARCHAR(500) NULL,
    CONSTRAINT CK_LichSuThanhToanCoc_soTien CHECK (soTien >= 0),
    CONSTRAINT FK_LichSuThanhToanCoc_PhieuDatPhong FOREIGN KEY (maDatPhong) REFERENCES dbo.PhieuDatPhong(maDatPhong),
    CONSTRAINT FK_LichSuThanhToanCoc_NhanVien FOREIGN KEY (maNhanVien) REFERENCES dbo.NhanVien(maNhanVien)
);
GO

CREATE TABLE dbo.HuyDatPhong (
    maHuy INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    maDatPhong VARCHAR(10) NOT NULL,
    maKhachHang VARCHAR(10) NOT NULL,
    ngayDatCoc DATETIME2 NULL,
    ngayHuy DATETIME2 NOT NULL,
    soNgaySauDatCoc INT NOT NULL CONSTRAINT DF_HuyDatPhong_soNgaySauDatCoc DEFAULT (0),
    tienCocBanDau DECIMAL(18,2) NOT NULL CONSTRAINT DF_HuyDatPhong_tienCocBanDau DEFAULT (0),
    tyLeHoanCoc DECIMAL(5,2) NOT NULL CONSTRAINT DF_HuyDatPhong_tyLeHoanCoc DEFAULT (0),
    tienHoanLai DECIMAL(18,2) NOT NULL CONSTRAINT DF_HuyDatPhong_tienHoanLai DEFAULT (0),
    tienKhachSanGiu DECIMAL(18,2) NOT NULL CONSTRAINT DF_HuyDatPhong_tienKhachSanGiu DEFAULT (0),
    lyDoHuy NVARCHAR(500) NOT NULL,
    hinhThucHoanTien NVARCHAR(50) NULL,
    maNhanVienXuLy VARCHAR(10) NULL,
    thoiGianXuLy DATETIME2 NOT NULL CONSTRAINT DF_HuyDatPhong_thoiGianXuLy DEFAULT (SYSDATETIME()),
    CONSTRAINT FK_HuyDatPhong_PhieuDatPhong FOREIGN KEY (maDatPhong) REFERENCES dbo.PhieuDatPhong(maDatPhong),
    CONSTRAINT FK_HuyDatPhong_KhachHang FOREIGN KEY (maKhachHang) REFERENCES dbo.KhachHang(maKhachHang),
    CONSTRAINT FK_HuyDatPhong_NhanVien FOREIGN KEY (maNhanVienXuLy) REFERENCES dbo.NhanVien(maNhanVien),
    CONSTRAINT CK_HuyDatPhong_Tien CHECK (
        tienCocBanDau >= 0
        AND tienHoanLai >= 0
        AND tienKhachSanGiu >= 0
        AND tyLeHoanCoc >= 0
        AND tyLeHoanCoc <= 100
    )
);
GO

CREATE INDEX IX_CTPD_MaPhong ON dbo.ChiTietPhieuDat(maPhong);
CREATE INDEX IX_CTHD_MaPhong ON dbo.ChiTietHoaDon(maPhong);
CREATE INDEX IX_CTHD_MaDichVu ON dbo.ChiTietHoaDon(maDichVu);
CREATE INDEX IX_PDP_MaKhachHang ON dbo.PhieuDatPhong(maKhachHang);
CREATE INDEX IX_HoaDon_MaNhanVien ON dbo.HoaDon(maNhanVien);
GO

PRINT N'TAO LAI DATABASE QUANLYKHACHSAN THANH CONG';
GO
