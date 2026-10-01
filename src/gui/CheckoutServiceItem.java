package gui;

import entity.ChiTietHoaDon;
import entity.DichVu;
import entity.HoaDon;
import entity.Phong;

public class CheckoutServiceItem {
    private final DichVu dichVu;
    private final String maPhong;
    private int soLuong;
    private double donGia;

    public CheckoutServiceItem(DichVu dichVu, int soLuong, double donGia) {
        this(dichVu, null, soLuong, donGia);
    }

    public CheckoutServiceItem(DichVu dichVu, String maPhong, int soLuong, double donGia) {
        this.dichVu = dichVu;
        this.maPhong = maPhong;
        this.soLuong = soLuong;
        this.donGia = donGia;
    }

    public static CheckoutServiceItem fromChiTietHoaDon(ChiTietHoaDon chiTiet) {
        return new CheckoutServiceItem(
                chiTiet.getDichVu(),
                chiTiet.getPhong() != null ? chiTiet.getPhong().getMaPhong() : null,
                chiTiet.getSoLuong(),
                chiTiet.getDonGiaLuuTru()
        );
    }

    public ChiTietHoaDon toChiTietHoaDon(HoaDon hoaDon) {
        return toChiTietHoaDon(hoaDon, maPhong);
    }

    public ChiTietHoaDon toChiTietHoaDon(HoaDon hoaDon, String roomId) {
        return new ChiTietHoaDon(hoaDon, dichVu, roomId != null ? new Phong(roomId) : null, soLuong, donGia);
    }

    public DichVu getDichVu() {
        return dichVu;
    }

    public String getMaPhong() {
        return maPhong;
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = Math.max(1, soLuong);
    }

    public double getDonGia() {
        return donGia;
    }

    public void setDonGia(double donGia) {
        this.donGia = donGia;
    }

    public double getThanhTien() {
        return soLuong * donGia;
    }
}
