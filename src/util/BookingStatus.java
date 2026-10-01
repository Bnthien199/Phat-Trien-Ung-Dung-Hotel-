package util;

public enum BookingStatus {
    CHO_COC("CHO_COC", "Chờ cọc"),
    DA_DAT_COC("DA_DAT_COC", "Đã đặt cọc"),
    CHO_XAC_NHAN("CHO_XAC_NHAN", "Chờ check-in"),
    DA_XAC_NHAN("DA_XAC_NHAN", "Đã xác nhận"),
    DA_NHAN_PHONG("DA_NHAN_PHONG", "Đã nhận phòng"),
    DA_THANH_TOAN("DA_THANH_TOAN", "Đã thanh toán"),
    DA_HUY("DA_HUY", "Đã hủy");

    private final String code;
    private final String displayName;

    BookingStatus(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static BookingStatus fromCode(String code) {
        return StatusUtils.toBookingStatus(code);
    }

    public static String[] allCodes() {
        BookingStatus[] values = values();
        String[] codes = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            codes[i] = values[i].code;
        }
        return codes;
    }
}
