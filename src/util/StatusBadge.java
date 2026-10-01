package util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Factory tạo badge trạng thái đồng bộ cho toàn hệ thống.
 * Dùng trong setCellFactory của TableColumn, hoặc đứng độc lập.
 */
public final class StatusBadge {

    private StatusBadge() {}

    // ── Booking status badge ─────────────────────────────────────────

    public static Label forBooking(String rawCode) {
        BookingStatus status = BookingStatus.fromCode(rawCode);
        String bg, fg;
        switch (status) {
            case CHO_COC       -> { bg = "#fef9c3"; fg = "#854d0e"; }
            case DA_DAT_COC    -> { bg = "#dcfce7"; fg = "#166534"; }
            case CHO_XAC_NHAN  -> { bg = AppTheme.STATUS_CHO_XAC_NHAN_BG;  fg = AppTheme.STATUS_CHO_XAC_NHAN_FG; }
            case DA_XAC_NHAN   -> { bg = AppTheme.STATUS_DA_XAC_NHAN_BG;   fg = AppTheme.STATUS_DA_XAC_NHAN_FG; }
            case DA_NHAN_PHONG -> { bg = AppTheme.STATUS_DA_NHAN_PHONG_BG; fg = AppTheme.STATUS_DA_NHAN_PHONG_FG; }
            case DA_THANH_TOAN -> { bg = AppTheme.STATUS_DA_THANH_TOAN_BG; fg = AppTheme.STATUS_DA_THANH_TOAN_FG; }
            case DA_HUY        -> { bg = AppTheme.STATUS_DA_HUY_BG;        fg = AppTheme.STATUS_DA_HUY_FG; }
            default            -> { bg = "#f1f5f9"; fg = AppTheme.MUTED; }
        }
        return buildBadge(status.getDisplayName(), bg, fg);
    }

    // ── Room status badge ─────────────────────────────────────────────

    public static Label forRoom(String rawCode) {
        RoomStatus status = StatusUtils.toRoomStatus(rawCode);
        String bg, fg;
        switch (status) {
            case TRONG        -> { bg = AppTheme.ROOM_TRONG_BG;   fg = AppTheme.ROOM_TRONG_FG; }
            case DANG_O       -> { bg = AppTheme.ROOM_DANG_O_BG;  fg = AppTheme.ROOM_DANG_O_FG; }
            case CHO_XAC_NHAN -> { bg = AppTheme.ROOM_PENDING_BG; fg = AppTheme.ROOM_PENDING_FG; }
            case SUA_CHUA     -> { bg = AppTheme.ROOM_REPAIR_BG;  fg = AppTheme.ROOM_REPAIR_FG; }
            default           -> { bg = "#f1f5f9"; fg = AppTheme.MUTED; }
        }
        return buildBadge(status.getDisplayName(), bg, fg);
    }

    // ── Generic ──────────────────────────────────────────────────────

    public static Label buildBadge(String text, String bgHex, String fgHex) {
        Label badge = new Label(text);
        badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        badge.setTextFill(Color.web(fgHex));
        badge.setStyle(
            "-fx-background-color: " + bgHex + ";"
            + "-fx-padding: 3 10 3 10;"
            + "-fx-background-radius: 999;"
        );
        return badge;
    }

    // ── Dot badge (icon + text) ───────────────────────────────────────

    public static HBox dotBadge(String text, String dotColor) {
        Circle dot = new Circle(4, Color.web(dotColor));
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", 12));
        lbl.setTextFill(Color.web(AppTheme.TEXT_SUB));
        HBox box = new HBox(6, dot, lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ── TableCell factory helper ──────────────────────────────────────

    /** Dùng: colTrangThai.setCellFactory(StatusBadge.bookingCellFactory()); */
    public static <T> javafx.util.Callback<
            javafx.scene.control.TableColumn<T, String>,
            javafx.scene.control.TableCell<T, String>> bookingCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : forBooking(item));
                setAlignment(Pos.CENTER);
            }
        };
    }

    public static <T> javafx.util.Callback<
            javafx.scene.control.TableColumn<T, String>,
            javafx.scene.control.TableCell<T, String>> roomCellFactory() {
        return col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : forRoom(item));
                setAlignment(Pos.CENTER);
            }
        };
    }
}
