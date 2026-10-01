package entity;

public class LoaiNgay {
    private String maLoaiNgay;
    private String tenLoaiNgay;
    private boolean trangThai;
    private boolean addNewItem;

    public LoaiNgay(String maLoaiNgay, String tenLoaiNgay, boolean trangThai) {
        this(maLoaiNgay, tenLoaiNgay, trangThai, false);
    }

    public LoaiNgay(String maLoaiNgay, String tenLoaiNgay, boolean trangThai, boolean addNewItem) {
        this.maLoaiNgay = maLoaiNgay;
        this.tenLoaiNgay = tenLoaiNgay;
        this.trangThai = trangThai;
        this.addNewItem = addNewItem;
    }

    public static LoaiNgay addNewItem() {
        return new LoaiNgay("", "+ Thêm loại ngày", true, true);
    }

    public String getMaLoaiNgay() {
        return maLoaiNgay;
    }

    public void setMaLoaiNgay(String maLoaiNgay) {
        this.maLoaiNgay = maLoaiNgay;
    }

    public String getTenLoaiNgay() {
        return tenLoaiNgay;
    }

    public void setTenLoaiNgay(String tenLoaiNgay) {
        this.tenLoaiNgay = tenLoaiNgay;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public boolean isAddNewItem() {
        return addNewItem;
    }

    public void setAddNewItem(boolean addNewItem) {
        this.addNewItem = addNewItem;
    }

    @Override
    public String toString() {
        return tenLoaiNgay != null ? tenLoaiNgay : "";
    }
}
