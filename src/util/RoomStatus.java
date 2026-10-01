package util;

public enum RoomStatus {
    TRONG("TRONG", "Trống"),
    DANG_O("DANG_O", "Đang ở"),
    SUA_CHUA("SUA_CHUA", "Sửa chữa"),
    CHO_XAC_NHAN("CHO_XAC_NHAN", "Đang giữ phòng");

    private final String code;
    private final String displayName;

    RoomStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }
}
