package util;

/**
 * Design system tokens — dùng nhất quán trong toàn bộ project.
 * Thay đổi màu tại đây = cập nhật toàn app.
 */
public final class AppTheme {

    // ── Sidebar ───────────────────────────────────────────────────────
    public static final String SIDEBAR_BG      = "#0f172a";
    public static final String SIDEBAR_TOP_END = "#0f766e";
    public static final String SIDEBAR_ACTIVE  = "rgba(96,165,250,0.16)";
    public static final String SIDEBAR_HOVER   = "rgba(96,165,250,0.10)";
    public static final String SIDEBAR_ACTIVE_ACCENT = "#60a5fa";

    // ── Page backgrounds ─────────────────────────────────────────────
    public static final String BG_LIGHT  = "#f4f7fb";
    public static final String BG_PAGE   = "#f8fafb";
    public static final String SURFACE   = "#ffffff";

    // ── Brand colors ─────────────────────────────────────────────────
    public static final String PRIMARY       = "#0d9488";
    public static final String PRIMARY_HOVER = "#0f766e";
    public static final String TEAL         = "#0d9488";
    public static final String TEAL_DARK    = "#0f766e";
    public static final String TEAL_LIGHT   = "#f0fdfa";
    public static final String SECONDARY       = "#f1f5f9";
    public static final String SECONDARY_HOVER = "#e2e8f0";
    public static final String SECONDARY_TEXT  = "#475569";

    // ── Semantic colors ──────────────────────────────────────────────
    public static final String INFO    = "#2563eb";
    public static final String SUCCESS = "#0f766e";
    public static final String WARNING = "#d97706";
    public static final String DANGER  = "#dc2626";

    // ── Status badge colors (tương ứng BookingStatus enum) ───────────
    public static final String STATUS_CHO_XAC_NHAN_BG   = "#fef3c7";
    public static final String STATUS_CHO_XAC_NHAN_FG   = "#b45309";
    public static final String STATUS_DA_XAC_NHAN_BG    = "#dbeafe";
    public static final String STATUS_DA_XAC_NHAN_FG    = "#0f766e";
    public static final String STATUS_DA_NHAN_PHONG_BG  = "#d1fae5";
    public static final String STATUS_DA_NHAN_PHONG_FG  = "#065f46";
    public static final String STATUS_DA_THANH_TOAN_BG  = "#ecfeff";
    public static final String STATUS_DA_THANH_TOAN_FG  = "#115e59";
    public static final String STATUS_DA_HUY_BG         = "#fee2e2";
    public static final String STATUS_DA_HUY_FG         = "#991b1b";

    // ── Room status badge colors ──────────────────────────────────────
    public static final String ROOM_TRONG_BG   = "#ccfbf1";
    public static final String ROOM_TRONG_FG   = "#0f766e";
    public static final String ROOM_DANG_O_BG  = "#fef3c7";
    public static final String ROOM_DANG_O_FG  = "#b45309";
    public static final String ROOM_PENDING_BG = "#ffedd5";
    public static final String ROOM_PENDING_FG = "#c2410c";
    public static final String ROOM_REPAIR_BG  = "#f1f5f9";
    public static final String ROOM_REPAIR_FG  = "#64748b";

    // ── Text ─────────────────────────────────────────────────────────
    public static final String TEXT       = "#0f172a";
    public static final String TEXT_SUB   = "#475569";
    public static final String TEXT_MED   = TEXT_SUB;
    public static final String MUTED      = "#64748b";
    public static final String TEXT_LIGHT = "#94a3b8";

    // ── Border / dividers ────────────────────────────────────────────
    public static final String BORDER       = "#e2e8f0";
    public static final String BORDER_LIGHT = "#f1f5f9";

    // ── Shadow snippets (dùng trong -fx-effect) ──────────────────────
    public static final String SHADOW_SM = "dropshadow(gaussian, rgba(0,0,0,0.05), 6,  0, 0, 1)";
    public static final String SHADOW_MD = "dropshadow(gaussian, rgba(0,0,0,0.07), 12, 0, 0, 3)";
    public static final String SHADOW_LG = "dropshadow(gaussian, rgba(0,0,0,0.10), 20, 0, 0, 6)";

    // ── Spacing constants (px) ────────────────────────────────────────
    public static final int SPACE_XS  = 4;
    public static final int SPACE_SM  = 8;
    public static final int SPACE_MD  = 16;
    public static final int SPACE_LG  = 24;
    public static final int SPACE_XL  = 32;

    // ── Border radius (px) ────────────────────────────────────────────
    public static final int RADIUS_SM  = 8;
    public static final int RADIUS_MD  = 12;
    public static final int RADIUS_LG  = 16;
    public static final int RADIUS_XL  = 20;
    public static final int RADIUS_PILL = 999;

    private AppTheme() {}
}
