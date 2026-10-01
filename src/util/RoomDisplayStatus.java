package util;

public enum RoomDisplayStatus {
    TRONG("Trống", AppTheme.TEAL, AppTheme.ROOM_TRONG_BG, "TRONG", "*"),
    DA_DAT("Chờ check-in", AppTheme.ROOM_PENDING_FG, AppTheme.ROOM_PENDING_BG, "CHO_XAC_NHAN", "*"),
    DANG_O("Đang ở", AppTheme.ROOM_DANG_O_FG, AppTheme.ROOM_DANG_O_BG, "DANG_O", "*"),
    SUA_CHUA("Sửa chữa", AppTheme.ROOM_REPAIR_FG, AppTheme.ROOM_REPAIR_BG, "SUA_CHUA", "*"),
    DA_THANH_TOAN("Đã thanh toán", AppTheme.TEXT_MED, "#e2e8f0", "DA_THANH_TOAN", "*");


    private final String label;
    private final String color;
    private final String backgroundColor;
    private final String filterValue;
    private final String icon;

    RoomDisplayStatus(String label, String color, String backgroundColor, String filterValue, String icon) {
        this.label = label;
        this.color = color;
        this.backgroundColor = backgroundColor;
        this.filterValue = filterValue;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public String getIcon() {
        return icon;
    }
}
