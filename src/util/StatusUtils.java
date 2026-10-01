package util;

import java.text.Normalizer;

public final class StatusUtils {
    private StatusUtils() {
    }

    public static BookingStatus toBookingStatus(String raw) {
        String n = squash(normalize(raw));

        if (matchesAny(n, "DATHANHTOAN", "DATRAPHONG", "TRAPHONG", "THANHTOAN")) {
            return BookingStatus.DA_THANH_TOAN;
        }
        if (matchesAny(n, "DAHUY", "HUY")) {
            return BookingStatus.DA_HUY;
        }
        if (matchesAny(n, "DANHANPHONG", "NHANPHONG", "DANGO")) {
            return BookingStatus.DA_NHAN_PHONG;
        }
        if (matchesAny(n, "CHOCOC", "CHODATCOC")) {
            return BookingStatus.CHO_COC;
        }
        if (matchesAny(n, "DADATCOC", "DATCOC", "DAGIUCOC")) {
            return BookingStatus.DA_DAT_COC;
        }
        if (matchesAny(n, "CHOXACNHAN")) {
            return BookingStatus.CHO_XAC_NHAN;
        }
        if (matchesAny(n, "DAXACNHAN", "XACNHAN")) {
            return BookingStatus.DA_XAC_NHAN;
        }
        return BookingStatus.CHO_XAC_NHAN;
    }

    public static RoomStatus toRoomStatus(String raw) {
        String n = squash(normalize(raw));

        if (matchesAny(n, "SUACHUA", "BAOTRI")) {
            return RoomStatus.SUA_CHUA;
        }
        if (matchesAny(n, "DANGO", "DANHAN", "NHANPHONG", "DANHANPHONG")) {
            return RoomStatus.DANG_O;
        }
        if (matchesAny(n, "CHOXACNHAN", "DADAT", "DANGGIUPHONG")) {
            return RoomStatus.CHO_XAC_NHAN;
        }
        return RoomStatus.TRONG;
    }

    public static String bookingCode(String raw) {
        return toBookingStatus(raw).getCode();
    }

    public static String roomCode(String raw) {
        return toRoomStatus(raw).getCode();
    }

    public static String bookingLabel(String raw) {
        return toBookingStatus(raw).getDisplayName();
    }

    public static String roomLabel(String raw) {
        return toRoomStatus(raw).getDisplayName();
    }

    public static boolean isBookingStatus(String raw, BookingStatus expected) {
        return toBookingStatus(raw) == expected;
    }

    public static boolean isRoomStatus(String raw, RoomStatus expected) {
        return toRoomStatus(raw) == expected;
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }

        String normalized = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replace('\u0110', 'D')
                .replace('\u0111', 'd')
                .replace('Đ', 'D')
                .replace('đ', 'd')
                .replaceAll("\\p{M}+", "");

        return normalized
                .replaceAll("[^\\p{Alnum}]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase();
    }

    private static String squash(String value) {
        return value == null ? "" : value.replace("_", "");
    }

    private static boolean matchesAny(String value, String... candidates) {
        for (String candidate : candidates) {
            if (candidate.equalsIgnoreCase(value)) {
                return true;
            }
        }
        return false;
    }
}
