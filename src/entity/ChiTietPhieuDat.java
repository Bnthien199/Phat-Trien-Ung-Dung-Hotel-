package entity;

import java.time.LocalDateTime;
import java.util.Objects;

public class ChiTietPhieuDat {
    public static final String TRANG_THAI_CHO_NHAN = "CHO_NHAN";
    public static final String TRANG_THAI_DA_NHAN = "DA_NHAN";
    public static final String TRANG_THAI_DA_TRA = "DA_TRA";

    private PhieuDatPhong phieuDatPhong;
    private Phong phong;
    private double giaThuePhong;
    private LocalDateTime ngayNhan;
    private LocalDateTime ngayTra;
    private String trangThai;
    private LocalDateTime thoiDiemNhanThucTe;
    private LocalDateTime thoiDiemTraThucTe;

    public ChiTietPhieuDat() {
        this.trangThai = TRANG_THAI_CHO_NHAN;
    }

    public ChiTietPhieuDat(PhieuDatPhong phieuDatPhong, Phong phong, double giaThuePhong,
                           LocalDateTime ngayNhan, LocalDateTime ngayTra) {
        this(phieuDatPhong, phong, giaThuePhong, ngayNhan, ngayTra, TRANG_THAI_CHO_NHAN, null, null);
    }

    public ChiTietPhieuDat(PhieuDatPhong phieuDatPhong, Phong phong, double giaThuePhong,
                           LocalDateTime ngayNhan, LocalDateTime ngayTra, String trangThai,
                           LocalDateTime thoiDiemNhanThucTe, LocalDateTime thoiDiemTraThucTe) {
        this.phieuDatPhong = phieuDatPhong;
        this.phong = phong;
        this.giaThuePhong = giaThuePhong;
        this.ngayNhan = ngayNhan;
        this.ngayTra = ngayTra;
        this.trangThai = trangThai == null || trangThai.isBlank() ? TRANG_THAI_CHO_NHAN : trangThai;
        this.thoiDiemNhanThucTe = thoiDiemNhanThucTe;
        this.thoiDiemTraThucTe = thoiDiemTraThucTe;
    }

    public PhieuDatPhong getPhieuDatPhong() {
        return phieuDatPhong;
    }

    public void setPhieuDatPhong(PhieuDatPhong phieuDatPhong) {
        this.phieuDatPhong = phieuDatPhong;
    }

    public Phong getPhong() {
        return phong;
    }

    public void setPhong(Phong phong) {
        this.phong = phong;
    }

    public double getGiaThuePhong() {
        return giaThuePhong;
    }

    public void setGiaThuePhong(double giaThuePhong) {
        if (giaThuePhong < 0) {
            throw new IllegalArgumentException("Gia thue phong phai >= 0");
        }
        this.giaThuePhong = giaThuePhong;
    }

    public LocalDateTime getNgayNhan() {
        return ngayNhan;
    }

    public void setNgayNhan(LocalDateTime ngayNhan) {
        this.ngayNhan = ngayNhan;
    }

    public LocalDateTime getNgayTra() {
        return ngayTra;
    }

    public void setNgayTra(LocalDateTime ngayTra) {
        if (ngayTra != null && ngayNhan != null && ngayTra.isBefore(ngayNhan)) {
            throw new IllegalArgumentException("Ngay tra phai >= ngay nhan");
        }
        this.ngayTra = ngayTra;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = (trangThai == null || trangThai.isBlank()) ? TRANG_THAI_CHO_NHAN : trangThai;
    }

    public LocalDateTime getThoiDiemNhanThucTe() {
        return thoiDiemNhanThucTe;
    }

    public void setThoiDiemNhanThucTe(LocalDateTime thoiDiemNhanThucTe) {
        this.thoiDiemNhanThucTe = thoiDiemNhanThucTe;
    }

    public LocalDateTime getThoiDiemTraThucTe() {
        return thoiDiemTraThucTe;
    }

    public void setThoiDiemTraThucTe(LocalDateTime thoiDiemTraThucTe) {
        this.thoiDiemTraThucTe = thoiDiemTraThucTe;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChiTietPhieuDat other = (ChiTietPhieuDat) obj;
        return Objects.equals(phieuDatPhong, other.phieuDatPhong) && Objects.equals(phong, other.phong);
    }

    @Override
    public int hashCode() {
        return Objects.hash(phieuDatPhong, phong);
    }
}
