package gui;

import dao.ChiTietHoaDonDAO;
import dao.ChiTietPhieuDatDAO;
import dao.HoaDonDAO;
import dao.PhieuDatPhongDAO;
import dao.PhongDAO;
import entity.ChiTietHoaDon;
import entity.ChiTietPhieuDat;
import entity.HoaDon;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuDatPhong;
import entity.Phong;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import util.AppTheme;
import util.RoomAvailabilityService;
import util.RoomDisplayStatus;
import util.StatusUtils;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.ContentDisplay;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RoomView {

    // ═══════════════════════════════════════════════════════════════
    //  PREMIUM THEME
    // ═══════════════════════════════════════════════════════════════
    private static final String BG = AppTheme.BG_LIGHT;
    private static final String CARD_BG = AppTheme.SURFACE;
    private static final String TEXT_DARK = AppTheme.TEXT;
    private static final String TEXT_MED = AppTheme.TEXT_SUB;
    private static final String TEXT_LIGHT = AppTheme.TEXT_LIGHT;
    private static final String BORDER = AppTheme.BORDER;

    // Status colors
    private static final String CLR_TRONG = AppTheme.ROOM_TRONG_FG;
    private static final String CLR_DA_DAT = AppTheme.ROOM_PENDING_FG;
    private static final String CLR_DANG_O = AppTheme.ROOM_DANG_O_FG;
    private static final String CLR_SUA_CHUA = AppTheme.ROOM_REPAIR_FG;

    private static final String CLR_TRONG_BG = AppTheme.ROOM_TRONG_BG;
    private static final String CLR_DA_DAT_BG = AppTheme.ROOM_PENDING_BG;
    private static final String CLR_DANG_O_BG = AppTheme.ROOM_DANG_O_BG;
    private static final String CLR_SUA_CHUA_BG = AppTheme.ROOM_REPAIR_BG;
    private static final String STATUS_FILTER_ALL = "Tất cả trạng thái";

    private final PhongDAO phongDAO = new PhongDAO();
    private final RoomAvailabilityService roomAvailabilityService = new RoomAvailabilityService();
    private final ChiTietPhieuDatDAO chiTietDAO = new ChiTietPhieuDatDAO();
    private final PhieuDatPhongDAO phieuDatDAO = new PhieuDatPhongDAO();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final ChiTietHoaDonDAO chiTietHoaDonDAO = new ChiTietHoaDonDAO();
    private final DecimalFormat df = new DecimalFormat("#,###");
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final NhanVien loggedInNhanVien;

    private FlowPane roomContainer;
    private List<Phong> allRooms = new ArrayList<>();
    private final Map<String, RoomDisplayStatus> roomStatusCache = new HashMap<>();
    private final Map<String, GuestInfo> roomGuestCache = new HashMap<>();
    private ComboBox<String> cbType, cbFloor, cbStatus;
    private DatePicker dpFilterDate;
    private Spinner<Integer> spFilterHour, spFilterMinute;
    private TextField txtSearch;
    private Label lblRoomCount;

    private boolean isGridView = false;
    private Button btnCardView;
    private Button btnGridView;
    private StackPane viewContainer;
    private ScrollPane cardGridView;
    private TableView<RoomDisplayModel> tableGridView;

    // Stat labels
    private Label statTotal, statOccupied, statVacant, statBooked;

    // Detail panel (right side)
    private VBox detailPanel;
    private VBox detailContent;
    private ScrollPane detailScrollPane;
    private boolean detailVisible = false;

    private java.util.function.Consumer<Phong> onBookingRequest;
    private java.util.function.Consumer<Phong> onCheckoutRequest;
    private int statusLoadVersion;
    private boolean isInitialLoading = true;
    private boolean suppressFilterEvents;
    private String selectedRoomCode;

    // ═══════════════════════════════════════════════════════════════
    //  GUEST INFO HOLDER
    // ═══════════════════════════════════════════════════════════════
    private static class GuestInfo {
        String tenKhach;
        String sdt;
        String maDatPhong;
        LocalDateTime ngayNhan;
        LocalDateTime ngayTra;
        double giaThue;
        String trangThaiPhieu;
        String trangThaiChiTiet;
        String trangThaiHoaDon;
        String soNguoiDangO;
    }

    // ═══════════════════════════════════════════════════════════════
    //  ROOM DISPLAY MODEL FOR GRID VIEW
    // ═══════════════════════════════════════════════════════════════
    public static class RoomDisplayModel {
        private final int stt;
        private final Phong room;
        private final RoomDisplayStatus status;
        private final GuestInfo guest;
        private final String roomCode;
        private final String roomType;
        private final int floor;
        private final int capacity;
        private final double price;
        private final String guestName;
        private final String bookingCode;
        private final String stayDuration;

        public RoomDisplayModel(int stt, Phong room, RoomDisplayStatus status, GuestInfo guest, LocalDate selectedDate) {
            this.stt = stt;
            this.room = room;
            this.status = status;
            this.guest = guest;
            this.roomCode = room.getMaPhong();
            this.roomType = room.getLoaiPhong();
            this.floor = room.getTang();
            this.capacity = room.getSoNguoi();
            this.price = room.getGiaPhong();
            this.guestName = (guest != null && guest.tenKhach != null) ? guest.tenKhach : "—";
            this.bookingCode = (guest != null && guest.maDatPhong != null) ? guest.maDatPhong : "—";
            this.stayDuration = (guest != null) ? formatStayRangeShort(guest, selectedDate) : "—";
        }

        private String formatStayRangeShort(GuestInfo gi, LocalDate selectedDate) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String from = gi.ngayNhan != null ? gi.ngayNhan.format(formatter) : "-";
            String to = gi.ngayTra != null ? gi.ngayTra.format(formatter) : "-";
            return from + " -> " + to;
        }

        public int getStt() { return stt; }
        public Phong getRoom() { return room; }
        public RoomDisplayStatus getStatus() { return status; }
        public GuestInfo getGuest() { return guest; }
        public String getRoomCode() { return roomCode; }
        public String getRoomType() { return roomType; }
        public int getFloor() { return floor; }
        public int getCapacity() { return capacity; }
        public double getPrice() { return price; }
        public String getGuestName() { return guestName; }
        public String getBookingCode() { return bookingCode; }
        public String getStayDuration() { return stayDuration; }
    }

    public RoomView(NhanVien loggedInNhanVien) {
        this.loggedInNhanVien = loggedInNhanVien;
    }

    public void setOnBookingRequest(java.util.function.Consumer<Phong> callback) {
        this.onBookingRequest = callback;
    }

    public void setOnCheckoutRequest(java.util.function.Consumer<Phong> callback) {
        this.onCheckoutRequest = callback;
    }

    // ═══════════════════════════════════════════════════════════════
    //  MAIN VIEW
    // ═══════════════════════════════════════════════════════════════
    public Node createView() {
        HBox mainLayout = new HBox(0);
        mainLayout.setStyle("-fx-background-color: " + BG + ";");
        mainLayout.setFillHeight(true);
        HBox.setHgrow(mainLayout, Priority.ALWAYS);
        VBox.setVgrow(mainLayout, Priority.ALWAYS);

        // Left: room grid
        VBox leftPane = new VBox(0);
        leftPane.setStyle("-fx-background-color: " + BG + ";");
        leftPane.setFillWidth(true);
        leftPane.setMinWidth(0);
        HBox.setHgrow(leftPane, Priority.ALWAYS);
        VBox.setVgrow(leftPane, Priority.ALWAYS);

        cardGridView = createRoomGrid();
        tableGridView = createTableGridView();

        VBox tableWrapper = new VBox(tableGridView);
        tableWrapper.setPadding(new Insets(4, 32, 32, 32));
        tableWrapper.setStyle("-fx-background-color: transparent;");

        viewContainer = new StackPane(cardGridView, tableWrapper);
        VBox.setVgrow(viewContainer, Priority.ALWAYS);
        HBox.setHgrow(viewContainer, Priority.ALWAYS);

        isGridView = false;
        switchViewMode();

        leftPane.getChildren().addAll(createHeader(), createStatsBar(), createFilterBar(), viewContainer);

        // Right: detail panel (hidden by default)
        detailPanel = createDetailPanel();
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);

        mainLayout.getChildren().addAll(leftPane, detailPanel);

        // Delay load để UI kịp render trước
        javafx.application.Platform.runLater(() -> loadData());
        return mainLayout;
    }

    // ═══════════════════════════════════════════════════════════════
    //  HEADER
    // ═══════════════════════════════════════════════════════════════
    private HBox createHeader() {
        HBox header = new HBox(16);
        header.setPadding(new Insets(28, 32, 16, 32));
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(52, 52);
        iconWrap.setMinSize(52, 52);
        iconWrap.setStyle("-fx-background-color: " + AppTheme.TEAL_LIGHT + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: #cfeeea;"
                + "-fx-border-radius: 18;");
        Label iconLbl = new Label("🛏");
        iconLbl.setFont(Font.font(23));
        iconWrap.getChildren().add(iconLbl);

        VBox titleBox = new VBox(2);
        Label lblTitle = new Label("Sơ đồ phòng");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        lblTitle.setTextFill(Color.web(TEXT_DARK));

        Label lblSub = new Label("Theo dõi trạng thái phòng, lọc nhanh và mở chi tiết đặt phòng ngay tại sơ đồ.");
        lblSub.setFont(Font.font("Segoe UI", 13));
        lblSub.setTextFill(Color.web(TEXT_MED));
        titleBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Room count badge
        lblRoomCount = new Label("— phòng");
        lblRoomCount.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblRoomCount.setTextFill(Color.web(AppTheme.PRIMARY));
        lblRoomCount.setStyle("-fx-background-color: white;"
                + "-fx-padding: 8 16; -fx-background-radius: 12;"
                + "-fx-border-color: " + BORDER + "; -fx-border-radius: 12;");

        Button btnRefresh = new Button("↻ Làm mới");
        btnRefresh.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        String btnNorm = "-fx-background-color: " + AppTheme.TEAL + "; -fx-text-fill: white;"
                + "-fx-background-radius: 12; -fx-padding: 10 18; -fx-cursor: hand;"
                + "-fx-effect: " + AppTheme.SHADOW_SM + ";";
        String btnHov = "-fx-background-color: " + AppTheme.TEAL_DARK + "; -fx-text-fill: white;"
                + "-fx-background-radius: 12; -fx-padding: 10 18; -fx-cursor: hand;"
                + "-fx-effect: " + AppTheme.SHADOW_MD + ";";
        btnRefresh.setStyle(btnNorm);
        btnRefresh.setOnMouseEntered(e -> btnRefresh.setStyle(btnHov));
        btnRefresh.setOnMouseExited(e -> btnRefresh.setStyle(btnNorm));
        btnRefresh.setOnAction(e -> handleRefresh(btnRefresh, btnNorm));

        header.getChildren().addAll(iconWrap, titleBox, spacer, lblRoomCount, btnRefresh);
        return header;
    }

    private void handleRefresh(Button btnRefresh, String normalStyle) {
        btnRefresh.setDisable(true);
        btnRefresh.setText("\u23f3 \u0110ang t\u1ea3i...");
        resetFiltersToCurrentTime();
        loadData();

        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(Duration.millis(800));
        pause.setOnFinished(ev -> {
            btnRefresh.setText("\u21bb L\u00e0m m\u1edbi");
            btnRefresh.setStyle(normalStyle);
            btnRefresh.setDisable(false);
        });
        pause.play();
    }

    private void resetFiltersToCurrentTime() {
        suppressFilterEvents = true;
        try {
            if (txtSearch != null) {
                txtSearch.clear();
            }
            if (cbType != null) {
                cbType.setValue("T\u1ea5t c\u1ea3 lo\u1ea1i ph\u00f2ng");
            }
            if (cbFloor != null) {
                cbFloor.setValue("T\u1ea5t c\u1ea3 t\u1ea7ng");
            }
            if (cbStatus != null) {
                cbStatus.setValue(STATUS_FILTER_ALL);
            }

            LocalDateTime now = LocalDateTime.now();
            if (dpFilterDate != null) {
                dpFilterDate.setValue(now.toLocalDate());
            }
            setSpinnerValue(spFilterHour, now.getHour());
            setSpinnerValue(spFilterMinute, now.getMinute());
        } finally {
            suppressFilterEvents = false;
        }
    }

    private void setSpinnerValue(Spinner<Integer> spinner, int value) {
        if (spinner == null || spinner.getValueFactory() == null) {
            return;
        }
        spinner.getValueFactory().setValue(value);
        if (spinner.getEditor() != null) {
            spinner.getEditor().setText(String.valueOf(value));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  STATS BAR
    // ═══════════════════════════════════════════════════════════════
    private HBox createStatsBar() {
        HBox statsBar = new HBox(14);
        statsBar.setPadding(new Insets(0, 32, 16, 32));

        statTotal = new Label("0");
        statOccupied = new Label("0");
        statVacant = new Label("0");
        statBooked = new Label("0");

        statsBar.getChildren().addAll(
                createMiniStat("Tổng", statTotal, CLR_TRONG, CLR_TRONG_BG),
                createMiniStat("Đang ở", statOccupied, CLR_DANG_O, CLR_DANG_O_BG),
                createMiniStat("Trống", statVacant, CLR_TRONG, CLR_TRONG_BG),
                createMiniStat("Chờ check-in", statBooked, CLR_DA_DAT, CLR_DA_DAT_BG)
        );
        return statsBar;
    }

    private HBox createMiniStat(String title, Label valueLabel, String color, String bgColor) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12, 18, 12, 18));
        box.setStyle("-fx-background-color: white; -fx-background-radius: 14;"
                + "-fx-border-color: " + BORDER + "; -fx-border-radius: 14; -fx-border-width: 1;"
                + "-fx-effect: " + AppTheme.SHADOW_SM + ";");

        Circle dot = new Circle(5);
        dot.setFill(Color.web(color));

        Label lbl = new Label(title);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        lbl.setTextFill(Color.web(TEXT_MED));

        valueLabel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        valueLabel.setTextFill(Color.web(color));

        box.getChildren().addAll(dot, lbl, valueLabel);
        return box;
    }

    // ═══════════════════════════════════════════════════════════════
    //  FILTER BAR
    // ═══════════════════════════════════════════════════════════════
    private HBox createFilterBar() {
        HBox bar = new HBox(12);
        bar.setPadding(new Insets(0, 32, 16, 32));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: transparent;");

        txtSearch = new TextField();
        txtSearch.setPromptText("🔎 Tìm mã phòng...");
        txtSearch.setPrefHeight(40);
        txtSearch.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        txtSearch.setStyle(baseFilterControlStyle());
        txtSearch.textProperty().addListener((o, ov, nv) -> {
            if (!suppressFilterEvents) renderRooms();
        });

        cbType = new ComboBox<>();
        cbType.getItems().add("Tất cả loại phòng");
        cbType.setValue("Tất cả loại phòng");
        cbType.setPrefHeight(40);
        cbType.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cbType, Priority.ALWAYS);
        cbType.setStyle(baseFilterControlStyle());
        cbType.setOnAction(e -> {
            if (!suppressFilterEvents) renderRooms();
        });

        cbFloor = new ComboBox<>();
        cbFloor.getItems().add("Tất cả tầng");
        cbFloor.setValue("Tất cả tầng");
        cbFloor.setPrefHeight(40);
        cbFloor.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cbFloor, Priority.ALWAYS);
        cbFloor.setStyle(baseFilterControlStyle());
        cbFloor.setOnAction(e -> {
            if (!suppressFilterEvents) renderRooms();
        });

        cbStatus = new ComboBox<>();
        cbStatus.getItems().add(STATUS_FILTER_ALL);
        cbStatus.setValue(STATUS_FILTER_ALL);
        cbStatus.setPrefHeight(40);
        cbStatus.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cbStatus, Priority.ALWAYS);
        cbStatus.setStyle(baseFilterControlStyle());
        cbStatus.setOnAction(e -> {
            if (!suppressFilterEvents) renderRooms();
        });

        dpFilterDate = new DatePicker(LocalDate.now());
        dpFilterDate.setPrefHeight(40);
        dpFilterDate.setStyle(baseFilterControlStyle());
        dpFilterDate.setOnAction(e -> {
            if (!suppressFilterEvents) loadRoomStatusesAsync();
        });

        LocalTime now = LocalTime.now();
        spFilterHour = new Spinner<>(0, 23, now.getHour());
        spFilterHour.setPrefWidth(96);
        spFilterHour.setMinWidth(96);
        spFilterHour.setPrefHeight(40);
        spFilterHour.setEditable(true);
        spFilterHour.setStyle(baseFilterControlStyle());
        spFilterHour.valueProperty().addListener((o, ov, nv) -> {
            if (!suppressFilterEvents) loadRoomStatusesAsync();
        });
        spFilterHour.getEditor().textProperty().addListener((o, ov, nv) -> {
            if (!suppressFilterEvents) loadRoomStatusesAsync();
        });

        spFilterMinute = new Spinner<>(0, 59, now.getMinute());
        spFilterMinute.setPrefWidth(96);
        spFilterMinute.setMinWidth(96);
        spFilterMinute.setPrefHeight(40);
        spFilterMinute.setEditable(true);
        spFilterMinute.setStyle(baseFilterControlStyle());
        spFilterMinute.valueProperty().addListener((o, ov, nv) -> {
            if (!suppressFilterEvents) loadRoomStatusesAsync();
        });
        spFilterMinute.getEditor().textProperty().addListener((o, ov, nv) -> {
            if (!suppressFilterEvents) loadRoomStatusesAsync();
        });

        Label lblTimeSeparator = new Label(":");
        lblTimeSeparator.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        lblTimeSeparator.setTextFill(Color.web(TEXT_MED));

        HBox timeBox = new HBox(4, spFilterHour, lblTimeSeparator, spFilterMinute);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        btnCardView = new Button("☰ Dạng thẻ");
        btnGridView = new Button("▤ Dạng lưới");
        updateViewModeButtonsStyle();

        btnCardView.setOnAction(e -> {
            if (isGridView) {
                isGridView = false;
                updateViewModeButtonsStyle();
                switchViewMode();
                renderRooms();
            }
        });

        btnGridView.setOnAction(e -> {
            if (!isGridView) {
                isGridView = true;
                updateViewModeButtonsStyle();
                switchViewMode();
                renderRooms();
            }
        });

        bar.getChildren().addAll(txtSearch, cbType, cbFloor, cbStatus, dpFilterDate, timeBox, spacer, btnCardView, btnGridView);
        return bar;
    }

    private String baseFilterControlStyle() {
        return "-fx-background-color: white;"
                + "-fx-background-radius: 12;"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-radius: 12;"
                + "-fx-border-width: 1;"
                + "-fx-padding: 0 12;"
                + "-fx-font-size: 13;";
    }

    // ═══════════════════════════════════════════════════════════════
    //  ROOM GRID
    // ═══════════════════════════════════════════════════════════════
    private ScrollPane createRoomGrid() {
        roomContainer = new FlowPane(16, 16);
        roomContainer.setPadding(new Insets(4, 32, 32, 32));
        roomContainer.setPrefWrapLength(900);
        roomContainer.setMinWidth(0);

        ScrollPane sp = new ScrollPane(roomContainer);
        sp.setFitToWidth(true);
        sp.setFitToHeight(false);
        sp.setPannable(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setStyle("-fx-background-color: transparent; -fx-background: " + BG + ";"
                + "-fx-padding: 0;");
        sp.viewportBoundsProperty().addListener((obs, oldBounds, bounds) -> {
            if (bounds != null) {
                roomContainer.setPrefWrapLength(Math.max(0, bounds.getWidth() - 64));
            }
        });
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ═══════════════════════════════════════════════════════════════
    //  DETAIL PANEL (RIGHT SIDE)
    // ═══════════════════════════════════════════════════════════════
    private VBox createDetailPanel() {
        VBox panel = new VBox(0);
        panel.setPrefWidth(380);
        panel.setMinWidth(380);
        panel.setMaxWidth(380);
        panel.setStyle("-fx-background-color: " + CARD_BG + ";"
                + "-fx-border-color: " + BORDER + ";"
                + "-fx-border-width: 0 0 0 1;"
                + "-fx-effect: " + AppTheme.SHADOW_MD + ";");
        panel.setFillWidth(true);

        detailContent = new VBox(0);
        detailContent.setFillWidth(true);
        detailContent.setMinWidth(0);
        detailContent.setStyle("-fx-background-color: " + CARD_BG + ";");

        detailScrollPane = new ScrollPane(detailContent);
        detailScrollPane.setFitToWidth(true);
        detailScrollPane.setFitToHeight(false);
        detailScrollPane.setPannable(true);
        detailScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailScrollPane.setPrefWidth(380);
        detailScrollPane.setStyle("-fx-background-color: transparent; -fx-background: " + CARD_BG + ";"
                + "-fx-padding: 0;");

        VBox.setVgrow(detailScrollPane, Priority.ALWAYS);
        panel.getChildren().add(detailScrollPane);
        VBox.setVgrow(panel, Priority.ALWAYS);
        return panel;
    }

    private void showDetailPanel(Phong room, RoomDisplayStatus status) {
        if (status == null) {
            status = RoomDisplayStatus.TRONG;
        }
        selectedRoomCode = room != null ? room.getMaPhong() : null;
        LocalDateTime selectedDateTime = getSelectedDateTime();
        LocalDate selectedDate = selectedDateTime.toLocalDate();
        detailContent.getChildren().clear();

        String statusColor = getStatusColor(status);
        String statusBg = getStatusBg(status);

        // Close button
        HBox topBar = new HBox();
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setPadding(new Insets(16, 20, 4, 20));
        Button btnClose = new Button("✕");
        btnClose.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: " + TEXT_MED + ";"
                + "-fx-font-size: 14; -fx-cursor: hand; -fx-padding: 6 10; -fx-background-radius: 10;");
        btnClose.setOnAction(e -> hideDetailPanel());
        topBar.getChildren().add(btnClose);

        VBox roomHeader = new VBox(8);
        roomHeader.setPadding(new Insets(0, 24, 18, 24));
        roomHeader.setAlignment(Pos.CENTER);

        StackPane roomBadge = new StackPane();
        roomBadge.setPrefSize(84, 84);
        String roomBadgeStyle = "-fx-background-color: " + statusBg + "; -fx-background-radius: 24;"
                + "-fx-border-color: " + statusColor + "33; -fx-border-radius: 24;"
                + "-fx-effect: dropshadow(gaussian, " + statusColor + "22, 16, 0, 0, 4);";
        roomBadge.setStyle(roomBadgeStyle);
        Label lblRoomIcon = new Label("🛏");
        lblRoomIcon.setFont(Font.font(34));
        roomBadge.getChildren().add(lblRoomIcon);

        Label lblRoomMa = new Label(room.getMaPhong());
        lblRoomMa.setFont(Font.font("Segoe UI", FontWeight.BOLD, 28));
        lblRoomMa.setTextFill(Color.web(TEXT_DARK));

        Label lblStatus = new Label("●  " + status.getLabel());
        lblStatus.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblStatus.setTextFill(Color.web(statusColor));
        lblStatus.setStyle("-fx-background-color: " + statusBg + ";"
                + "-fx-padding: 7 16; -fx-background-radius: 999;");

        Label lblRoomSub = new Label("Chi tiết phòng và thao tác nhanh");
        lblRoomSub.setFont(Font.font("Segoe UI", 12));
        lblRoomSub.setTextFill(Color.web(TEXT_MED));

        roomHeader.getChildren().addAll(roomBadge, lblRoomMa, lblStatus, lblRoomSub);

        // Divider
        Region div1 = new Region();
        div1.setPrefHeight(1);
        div1.setStyle("-fx-background-color: " + BORDER + ";");
        VBox.setMargin(div1, new Insets(0, 24, 0, 24));

        VBox infoSection = new VBox(12);
        infoSection.setPadding(new Insets(20, 24, 20, 24));

        Label lblInfoTitle = new Label("Thông tin phòng");
        lblInfoTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        lblInfoTitle.setTextFill(Color.web(TEXT_DARK));

        infoSection.getChildren().addAll(
                lblInfoTitle,
                infoRow("🛏", "Loại phòng", room.getLoaiPhong()),
                infoRow("🔖", "Mã loại", room.getMaLoai() != null ? room.getMaLoai() : "Chưa cập nhật"),
                infoRow("⌂", "Tầng", "Tầng " + room.getTang()),
                infoRow("👤", "Sức chứa", room.getSoNguoi() + " người"),
                infoRow("₫", "Giá", df.format(room.getGiaPhong()) + " VNĐ/đêm")
        );

        detailContent.getChildren().addAll(topBar, roomHeader, div1, infoSection);
        infoSection.getChildren().add(infoRow("●", "Trạng thái", status.getLabel()));

        GuestInfo guest = loadGuestInfo(room.getMaPhong(), selectedDateTime, status);
        if (guest != null) {
            roomGuestCache.put(room.getMaPhong(), guest);
        } else {
            roomGuestCache.remove(room.getMaPhong());
        }

        if (guest != null) {
            Region div2 = new Region();
            div2.setPrefHeight(1);
            div2.setStyle("-fx-background-color: " + BORDER + ";");
            VBox.setMargin(div2, new Insets(0, 24, 0, 24));

            VBox guestSection = new VBox(12);
            guestSection.setPadding(new Insets(20, 24, 20, 24));

            Label lblGuestTitle = new Label("Thông tin lưu trú");
            lblGuestTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            lblGuestTitle.setTextFill(Color.web(TEXT_DARK));

            guestSection.getChildren().add(lblGuestTitle);

            if (true) {
                guestSection.getChildren().add(infoRow("👤", getGuestLabelForDate(selectedDate, status), guest.tenKhach != null ? guest.tenKhach : "—"));
                guestSection.getChildren().add(infoRow("☎", "SĐT", guest.sdt != null ? guest.sdt : "—"));
                guestSection.getChildren().add(infoRow("📋", "Mã phiếu", guest.maDatPhong != null ? guest.maDatPhong : "—"));

                if (selectedDate.isBefore(LocalDate.now())) {
                    guestSection.getChildren().add(infoRow("📅", "Thời gian lưu trú", formatStayRange(guest)));
                } else {
                    if (guest.ngayNhan != null) {
                        guestSection.getChildren().add(infoRow("📅", "Ngày nhận", guest.ngayNhan.format(dtf)));
                    }
                    if (guest.ngayTra != null) {
                        guestSection.getChildren().add(infoRow("📅", "Ngày trả", guest.ngayTra.format(dtf)));
                    }
                }

                guestSection.getChildren().add(infoRow("●", "Trạng thái", getDisplayStatusTextForDate(selectedDate, status, guest)));
            } else {
                boolean isOccupied = (status == RoomDisplayStatus.DANG_O);
            if (isOccupied) {
                guestSection.getChildren().add(infoRow("👤", "Khách hiện tại", guest.tenKhach != null ? guest.tenKhach : "—"));
                guestSection.getChildren().add(infoRow("☎", "SĐT", guest.sdt != null ? guest.sdt : "—"));
                guestSection.getChildren().add(infoRow("📋", "Mã phiếu", guest.maDatPhong != null ? guest.maDatPhong : "—"));
                if (guest.ngayNhan != null)
                    guestSection.getChildren().add(infoRow("📅", "Ngày nhận", guest.ngayNhan.format(dtf)));
                if (guest.ngayTra != null)
                    guestSection.getChildren().add(infoRow("📅", "Ngày trả", guest.ngayTra.format(dtf)));
                guestSection.getChildren().add(infoRow("●", "Trạng thái", "Đang ở"));
            } else {
                guestSection.getChildren().add(infoRow("👤", "Khách đặt phòng", guest.tenKhach != null ? guest.tenKhach : "—"));
                guestSection.getChildren().add(infoRow("☎", "SĐT", guest.sdt != null ? guest.sdt : "—"));
                guestSection.getChildren().add(infoRow("📋", "Mã phiếu", guest.maDatPhong != null ? guest.maDatPhong : "—"));
                guestSection.getChildren().add(infoRow("●", "Trạng thái", "Chờ nhận phòng"));
            }
            }

            if (guest.giaThue > 0)
                guestSection.getChildren().add(infoRow("₫", "Giá thuê", df.format(guest.giaThue) + " VNĐ"));
            if (guest.soNguoiDangO != null && !guest.soNguoiDangO.contains("Chua co"))
                guestSection.getChildren().add(infoRow("👥", "Số người đang ở", guest.soNguoiDangO));

            detailContent.getChildren().addAll(div2, guestSection);
        } else {
            Region div2 = new Region();
            div2.setPrefHeight(1);
            div2.setStyle("-fx-background-color: " + BORDER + ";");
            VBox.setMargin(div2, new Insets(0, 24, 0, 24));

            VBox guestSection = new VBox(12);
            guestSection.setPadding(new Insets(20, 24, 20, 24));

            Label lblGuestTitle = new Label("Thông tin lưu trú");
            lblGuestTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            lblGuestTitle.setTextFill(Color.web(TEXT_DARK));

            guestSection.getChildren().addAll(
                    lblGuestTitle,
                    infoRow("ℹ", "Thông tin", "Không có khách trong ngày này")
            );
            detailContent.getChildren().addAll(div2, guestSection);
        }

        VBox serviceSection = buildServiceSection(room, guest);
        if (serviceSection != null) {
            Region div3 = new Region();
            div3.setPrefHeight(1);
            div3.setStyle("-fx-background-color: " + BORDER + ";");
            VBox.setMargin(div3, new Insets(0, 24, 0, 24));
            detailContent.getChildren().addAll(div3, serviceSection);
        }

        // Action buttons
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        detailContent.getChildren().add(spacer);

        VBox actionBox = new VBox(10);
        actionBox.setPadding(new Insets(16, 24, 24, 24));
        final GuestInfo selectedGuest = guest;

        if (status == RoomDisplayStatus.TRONG) {
            Button btnBook = createActionButton("🛏  Đặt phòng này", AppTheme.TEAL, AppTheme.TEAL_DARK);
            btnBook.setPrefHeight(48);
            btnBook.setOnAction(e -> {
                if (onBookingRequest != null) onBookingRequest.accept(room);
            });
            actionBox.getChildren().add(btnBook);
        } else if (status == RoomDisplayStatus.DANG_O) {
            Button btnAddService = createActionButton("➕  Thêm dịch vụ", AppTheme.WARNING, "#b45309");
            btnAddService.setOnAction(e -> openAddServiceDialog(room, selectedGuest));
            Button btnCheckout = createActionButton("🔑 Trả phòng / Thanh toán", CLR_DANG_O, "#059669");
            btnCheckout.setOnAction(e -> {
                if (onCheckoutRequest != null) onCheckoutRequest.accept(room);
            });
            actionBox.getChildren().add(btnAddService);
            actionBox.getChildren().add(btnCheckout);
        } else if (status == RoomDisplayStatus.DA_DAT) {
            Button btnCheckin = createActionButton("✅ Nhận phòng", AppTheme.TEAL, AppTheme.TEAL_DARK);
            btnCheckin.setOnAction(e -> {
                if (onBookingRequest != null) onBookingRequest.accept(room);
            });
            actionBox.getChildren().add(btnCheckin);
        }

        detailContent.getChildren().add(actionBox);

        // Show with animation
        detailScrollPane.setVvalue(0);

        if (!detailVisible) {
            detailPanel.setVisible(true);
            detailPanel.setManaged(true);
            detailPanel.setTranslateX(380);
            TranslateTransition tt = new TranslateTransition(Duration.millis(250), detailPanel);
            tt.setFromX(380);
            tt.setToX(0);
            tt.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
            tt.play();
            detailVisible = true;
        }
    }

    private void hideDetailPanel() {
        if (detailVisible) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(200), detailPanel);
            tt.setFromX(0);
            tt.setToX(380);
            tt.setOnFinished(e -> {
                detailPanel.setVisible(false);
                detailPanel.setManaged(false);
            });
            tt.play();
            detailVisible = false;
        }
    }

    private HBox infoRow(String emoji, String label, String value) {
        String displayEmoji = normalizeDisplayText(emoji);
        String displayLabel = normalizeDisplayText(label);
        String displayValue = normalizeDisplayText(value);
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 14;"
                + "-fx-border-color: #edf2f7; -fx-border-radius: 14;");

        Label lblEmoji = new Label(displayEmoji);
        lblEmoji.setFont(Font.font(14));
        lblEmoji.setMinWidth(24);

        VBox textBox = new VBox(1);
        Label lblLabel = new Label(displayLabel);
        lblLabel.setFont(Font.font("Segoe UI", 11));
        lblLabel.setTextFill(Color.web(TEXT_LIGHT));
        Label lblValue = new Label(displayValue);
        lblValue.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblValue.setTextFill(Color.web(TEXT_DARK));
        textBox.getChildren().addAll(lblLabel, lblValue);

        row.getChildren().addAll(lblEmoji, textBox);
        return row;
    }

    private String normalizeDisplayText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text
                .replace("â—", "")
                .replace("â„¹", "")
                .replace("â‚«", "")
                .replace("ðŸ‘¤", "")
                .replace("ðŸ‘¥", "")
                .replace("â˜Ž", "")
                .replace("ðŸ“‹", "")
                .replace("ðŸ“…", "")
                .replace("Ã°Å¸â€˜Â¤", "")
                .replace("Ã¢ËœÅ½", "")
                .replace("Ã°Å¸â€œâ€¹", "")
                .replace("Ã°Å¸â€œâ€¦", "")
                .replace("Ã¢â€”Â", "")
                .replace("ThÃ´ng tin lÆ°u trÃº", "Thông tin lưu trú")
                .replace("Tráº¡ng thÃ¡i", "Trạng thái")
                .replace("Tráº¡ng thÃ¡i:", "Trạng thái:")
                .replace("KhÃ¡ch hiá»‡n táº¡i", "Khách hiện tại")
                .replace("KhÃ¡ch Ä‘áº·t phÃ²ng", "Khách đặt phòng")
                .replace("KhÃ¡ch Ä‘Ã£ lÆ°u trÃº", "Khách đã ở")
                .replace("KhÃ¡ch Ä‘Ã£ Ä‘áº·t", "Khách đặt phòng")
                .replace("SÄT", "SĐT")
                .replace("MÃ£ phiáº¿u", "Mã phiếu")
                .replace("NgÃ y nháº­n", "Ngày nhận")
                .replace("NgÃ y tráº£", "Ngày trả")
                .replace("GiÃ¡ thuÃª", "Giá thuê")
                .replace("Sá»‘ ngÆ°á»i Ä‘ang á»Ÿ", "Số người đang ở")
                .replace("ThÃ¡Â»Âi gian lÃ†Â°u trÃƒÂº", "Thời gian lưu trú")
                .replace("KhÃ´ng cÃ³ khÃ¡ch trong ngÃ y nÃ y", "Không có khách trong ngày này")
                .replace("Äang á»Ÿ", "Đang ở")
                .replace("Chá» nháº­n phÃ²ng", "Chờ check-in")
                .replace("Ã¢â‚¬â€", "-")
                .trim();
        return normalized;
    }

    private Button createActionButton(String text, String color, String hoverColor) {
        Button btn = new Button(text);
        btn.setPrefHeight(44);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        String norm = "-fx-background-color: " + color + "; -fx-text-fill: white;"
                + "-fx-background-radius: 12; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, " + color + "44, 8, 0, 0, 2);";
        String hov = "-fx-background-color: " + hoverColor + "; -fx-text-fill: white;"
                + "-fx-background-radius: 12; -fx-cursor: hand;"
                + "-fx-effect: dropshadow(gaussian, " + hoverColor + "66, 12, 0, 0, 3);";
        btn.setStyle(norm);
        btn.setOnMouseEntered(e -> btn.setStyle(hov));
        btn.setOnMouseExited(e -> btn.setStyle(norm));
        return btn;
    }

    private VBox buildServiceSection(Phong room, GuestInfo guest) {
        VBox serviceSection = new VBox(10);
        serviceSection.setPadding(new Insets(20, 24, 20, 24));

        Label title = new Label("Dịch vụ đã thêm");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
        title.setTextFill(Color.web(TEXT_DARK));
        serviceSection.getChildren().add(title);

        if (guest == null || guest.maDatPhong == null || guest.maDatPhong.isBlank()) {
            serviceSection.getChildren().add(infoRow("ℹ", "Dịch vụ", "Chưa có dữ liệu dịch vụ cho phòng này"));
            return serviceSection;
        }

        System.out.println("[DEBUG] Loading services for booking " + guest.maDatPhong + " and room " + room.getMaPhong());
        HoaDon hoaDon = hoaDonDAO.getHoaDonByMaPhieu(guest.maDatPhong);
        if (hoaDon == null || hoaDon.getMaHoaDon() == null || hoaDon.getMaHoaDon().isBlank()) {
            serviceSection.getChildren().add(infoRow("ℹ", "Dịch vụ", "Chưa phát sinh dịch vụ"));
            return serviceSection;
        }

        List<ChiTietHoaDon> details = chiTietHoaDonDAO.getByHoaDonAndRoom(hoaDon.getMaHoaDon(), room.getMaPhong());
        if (details.isEmpty()) {
            serviceSection.getChildren().add(infoRow("ℹ", "Dịch vụ", "Chưa phát sinh dịch vụ"));
            return serviceSection;
        }

        double total = 0;
        for (ChiTietHoaDon detail : details) {
            String serviceName = detail.getDichVu() != null
                    ? safeValue(detail.getDichVu().getTenDichVu())
                    : "Dịch vụ";
            double lineTotal = detail.getSoLuong() * detail.getDonGiaLuuTru();
            total += lineTotal;
            serviceSection.getChildren().add(infoRow("•", safeValue(serviceName),
                    detail.getSoLuong() + " x " + df.format(detail.getDonGiaLuuTru()) + " = " + df.format(lineTotal) + " VNĐ"));
        }
        serviceSection.getChildren().add(infoRow("₫", "Tổng dịch vụ", df.format(total) + " VNĐ"));
        return serviceSection;
    }

    private void openAddServiceDialog(Phong room, GuestInfo guest) {
        if (room == null) {
            showWarning("Không có phòng được chọn.");
            return;
        }
        if (guest == null || guest.maDatPhong == null || guest.maDatPhong.isBlank()) {
            showWarning("Phòng này chưa có khách đang ở, không thể thêm dịch vụ.");
            return;
        }

        HoaDon hoaDon;
        try {
            hoaDon = ensureHoaDonExistsForBooking(guest.maDatPhong);
        } catch (RuntimeException ex) {
            showError("Không thể chuẩn bị hóa đơn cho phòng " + room.getMaPhong(), ex);
            return;
        }

        CheckoutServiceDialog dialog = new CheckoutServiceDialog(detailPanel.getScene().getWindow());
        HoaDon currentInvoice = hoaDon;
        dialog.show(room.getMaPhong(), item -> {
            boolean saved = chiTietHoaDonDAO.saveOrUpdateService(
                    currentInvoice.getMaHoaDon(),
                    room.getMaPhong(),
                    item.getDichVu().getMaDichVu(),
                    item.getSoLuong(),
                    item.getDonGia()
            );
            if (!saved) {
                throw new RuntimeException("Không thể lưu dịch vụ cho phòng " + room.getMaPhong());
            }
            refreshSelectedRoomDetail();
        });
    }

    private HoaDon ensureHoaDonExistsForBooking(String maDatPhong) {
        HoaDon existing = hoaDonDAO.getHoaDonByMaPhieu(maDatPhong);
        if (existing != null) {
            return existing;
        }
        if (loggedInNhanVien == null) {
            throw new RuntimeException("Thiếu nhân viên đăng nhập để tạo hóa đơn tạm.");
        }

        HoaDon draft = new HoaDon(
                hoaDonDAO.generateNextId(),
                LocalDateTime.now(),
                0.08,
                0,
                0,
                new PhieuDatPhong(maDatPhong),
                loggedInNhanVien
        );
        if (!hoaDonDAO.create(draft)) {
            throw new RuntimeException("Tạo hóa đơn tạm thất bại cho phiếu " + maDatPhong);
        }
        return draft;
    }

    private void refreshSelectedRoomDetail() {
        if (selectedRoomCode == null || selectedRoomCode.isBlank()) {
            return;
        }
        GuestInfo refreshedGuest = loadGuestInfo(selectedRoomCode,
                getSelectedDateTime(),
                roomStatusCache.getOrDefault(selectedRoomCode, RoomDisplayStatus.TRONG));
        if (refreshedGuest != null) {
            roomGuestCache.put(selectedRoomCode, refreshedGuest);
        } else {
            roomGuestCache.remove(selectedRoomCode);
        }
        for (Phong room : allRooms) {
            if (selectedRoomCode.equals(room.getMaPhong())) {
                showDetailPanel(room, roomStatusCache.getOrDefault(selectedRoomCode, RoomDisplayStatus.TRONG));
                break;
            }
        }
    }

    private void refreshVisibleDetailPanel() {
        if (!detailVisible || selectedRoomCode == null || selectedRoomCode.isBlank()) {
            return;
        }
        for (Phong room : allRooms) {
            if (selectedRoomCode.equals(room.getMaPhong())) {
                showDetailPanel(room, roomStatusCache.getOrDefault(selectedRoomCode, RoomDisplayStatus.TRONG));
                break;
            }
        }
    }

    private boolean hasStayData(RoomDisplayStatus status) {
        return status == RoomDisplayStatus.DANG_O
                || status == RoomDisplayStatus.DA_DAT
                || status == RoomDisplayStatus.DA_THANH_TOAN;
    }

    private String getStayLabelForDate(LocalDate selectedDate) {
        LocalDate targetDate = selectedDate != null ? selectedDate : LocalDate.now();
        LocalDate today = LocalDate.now();
        if (targetDate.isBefore(today)) {
            return "Khách đã lưu trú";
        }
        if (targetDate.isAfter(today)) {
            return "Khách đã đặt";
        }
        return "Khách hiện tại";
    }

    private String getGuestLabelForDate(LocalDate selectedDate, RoomDisplayStatus status) {
        if (selectedDate == null) {
            return "Khách lưu trú";
        }
        LocalDate today = LocalDate.now();
        if (selectedDate.isBefore(today)) {
            return "Khách đã ở";
        }
        if (selectedDate.isAfter(today)) {
            return status == RoomDisplayStatus.DANG_O ? "Khách lưu trú" : "Khách đặt phòng";
        }
        if (status == RoomDisplayStatus.DA_DAT) {
            return "Khách đặt phòng";
        }
        return "Khách hiện tại";
    }

    private String getDisplayStatusTextForDate(LocalDate selectedDate, RoomDisplayStatus status, GuestInfo guest) {
        if (selectedDate == null) {
            return status != null ? status.getLabel() : "Trống";
        }
        LocalDate today = LocalDate.now();
        if (selectedDate.isBefore(today)) {
            return guest != null && guest.trangThaiHoaDon != null ? "Đã thanh toán" : "Đã trả phòng";
        }
        if (selectedDate.isAfter(today)) {
            return RoomDisplayStatus.DA_DAT.equals(status) ? "Chờ check-in" : status.getLabel();
        }
        if (RoomDisplayStatus.DA_DAT.equals(status)) {
            return "Chờ check-in";
        }
        if (RoomDisplayStatus.DANG_O.equals(status)) {
            return "Đang ở";
        }
        return status != null ? status.getLabel() : "Trống";
    }

    private String formatStayRange(GuestInfo guest) {
        if (guest == null) {
            return "-";
        }
        String from = guest.ngayNhan != null ? guest.ngayNhan.format(dtf) : "-";
        String to = guest.ngayTra != null ? guest.ngayTra.format(dtf) : "-";
        return from + " -> " + to;
    }

    private boolean isDateInRange(LocalDate selectedDate, LocalDateTime ngayNhan, LocalDateTime ngayTra) {
        if (selectedDate == null || ngayNhan == null) {
            return false;
        }
        LocalDate from = ngayNhan.toLocalDate();
        LocalDate to = ngayTra != null ? ngayTra.toLocalDate() : null;
        return !selectedDate.isBefore(from) && (to == null || !selectedDate.isAfter(to));
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message, Throwable throwable) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Lỗi");
        String detail = throwable != null && throwable.getMessage() != null ? throwable.getMessage() : "";
        alert.setContentText(detail.isBlank() ? message : message + "\n" + detail);
        alert.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  ROOM CARD
    // ═══════════════════════════════════════════════════════════════
    private Node createRoomCard(Phong room, RoomDisplayStatus displayStatus) {
        String statusColor = getStatusColor(displayStatus);
        String statusBg = getStatusBg(displayStatus);
        String statusIcon = getStatusIcon(displayStatus);

        VBox card = new VBox(10);
        card.setPrefWidth(210);
        card.setPrefHeight(188);
        card.setPadding(new Insets(0, 18, 16, 18));
        card.setAlignment(Pos.TOP_LEFT);

        String cardNorm = "-fx-background-color: " + CARD_BG + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: " + statusColor + ";"
                + "-fx-border-width: 1.2;"
                + "-fx-border-radius: 18;"
                + "-fx-effect: " + AppTheme.SHADOW_SM + ";"
                + "-fx-cursor: hand;";
        String cardHov = "-fx-background-color: " + CARD_BG + ";"
                + "-fx-background-radius: 18;"
                + "-fx-border-color: " + statusColor + ";"
                + "-fx-border-width: 1.8;"
                + "-fx-border-radius: 18;"
                + "-fx-effect: dropshadow(gaussian, " + statusColor + "33, 18, 0, 0, 5);"
                + "-fx-cursor: hand;";

        card.setStyle(cardNorm);
        card.setOnMouseEntered(e -> {
            card.setStyle(cardHov);
            card.setTranslateY(-2);
        });
        card.setOnMouseExited(e -> {
            card.setStyle(cardNorm);
            card.setTranslateY(0);
        });

        Region accentBar = new Region();
        accentBar.setPrefHeight(6);
        accentBar.setMinHeight(6);
        accentBar.setMaxWidth(Double.MAX_VALUE);
        accentBar.setStyle("-fx-background-color: " + statusColor + ";"
                + "-fx-background-radius: 18 18 0 0;");

        // Top row: room code + status dot
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(14, 0, 0, 0));
        Label lblMa = new Label(room.getMaPhong());
        lblMa.setFont(Font.font("Segoe UI", FontWeight.BOLD, 21));
        lblMa.setTextFill(Color.web(TEXT_DARK));

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        Label lblStatusBadge = new Label(statusIcon + "  " + displayStatus.getLabel());
        lblStatusBadge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblStatusBadge.setTextFill(Color.web(statusColor));
        lblStatusBadge.setStyle("-fx-background-color: " + statusBg + ";"
                + "-fx-padding: 4 10; -fx-background-radius: 999;");
        topRow.getChildren().addAll(lblMa, topSpacer, lblStatusBadge);

        // Room type
        Label lblLoai = new Label("🛏 " + room.getLoaiPhong());
        lblLoai.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        lblLoai.setTextFill(Color.web(statusColor));
        lblLoai.setStyle("-fx-background-color: " + statusBg + ";"
                + "-fx-padding: 4 10; -fx-background-radius: 999;");

        Label lblTang = new Label("⌂ Tầng " + room.getTang() + "  •  👤 " + room.getSoNguoi() + " người");
        lblTang.setFont(Font.font("Segoe UI", 11));
        lblTang.setTextFill(Color.web(TEXT_MED));

        Label lblGia = new Label("₫ " + df.format(room.getGiaPhong()) + " VND");
        lblGia.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
        lblGia.setTextFill(Color.web(TEXT_DARK));

        Label lblTrangThai = new Label("Trạng thái: " + displayStatus.getLabel());
        lblTrangThai.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        lblTrangThai.setTextFill(Color.web(statusColor));
        lblTrangThai.setStyle("-fx-background-color: " + statusBg + ";"
                + "-fx-padding: 3 10; -fx-background-radius: 999;");

        // Guest name if occupied
        GuestInfo guest = roomGuestCache.get(room.getMaPhong());
        if (guest != null && hasStayData(displayStatus)) {
            Label lblGuest = new Label("👤 " + (guest.tenKhach != null ? guest.tenKhach : "—"));
            lblGuest.setFont(Font.font("Segoe UI", 11));
            lblGuest.setTextFill(Color.web(TEXT_MED));
            lblGuest.setMaxWidth(160);
            lblGuest.setEllipsisString("...");
            card.getChildren().addAll(accentBar, topRow, lblLoai, lblTang, lblGia, lblTrangThai, lblGuest);
        } else {
            card.getChildren().addAll(accentBar, topRow, lblLoai, lblTang, lblGia, lblTrangThai);
        }

        card.setOnMouseClicked(e -> {
            card.setStyle(cardHov);
            showDetailPanel(room, displayStatus);
        });

        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    //  DATA LOADING
    // ═══════════════════════════════════════════════════════════════
    public void loadData() {
        isInitialLoading = true;
        Task<List<Phong>> task = new Task<>() {
            @Override
            protected List<Phong> call() {
                return phongDAO.getAllPhong();
            }
        };
        task.setOnSucceeded(e -> {
            allRooms = task.getValue() != null ? task.getValue() : new ArrayList<>();
            rebuildFilterOptions();
            loadRoomStatusesAsync();
        });
        startDaemonTask(task, "room-view-load-data");
    }

    private void loadRoomStatusesAsync() {
        int requestVersion = ++statusLoadVersion;
        LocalDateTime selectedDateTime = getSelectedDateTime();

        roomContainer.getChildren().clear();
        Label loading = new Label("⏳  Đang tải trạng thái phòng...");
        loading.setFont(Font.font("Segoe UI", 14));
        loading.setTextFill(Color.web(TEXT_LIGHT));
        loading.setPadding(new Insets(40));
        roomContainer.getChildren().add(loading);

        Task<Map<String, Object[]>> task = new Task<>() {
            @Override
            protected Map<String, Object[]> call() {
                Map<String, Object[]> result = new HashMap<>();
                for (Phong room : allRooms) {
                    RoomDisplayStatus st = roomAvailabilityService.getRoomStatusAt(room, selectedDateTime);
                    GuestInfo gi = null;

                    if (hasStayData(st)) {
                        gi = loadGuestInfo(room.getMaPhong(), selectedDateTime, st);
                    }
                    result.put(room.getMaPhong(), new Object[]{st, gi});
                }
                return result;
            }
        };
        task.setOnSucceeded(e -> {
            if (requestVersion != statusLoadVersion) return;
            roomStatusCache.clear();
            roomGuestCache.clear();
            Map<String, Object[]> data = task.getValue();
            for (Map.Entry<String, Object[]> entry : data.entrySet()) {
                roomStatusCache.put(entry.getKey(), (RoomDisplayStatus) entry.getValue()[0]);
                if (entry.getValue()[1] != null) {
                    roomGuestCache.put(entry.getKey(), (GuestInfo) entry.getValue()[1]);
                }
            }
            isInitialLoading = false;
            renderRooms();
            updateStats();
            refreshVisibleDetailPanel();
        });
        task.setOnFailed(e -> {
            if (requestVersion == statusLoadVersion) {
                isInitialLoading = false;
                roomContainer.getChildren().clear();
                Label err = new Label("❌  Không thể tải trạng thái phòng.");
                err.setFont(Font.font("Segoe UI", 14));
                err.setTextFill(Color.web("#ef4444"));
                roomContainer.getChildren().add(err);
            }
            if (task.getException() != null) task.getException().printStackTrace();
        });
        startDaemonTask(task, "room-view-load-status");
    }

    private LocalDateTime getSelectedDateTime() {
        LocalDate date = dpFilterDate != null && dpFilterDate.getValue() != null
                ? dpFilterDate.getValue()
                : LocalDate.now();
        LocalTime now = LocalTime.now();
        int hour = readSpinnerValue(spFilterHour, 0, 23, now.getHour());
        int minute = readSpinnerValue(spFilterMinute, 0, 59, now.getMinute());
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }

    private int readSpinnerValue(Spinner<Integer> spinner, int min, int max, int fallback) {
        int safeFallback = Math.max(min, Math.min(max, fallback));
        if (spinner == null) {
            return safeFallback;
        }

        Integer value = spinner.getValue();
        TextField editor = spinner.getEditor();
        String text = editor != null ? editor.getText() : null;
        if (text != null && !text.isBlank()) {
            try {
                value = Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                value = safeFallback;
            }
        }

        int normalized = value != null ? value : safeFallback;
        normalized = Math.max(min, Math.min(max, normalized));
        if (spinner.getValueFactory() != null && !Objects.equals(spinner.getValue(), normalized)) {
            spinner.getValueFactory().setValue(normalized);
        }
        if (editor != null && !Objects.equals(editor.getText(), String.valueOf(normalized))) {
            editor.setText(String.valueOf(normalized));
        }
        return normalized;
    }

    private GuestInfo loadGuestInfo(String maPhong, LocalDateTime selectedDateTime, RoomDisplayStatus status) {
        try {
            if (maPhong != null && !maPhong.isBlank()) {
                LocalDate selectedDate = selectedDateTime != null ? selectedDateTime.toLocalDate() : LocalDate.now();
                ChiTietPhieuDat ct = chiTietDAO.findStayAt(maPhong, selectedDateTime);

                String maPhieu = null;
                String tenKhachHang = null;
                String trangThai = null;
                String trangThaiHoaDon = null;
                LocalDateTime ngayNhan = null;
                LocalDateTime ngayTra = null;
                boolean isDateInRange = false;

                GuestInfo gi = null;
                if (ct != null && ct.getPhieuDatPhong() != null) {
                    gi = new GuestInfo();
                    gi.maDatPhong = ct.getPhieuDatPhong().getMaDatPhong();
                    gi.ngayNhan = ct.getNgayNhan();
                    gi.ngayTra = ct.getNgayTra();
                    gi.giaThue = ct.getGiaThuePhong();
                    gi.trangThaiPhieu = StatusUtils.bookingCode(ct.getPhieuDatPhong().getTrangThai());
                    gi.trangThaiChiTiet = ct.getTrangThai();
                    gi.soNguoiDangO = "Chua co du lieu";
                    HoaDon hoaDon = hoaDonDAO.getHoaDonByMaPhieu(gi.maDatPhong);
                    gi.trangThaiHoaDon = hoaDon != null ? "DA_THANH_TOAN" : null;

                    KhachHang kh = ct.getPhieuDatPhong().getKhachHang();
                    if (kh != null) {
                        gi.tenKhach = kh.getTenKhachHang();
                        gi.sdt = kh.getSoDienThoai();
                    }

                    maPhieu = gi.maDatPhong;
                    tenKhachHang = gi.tenKhach;
                    trangThai = ct.getPhieuDatPhong().getTrangThai();
                    trangThaiHoaDon = gi.trangThaiHoaDon;
                    ngayNhan = gi.ngayNhan;
                    ngayTra = gi.ngayTra;
                    isDateInRange = isDateInRange(selectedDate, ngayNhan, ngayTra);
                }

                System.out.println("[ROOM MAP] selectedDate = " + selectedDate);
                System.out.println("[ROOM STATUS] maPhong = " + maPhong);
                System.out.println("[BOOKING RANGE] from = " + ngayNhan + ", to = " + ngayTra);
                System.out.println("[MATCH DATE] " + isDateInRange);
                System.out.println("[DISPLAY STATUS] " + status);
                System.out.println("[ROOM DETAIL] maPhong = " + maPhong);
                System.out.println("[ROOM DETAIL] found booking = " + maPhieu);
                System.out.println("[ROOM DETAIL] customer = " + tenKhachHang);
                System.out.println("[ROOM DETAIL] selectedDate = " + selectedDate);
                System.out.println("[ROOM DETAIL] maPhong = " + maPhong);
                System.out.println("[ROOM DETAIL] customer = " + tenKhachHang);
                System.out.println("[ROOM DETAIL] invoiceStatus = " + trangThaiHoaDon);
                System.out.println("[ROOM DETAIL] displayStatus = " + getDisplayStatusTextForDate(selectedDate, status, gi));
                System.out.println("[ROOM DETAIL] bookingStatus = " + trangThai);

                return gi;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  RENDER & FILTER
    // ═══════════════════════════════════════════════════════════════
    private void renderRooms() {
        if (isInitialLoading) return;
        roomContainer.getChildren().clear();
        if (allRooms == null || allRooms.isEmpty()) {
            Label empty = new Label("📭  Không có dữ liệu phòng.");
            empty.setFont(Font.font("Segoe UI", 14));
            empty.setTextFill(Color.web(TEXT_LIGHT));
            empty.setPadding(new Insets(40));
            roomContainer.getChildren().add(empty);
            if (tableGridView != null) {
                tableGridView.getItems().clear();
            }
            return;
        }

        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase();
        String selectedType = cbType.getValue();
        String selectedStatus = cbStatus != null ? cbStatus.getValue() : STATUS_FILTER_ALL;
        Integer selectedFloor = parseSelectedFloor();
        List<Phong> filtered = roomAvailabilityService.filterRoomsByFloor(allRooms, selectedFloor).stream()
                .filter(r -> {
                    if (keyword.isBlank()) return true;
                    String roomCode = r.getMaPhong() != null ? r.getMaPhong().toLowerCase() : "";
                    String roomType = r.getLoaiPhong() != null ? r.getLoaiPhong().toLowerCase() : "";
                    String floorText = ("tang " + r.getTang()).toLowerCase();
                    return roomCode.contains(keyword) || roomType.contains(keyword) || floorText.contains(keyword);
                })
                .filter(r -> "Tất cả loại phòng".equals(selectedType) || r.getLoaiPhong().equals(selectedType))
                .filter(r -> {
                    RoomDisplayStatus status = roomStatusCache.getOrDefault(r.getMaPhong(), RoomDisplayStatus.TRONG);
                    return STATUS_FILTER_ALL.equals(selectedStatus) || status.getLabel().equals(selectedStatus);
                })
                .sorted(Comparator.comparingInt(Phong::getTang).thenComparing(Phong::getMaPhong))
                .collect(Collectors.toList());

        lblRoomCount.setText(filtered.size() + " phòng");

        if (isGridView) {
            LocalDate selectedDate = dpFilterDate.getValue() != null ? dpFilterDate.getValue() : LocalDate.now();
            List<RoomDisplayModel> modelList = new ArrayList<>();
            int stt = 1;
            for (Phong r : filtered) {
                RoomDisplayStatus status = roomStatusCache.getOrDefault(r.getMaPhong(), RoomDisplayStatus.TRONG);
                GuestInfo guest = roomGuestCache.get(r.getMaPhong());
                modelList.add(new RoomDisplayModel(stt++, r, status, guest, selectedDate));
            }
            if (tableGridView != null) {
                tableGridView.getItems().setAll(modelList);
            }
        } else {
            int index = 0;
            for (Phong room : filtered) {
                RoomDisplayStatus st = roomStatusCache.getOrDefault(room.getMaPhong(), RoomDisplayStatus.TRONG);
                Node card = createRoomCard(room, st);

                // Stagger animation
                card.setOpacity(0);
                card.setTranslateY(15);
                FadeTransition ft = new FadeTransition(Duration.millis(250), card);
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.setDelay(Duration.millis(index * 30));
                TranslateTransition tt = new TranslateTransition(Duration.millis(250), card);
                tt.setFromY(15);
                tt.setToY(0);
                tt.setDelay(Duration.millis(index * 30));
                ft.play();
                tt.play();

                roomContainer.getChildren().add(card);
                index++;
            }
        }
    }

    private void updateStats() {
        long total = allRooms.size();
        long occupied = roomStatusCache.values().stream().filter(s -> s == RoomDisplayStatus.DANG_O).count();
        long vacant = roomStatusCache.values().stream().filter(s -> s == RoomDisplayStatus.TRONG).count();
        long booked = roomStatusCache.values().stream().filter(s -> s == RoomDisplayStatus.DA_DAT).count();

        statTotal.setText(String.valueOf(total));
        statOccupied.setText(String.valueOf(occupied));
        statVacant.setText(String.valueOf(vacant));
        statBooked.setText(String.valueOf(booked));
    }

    private void rebuildFilterOptions() {
        List<String> roomTypes = allRooms.stream()
                .map(Phong::getLoaiPhong)
                .filter(Objects::nonNull)
                .distinct().sorted().toList();
        String selType = cbType.getValue();
        cbType.getItems().setAll("Tất cả loại phòng");
        cbType.getItems().addAll(roomTypes);
        cbType.setValue(cbType.getItems().contains(selType) ? selType : "Tất cả loại phòng");

        List<String> floors = allRooms.stream()
                .map(Phong::getTang)
                .distinct().sorted()
                .map(f -> "Tầng " + f).toList();
        String selFloor = cbFloor.getValue();
        cbFloor.getItems().setAll("Tất cả tầng");
        cbFloor.getItems().addAll(floors);
        cbFloor.setValue(cbFloor.getItems().contains(selFloor) ? selFloor : "Tất cả tầng");

        String selectedStatus = cbStatus.getValue();
        cbStatus.getItems().setAll(STATUS_FILTER_ALL);
        for (RoomDisplayStatus status : RoomDisplayStatus.values()) {
            cbStatus.getItems().add(status.getLabel());
        }
        cbStatus.setValue(cbStatus.getItems().contains(selectedStatus) ? selectedStatus : STATUS_FILTER_ALL);
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════
    private String getStatusColor(RoomDisplayStatus s) {
        return s != null ? s.getColor() : CLR_TRONG;
    }

    private String getStatusBg(RoomDisplayStatus s) {
        return s != null ? s.getBackgroundColor() : CLR_TRONG_BG;
    }

    private String getStatusIcon(RoomDisplayStatus s) {
        return s != null ? s.getIcon() : RoomDisplayStatus.TRONG.getIcon();
    }

    private Integer parseSelectedFloor() {
        String value = cbFloor.getValue();
        if (value == null || "Tất cả tầng".equals(value)) return null;
        try {
            return Integer.parseInt(value.replace("Tầng", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void switchViewMode() {
        if (isGridView) {
            if (cardGridView != null) {
                cardGridView.setVisible(false);
                cardGridView.setManaged(false);
            }
            if (tableGridView != null) {
                tableGridView.setVisible(true);
                tableGridView.setManaged(true);
            }
        } else {
            if (cardGridView != null) {
                cardGridView.setVisible(true);
                cardGridView.setManaged(true);
            }
            if (tableGridView != null) {
                tableGridView.setVisible(false);
                tableGridView.setManaged(false);
            }
        }
    }

    private void updateViewModeButtonsStyle() {
        String activeStyle = "-fx-background-color: " + AppTheme.TEAL + "; -fx-text-fill: white;"
                + "-fx-background-radius: 999; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;"
                + "-fx-effect: " + AppTheme.SHADOW_SM + ";";
        String inactiveStyle = "-fx-background-color: white; -fx-text-fill: " + TEXT_DARK + ";"
                + "-fx-background-radius: 999; -fx-padding: 8 16; -fx-font-weight: bold; -fx-cursor: hand;"
                + "-fx-border-color: " + BORDER + "; -fx-border-radius: 999; -fx-border-width: 1;";

        if (btnCardView != null && btnGridView != null) {
            if (isGridView) {
                btnCardView.setStyle(inactiveStyle);
                btnGridView.setStyle(activeStyle);
            } else {
                btnCardView.setStyle(activeStyle);
                btnGridView.setStyle(inactiveStyle);
            }
        }
    }

    private TableView<RoomDisplayModel> createTableGridView() {
        TableView<RoomDisplayModel> table = new TableView<>();
        table.getStyleClass().add("app-table");
        table.setStyle("-fx-background-color: transparent;");

        TableColumn<RoomDisplayModel, Integer> colStt = new TableColumn<>("STT");
        colStt.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStt()));
        colStt.setPrefWidth(60);
        colStt.setStyle("-fx-alignment: CENTER;");

        TableColumn<RoomDisplayModel, String> colMa = new TableColumn<>("Mã phòng");
        colMa.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomCode()));
        colMa.setPrefWidth(100);
        colMa.setStyle("-fx-alignment: CENTER-LEFT; -fx-font-weight: bold;");

        TableColumn<RoomDisplayModel, String> colLoai = new TableColumn<>("Loại phòng");
        colLoai.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getRoomType()));
        colLoai.setPrefWidth(120);

        TableColumn<RoomDisplayModel, Integer> colTang = new TableColumn<>("Tầng");
        colTang.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getFloor()));
        colTang.setPrefWidth(70);
        colTang.setStyle("-fx-alignment: CENTER;");

        TableColumn<RoomDisplayModel, Integer> colCapacity = new TableColumn<>("Sức chứa");
        colCapacity.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCapacity()));
        colCapacity.setPrefWidth(90);
        colCapacity.setStyle("-fx-alignment: CENTER;");

        TableColumn<RoomDisplayModel, String> colGia = new TableColumn<>("Giá phòng");
        colGia.setCellValueFactory(cellData -> new SimpleStringProperty(df.format(cellData.getValue().getPrice()) + " VNĐ"));
        colGia.setPrefWidth(120);
        colGia.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<RoomDisplayModel, RoomDisplayStatus> colStatus = new TableColumn<>("Trạng thái");
        colStatus.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getStatus()));
        colStatus.setPrefWidth(120);
        colStatus.setCellFactory(column -> new TableCell<RoomDisplayModel, RoomDisplayStatus>() {
            @Override
            protected void updateItem(RoomDisplayStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item.getLabel());
                    badge.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                    badge.setStyle(getGridStatusBadgeStyle(item));
                    setGraphic(badge);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        TableColumn<RoomDisplayModel, String> colGuest = new TableColumn<>("Khách đang/đã ở");
        colGuest.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getGuestName()));
        colGuest.setPrefWidth(160);

        TableColumn<RoomDisplayModel, String> colBookingCode = new TableColumn<>("Mã phiếu");
        colBookingCode.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBookingCode()));
        colBookingCode.setPrefWidth(100);

        TableColumn<RoomDisplayModel, String> colStay = new TableColumn<>("Thời gian lưu trú");
        colStay.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStayDuration()));
        colStay.setPrefWidth(220);

        TableColumn<RoomDisplayModel, RoomDisplayModel> colActions = new TableColumn<>("Thao tác");
        colActions.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue()));
        colActions.setPrefWidth(150);
        colActions.setCellFactory(column -> new TableCell<RoomDisplayModel, RoomDisplayModel>() {
            @Override
            protected void updateItem(RoomDisplayModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Button btnDetail = new Button("Chi tiết");
                    btnDetail.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                    btnDetail.setStyle("-fx-background-color: " + AppTheme.SECONDARY + ";"
                            + "-fx-text-fill: " + AppTheme.SECONDARY_TEXT + ";"
                            + "-fx-background-radius: 8; -fx-padding: 4 8; -fx-cursor: hand;"
                            + "-fx-border-color: " + AppTheme.BORDER + "; -fx-border-radius: 8;");
                    btnDetail.setOnAction(e -> {
                        showDetailPanel(item.getRoom(), item.getStatus());
                    });
                    
                    box.getChildren().add(btnDetail);

                    // Add quick actions if room status is active
                    if (item.getStatus() == RoomDisplayStatus.TRONG) {
                        Button btnBook = new Button("Đặt");
                        btnBook.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                        btnBook.setStyle("-fx-background-color: " + AppTheme.TEAL + "; -fx-text-fill: white;"
                                + "-fx-background-radius: 8; -fx-padding: 4 8; -fx-cursor: hand;");
                        btnBook.setOnAction(e -> {
                            if (onBookingRequest != null) onBookingRequest.accept(item.getRoom());
                        });
                        box.getChildren().add(btnBook);
                    } else if (item.getStatus() == RoomDisplayStatus.DA_DAT) {
                        Button btnCheckin = new Button("Nhận");
                        btnCheckin.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                        btnCheckin.setStyle("-fx-background-color: " + AppTheme.TEAL + "; -fx-text-fill: white;"
                                + "-fx-background-radius: 8; -fx-padding: 4 8; -fx-cursor: hand;");
                        btnCheckin.setOnAction(e -> {
                            if (onBookingRequest != null) onBookingRequest.accept(item.getRoom());
                        });
                        box.getChildren().add(btnCheckin);
                    } else if (item.getStatus() == RoomDisplayStatus.DANG_O) {
                        Button btnPay = new Button("Trả");
                        btnPay.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
                        btnPay.setStyle("-fx-background-color: " + CLR_DANG_O + "; -fx-text-fill: white;"
                                + "-fx-background-radius: 8; -fx-padding: 4 8; -fx-cursor: hand;");
                        btnPay.setOnAction(e -> {
                            if (onCheckoutRequest != null) onCheckoutRequest.accept(item.getRoom());
                        });
                        box.getChildren().add(btnPay);
                    }

                    setGraphic(box);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                }
            }
        });

        table.getColumns().addAll(colStt, colMa, colLoai, colTang, colCapacity, colGia, colStatus, colGuest, colBookingCode, colStay, colActions);

        // Table event: select a row shows detail panel
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showDetailPanel(newVal.getRoom(), newVal.getStatus());
            }
        });

        // Column widths responsive
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    private String getGridStatusBadgeStyle(RoomDisplayStatus s) {
        String bg, fg;
        if (s == null) {
            bg = "#ccfbf1"; fg = "#0f766e";
        } else {
            switch (s) {
                case TRONG:
                    bg = "#ccfbf1"; fg = "#0f766e"; // xanh ngọc
                    break;
                case DANG_O:
                    bg = "#fee2e2"; fg = "#dc2626"; // đỏ/cam
                    break;
                case DA_DAT:
                    bg = "#fef3c7"; fg = "#d97706"; // vàng/cam
                    break;
                case DA_THANH_TOAN:
                    bg = "#d1fae5"; fg = "#065f46"; // xanh lá
                    break;
                case SUA_CHUA:
                default:
                    bg = "#f1f5f9"; fg = "#64748b";
                    break;
            }
        }
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg + ";"
                + "-fx-padding: 5 12; -fx-background-radius: 999; -fx-alignment: center; -fx-font-weight: bold;";
    }

    private void startDaemonTask(Task<?> task, String threadName) {
        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }
}
