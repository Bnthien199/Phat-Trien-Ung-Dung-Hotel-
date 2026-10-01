package entity;

import java.util.Objects;

public class Phong {
    private String maPhong;
    private String loaiPhong;
    private String maLoai;
    private int soNguoi;
    private int tang;
    private String trangThai;
    private double giaPhong;

    public Phong() {
    }

    public Phong(String maPhong) {
        this.maPhong = maPhong;
    }

    public Phong(String maPhong, String loaiPhong, int soNguoi, String trangThai, double giaPhong) {
        this(maPhong, loaiPhong, soNguoi, 1, trangThai, giaPhong);
    }

    public Phong(String maPhong, String loaiPhong, int soNguoi, int tang, String trangThai, double giaPhong) {
        this.maPhong = maPhong;
        this.loaiPhong = loaiPhong;
        this.soNguoi = soNguoi;
        this.tang = tang;
        this.trangThai = trangThai;
        this.giaPhong = giaPhong;
    }

    public String getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(String maPhong) {
        if (maPhong == null || !maPhong.matches("^P\\d{3,}$")) {
            throw new IllegalArgumentException("Mã phòng phải bắt đầu bằng P và theo sau là ít nhất 3 chữ số");
        }
        this.maPhong = maPhong;
    }

    public String getLoaiPhong() {
        return loaiPhong;
    }

    public String getMaLoai() {
        return maLoai;
    }

    public String getMaLoaiPhong() {
        return maLoai;
    }

    public void setLoaiPhong(String loaiPhong) {
        if (loaiPhong == null || loaiPhong.trim().isEmpty()) {
            throw new IllegalArgumentException("Loại phòng không được rỗng");
        }
        this.loaiPhong = loaiPhong.trim();
    }

    public void setMaLoaiPhong(String maLoaiPhong) {
        if (maLoaiPhong == null || maLoaiPhong.trim().isEmpty()) {
            this.maLoai = null;
            return;
        }
        this.maLoai = maLoaiPhong.trim();
    }

    public int getSoNguoi() {
        return soNguoi;
    }

    public void setSoNguoi(int soNguoi) {
        if (soNguoi <= 0) {
            throw new IllegalArgumentException("Số người phải > 0");
        }
        this.soNguoi = soNguoi;
    }

    public int getTang() {
        return tang;
    }

    public void setTang(int tang) {
        if (tang < 0 || tang > 200) {
            throw new IllegalArgumentException("Tầng không hợp lệ");
        }
        this.tang = tang;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        if (trangThai == null || trangThai.trim().isEmpty()) {
            throw new IllegalArgumentException("Trạng thái không được rỗng");
        }
        this.trangThai = trangThai.trim();
    }

    public double getGiaPhong() {
        return giaPhong;
    }

    public void setGiaPhong(double giaPhong) {
        if (giaPhong < 0) {
            throw new IllegalArgumentException("Giá phòng phải >= 0");
        }
        this.giaPhong = giaPhong;
    }

    public int getSoGiuong() {
        return getSoNguoi();
    }

    public void setSoGiuong(int soGiuong) {
        setSoNguoi(soGiuong);
    }

    public int getSucChua() {
        return getSoNguoi();
    }

    public void setSucChua(int sucChua) {
        setSoNguoi(sucChua);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maPhong);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Phong other = (Phong) obj;
        return Objects.equals(maPhong, other.maPhong);
    }

    @Override
    public String toString() {
        return "Phong [maPhong=" + maPhong + ", loaiPhong=" + loaiPhong + ", tang=" + tang + ", trangThai=" + trangThai + "]";
    }
}
