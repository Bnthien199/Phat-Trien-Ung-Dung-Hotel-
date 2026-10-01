package entity;

import java.time.LocalDateTime;

public class HuyDatPhong {
    private int maHuy;
    private String maDatPhong;
    private KhachHang khachHang;
    private LocalDateTime ngayDatCoc;
    private LocalDateTime ngayHuy;
    private int soNgaySauDatCoc;
    private double tienCocBanDau;
    private double tyLeHoanCoc;
    private double tienHoanLai;
    private double tienKhachSanGiu;
    private String lyDoHuy;
    private String hinhThucHoanTien;
    private NhanVien nhanVienXuLy;
    private LocalDateTime thoiGianXuLy;

    public int getMaHuy() {
        return maHuy;
    }

    public void setMaHuy(int maHuy) {
        this.maHuy = maHuy;
    }

    public String getMaDatPhong() {
        return maDatPhong;
    }

    public void setMaDatPhong(String maDatPhong) {
        this.maDatPhong = maDatPhong;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public LocalDateTime getNgayDatCoc() {
        return ngayDatCoc;
    }

    public void setNgayDatCoc(LocalDateTime ngayDatCoc) {
        this.ngayDatCoc = ngayDatCoc;
    }

    public LocalDateTime getNgayHuy() {
        return ngayHuy;
    }

    public void setNgayHuy(LocalDateTime ngayHuy) {
        this.ngayHuy = ngayHuy;
    }

    public int getSoNgaySauDatCoc() {
        return soNgaySauDatCoc;
    }

    public void setSoNgaySauDatCoc(int soNgaySauDatCoc) {
        this.soNgaySauDatCoc = soNgaySauDatCoc;
    }

    public double getTienCocBanDau() {
        return tienCocBanDau;
    }

    public void setTienCocBanDau(double tienCocBanDau) {
        this.tienCocBanDau = tienCocBanDau;
    }

    public double getTyLeHoanCoc() {
        return tyLeHoanCoc;
    }

    public void setTyLeHoanCoc(double tyLeHoanCoc) {
        this.tyLeHoanCoc = tyLeHoanCoc;
    }

    public double getTienHoanLai() {
        return tienHoanLai;
    }

    public void setTienHoanLai(double tienHoanLai) {
        this.tienHoanLai = tienHoanLai;
    }

    public double getTienKhachSanGiu() {
        return tienKhachSanGiu;
    }

    public void setTienKhachSanGiu(double tienKhachSanGiu) {
        this.tienKhachSanGiu = tienKhachSanGiu;
    }

    public String getLyDoHuy() {
        return lyDoHuy;
    }

    public void setLyDoHuy(String lyDoHuy) {
        this.lyDoHuy = lyDoHuy;
    }

    public String getHinhThucHoanTien() {
        return hinhThucHoanTien;
    }

    public void setHinhThucHoanTien(String hinhThucHoanTien) {
        this.hinhThucHoanTien = hinhThucHoanTien;
    }

    public NhanVien getNhanVienXuLy() {
        return nhanVienXuLy;
    }

    public void setNhanVienXuLy(NhanVien nhanVienXuLy) {
        this.nhanVienXuLy = nhanVienXuLy;
    }

    public LocalDateTime getThoiGianXuLy() {
        return thoiGianXuLy;
    }

    public void setThoiGianXuLy(LocalDateTime thoiGianXuLy) {
        this.thoiGianXuLy = thoiGianXuLy;
    }
}
