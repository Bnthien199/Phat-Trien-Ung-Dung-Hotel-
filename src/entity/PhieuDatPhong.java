package entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class PhieuDatPhong {
    private String maDatPhong;
    private LocalDateTime ngayDat;
    private String trangThai;
    private NhanVien nhanVien;
    private KhachHang khachHang;
    private LocalDateTime ngayNhan;
    private LocalDateTime ngayTra;
    private String dsMaPhong;
    private int soLuongPhong;
    private int soDemLuuTru;
    private double tongTienPhong;
    private double tienDatCoc;
    private double tienConLai;
    private double tyLeDatCoc = 50.0;
    private String hinhThucDatCoc;
    private LocalDateTime ngayDatCoc;
    private NhanVien nhanVienNhanCoc;
    private String ghiChuDatCoc;
    private double soTienKhachDua;
    private double soTienThucNhan;
    private double tienThuaTraKhach;

    public PhieuDatPhong() {
    }

    public PhieuDatPhong(String maDatPhong) {
        this.maDatPhong = maDatPhong;
    }

    public PhieuDatPhong(String maDatPhong, LocalDateTime ngayDat, String trangThai, NhanVien nhanVien, KhachHang khachHang) {
        setMaDatPhong(maDatPhong);
        setNgayDat(ngayDat);
        this.trangThai = trangThai;
        this.nhanVien = nhanVien;
        this.khachHang = khachHang;
    }

    public String getMaDatPhong() { return maDatPhong; }

    public void setMaDatPhong(String maDatPhong) {
        if (maDatPhong == null || !maDatPhong.matches("^DP\\d{3,}$")) {
            throw new IllegalArgumentException("Mã đặt phòng phải bắt đầu bằng DP và theo sau là ít nhất 3 chữ số");
        }
        this.maDatPhong = maDatPhong;
    }

    public LocalDateTime getNgayDat() { return ngayDat; }
    public void setNgayDat(LocalDateTime ngayDat) { this.ngayDat = ngayDat; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public NhanVien getNhanVien() { return nhanVien; }
    public void setNhanVien(NhanVien nhanVien) { this.nhanVien = nhanVien; }

    public KhachHang getKhachHang() { return khachHang; }
    public void setKhachHang(KhachHang khachHang) { this.khachHang = khachHang; }

    public LocalDateTime getNgayNhan() { return ngayNhan; }
    public void setNgayNhan(LocalDateTime ngayNhan) { this.ngayNhan = ngayNhan; }

    public LocalDateTime getNgayTra() { return ngayTra; }
    public void setNgayTra(LocalDateTime ngayTra) { this.ngayTra = ngayTra; }

    public String getDsMaPhong() { return dsMaPhong; }
    public void setDsMaPhong(String dsMaPhong) { this.dsMaPhong = dsMaPhong; }

    public int getSoLuongPhong() { return soLuongPhong; }
    public void setSoLuongPhong(int soLuongPhong) { this.soLuongPhong = soLuongPhong; }

    public int getSoDemLuuTru() { return soDemLuuTru; }
    public void setSoDemLuuTru(int soDemLuuTru) {
        if (soDemLuuTru < 0) {
            throw new IllegalArgumentException("Số đếm lưu trữ phải >= 0");
        }
        this.soDemLuuTru = soDemLuuTru;
    }

    public double getTongTienPhong() { return tongTienPhong; }
    public void setTongTienPhong(double tongTienPhong) {
        if (tongTienPhong < 0) {
            throw new IllegalArgumentException("Tổng tiền phòng phải >= 0");
        }
        this.tongTienPhong = tongTienPhong;
    }

    public double getTienDatCoc() { return tienDatCoc; }
    public void setTienDatCoc(double tienDatCoc) {
        if (tienDatCoc < 0) {
            throw new IllegalArgumentException("Tiền đặt cọc phải >= 0");
        }
        this.tienDatCoc = tienDatCoc;
    }

    public double getTienConLai() { return tienConLai; }
    public void setTienConLai(double tienConLai) {
        if (tienConLai < 0) {
            throw new IllegalArgumentException("Tiền còn lại phải >= 0");
        }
        this.tienConLai = tienConLai;
    }

    public double getTyLeDatCoc() { return tyLeDatCoc; }
    public void setTyLeDatCoc(double tyLeDatCoc) {
        if (tyLeDatCoc < 0 || tyLeDatCoc > 100) {
            throw new IllegalArgumentException("Tỷ lệ đặt cọc phải nằm trong khoảng 0-100");
        }
        this.tyLeDatCoc = tyLeDatCoc;
    }

    public String getHinhThucDatCoc() { return hinhThucDatCoc; }
    public void setHinhThucDatCoc(String hinhThucDatCoc) {
        this.hinhThucDatCoc = hinhThucDatCoc == null ? null : hinhThucDatCoc.trim();
    }

    public LocalDateTime getNgayDatCoc() { return ngayDatCoc; }
    public void setNgayDatCoc(LocalDateTime ngayDatCoc) { this.ngayDatCoc = ngayDatCoc; }

    public NhanVien getNhanVienNhanCoc() { return nhanVienNhanCoc; }
    public void setNhanVienNhanCoc(NhanVien nhanVienNhanCoc) { this.nhanVienNhanCoc = nhanVienNhanCoc; }

    public String getGhiChuDatCoc() { return ghiChuDatCoc; }
    public void setGhiChuDatCoc(String ghiChuDatCoc) {
        this.ghiChuDatCoc = ghiChuDatCoc == null ? null : ghiChuDatCoc.trim();
    }

    public double getSoTienKhachDua() { return soTienKhachDua; }
    public void setSoTienKhachDua(double soTienKhachDua) {
        this.soTienKhachDua = soTienKhachDua;
    }

    public double getSoTienThucNhan() { return soTienThucNhan; }
    public void setSoTienThucNhan(double soTienThucNhan) {
        this.soTienThucNhan = soTienThucNhan;
    }

    public double getTienThuaTraKhach() { return tienThuaTraKhach; }
    public void setTienThuaTraKhach(double tienThuaTraKhach) {
        this.tienThuaTraKhach = tienThuaTraKhach;
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDatPhong);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PhieuDatPhong other = (PhieuDatPhong) obj;
        return Objects.equals(maDatPhong, other.maDatPhong);
    }
}
