package gui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.css.PseudoClass;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dao.BangGiaDetailDAO;
import dao.BangGiaHeaderDAO;
import dao.ChiTietPhieuDatDAO;
import dao.PhieuDatPhongDAO;
import dao.PhongDAO;
import entity.BangGiaDetail;
import entity.BangGiaHeader;
import entity.ChiTietPhieuDat;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuDatPhong;
import entity.Phong;
import util.AppTheme;
import util.BookingStatus;
import util.DepositPolicy;
import util.PricingService;
import util.TextInputSafeUtils;
import util.DepositReceiptPdfExporter;
import util.StatusUtils;

public class BookingView {
    private static final int PHONE_LENGTH = 10;

    private static final String BG_LIGHT = AppTheme.BG_LIGHT;
    private static final String PRIMARY_COLOR = AppTheme.TEAL;
    private static final String PRIMARY_HOVER = AppTheme.TEAL_DARK;
    private static final String SECONDARY_COLOR = AppTheme.SECONDARY;
    private static final String SECONDARY_HOVER = AppTheme.SECONDARY_HOVER;
    private static final String SUCCESS_COLOR = AppTheme.SUCCESS;
    private static final String DANGER_COLOR = AppTheme.DANGER;
    private static final String MUTED_TEXT = AppTheme.MUTED;
    private static final String FIELD_BASE_STYLE = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #d7deea; "
            +
            "-fx-border-radius: 10; -fx-border-width: 1.2; -fx-padding: 0 12 0 12;";
    private static final String FIELD_FOCUS_STYLE = "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: "
            + PRIMARY_COLOR + "; " +
            "-fx-border-radius: 10; -fx-border-width: 1.8; -fx-effect: dropshadow(gaussian, rgba(13,148,136,0.16), 12, 0.2, 0, 2); -fx-padding: 0 12 0 12;";
    private static final String CARD_STYLE = "-fx-background-color: white; -fx-padding: 24; -fx-background-radius: 16;"
            +
            "-fx-border-color: #e5edf8; -fx-border-radius: 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.07), 18, 0.18, 0, 6);";

    private TextField txtSdt, txtTen, txtDiaChi, txtSoGiayTo;
    private RadioButton rbNam, rbNu;
    private ComboBox<String> cbLoaiGiayTo;
    private DatePicker dpNgayNhan, dpNgayTra;
    private Spinner<Integer> spGioNhan, spPhutNhan, spGioTra, spPhutTra;
    private CheckBox chkQuickCheckin;

    private TableView<Phong> tvPhongTrong;
    private VBox selectedRoomsContainer;
    private final List<Phong> selectedRooms = new ArrayList<>();
    private final Map<Phong, Integer> guestCountMap = new HashMap<>();
    private Label lblSelectedRoomSummary;
    private Label lblSoDemLuuTru, lblTongTienPhong, lblTienDatCoc, lblTienConLai;
    private ComboBox<String> cbHinhThucDatCoc;
    private TextArea txtGhiChuDatCoc;
    private TextField txtSoTienKhachDua;
    private Label lblTienThuaTraKhach;
    private HBox rowSoTienKhachDua;
    private HBox rowTienThuaTraKhach;
    private PhieuDatPhong lastSavedDepositBooking;
    private List<Phong> lastSavedDepositRooms = new ArrayList<>();

    private TextField txtFilterMa;
    private ComboBox<String> cbFilterTang, cbFilterLoai, cbFilterGiuong, cbFilterSucChua;

    private ObservableList<Phong> allPhongTrong = FXCollections.observableArrayList();
    private FilteredList<Phong> filteredPhong;

    private StackPane wizardPane;
    private Node step1Panel, step2Panel;

    private final NhanVien loggedInNhanVien;
    private final KhachHangService khService = new KhachHangService();
    private final PhieuDatPhongDAO pdpDAO = new PhieuDatPhongDAO();
    private final ChiTietPhieuDatDAO chiTietPhieuDatDAO = new ChiTietPhieuDatDAO();
    private final BangGiaHeaderDAO bangGiaHeaderDAO = new BangGiaHeaderDAO();
    private final BangGiaDetailDAO bangGiaDetailDAO = new BangGiaDetailDAO();
    private final PricingService pricingService = new PricingService();

    private BangGiaHeader bangGiaHienTai = null;
    private Label lblBangGiaInfo;

    private KhachHang currentKH = null;
    private Task<BangGiaHeader> bangGiaTask = null;
    private Task<KhachHang> searchTask = null;
    private boolean isLoading = false;
    private boolean isAutoFilling = false;
    private double lastSuggestedDepositAmount = -1;
    private String lastResolvedCustomerPhone = "";
    private String lastCustomerLookupKey = "";
    private String lastPromptedPhone = "";
    private Runnable onBookingSaved;

    // ── vacant-rooms section UI state ────────────────────────────────
    private StackPane tableStack;
    private Label lblRoomCount;
    private VBox emptyState;
    private StackPane loadingOverlay;
    private Label emptyStateTitle;
    private Label emptyStateSubtitle;
    private StackPane roomTableIcon;

    private static final PseudoClass ROOM_PICKED = PseudoClass.getPseudoClass("room-picked");

    public BookingView(NhanVien nhanVien) {
        this.loggedInNhanVien = nhanVien;
    }

    public void setOnBookingSaved(Runnable onBookingSaved) {
        this.onBookingSaved = onBookingSaved;
    }

    public Node createView() {
        VBox root = new VBox(0);
        root.setStyle("-fx-background-color: " + BG_LIGHT + ";");
        VBox.setVgrow(root, Priority.ALWAYS);
        String bookingCss = getBookingStylesheet();
        if (!root.getStylesheets().contains(bookingCss)) {
            root.getStylesheets().add(bookingCss);
        }

        HBox header = new HBox();
        header.setPadding(new Insets(25, 30, 15, 30));
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label("LẬP PHIẾU ĐẶT PHÒNG");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        lblTitle.setText("LẬP PHIẾU ĐẶT PHÒNG");
        lblTitle.setTextFill(Color.web("#2c3e50"));
        header.getChildren().add(lblTitle);

        HBox breadcrumb = buildBreadcrumb();
        breadcrumb.setPadding(new Insets(0, 30, 15, 30));

        step1Panel = createStep1Panel();
        step2Panel = createStep2Panel();
        step2Panel.setVisible(false);
        step2Panel.setManaged(false);

        wizardPane = new StackPane(step1Panel, step2Panel);
        VBox.setVgrow(wizardPane, Priority.ALWAYS);

        root.getChildren().addAll(header, breadcrumb, wizardPane);

        loadPhongTrong();
        refreshBangGiaApDung();
        return root;
    }

    private Label lblStep1Crumb, lblStep2Crumb;

    private HBox buildBreadcrumb() {
        lblStep1Crumb = new Label("① Thông tin & Thời gian");
        lblStep1Crumb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblStep1Crumb.setText("① Thông tin & Thời gian");

        Label lblArrow = new Label("  ›  ");
        lblArrow.setText("  ›  ");
        lblArrow.setTextFill(Color.web("#aaa"));

        lblStep2Crumb = new Label("② Chọn phòng & Xác nhận");
        lblStep2Crumb.setFont(Font.font("Segoe UI", 13));
        lblStep2Crumb.setText("② Chọn phòng & Xác nhận");
        lblStep2Crumb.setTextFill(Color.web("#aaa"));

        HBox box = new HBox(4, lblStep1Crumb, lblArrow, lblStep2Crumb);
        box.setAlignment(Pos.CENTER_LEFT);
        updateBreadcrumb(1);
        return box;
    }

    private void updateBreadcrumb(int activeStep) {
        if (activeStep == 1) {
            lblStep1Crumb.setTextFill(Color.web(PRIMARY_COLOR));
            lblStep2Crumb.setTextFill(Color.web("#aaa"));
            lblStep2Crumb.setFont(Font.font("Segoe UI", 13));
            lblStep1Crumb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        } else {
            lblStep1Crumb.setTextFill(Color.web("#aaa"));
            lblStep1Crumb.setFont(Font.font("Segoe UI", 13));
            lblStep2Crumb.setTextFill(Color.web(PRIMARY_COLOR));
            lblStep2Crumb.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        }
    }

    private Node createStep1Panel() {
        VBox panel = new VBox(0);
        panel.setPadding(new Insets(0, 30, 30, 30));
        VBox.setVgrow(panel, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: " + BG_LIGHT + ";");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(22);
        content.setPadding(new Insets(6, 6, 12, 6));

        VBox khCard = new VBox(18);
        khCard.setStyle(CARD_STYLE);

        HBox khHeader = new HBox(10);
        khHeader.setAlignment(Pos.CENTER_LEFT);
        Label lblKH = new Label("1. THÔNG TIN KHÁCH HÀNG");
        lblKH.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblKH.setText("1. THÔNG TIN KHÁCH HÀNG");
        lblKH.setTextFill(Color.web("#0f172a"));
        Region khSpacer = new Region();
        HBox.setHgrow(khSpacer, Priority.ALWAYS);
        Button btnSearchKH = new Button("Tìm khách hàng");
        btnSearchKH.setText("Tìm khách hàng");
        styleSecondaryButton(btnSearchKH);
        btnSearchKH.setOnAction(e -> handleSearchCustomerAction());
        Button btnRefreshCustomer = new Button("Làm mới");
        btnRefreshCustomer.setText("Làm mới");
        styleSecondaryButton(btnRefreshCustomer);
        btnRefreshCustomer.setOnAction(e -> resetCustomerForm());
        khHeader.getChildren().addAll(lblKH, khSpacer, btnSearchKH, btnRefreshCustomer);

        txtSdt = createStyledTextField("Nhập số điện thoại khách...");
        txtTen = createStyledTextField("Họ và tên khách hàng");
        txtTen.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                txtTen.setText(capitalizeWords(txtTen.getText()));
            }
        });
        txtDiaChi = createStyledTextField("Địa chỉ (Tỉnh/Thành phố)");

        installPhoneTextFormatter();
        cbLoaiGiayTo = new ComboBox<>();
        cbLoaiGiayTo.getItems().addAll(KhachHangService.GIAY_TO_CCCD, KhachHangService.GIAY_TO_HO_CHIEU);
        cbLoaiGiayTo.setValue(KhachHangService.GIAY_TO_CCCD);
        cbLoaiGiayTo.setPrefHeight(40);
        styleComboBox(cbLoaiGiayTo);
        txtSoGiayTo = createStyledTextField("");
        installDocumentTextFormatter();
        syncDocumentPromptAndInput();

        txtSdt.textProperty().addListener((obs, oldVal, newVal) -> handlePhoneTextChanged(oldVal, newVal));
        txtTen.textProperty().addListener((o, v, n) -> resetStyle(txtTen));
        txtDiaChi.textProperty().addListener((o, v, n) -> resetStyle(txtDiaChi));
        txtSoGiayTo.textProperty().addListener((o, oldVal, newVal) -> {
            if (isAutoFilling)
                return;

            // Live char filtering only — no validation, no search while typing
            if (txtSdt.getText().trim().isEmpty() && newVal.trim().isEmpty()) {
                resetCustomerForm();
            }
            resetStyle(txtSoGiayTo);
        });

        // Validate document number on blur (focus lost) — not during typing
        txtSoGiayTo.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                // User entered the field — clear any previous error immediately
                resetStyle(txtSoGiayTo);
                return;
            }
            // User left the field — validate if something was typed
            String val = txtSoGiayTo.getText().trim();
            if (val.isEmpty())
                return; // pristine/empty: no error yet, validateStep1 will catch it
            String err = khService.validateSoGiayTo(cbLoaiGiayTo.getValue(), val);
            if (err != null) {
                setInvalidStyle(txtSoGiayTo);
            }
        });

        cbLoaiGiayTo.setOnAction(e -> {
            syncDocumentPromptAndInput();
            resetStyle(txtSoGiayTo);
        });

        ToggleGroup groupGT = new ToggleGroup();
        rbNam = new RadioButton("Nam");
        rbNam.setToggleGroup(groupGT);
        rbNam.setSelected(true);
        styleRadioButton(rbNam);
        rbNu = new RadioButton("Nữ");
        rbNu.setToggleGroup(groupGT);
        rbNu.setText("Nữ");
        styleRadioButton(rbNu);
        HBox gtBox = new HBox(18, new Label("Giới tính:"), rbNam, rbNu);
        gtBox.setAlignment(Pos.CENTER_LEFT);
        ((Label) gtBox.getChildren().get(0)).setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        ((Label) gtBox.getChildren().get(0)).setTextFill(Color.web(MUTED_TEXT));

        txtSdt.setOnAction(e -> txtSoGiayTo.requestFocus());
        txtSoGiayTo.setOnAction(e -> txtTen.requestFocus());
        txtTen.setOnAction(e -> txtDiaChi.requestFocus());

        khCard.getChildren().addAll(khHeader,
                fieldRow("Số điện thoại:", txtSdt),
                fieldRow("Họ và tên:", txtTen),
                fieldRow("Địa chỉ:", txtDiaChi),
                gtBox);

        khCard.getChildren().add(2, fieldRow("Giấy tờ:", cbLoaiGiayTo));
        khCard.getChildren().add(3, fieldRow("Số CCCD / Hộ chiếu:", txtSoGiayTo));
        updateFieldRowLabel((HBox) khCard.getChildren().get(1), "Số điện thoại:");
        updateFieldRowLabel((HBox) khCard.getChildren().get(2), "Giấy tờ:");
        updateFieldRowLabel((HBox) khCard.getChildren().get(3), "Số CCCD / Hộ chiếu:");
        updateFieldRowLabel((HBox) khCard.getChildren().get(4), "Họ và tên:");
        updateFieldRowLabel((HBox) khCard.getChildren().get(5), "Địa chỉ:");

        VBox dateCard = new VBox(18);
        dateCard.setStyle(CARD_STYLE);

        Label lblTime = new Label("2. THỜI GIAN ĐẶT PHÒNG");
        lblTime.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));

        LocalDateTime defaultCheckIn = getSuggestedCheckInDateTime();
        LocalDateTime defaultCheckOut = getSuggestedCheckOutDateTime(defaultCheckIn);

        dpNgayNhan = new DatePicker(defaultCheckIn.toLocalDate());
        dpNgayNhan.setPrefHeight(40);
        dpNgayNhan.setMaxWidth(Double.MAX_VALUE);
        styleDatePicker(dpNgayNhan);
        dpNgayTra = new DatePicker(defaultCheckOut.toLocalDate());
        dpNgayTra.setPrefHeight(40);
        dpNgayTra.setMaxWidth(Double.MAX_VALUE);
        styleDatePicker(dpNgayTra);

        dpNgayNhan.setOnAction(e -> {
            resetStyle(dpNgayNhan);
            onDatesChanged();
        });
        dpNgayTra.setOnAction(e -> {
            resetStyle(dpNgayTra);
            onDatesChanged();
        });

        spGioNhan = createTimeSpinner(23, defaultCheckIn.getHour());
        spPhutNhan = createTimeSpinner(59, defaultCheckIn.getMinute());
        spGioTra = createTimeSpinner(23, defaultCheckOut.getHour());
        spPhutTra = createTimeSpinner(59, defaultCheckOut.getMinute());

        HBox dateRow = new HBox(18);

        VBox nhanBox = new VBox(6, new Label("Ngày nhận phòng:"), dpNgayNhan);
        HBox timeNhanBox = new HBox(8, new Label("Lúc:"), spGioNhan, new Label(":"), spPhutNhan);
        timeNhanBox.setAlignment(Pos.CENTER_LEFT);
        nhanBox.getChildren().add(timeNhanBox);
        ((Label) nhanBox.getChildren().get(0)).setTextFill(Color.web(MUTED_TEXT));
        ((Label) timeNhanBox.getChildren().get(0)).setTextFill(Color.web(MUTED_TEXT));
        HBox.setHgrow(nhanBox, Priority.ALWAYS);

        VBox traBox = new VBox(6, new Label("Ngày trả dự kiến:"), dpNgayTra);
        HBox timeTraBox = new HBox(8, new Label("Lúc:"), spGioTra, new Label(":"), spPhutTra);
        timeTraBox.setAlignment(Pos.CENTER_LEFT);
        traBox.getChildren().add(timeTraBox);
        ((Label) traBox.getChildren().get(0)).setTextFill(Color.web(MUTED_TEXT));
        ((Label) timeTraBox.getChildren().get(0)).setTextFill(Color.web(MUTED_TEXT));
        HBox.setHgrow(traBox, Priority.ALWAYS);

        dateRow.getChildren().addAll(nhanBox, traBox);

        chkQuickCheckin = new CheckBox("Nhận phòng nhanh (Khách đã đến)");
        chkQuickCheckin.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        chkQuickCheckin.setTextFill(Color.web("#0f172a"));
        chkQuickCheckin.setPadding(new Insets(6, 0, 4, 0));
        chkQuickCheckin.selectedProperty().addListener((obs, oldV, newV) -> {
            if (newV) {
                dpNgayNhan.setValue(LocalDate.now());
                dpNgayNhan.setDisable(true);
            } else {
                dpNgayNhan.setDisable(false);
            }
            onDatesChanged();
        });

        lblBangGiaInfo = new Label("Bảng giá: Đang tải...");
        lblBangGiaInfo.setFont(Font.font("Segoe UI", 12));
        lblBangGiaInfo.setTextFill(Color.web(MUTED_TEXT));
        lblBangGiaInfo.setStyle(
                "-fx-background-color: #f0fdfa; -fx-padding: 10 14; -fx-background-radius: 10; -fx-border-color: #cfeeea; -fx-border-radius: 10;");

        dateCard.getChildren().addAll(lblTime, dateRow, chkQuickCheckin, lblBangGiaInfo);

        Button btnNext = new Button("XÁC NHẬN");
        btnNext.setPrefHeight(50);
        btnNext.setMaxWidth(Double.MAX_VALUE);
        stylePrimaryButton(btnNext);
        btnNext.setOnAction(e -> goToStep2());
        btnNext.setDefaultButton(true);

        HBox nextRow = new HBox();
        nextRow.setAlignment(Pos.CENTER_RIGHT);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setHgrow(btnNext, Priority.ALWAYS);
        nextRow.getChildren().add(btnNext);

        content.getChildren().addAll(khCard, dateCard, nextRow);
        scroll.setContent(content);
        panel.getChildren().add(scroll);
        return panel;
    }

    private HBox fieldRow(String label, Control field) {
        Label lbl = new Label(label);
        lbl.setMinWidth(156);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        lbl.setTextFill(Color.web(MUTED_TEXT));
        HBox row = new HBox(16, lbl, field);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(1, 0, 1, 0));
        HBox.setHgrow(field, Priority.ALWAYS);
        return row;
    }

    private void updateFieldRowLabel(HBox row, String text) {
        if (row != null && !row.getChildren().isEmpty() && row.getChildren().get(0) instanceof Label lbl) {
            lbl.setText(text);
        }
    }

    private Label buildFieldCaption(String text) {
        Label label = new Label(text);
        label.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
        label.setTextFill(Color.web(MUTED_TEXT));
        return label;
    }

    private VBox labeledFilter(String label, Control field) {
        Label lbl = new Label(label);
        lbl.setFont(Font.font("Segoe UI", 11));
        lbl.setTextFill(Color.web("#888"));
        VBox box = new VBox(3, lbl, field);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("room-filter-item");
        lbl.getStyleClass().add("room-filter-label");
        return box;
    }

    private Node createStep2Panel() {
        VBox panel = new VBox(20);
        panel.setPadding(new Insets(0, 30, 30, 30));
        VBox.setVgrow(panel, Priority.ALWAYS);

        String roomAvailabilityCss = getRoomAvailabilityStylesheet();
        if (!panel.getStylesheets().contains(roomAvailabilityCss)) {
            panel.getStylesheets().add(roomAvailabilityCss);
        }

        Button btnBack = new Button("←  QUAY LẠI");
        styleOutlineButton(btnBack);
        btnBack.setOnAction(e -> goToStep1());

        HBox backRow = new HBox(btnBack);
        backRow.setAlignment(Pos.CENTER_LEFT);

        HBox mainContent = new HBox(20);
        VBox.setVgrow(mainContent, Priority.ALWAYS);

        // ── Room list card ────────────────────────────────────────────
        VBox roomBox = new VBox(0);
        roomBox.getStyleClass().add("room-availability-card");
        VBox.setVgrow(roomBox, Priority.ALWAYS);
        HBox.setHgrow(roomBox, Priority.ALWAYS);

        // ── Header strip ─────────────────────────────────────────────
        Circle roomDot = new Circle(19, Color.web("#dcfce7"));
        roomDot.setStroke(Color.web("#86efac"));
        roomDot.setStrokeWidth(1.2);
        Circle roomCore = new Circle(8, Color.web(PRIMARY_COLOR));
        roomCore.setOpacity(0.9);
        roomTableIcon = new StackPane(roomDot, roomCore);
        roomTableIcon.getStyleClass().add("room-header-icon");
        roomTableIcon.setPrefSize(40, 40);

        Label modernRoomTitle = new Label("Danh sách phòng đang trống");
        modernRoomTitle.getStyleClass().add("room-header-title");

        Label modernRoomSub = new Label("Chọn nhanh phòng phù hợp cho khách với trạng thái hiển thị trực quan.");
        modernRoomSub.getStyleClass().add("room-header-subtitle");

        VBox modernTitleBox = new VBox(4, modernRoomTitle, modernRoomSub);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        lblRoomCount = new Label("0 phòng");
        lblRoomCount.getStyleClass().add("room-count-badge");

        HBox roomHeader = new HBox(14, roomTableIcon, modernTitleBox, headerSpacer, lblRoomCount);
        roomHeader.setAlignment(Pos.CENTER_LEFT);
        roomHeader.setPadding(new Insets(22, 24, 16, 24));
        roomHeader.getStyleClass().add("room-header");

        // ── Filter bar wrapper ────────────────────────────────────────
        VBox filterWrap = new VBox(buildFilterBar());
        filterWrap.setPadding(new Insets(12, 24, 18, 24));
        filterWrap.getStyleClass().add("room-filter-wrap");

        // ── Table ─────────────────────────────────────────────────────
        filteredPhong = new FilteredList<>(allPhongTrong, p -> true);

        tvPhongTrong = new TableView<>(filteredPhong);
        tvPhongTrong.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tvPhongTrong.getStyleClass().add("phong-trong-table");
        tvPhongTrong.setPlaceholder(new Pane());
        VBox.setVgrow(tvPhongTrong, Priority.ALWAYS);

        TableColumn<Phong, String> colMa = new TableColumn<>("Mã Phòng");
        colMa.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getMaPhong()));

        TableColumn<Phong, String> colLoai = new TableColumn<>("Loại");
        colLoai.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getLoaiPhong()));

        TableColumn<Phong, String> colGiuong = new TableColumn<>("Giường");
        colGiuong.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(d.getValue().getSoGiuong())));

        TableColumn<Phong, String> colSucChua = new TableColumn<>("Sức chứa");
        colSucChua.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getSucChua() + " người"));

        TableColumn<Phong, String> colGia = new TableColumn<>("Giá/Ngày");
        colGia.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                String.format("%,.0f VNĐ", getDonGiaNgay(d.getValue()))));

        colGia.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                formatCurrency(getDonGiaNgay(d.getValue()))));
        colMa.setPrefWidth(120);
        colLoai.setPrefWidth(180);
        colSucChua.setPrefWidth(130);
        colGia.setPrefWidth(180);
        tvPhongTrong.getColumns().addAll(colMa, colLoai, colGiuong, colSucChua, colGia);

        tvPhongTrong.setRowFactory(tv -> {
            TableRow<Phong> row = new TableRow<Phong>() {
                @Override
                protected void updateItem(Phong item, boolean empty) {
                    super.updateItem(item, empty);
                    pseudoClassStateChanged(ROOM_PICKED, !empty && item != null && selectedRooms.contains(item));
                }
            };
            row.getStyleClass().add("room-table-row");
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty()) {
                    Phong p = row.getItem();
                    if (selectedRooms.contains(p)) {
                        selectedRooms.remove(p);
                        guestCountMap.remove(p);
                    } else {
                        selectedRooms.add(p);
                        guestCountMap.putIfAbsent(p, 1);
                    }
                    tvPhongTrong.refresh();
                    renderSelectedRooms();
                }
            });
            return row;
        });

        // ── Empty state overlay ───────────────────────────────────────
        emptyState = new VBox(16);
        emptyState.setAlignment(Pos.CENTER);
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        emptyState.getStyleClass().add("room-empty-state");

        Circle emptyCircle = new Circle(42, Color.web("#eef6ff"));
        emptyCircle.setStroke(Color.web("#d7e7fb"));
        emptyCircle.setStrokeWidth(1.2);
        Circle emptyCore = new Circle(12, Color.web(PRIMARY_COLOR));
        emptyCore.setOpacity(0.72);
        StackPane emptyIconWrap = new StackPane(emptyCircle, emptyCore);
        emptyIconWrap.getStyleClass().add("room-empty-icon");

        emptyStateTitle = new Label("Không có phòng trống");
        emptyStateTitle.getStyleClass().add("room-empty-title");

        emptyStateSubtitle = new Label("Tất cả phòng hiện đang được sử dụng.");
        emptyStateSubtitle.getStyleClass().add("room-empty-subtitle");
        emptyStateSubtitle.setWrapText(true);
        emptyStateSubtitle.setTextAlignment(TextAlignment.CENTER);

        emptyState.getChildren().addAll(emptyIconWrap, emptyStateTitle, emptyStateSubtitle);

        // ── Loading overlay ───────────────────────────────────────────
        loadingOverlay = new StackPane();
        loadingOverlay.setStyle("-fx-background-color: rgba(255,255,255,0.88);");
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);
        loadingOverlay.getStyleClass().add("room-loading-overlay");

        ProgressIndicator spinner = new ProgressIndicator(-1);
        spinner.setPrefSize(44, 44);
        spinner.getStyleClass().add("room-loading-spinner");
        Label loadingLbl = new Label("Đang tải danh sách phòng trống...");
        loadingLbl.setFont(Font.font("Segoe UI", 13));
        loadingLbl.setTextFill(Color.web("#64748b"));
        VBox loadingContent = new VBox(12, spinner, loadingLbl);
        loadingContent.setAlignment(Pos.CENTER);
        loadingLbl.getStyleClass().add("room-loading-label");
        loadingContent.getStyleClass().add("room-loading-content");
        loadingOverlay.getChildren().add(loadingContent);

        // ── Stack: table + empty state + loading ──────────────────────
        tableStack = new StackPane(tvPhongTrong, emptyState, loadingOverlay);
        tableStack.getStyleClass().add("room-table-stack");
        VBox.setVgrow(tableStack, Priority.ALWAYS);

        roomBox.getChildren().addAll(roomHeader, filterWrap, tableStack);

        VBox selBox = new VBox(16);
        selBox.setStyle(CARD_STYLE);
        selBox.setPrefWidth(390);
        selBox.setMinWidth(340);
        VBox.setVgrow(selBox, Priority.ALWAYS);

        Label lblSel = new Label("PHÒNG ĐÃ CHỌN");
        lblSel.setFont(Font.font("Segoe UI", FontWeight.BOLD, 15));
        lblSel.setText("PHÒNG ĐÃ CHỌN");
        lblSel.setTextFill(Color.web("#0f172a"));

        selectedRoomsContainer = new VBox(10);
        ScrollPane spSelected = new ScrollPane(selectedRoomsContainer);
        spSelected.setFitToWidth(true);
        spSelected.setStyle(
                "-fx-background-color: transparent; -fx-background: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-background-radius: 12;");
        spSelected.setPrefViewportHeight(260);
        VBox.setVgrow(spSelected, Priority.SOMETIMES);

        lblSelectedRoomSummary = new Label("Chưa có phòng nào được chọn.");
        lblSelectedRoomSummary.setWrapText(true);
        lblSelectedRoomSummary.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        lblSelectedRoomSummary.setTextFill(Color.web(PRIMARY_COLOR));
        lblSelectedRoomSummary.setStyle(
                "-fx-background-color: #f0fdfa;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #99f6e4;" +
                        "-fx-border-radius: 14;" +
                        "-fx-padding: 14 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.08), 8, 0, 0, 2);");

        Button btnConfirm = new Button("XÁC NHẬN ĐẶT PHÒNG");
        VBox depositBox = createDepositInfoBox();
        btnConfirm.setText("XÁC NHẬN ĐẶT CỌC");
        btnConfirm.setPrefHeight(50);
        btnConfirm.setMaxWidth(Double.MAX_VALUE);
        stylePrimaryButton(btnConfirm);
        btnConfirm.setOnAction(e -> handleBooking(BookingStatus.DA_DAT_COC.getCode()));

        Button btnReset = new Button("LÀM MỚI");
        btnReset.setPrefHeight(40);
        btnReset.setMaxWidth(Double.MAX_VALUE);
        styleSecondaryButton(btnReset);
        btnReset.setOnAction(e -> clearAllAndReset());

        selBox.getChildren().addAll(lblSel, spSelected, lblSelectedRoomSummary, depositBox, btnConfirm,
                btnReset);

        mainContent.getChildren().addAll(roomBox, selBox);
        panel.getChildren().addAll(backRow, mainContent);
        return panel;
    }

    private static final String ALL_OPTION = "Tất cả";

    private VBox createDepositInfoBox() {
        VBox box = new VBox(10);
        box.setStyle(
                "-fx-background-color: #f8fafc; -fx-background-radius: 14; -fx-border-color: #dbe4ee; -fx-border-radius: 14; -fx-padding: 14;");

        Label title = new Label("THÔNG TIN ĐẶT CỌC");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        title.setTextFill(Color.web("#0f172a"));

        lblSoDemLuuTru = createDepositValueLabel();
        lblTongTienPhong = createDepositValueLabel();
        lblTienDatCoc = createDepositValueLabel();
        lblTienConLai = createDepositValueLabel();

        cbHinhThucDatCoc = new ComboBox<>(FXCollections.observableArrayList(
                "Tiền mặt", "Chuyển khoản", "Thẻ", "Ví điện tử"));
        cbHinhThucDatCoc.setValue("Tiền mặt");
        cbHinhThucDatCoc.setPrefHeight(36);
        cbHinhThucDatCoc.setMaxWidth(Double.MAX_VALUE);
        styleComboBox(cbHinhThucDatCoc);

        txtSoTienKhachDua = createStyledTextField("Nhập số tiền khách đưa...");
        txtSoTienKhachDua.setTextFormatter(TextInputSafeUtils.filteringFormatter(s -> s.replaceAll("[^0-9]", "")));
        txtSoTienKhachDua.textProperty().addListener((obs, oldVal, newVal) -> updateCashDepositCalculations());
        txtSoTienKhachDua.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (!focused) {
                enforceMinimumCustomerPaidAmount();
            }
        });

        lblTienThuaTraKhach = createDepositValueLabel();

        txtGhiChuDatCoc = new TextArea();
        txtGhiChuDatCoc.setPromptText("Ghi chú thanh toán cọc nếu có...");
        txtGhiChuDatCoc.setPrefRowCount(2);
        txtGhiChuDatCoc.setWrapText(true);
        txtGhiChuDatCoc.setStyle(FIELD_BASE_STYLE + "-fx-padding: 8 10 8 10;");

        rowSoTienKhachDua = depositRow("Số tiền khách đưa", txtSoTienKhachDua);
        rowTienThuaTraKhach = depositRow("Tiền thừa trả khách", lblTienThuaTraKhach);

        box.getChildren().addAll(
                title,
                depositRow("Số đêm", lblSoDemLuuTru),
                depositRow("Tổng tiền phòng", lblTongTienPhong),
                depositRow("Tiền cọc tối thiểu 50%", lblTienDatCoc),
                rowSoTienKhachDua,
                rowTienThuaTraKhach,
                depositRow("Số tiền còn lại", lblTienConLai),
                depositRow("Hình thức", cbHinhThucDatCoc),
                new VBox(6, buildFieldCaption("Ghi chú"), txtGhiChuDatCoc));

        cbHinhThucDatCoc.valueProperty().addListener((obs, oldV, newV) -> {
            boolean isCash = "Tiền mặt".equals(newV);
            rowSoTienKhachDua.setVisible(isCash);
            rowSoTienKhachDua.setManaged(isCash);
            rowTienThuaTraKhach.setVisible(isCash);
            rowTienThuaTraKhach.setManaged(isCash);
            if (isCash) {
                double tongTien = calculateTongTienPhong();
                double tienCoc = DepositPolicy.calculateDeposit(tongTien);
                fillCustomerPaidAmount(tienCoc);
                updateCashDepositCalculations();
            } else {
                updateDepositSummary();
            }
        });

        // Initial visibility
        boolean isCashInit = "Tiền mặt".equals(cbHinhThucDatCoc.getValue());
        rowSoTienKhachDua.setVisible(isCashInit);
        rowSoTienKhachDua.setManaged(isCashInit);
        rowTienThuaTraKhach.setVisible(isCashInit);
        rowTienThuaTraKhach.setManaged(isCashInit);

        updateDepositSummary();
        return box;
    }

    private Label createDepositValueLabel() {
        Label label = new Label("0 VND");
        label.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        label.setTextFill(Color.web("#0f172a"));
        label.setWrapText(true);
        return label;
    }

    private HBox depositRow(String label, Node value) {
        Label lbl = buildFieldCaption(label);
        lbl.setMinWidth(124);
        HBox row = new HBox(10, lbl, value);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(value, Priority.ALWAYS);
        return row;
    }

    private HBox buildFilterBar() {
        String inputStyle = FIELD_BASE_STYLE + "-fx-font-size: 12;";
        String comboStyle = FIELD_BASE_STYLE + "-fx-font-size: 12;";

        txtFilterMa = new TextField();
        txtFilterMa.setPromptText("Mã phòng...");
        txtFilterMa.setPrefHeight(34);
        txtFilterMa.setPrefWidth(130);
        txtFilterMa.setStyle(inputStyle);
        txtFilterMa.getStyleClass().add("room-filter-input");

        cbFilterTang = new ComboBox<>();
        cbFilterTang.setPrefHeight(34);
        cbFilterTang.setPrefWidth(105);
        cbFilterTang.setStyle(comboStyle);
        cbFilterTang.getStyleClass().add("room-filter-combo");

        cbFilterLoai = new ComboBox<>();
        cbFilterLoai.setPrefHeight(34);
        cbFilterLoai.setPrefWidth(125);
        cbFilterLoai.setStyle(comboStyle);
        cbFilterLoai.getStyleClass().add("room-filter-combo");

        cbFilterGiuong = new ComboBox<>();
        cbFilterGiuong.setPrefHeight(34);
        cbFilterGiuong.setPrefWidth(110);
        cbFilterGiuong.setStyle(comboStyle);
        cbFilterGiuong.getStyleClass().add("room-filter-combo");

        cbFilterSucChua = new ComboBox<>();
        cbFilterSucChua.setPrefHeight(34);
        cbFilterSucChua.setPrefWidth(110);
        cbFilterSucChua.setStyle(comboStyle);
        cbFilterSucChua.getStyleClass().add("room-filter-combo");

        Button btnClearFilter = new Button("✕ Xóa lọc");
        btnClearFilter.setPrefHeight(34);
        String btnBase = "-fx-background-color: #f8fafc;" +
                "-fx-text-fill: " + "#0f172a" + ";" +
                "-fx-border-color: #cbd5e1;" +
                "-fx-border-width: 1.2;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: 700;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0 12;";
        String btnHov = "-fx-background-color: #e0f2fe;" +
                "-fx-text-fill: " + SECONDARY_HOVER + ";" +
                "-fx-border-color: #7dd3fc;" +
                "-fx-border-width: 1.2;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-font-size: 12;" +
                "-fx-font-weight: 700;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 0 12;";
        btnClearFilter.setStyle(btnBase);
        btnClearFilter.getStyleClass().add("room-filter-clear");
        btnClearFilter.setOnMouseEntered(e -> btnClearFilter.setStyle(btnHov));
        btnClearFilter.setOnMouseExited(e -> btnClearFilter.setStyle(btnBase));
        btnClearFilter.setOnAction(e -> clearFilters());

        txtFilterMa.textProperty().addListener((obs, o, n) -> applyFilters());
        cbFilterTang.valueProperty().addListener((obs, o, n) -> applyFilters());
        cbFilterLoai.valueProperty().addListener((obs, o, n) -> applyFilters());
        cbFilterGiuong.valueProperty().addListener((obs, o, n) -> applyFilters());
        cbFilterSucChua.valueProperty().addListener((obs, o, n) -> applyFilters());

        HBox bar = new HBox(10,
                labeledFilter("Mã phòng", txtFilterMa),
                labeledFilter("Tầng", cbFilterTang),
                labeledFilter("Loại phòng", cbFilterLoai),
                labeledFilter("Số giường", cbFilterGiuong),
                labeledFilter("Sức chứa", cbFilterSucChua),
                btnClearFilter);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("room-filter-bar");
        return bar;
    }

    private boolean isAll(String val) {
        return val == null || val.isEmpty() || ALL_OPTION.equals(val);
    }

    private void applyFilters() {
        if (filteredPhong == null)
            return;
        String maTxt = txtFilterMa.getText().trim().toLowerCase();
        String tang = cbFilterTang.getValue();
        String loai = cbFilterLoai.getValue();
        String giuong = cbFilterGiuong.getValue();
        String sucChua = cbFilterSucChua.getValue();

        filteredPhong.setPredicate(p -> {
            if (!maTxt.isEmpty() && !p.getMaPhong().toLowerCase().contains(maTxt))
                return false;
            if (!isAll(tang) && !tang.equals(extractTang(p.getMaPhong())))
                return false;
            if (!isAll(loai) && !loai.equalsIgnoreCase(p.getLoaiPhong()))
                return false;
            if (!isAll(giuong)) {
                try {
                    if (p.getSoGiuong() != Integer.parseInt(giuong.replaceAll("[^0-9]", "")))
                        return false;
                } catch (NumberFormatException ignored) {
                }
            }
            if (!isAll(sucChua)) {
                try {
                    if (p.getSucChua() != Integer.parseInt(sucChua.replaceAll("[^0-9]", "")))
                        return false;
                } catch (NumberFormatException ignored) {
                }
            }
            return true;
        });
        updateRoomAvailabilityState(allPhongTrong.isEmpty(), filteredPhong.isEmpty(), false);
    }

    private void clearFilters() {
        txtFilterMa.clear();
        cbFilterTang.setValue(ALL_OPTION);
        cbFilterLoai.setValue(ALL_OPTION);
        cbFilterGiuong.setValue(ALL_OPTION);
        cbFilterSucChua.setValue(ALL_OPTION);
    }

    private void updateRoomAvailabilityState(boolean noRooms, boolean filteredEmpty, boolean loading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(loading);
            loadingOverlay.setManaged(loading);
        }

        boolean showEmpty = !loading && filteredEmpty;
        if (emptyState != null) {
            emptyState.setVisible(showEmpty);
            emptyState.setManaged(showEmpty);
        }

        if (emptyStateTitle != null) {
            emptyStateTitle.setText(noRooms ? "Không có phòng trống" : "Không tìm thấy phòng phù hợp");
        }
        if (emptyStateSubtitle != null) {
            emptyStateSubtitle.setText(noRooms
                    ? "Tất cả phòng hiện đang được sử dụng."
                    : "Hãy thử điều chỉnh thời gian hoặc bộ lọc để xem thêm kết quả.");
        }

        if (tvPhongTrong != null) {
            tvPhongTrong.setMouseTransparent(showEmpty);
            tvPhongTrong.setOpacity(showEmpty ? 0.18 : 1.0);
        }

        if (lblRoomCount != null) {
            int count = filteredPhong != null ? filteredPhong.size() : 0;
            lblRoomCount.setText(count + " phòng");
        }

        if (!loading && !showEmpty && tvPhongTrong != null) {
            playRoomTableReveal();
        }
    }

    private void playRoomTableReveal() {
        FadeTransition fade = new FadeTransition(Duration.millis(260), tvPhongTrong);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition slide = new TranslateTransition(Duration.millis(260), tvPhongTrong);
        slide.setFromY(10);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        new ParallelTransition(fade, slide).play();
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.US, "%,.0f VNĐ", amount);
    }

    private String getRoomAvailabilityStylesheet() {
        return Paths.get("src", "gui", "booking-room-list.css").toAbsolutePath().toUri().toString();
    }

    private String getBookingStylesheet() {
        return Paths.get("src", "gui", "booking-room-list.css").toAbsolutePath().toUri().toString();
    }

    private String extractTang(String maPhong) {
        if (maPhong == null || maPhong.length() < 2)
            return "?";
        try {
            String digits = maPhong.replaceAll("[^0-9]", "");
            if (digits.length() >= 3) {
                return "Tầng " + digits.charAt(0);
            }
        } catch (Exception ignored) {
        }
        return "?";
    }

    private void populateFilterCombos(List<Phong> list) {
        String selTang = cbFilterTang.getValue();
        String selLoai = cbFilterLoai.getValue();
        String selGiuong = cbFilterGiuong.getValue();
        String selSucChua = cbFilterSucChua.getValue();

        List<String> tangs = new ArrayList<>();
        tangs.add(ALL_OPTION);
        list.stream().map(p -> extractTang(p.getMaPhong())).distinct().sorted().forEach(tangs::add);
        cbFilterTang.getItems().setAll(tangs);
        cbFilterTang.setValue(tangs.contains(selTang) ? selTang : ALL_OPTION);

        List<String> loais = new ArrayList<>();
        loais.add(ALL_OPTION);
        list.stream().map(Phong::getLoaiPhong).distinct().sorted().forEach(loais::add);
        cbFilterLoai.getItems().setAll(loais);
        cbFilterLoai.setValue(loais.contains(selLoai) ? selLoai : ALL_OPTION);

        List<String> giuongs = new ArrayList<>();
        giuongs.add(ALL_OPTION);
        list.stream().map(p -> p.getSoGiuong() + " giường").distinct().sorted().forEach(giuongs::add);
        cbFilterGiuong.getItems().setAll(giuongs);
        cbFilterGiuong.setValue(giuongs.contains(selGiuong) ? selGiuong : ALL_OPTION);

        List<String> sucChuas = new ArrayList<>();
        sucChuas.add(ALL_OPTION);
        list.stream().map(p -> p.getSucChua() + " người").distinct().sorted().forEach(sucChuas::add);
        cbFilterSucChua.getItems().setAll(sucChuas);
        cbFilterSucChua.setValue(sucChuas.contains(selSucChua) ? selSucChua : ALL_OPTION);
    }

    private void goToStep2() {
        if (!validateStep1())
            return;

        step1Panel.setVisible(false);
        step1Panel.setManaged(false);
        step2Panel.setVisible(true);
        step2Panel.setManaged(true);
        updateBreadcrumb(2);

        loadPhongTrong();
        renderSelectedRooms();
    }

    private void goToStep1() {
        step2Panel.setVisible(false);
        step2Panel.setManaged(false);
        step1Panel.setVisible(true);
        step1Panel.setManaged(true);
        updateBreadcrumb(1);
    }

    private void onDatesChanged() {
        if (!selectedRooms.isEmpty()) {
            selectedRooms.clear();
            guestCountMap.clear();
            if (tvPhongTrong != null)
                tvPhongTrong.refresh();
            if (selectedRoomsContainer != null)
                renderSelectedRooms();
        }

        loadPhongTrong();
        refreshBangGiaApDung();
    }

    public void refreshBangGiaApDung() {
        lookupBangGia();
    }

    private void showBangGiaLoading() {
        if (lblBangGiaInfo == null) {
            return;
        }
        lblBangGiaInfo.setText("Bảng giá áp dụng: Đang tải...");
        lblBangGiaInfo.setStyle(
                "-fx-background-color: #f0fdfa; -fx-padding: 10 14; -fx-background-radius: 10; -fx-border-color: #cfeeea; -fx-border-radius: 10;");
        lblBangGiaInfo.setTextFill(Color.web(MUTED_TEXT));
    }

    private void showBangGiaLoadFailed() {
        if (lblBangGiaInfo == null) {
            return;
        }
        lblBangGiaInfo.setText("Bảng giá áp dụng: Không thể tải dữ liệu");
        lblBangGiaInfo.setStyle("-fx-background-color: #fee2e2; -fx-padding: 6 12; -fx-background-radius: 6;");
        lblBangGiaInfo.setTextFill(Color.web("#991b1b"));
    }

    private void showBangGiaLoaded(BangGiaHeader bangGia) {
        if (lblBangGiaInfo == null || bangGia == null) {
            return;
        }
        lblBangGiaInfo.setText("Bảng giá áp dụng: " + bangGia.getTenBangGia()
                + "  (" + bangGia.getMaBangGia() + ")");
        lblBangGiaInfo.setStyle("-fx-background-color: #e8f8f0; -fx-padding: 6 12; -fx-background-radius: 6;");
        lblBangGiaInfo.setTextFill(Color.web("#1e8449"));
    }

    private void showBangGiaEmpty() {
        if (lblBangGiaInfo == null) {
            return;
        }
        lblBangGiaInfo.setText("Bảng giá áp dụng: Chưa có bảng giá đang áp dụng");
        lblBangGiaInfo.setStyle("-fx-background-color: #fef9e7; -fx-padding: 6 12; -fx-background-radius: 6;");
        lblBangGiaInfo.setTextFill(Color.web("#b7770d"));
    }

    private void lookupBangGia() {
        LocalDate ngayNhan = dpNgayNhan != null ? dpNgayNhan.getValue() : LocalDate.now();
        if (ngayNhan == null)
            return;

        if (bangGiaTask != null && bangGiaTask.isRunning()) {
            bangGiaTask.cancel();
        }

        showBangGiaLoading();

        Task<BangGiaHeader> task = new Task<BangGiaHeader>() {
            @Override
            protected BangGiaHeader call() {
                return bangGiaHeaderDAO.getBangGiaTheoNgay(ngayNhan);
            }
        };
        bangGiaTask = task;

        task.setOnSucceeded(e -> {
            if (task != bangGiaTask) {
                return;
            }
            bangGiaHienTai = task.getValue();
            if (lblBangGiaInfo != null) {
                if (bangGiaHienTai != null) {
                    lblBangGiaInfo.setText("Bảng giá áp dụng: " + bangGiaHienTai.getTenBangGia()
                            + "  (" + bangGiaHienTai.getMaBangGia() + ")");
                    lblBangGiaInfo
                            .setStyle("-fx-background-color: #e8f8f0; -fx-padding: 6 12; -fx-background-radius: 6;");
                    lblBangGiaInfo.setTextFill(Color.web("#1e8449"));
                } else {
                    lblBangGiaInfo.setText("Không có bảng giá hiệu lực — dùng giá gốc phòng.");
                    lblBangGiaInfo
                            .setStyle("-fx-background-color: #fef9e7; -fx-padding: 6 12; -fx-background-radius: 6;");
                    lblBangGiaInfo.setTextFill(Color.web("#b7770d"));
                }
            }
            if (bangGiaHienTai != null) {
                showBangGiaLoaded(bangGiaHienTai);
            } else {
                showBangGiaEmpty();
            }
            if (tvPhongTrong != null)
                tvPhongTrong.refresh();
            renderSelectedRooms();
        });
        task.setOnFailed(e -> {
            if (task != bangGiaTask) {
                return;
            }
            bangGiaHienTai = null;
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
            showBangGiaLoadFailed();
        });

        Thread th = new Thread(task, "booking-price-list-refresh");
        th.setDaemon(true);
        th.start();
    }

    private double getDonGiaNgay(Phong p) {
        if (p == null)
            return 0;
        LocalDate nhan = (dpNgayNhan != null) ? dpNgayNhan.getValue() : LocalDate.now();
        return pricingService.getAppliedPriceByDate(p, nhan);
    }

    private double getGiaApDung(Phong p) {
        if (p == null)
            return 0;

        LocalDate nhan = (dpNgayNhan != null) ? dpNgayNhan.getValue() : LocalDate.now();
        LocalDate tra = (dpNgayTra != null) ? dpNgayTra.getValue() : LocalDate.now().plusDays(1);
        int hIn = (spGioNhan != null) ? spGioNhan.getValue() : 12;
        int mIn = (spPhutNhan != null) ? spPhutNhan.getValue() : 0;
        int hOut = (spGioTra != null) ? spGioTra.getValue() : 12;
        int mOut = (spPhutTra != null) ? spPhutTra.getValue() : 0;

        return pricingService.calculateEstimatedBookingPrice(
                p,
                LocalDateTime.of(nhan, LocalTime.of(hIn, mIn)),
                LocalDateTime.of(tra, LocalTime.of(hOut, mOut)));
    }

    private int calculateSoDemLuuTru() {
        return DepositPolicy.calculateNights(getSelectedCheckInDateTime(), getSelectedCheckOutDateTime());
    }

    private double calculateTongTienPhong() {
        return DepositPolicy.roundMoney(selectedRooms.stream()
                .mapToDouble(this::getGiaApDung)
                .sum());
    }

    private void updateDepositSummary() {
        if (lblSoDemLuuTru == null || lblTongTienPhong == null
                || lblTienDatCoc == null || lblTienConLai == null) {
            return;
        }
        int soDem = calculateSoDemLuuTru();
        double tongTien = calculateTongTienPhong();
        double tienCocToiThieu = DepositPolicy.calculateDeposit(tongTien);
        double tienConLaiMacDinh = DepositPolicy.calculateRemaining(tongTien, tienCocToiThieu);

        lblSoDemLuuTru.setText(soDem + " đêm x " + selectedRooms.size() + " phòng");
        lblTongTienPhong.setText(formatCurrency(tongTien));
        lblTienDatCoc.setText(formatCurrency(tienCocToiThieu));
        lblTienConLai.setText(formatCurrency(tienConLaiMacDinh));

        boolean depositChanged = Math.abs(tienCocToiThieu - lastSuggestedDepositAmount) > 0.01;
        lastSuggestedDepositAmount = tienCocToiThieu;
        if (isCashDepositMethod()) {
            if (txtSoTienKhachDua != null
                    && (txtSoTienKhachDua.getText().isEmpty() || depositChanged)) {
                fillCustomerPaidAmount(tienCocToiThieu);
            }
            updateCashDepositCalculations();
        }
    }

    private boolean isCashDepositMethod() {
        return cbHinhThucDatCoc != null && "Tiền mặt".equals(cbHinhThucDatCoc.getValue());
    }

    private void fillCustomerPaidAmount(double tienCocToiThieu) {
        if (txtSoTienKhachDua == null) {
            return;
        }
        TextInputSafeUtils.safeSetText(txtSoTienKhachDua,
                String.format(Locale.US, "%.0f", Math.max(0, tienCocToiThieu)));
    }

    private double parseCustomerPaidAmount() {
        if (txtSoTienKhachDua == null || txtSoTienKhachDua.getText() == null) {
            return 0;
        }
        String text = txtSoTienKhachDua.getText().replaceAll("[^0-9]", "");
        return text.isEmpty() ? 0 : Double.parseDouble(text);
    }

    private double getCustomerPaidAmountForDeposit(double tienCocToiThieu) {
        if (!isCashDepositMethod()) {
            return tienCocToiThieu;
        }
        return parseCustomerPaidAmount();
    }

    private void enforceMinimumCustomerPaidAmount() {
        if (!isCashDepositMethod() || txtSoTienKhachDua == null) {
            return;
        }
        double tongTien = calculateTongTienPhong();
        double tienCocToiThieu = DepositPolicy.calculateDeposit(tongTien);
        if (tongTien <= 0 || tienCocToiThieu <= 0) {
            return;
        }
        try {
            if (parseCustomerPaidAmount() + 0.01 < tienCocToiThieu) {
                fillCustomerPaidAmount(tienCocToiThieu);
            }
        } catch (NumberFormatException e) {
            fillCustomerPaidAmount(tienCocToiThieu);
        }
        updateCashDepositCalculations();
    }

    private void updateCashDepositCalculations() {
        if (txtSoTienKhachDua == null || lblTienThuaTraKhach == null
                || lblTienConLai == null || cbHinhThucDatCoc == null)
            return;
        if (!isCashDepositMethod())
            return;

        double tongTien = calculateTongTienPhong();
        double tienCocToiThieu = DepositPolicy.calculateDeposit(tongTien);

        try {
            double khachDua = parseCustomerPaidAmount();
            double tienDatCocThucNhan = DepositPolicy.calculateReceivedDeposit(tongTien, khachDua);
            double tienConLai = DepositPolicy.calculateRemaining(tongTien, tienDatCocThucNhan);
            double tienThua = DepositPolicy.calculateChange(tongTien, khachDua);
            lblTienConLai.setText(formatCurrency(tienConLai));
            lblTienThuaTraKhach.setText(formatCurrency(tienThua));
            if (khachDua + 0.01 < tienCocToiThieu) {
                setInvalidStyle(txtSoTienKhachDua);
            } else {
                resetStyle(txtSoTienKhachDua);
            }
        } catch (NumberFormatException e) {
            lblTienConLai.setText(formatCurrency(tongTien));
            lblTienThuaTraKhach.setText(formatCurrency(0));
        }
    }

    private boolean validateDepositInput() {
        double tongTien = calculateTongTienPhong();
        double tienCoc = DepositPolicy.calculateDeposit(tongTien);
        try {
            double khachDua = parseCustomerPaidAmount();
            if (isCashDepositMethod() && (khachDua + 0.01 < tienCoc)) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private void exportDepositReceipt(String bookingId) {
        Task<DepositReceiptSource> task = new Task<>() {
            @Override
            protected DepositReceiptSource call() {
                PhieuDatPhong booking = pdpDAO.getPhieuDatPhongByMa(bookingId);
                if (booking == null) {
                    throw new IllegalStateException("Không tìm thấy phiếu đặt phòng " + bookingId + ".");
                }
                List<ChiTietPhieuDat> details = chiTietPhieuDatDAO.getDSChiTietByMaPhieu(bookingId);
                if (isBookingFullyPaid(booking)) {
                    throw new IllegalStateException("Phi?u ?? thanh to?n ho?n t?t. H?y xu?t h?a ??n ho?c in phi?u thanh to?n.");
                }
                return new DepositReceiptSource(booking, details != null ? details : List.of());
            }
        };

        task.setOnSucceeded(e -> exportLoadedDepositReceipt(task.getValue()));
        task.setOnFailed(e -> showAlert("Lỗi xuất phiếu",
                task.getException() != null ? task.getException().getMessage() : "Không thể tải dữ liệu phiếu đặt cọc."));
        Thread thread = new Thread(task, "deposit-receipt-export");
        thread.setDaemon(true);
        thread.start();
    }

    private void exportLoadedDepositReceipt(DepositReceiptSource source) {
        try {
            File file = DepositReceiptPdfExporter.chooseSavePath(null, source.booking().getMaDatPhong());
            if (file == null) {
                return;
            }
            DepositReceiptPdfExporter.export(file, source.booking(), source.details());
            DepositReceiptPdfExporter.showSuccessAndAskToOpen(null, file);
        } catch (IllegalArgumentException ex) {
            showAlert("Không thể xuất phiếu", ex.getMessage());
        } catch (Exception ex) {
            showAlert("Lỗi xuất phiếu", "Không thể xuất phiếu đặt cọc: " + ex.getMessage());
        }
    }

    private record DepositReceiptSource(PhieuDatPhong booking, List<ChiTietPhieuDat> details) {
    }

    private boolean isBookingFullyPaid(PhieuDatPhong booking) {
        return booking != null
                && (StatusUtils.isBookingStatus(booking.getTrangThai(), BookingStatus.DA_THANH_TOAN)
                || booking.getTienConLai() <= 0.0001);
    }

    private LocalDateTime getSuggestedCheckInDateTime() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        if (now.getMinute() == 0) {
            return now;
        }
        return now.plusHours(1).withMinute(0);
    }

    private LocalDateTime getSuggestedCheckOutDateTime(LocalDateTime checkIn) {
        return checkIn.plusDays(1);
    }

    private LocalDateTime getSelectedCheckInDateTime() {
        LocalDate ngayNhan = dpNgayNhan == null ? null : dpNgayNhan.getValue();
        if (ngayNhan == null) {
            return null;
        }
        int hIn = spGioNhan == null ? 0 : spGioNhan.getValue();
        int mIn = spPhutNhan == null ? 0 : spPhutNhan.getValue();
        return ngayNhan.atTime(hIn, mIn);
    }

    private LocalDateTime getSelectedCheckOutDateTime() {
        LocalDate ngayTra = dpNgayTra == null ? null : dpNgayTra.getValue();
        if (ngayTra == null) {
            return null;
        }
        int hOut = spGioTra == null ? 0 : spGioTra.getValue();
        int mOut = spPhutTra == null ? 0 : spPhutTra.getValue();
        return ngayTra.atTime(hOut, mOut);
    }

    private boolean validateBookingDateTime(boolean showAlertOnError) {
        LocalDateTime start = getSelectedCheckInDateTime();
        LocalDateTime end = getSelectedCheckOutDateTime();

        if (start == null || end == null) {
            return true;
        }

        if (start.isBefore(LocalDateTime.now())) {
            setInvalidStyle(dpNgayNhan);
            if (showAlertOnError) {
                showAlert("Thời gian không hợp lệ", "Không thể đặt phòng với thời gian nhận phòng trong quá khứ.");
                dpNgayNhan.requestFocus();
            }
            return false;
        }

        if (!end.isAfter(start)) {
            setInvalidStyle(dpNgayTra);
            if (showAlertOnError) {
                showAlert("Thời gian không hợp lệ", "Thời gian trả phòng dự kiến phải sau thời gian nhận phòng.");
                dpNgayTra.requestFocus();
            }
            return false;
        }

        return true;
    }

    private void applySuggestedBookingDateTimeDefaults() {
        LocalDateTime defaultCheckIn = getSuggestedCheckInDateTime();
        LocalDateTime defaultCheckOut = getSuggestedCheckOutDateTime(defaultCheckIn);

        if (dpNgayNhan != null) {
            dpNgayNhan.setValue(defaultCheckIn.toLocalDate());
        }
        if (spGioNhan != null && spGioNhan.getValueFactory() != null) {
            spGioNhan.getValueFactory().setValue(defaultCheckIn.getHour());
        }
        if (spPhutNhan != null && spPhutNhan.getValueFactory() != null) {
            spPhutNhan.getValueFactory().setValue(defaultCheckIn.getMinute());
        }
        if (dpNgayTra != null) {
            dpNgayTra.setValue(defaultCheckOut.toLocalDate());
        }
        if (spGioTra != null && spGioTra.getValueFactory() != null) {
            spGioTra.getValueFactory().setValue(defaultCheckOut.getHour());
        }
        if (spPhutTra != null && spPhutTra.getValueFactory() != null) {
            spPhutTra.getValueFactory().setValue(defaultCheckOut.getMinute());
        }
    }

    private boolean validateStep1() {
        List<String> errors = new ArrayList<>();
        Control firstError = null;

        String sdt = txtSdt.getText().trim();
        if (sdt.isEmpty() || !sdt.matches("^0\\d{9}$")) {
            errors.add("- Số điện thoại không hợp lệ (phải bắt đâu bằng 0 va đủ 10 số).");
            setInvalidStyle(txtSdt);
            if (firstError == null)
                firstError = txtSdt;
        }

        String tenError = khService.validateTen(txtTen.getText());
        if (tenError != null) {
            errors.add("- " + tenError);
            setInvalidStyle(txtTen);
            if (firstError == null)
                firstError = txtTen;
        }

        String docTypeError = khService.validateLoaiGiayTo(cbLoaiGiayTo.getValue());
        if (docTypeError != null) {
            errors.add("- " + docTypeError);
            setInvalidStyle(cbLoaiGiayTo);
            if (firstError == null)
                firstError = cbLoaiGiayTo;
        }

        String docNumberError = khService.validateSoGiayTo(cbLoaiGiayTo.getValue(), txtSoGiayTo.getText());
        if (docNumberError != null) {
            errors.add("- " + docNumberError);
            setInvalidStyle(txtSoGiayTo);
            if (firstError == null)
                firstError = txtSoGiayTo;
        }

        if (txtDiaChi.getText().trim().isEmpty()) {
            errors.add("- Vui lòng nhập địa chỉ.");
            setInvalidStyle(txtDiaChi);
            if (firstError == null)
                firstError = txtDiaChi;
        }

        LocalDate nhan = dpNgayNhan.getValue();
        LocalDate tra = dpNgayTra.getValue();
        if (nhan == null) {
            errors.add("- Vui lòng chọn phon ngay.");
            setInvalidStyle(dpNgayNhan);
            if (firstError == null)
                firstError = dpNgayNhan;
        }

        if (tra == null) {
            errors.add("- Vui lòng chọn ngày trả phòng.");
            setInvalidStyle(dpNgayTra);
            if (firstError == null)
                firstError = dpNgayTra;
        }

        if (nhan != null && tra != null && !validateBookingDateTime(false)) {
            LocalDateTime start = getSelectedCheckInDateTime();
            LocalDateTime end = getSelectedCheckOutDateTime();
            if (start != null && start.isBefore(LocalDateTime.now())) {
                errors.add("- Không thể đặt phòng với thời gian nhận phòng trong quá khứ.");
                setInvalidStyle(dpNgayNhan);
                if (firstError == null)
                    firstError = dpNgayNhan;
            } else if (start != null && end != null && !end.isAfter(start)) {
                errors.add("- Thời gian trả phòng dự kiến phải sau thời gian nhận phòng.");
                setInvalidStyle(dpNgayTra);
                if (firstError == null)
                    firstError = dpNgayTra;
            }
        }

        if (!errors.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Thông tin chưa hợp lệ");
            alert.setHeaderText("Vui lòng kiểm tra lại " + errors.size() + " mục:");
            alert.setContentText(String.join("\n", errors));
            final Control target = firstError;
            alert.showAndWait().ifPresent(r -> {
                if (target != null)
                    target.requestFocus();
            });
            return false;
        }
        return true;
    }

    private void handleBooking(String trangThai) {
        if (currentKH != null && !currentKH.isDangHoatDong()) {
            showAlert("Khách hàng đã bị vô hiệu hóa",
                    "Khách hàng đã bị vô hiệu hóa. Không thể tạo phiếu đặt phòng mới.");
            return;
        }
        if (!validateBookingDateTime(true)) {
            return;
        }
        if (selectedRooms.isEmpty()) {
            tvPhongTrong.setStyle("-fx-border-color: " + DANGER_COLOR + "; -fx-border-width: 2px;");
            showAlert("Thiếu thông tin", "Vui lòng chọn ít nhất 1 phòng trước khi xác nhận.");
            return;
        }
        if (!validateGuestAssignments()) {
            return;
        }
        if (!validateDepositInput()) {
            return;
        }
        tvPhongTrong.setStyle("");

        LocalDateTime start = getSelectedCheckInDateTime();
        LocalDateTime end = getSelectedCheckOutDateTime();
        List<Phong> bookedRooms = new ArrayList<>(selectedRooms);
        Map<String, Double> roomPrices = new HashMap<>();
        for (Phong room : bookedRooms) {
            roomPrices.put(room.getMaPhong(), getGiaApDung(room));
        }
        double tongTienPhong = calculateTongTienPhong();
        double tienCocToiThieu = DepositPolicy.calculateDeposit(tongTienPhong);
        double soTienKhachDua = getCustomerPaidAmountForDeposit(tienCocToiThieu);
        double tienDatCoc = DepositPolicy.calculateReceivedDeposit(tongTienPhong, soTienKhachDua);
        double tienConLai = DepositPolicy.calculateRemaining(tongTienPhong, tienDatCoc);
        double tienThuaTraKhach = DepositPolicy.calculateChange(tongTienPhong, soTienKhachDua);
        int soDemLuuTru = calculateSoDemLuuTru();
        String hinhThucDatCoc = cbHinhThucDatCoc.getValue();
        String ghiChuDatCoc = txtGhiChuDatCoc != null ? txtGhiChuDatCoc.getText().trim() : "";
        String loaiGiayToInput = cbLoaiGiayTo.getValue();
        String soGiayToInput = txtSoGiayTo.getText().trim();
        String tenKhachInput = txtTen.getText().trim();
        String diaChiInput = txtDiaChi.getText();
        String sdtInput = txtSdt.getText();
        boolean gioiTinhNam = rbNam.isSelected();

        Task<PhieuDatPhong> saveTask = new Task<PhieuDatPhong>() {
            @Override
            protected PhieuDatPhong call() throws Exception {
                String loaiGiayTo = khService.normalizeLoaiGiayTo(loaiGiayToInput);
                String soGiayTo = soGiayToInput;
                if (!hasValidCustomerId(currentKH)) {
                    String maKH = khService.generateNextId();
                    currentKH = new KhachHang(maKH, tenKhachInput, diaChiInput,
                            gioiTinhNam, sdtInput, loaiGiayTo, soGiayTo);
                    khService.create(currentKH);
                } else {
                    currentKH.setTenKhachHang(tenKhachInput);
                    currentKH.setDiaChi(diaChiInput);
                    currentKH.setGioiTinh(gioiTinhNam);
                    currentKH.setSoDienThoai(sdtInput);
                    currentKH.setLoaiGiayTo(loaiGiayTo);
                    currentKH.setSoGiayTo(soGiayTo);
                    khService.update(currentKH);
                }

                String maDP = pdpDAO.generateNextId();
                PhieuDatPhong pdp = new PhieuDatPhong(maDP, LocalDateTime.now(), trangThai,
                        loggedInNhanVien, currentKH);
                pdp.setSoDemLuuTru(soDemLuuTru);
                pdp.setTongTienPhong(tongTienPhong);
                pdp.setTienDatCoc(tienDatCoc);
                pdp.setTienConLai(tienConLai);
                pdp.setTyLeDatCoc(DepositPolicy.DEPOSIT_RATE_PERCENT);
                pdp.setHinhThucDatCoc(hinhThucDatCoc);
                pdp.setNgayDatCoc(LocalDateTime.now());
                pdp.setNhanVienNhanCoc(loggedInNhanVien);
                pdp.setGhiChuDatCoc(ghiChuDatCoc);
                pdp.setSoTienKhachDua(soTienKhachDua);
                pdp.setSoTienThucNhan(tienDatCoc);
                pdp.setTienThuaTraKhach(tienThuaTraKhach);
                pdp.setDsMaPhong(String.join(", ", bookedRooms.stream().map(Phong::getMaPhong).toList()));
                pdp.setSoLuongPhong(bookedRooms.size());

                List<ChiTietPhieuDat> dsCT = new ArrayList<>();
                for (Phong p : bookedRooms) {
                    dsCT.add(new ChiTietPhieuDat(
                            pdp, p, roomPrices.getOrDefault(p.getMaPhong(), p.getGiaPhong()), start, end));
                }
                return pdpDAO.datPhong(pdp, dsCT) ? pdp : null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            PhieuDatPhong savedBooking = saveTask.getValue();
            if (savedBooking != null) {
                lastSavedDepositBooking = savedBooking;
                lastSavedDepositRooms = new ArrayList<>(bookedRooms);
                showAlert("Thành công", "Đặt phòng thành công");
                if (onBookingSaved != null) {
                    onBookingSaved.run();
                }
                clearAllAndReset();
            } else {
                showAlert("Lỗi", "Không thể lưu dữ liệu vào cơ sở dữ liệu!");
            }
        });

        saveTask.setOnFailed(ev -> showAlert("Lỗi hệ thống",
                "Co loi xay ra: " + saveTask.getException().getMessage()));

        Thread th = new Thread(saveTask);
        th.setDaemon(true);
        th.start();
    }

    public void loadPhongTrong() {
        if (isLoading)
            return;
        isLoading = true;
        updateRoomAvailabilityState(false, false, true);

        LocalDate nhan = (dpNgayNhan != null) ? dpNgayNhan.getValue() : LocalDate.now();
        LocalDate tra = (dpNgayTra != null) ? dpNgayTra.getValue() : LocalDate.now().plusDays(1);

        int hIn = (spGioNhan != null) ? spGioNhan.getValue() : 14;
        int mIn = (spPhutNhan != null) ? spPhutNhan.getValue() : 0;
        int hOut = (spGioTra != null) ? spGioTra.getValue() : 12;
        int mOut = (spPhutTra != null) ? spPhutTra.getValue() : 0;

        final LocalDateTime start = (nhan != null) ? nhan.atTime(hIn, mIn) : LocalDateTime.now();
        final LocalDateTime end = (tra != null) ? tra.atTime(hOut, mOut) : LocalDateTime.now().plusDays(1);

        Task<List<Phong>> task = new Task<List<Phong>>() {
            @Override
            protected List<Phong> call() {
                System.out.println("=== ĐANG TRUY VẤN PHÒNG TRỐNG ===");
                System.out.println("Nhận phòng: " + start + " | Trả phòng: " + end);
                List<Phong> result = new PhongDAO().findAvailableRooms(start, end);
                System.out.println("=== TÌM THẤY " + (result != null ? result.size() : 0) + " PHÒNG ===");
                return result;
            }
        };

        task.setOnSucceeded(e -> {
            isLoading = false;

            List<Phong> list = task.getValue();
            allPhongTrong.clear();

            if (list != null) {
                System.out.println("=== CẬP NHẬT BẢNG VỚI " + list.size() + " PHÒNG ===");
                allPhongTrong.setAll(list);

                if (filteredPhong != null) {
                    populateFilterCombos(list);
                    applyFilters();
                }

                if (tvPhongTrong != null)
                    tvPhongTrong.refresh();
            }
            updateRoomAvailabilityState(allPhongTrong.isEmpty(), filteredPhong == null || filteredPhong.isEmpty(),
                    false);

            // Update count badge
            if (lblRoomCount != null) {
                int cnt = (list != null) ? list.size() : 0;
                lblRoomCount.setText(cnt + " Phòng trống");
            }
        });

        task.setOnFailed(ev -> {
            isLoading = false;
            if (loadingOverlay != null) {
                loadingOverlay.setVisible(false);
                loadingOverlay.setManaged(false);
            }
            if (emptyState != null) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
            }
            updateRoomAvailabilityState(true, true, false);
            Throwable ex = task.getException();
            System.out.println("=== LOI LOAD PHONG ===");
            if (ex != null)
                ex.printStackTrace();
            showAlert("Lỗi tải phòng", ex != null ? ex.getMessage() : "Không thể tải danh sách phòng trống.");
        });

        Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
    }

    private void renderSelectedRooms() {
        selectedRoomsContainer.getChildren().clear();
        syncGuestCounts();

        if (selectedRooms.isEmpty()) {
            Label hint = new Label("Chưa chọn phòng nào.\nNhấn vào danh sách bên trái để thêm phòng vào phiếu đặt.");
            hint.setTextFill(Color.web(MUTED_TEXT));
            hint.setFont(Font.font("Segoe UI", 13));
            hint.setWrapText(true);
            selectedRoomsContainer.getChildren().add(hint);
            updateSelectedRoomSummary();
            return;
        }

        for (Phong p : selectedRooms) {
            HBox card = new HBox(16);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(14, 16, 14, 16));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 16;" +
                    "-fx-border-color: #dbe4ee; -fx-border-radius: 16;" +
                    "-fx-effect: dropshadow(gaussian, rgba(15,23,42,0.06), 14, 0.16, 0, 4);");

            VBox info = new VBox(4);
            Label lblMa = new Label("🛏  " + p.getMaPhong());
            lblMa.setFont(Font.font("Segoe UI", FontWeight.BOLD, 14));
            lblMa.setTextFill(Color.web("#0f172a"));

            Label lblType = new Label("Loại phòng: " + p.getLoaiPhong());
            lblType.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
            lblType.setTextFill(Color.web(PRIMARY_COLOR));

            Label lblInfo = new Label(
                    "Giá dự kiến: " + formatCurrency(getGiaApDung(p))
                            + "  •  Sức chứa: " + p.getSucChua() + " người");
            lblInfo.setTextFill(Color.web(MUTED_TEXT));
            lblInfo.setFont(Font.font("Segoe UI", 12));

            info.getChildren().addAll(lblMa, lblType, lblInfo);
            HBox.setHgrow(info, Priority.ALWAYS);

            VBox guestBox = new VBox(6);
            Label lblGuestCount = new Label("Số người");
            lblGuestCount.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12));
            lblGuestCount.setTextFill(Color.web(MUTED_TEXT));
            Spinner<Integer> spGuestCount = createGuestCountSpinner(p);
            guestBox.getChildren().addAll(lblGuestCount, spGuestCount);

            Button btnRemove = new Button("🗑");
            btnRemove.setMinWidth(36);
            btnRemove.setMinHeight(36);
            btnRemove.setFont(Font.font("Segoe UI", FontWeight.BOLD, 12));
            btnRemove.setStyle(buildRemoveButtonStyle(false));
            btnRemove.setOnMouseEntered(e -> btnRemove.setStyle(buildRemoveButtonStyle(true)));
            btnRemove.setOnMouseExited(e -> btnRemove.setStyle(buildRemoveButtonStyle(false)));
            btnRemove.setOnAction(e -> {
                removeSelectedRoom(p);
            });

            card.getChildren().addAll(info, guestBox, btnRemove);
            selectedRoomsContainer.getChildren().add(card);
        }

        updateSelectedRoomSummary();
    }

    private void syncGuestCounts() {
        guestCountMap.keySet().removeIf(room -> !selectedRooms.contains(room));
        for (Phong room : selectedRooms) {
            int defaultCount = guestCountMap.getOrDefault(room, 1);
            if (defaultCount <= 0) {
                defaultCount = 1;
            }
            guestCountMap.put(room, Math.min(defaultCount, room.getSucChua()));
        }
    }

    private Spinner<Integer> createGuestCountSpinner(Phong room) {
        int current = Math.max(1, guestCountMap.getOrDefault(room, 1));
        Spinner<Integer> spinner = new Spinner<>(1, Math.max(1, room.getSucChua()), current);
        spinner.setEditable(true);
        spinner.setPrefWidth(108);
        spinner.setMinWidth(108);
        spinner.setPrefHeight(38);
        spinner.setStyle(
                "-fx-background-color: #ffffff; -fx-background-insets: 0;" +
                        "-fx-background-radius: 10; -fx-border-color: #cbd5e1;" +
                        "-fx-border-radius: 10; -fx-border-width: 1.4;");
        spinner.getEditor().setStyle(
                "-fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: 700;" +
                        "-fx-alignment: center; -fx-background-color: #ffffff;" +
                        "-fx-highlight-fill: #0f172a; -fx-highlight-text-fill: white;" +
                        "-fx-padding: 0 8; -fx-background-insets: 0;");
        spinner.getEditor().setPrefWidth(56);
        spinner.getEditor().setAlignment(Pos.CENTER);
        spinner.getEditor().setTextFormatter(
                TextInputSafeUtils.filteringFormatter(value -> value == null ? "" : value.replaceAll("\\D", "")));
        spinner.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isBlank()) {
                try {
                    int liveValue = Math.max(1, Math.min(Integer.parseInt(newVal), room.getSucChua()));
                    guestCountMap.put(room, liveValue);
                    updateSelectedRoomSummary();
                } catch (NumberFormatException ignored) {
                }
            }
        });
        spinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            guestCountMap.put(room, newVal == null ? 1 : newVal);
            updateSelectedRoomSummary();
        });
        spinner.focusedProperty().addListener((obs, oldVal, focused) -> {
            spinner.setStyle(focused
                    ? "-fx-background-color: #ffffff; -fx-background-insets: 0;" +
                            "-fx-background-radius: 10; -fx-border-color: " + PRIMARY_COLOR + ";" +
                            "-fx-border-radius: 10; -fx-border-width: 1.8;" +
                            "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.18), 10, 0.16, 0, 2);"
                    : "-fx-background-color: #ffffff; -fx-background-insets: 0;" +
                            "-fx-background-radius: 10; -fx-border-color: #cbd5e1;" +
                            "-fx-border-radius: 10; -fx-border-width: 1.4;");
            if (!focused) {
                commitGuestCountEditor(room, spinner);
            }
        });
        spinner.getEditor().setOnAction(e -> commitGuestCountEditor(room, spinner));
        return spinner;
    }

    private void removeSelectedRoom(Phong room) {
        selectedRooms.remove(room);
        guestCountMap.remove(room);
        if (tvPhongTrong != null) {
            tvPhongTrong.refresh();
        }
        renderSelectedRooms();
    }

    private void commitGuestCountEditor(Phong room, Spinner<Integer> spinner) {
        String text = spinner.getEditor().getText() == null ? "" : spinner.getEditor().getText().trim();
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            value = guestCountMap.getOrDefault(room, 1);
        }
        value = Math.max(1, Math.min(value, room.getSucChua()));
        spinner.getValueFactory().setValue(value);
        guestCountMap.put(room, value);
        updateSelectedRoomSummary();
    }

    private void updateSelectedRoomSummary() {
        if (lblSelectedRoomSummary == null) {
            return;
        }
        if (selectedRooms.isEmpty()) {
            lblSelectedRoomSummary.setText("Chưa có phòng nào được chọn.");
            lblSelectedRoomSummary.setStyle(
                    "-fx-background-color: #f8fafc;" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-color: #e2e8f0;" +
                            "-fx-border-radius: 14;" +
                            "-fx-padding: 14 16;");
            updateDepositSummary();
            return;
        }
        int totalGuests = selectedRooms.stream()
                .mapToInt(room -> guestCountMap.getOrDefault(room, 1))
                .sum();
        lblSelectedRoomSummary.setStyle(
                "-fx-background-color: #f0fdfa;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #99f6e4;" +
                        "-fx-border-radius: 14;" +
                        "-fx-padding: 14 16;" +
                        "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.08), 8, 0, 0, 2);");
        lblSelectedRoomSummary.setText(
                "Phòng đã chọn: " + selectedRooms.size() + "  •  Tổng số người: " + totalGuests
                        + "\nMỗi phòng cần nhập số người không vượt quá sức chứa tối đa.");
        updateDepositSummary();
    }

    private boolean validateGuestAssignments() {
        syncGuestCounts();
        List<String> errors = new ArrayList<>();

        for (Phong room : selectedRooms) {
            int guestCount = guestCountMap.getOrDefault(room, 0);
            if (guestCount <= 0) {
                errors.add("- Phòng " + room.getMaPhong() + " chưa nhập số người hợp lệ.");
            }
            if (guestCount > room.getSucChua()) {
                errors.add("- Phòng " + room.getMaPhong() + " vượt sức chứa tối đa (" + guestCount + "/"
                        + room.getSucChua() + ").");
            }
        }

        if (!errors.isEmpty()) {
            showAlert("Số người chưa hợp lệ", String.join("\n", errors));
            return false;
        }
        return true;
    }

    private String normalizeLookupValue(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean hasLoadedCustomerBinding() {
        return currentKH != null || !lastResolvedCustomerPhone.isBlank();
    }

    private boolean hasCompletePhone(String phone) {
        return normalizeLookupValue(phone).length() == PHONE_LENGTH;
    }

    private void handlePhoneTextChanged(String oldVal, String newVal) {
        if (isAutoFilling) {
            return;
        }

        String normalizedOld = normalizeLookupValue(oldVal);
        String normalizedNew = normalizeLookupValue(newVal);
        resetStyle(txtSdt);

        if (hasLoadedCustomerBinding() && !normalizedOld.equals(normalizedNew)) {
            resetCustomerFormExceptPhone();
        }

        if (!hasCompletePhone(normalizedNew)) {
            lastPromptedPhone = "";
            resetCustomerFormExceptPhone();
            return;
        }

        searchKhachHangByPhone(normalizedNew, true, false);
    }

    private void clearCurrentCustomerBinding() {
        currentKH = null;
        lastResolvedCustomerPhone = "";
    }

    private void resetCustomerForm() {
        if (searchTask != null && searchTask.isRunning()) {
            searchTask.cancel();
        }
        isAutoFilling = true;
        clearCurrentCustomerBinding();
        lastCustomerLookupKey = "";
        lastPromptedPhone = "";
        txtSdt.clear();
        txtTen.clear();
        txtDiaChi.clear();
        if (txtSoGiayTo != null) {
            txtSoGiayTo.clear();
            resetStyle(txtSoGiayTo);
        }
        if (cbLoaiGiayTo != null) {
            cbLoaiGiayTo.setValue(KhachHangService.GIAY_TO_CCCD);
        }
        if (rbNam != null) {
            rbNam.setSelected(true);
        }
        txtTen.setEditable(true);
        txtDiaChi.setEditable(true);
        resetStyle(txtSdt);
        resetStyle(txtTen);
        resetStyle(txtDiaChi);
        isAutoFilling = false;
        if (txtSdt != null) {
            txtSdt.requestFocus();
        }
    }

    private void resetCustomerFormExceptPhone() {
        if (searchTask != null && searchTask.isRunning()) {
            searchTask.cancel();
        }
        isAutoFilling = true;
        clearCurrentCustomerBinding();
        lastCustomerLookupKey = "";
        if (!hasCompletePhone(txtSdt == null ? "" : txtSdt.getText())) {
            lastPromptedPhone = "";
        }
        txtTen.clear();
        txtDiaChi.clear();
        if (txtSoGiayTo != null) {
            txtSoGiayTo.clear();
            resetStyle(txtSoGiayTo);
        }
        if (cbLoaiGiayTo != null) {
            cbLoaiGiayTo.setValue(KhachHangService.GIAY_TO_CCCD);
        }
        if (rbNam != null) {
            rbNam.setSelected(true);
        }
        txtTen.setEditable(true);
        txtDiaChi.setEditable(true);
        resetStyle(txtSdt);
        resetStyle(txtTen);
        resetStyle(txtDiaChi);
        isAutoFilling = false;
    }

    private void prepareInlineNewCustomer(String phone) {
        isAutoFilling = true;
        clearCurrentCustomerBinding();
        if (txtSdt != null) {
            txtSdt.setText(phone == null ? "" : phone);
        }
        if (txtSoGiayTo != null) {
            txtSoGiayTo.clear();
            resetStyle(txtSoGiayTo);
        }
        if (cbLoaiGiayTo != null) {
            cbLoaiGiayTo.setValue(KhachHangService.GIAY_TO_CCCD);
        }
        txtTen.clear();
        txtDiaChi.clear();
        if (rbNam != null) {
            rbNam.setSelected(true);
        }
        resetStyle(txtSdt);
        resetStyle(txtTen);
        resetStyle(txtDiaChi);
        isAutoFilling = false;
        if (txtSoGiayTo != null) {
            txtSoGiayTo.requestFocus();
        } else if (txtTen != null) {
            txtTen.requestFocus();
        }
    }

    private void promptCreateInlineCustomer(String phone, boolean userInitiated) {
        String normalizedPhone = normalizeLookupValue(phone);
        if (normalizedPhone.isEmpty()) {
            return;
        }
        if (!userInitiated && normalizedPhone.equals(lastPromptedPhone)) {
            return;
        }
        lastPromptedPhone = normalizedPhone;

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Không tìm thấy khách hàng");
        confirmDialog.setHeaderText("Số điện thoại \"" + normalizedPhone + "\" chưa được đăng ký.");
        confirmDialog.setContentText("Bạn có muốn thêm mới khách hàng với số điện thoại này không?");

        ButtonType btnThem = new ButtonType("Đồng ý", ButtonBar.ButtonData.YES);
        ButtonType btnHuy = new ButtonType("Không", ButtonBar.ButtonData.NO);
        confirmDialog.getButtonTypes().setAll(btnThem, btnHuy);

        confirmDialog.showAndWait().ifPresent(result -> {
            if (!normalizedPhone.equals(normalizeLookupValue(txtSdt == null ? "" : txtSdt.getText()))) {
                return;
            }
            if (result == btnThem) {
                KhachHang newCustomer = openAddCustomerDialog(normalizedPhone);
                if (newCustomer != null) {
                    lastPromptedPhone = "";
                    fillCustomerForm(newCustomer);
                } else {
                    resetStyle(txtSdt);
                    txtSdt.requestFocus();
                }
            } else {
                resetStyle(txtSdt);
                txtSdt.requestFocus();
            }
        });
    }

    private void fillCustomerForm(KhachHang customer) {
        if (customer == null) {
            resetCustomerForm();
            return;
        }
        isAutoFilling = true;
        currentKH = customer;
        lastResolvedCustomerPhone = normalizeLookupValue(customer.getSoDienThoai());
        lastCustomerLookupKey = buildLookupKey(customer.getSoDienThoai(), customer.getSoGiayTo());
        lastPromptedPhone = "";
        String currentPhone = normalizeLookupValue(txtSdt.getText());
        int currentCaret = txtSdt.getCaretPosition();
        boolean phoneChanged = !currentPhone.equals(lastResolvedCustomerPhone);
        if (phoneChanged) {
            txtSdt.setText(lastResolvedCustomerPhone);
        }
        txtSoGiayTo.setText(customer.getSoGiayTo() == null ? "" : customer.getSoGiayTo());
        cbLoaiGiayTo.setValue(customer.getLoaiGiayTo() == null || customer.getLoaiGiayTo().isBlank()
                ? KhachHangService.GIAY_TO_CCCD
                : customer.getLoaiGiayTo());
        txtTen.setText(customer.getTenKhachHang() == null ? "" : customer.getTenKhachHang());
        txtDiaChi.setText(customer.getDiaChi() == null ? "" : customer.getDiaChi());
        if (customer.isGioiTinh())
            rbNam.setSelected(true);
        else
            rbNu.setSelected(true);
        txtTen.setEditable(true);
        txtDiaChi.setEditable(true);
        resetStyle(txtSdt);
        resetStyle(txtSoGiayTo);
        resetStyle(txtTen);
        resetStyle(txtDiaChi);
        isAutoFilling = false;
        int targetCaret = phoneChanged
                ? lastResolvedCustomerPhone.length()
                : Math.max(0, Math.min(currentCaret, txtSdt.getText().length()));
        Platform.runLater(() -> {
            if (txtSdt != null) {
                txtSdt.positionCaret(Math.max(0, Math.min(targetCaret, txtSdt.getText().length())));
            }
        });
    }

    private String buildLookupKey(String phone, String soGiayTo) {
        return normalizeLookupValue(phone) + "|" + normalizeLookupValue(soGiayTo);
    }

    private KhachHang openAddCustomerDialog(String phone) {
        javafx.stage.Window owner = txtSdt.getScene() == null ? null : txtSdt.getScene().getWindow();
        CustomerDialog dialog = new CustomerDialog(owner, null, khService, null);
        dialog.setInitialPhone(phone);
        return dialog.showAndGetCustomer();
    }

    private void handleSearchCustomerAction() {
        showCustomerSearchPopup();
        /*
         * if (phone.isEmpty()) {
         * showCustomerSearchPopup();
         * return;
         * }
         * if (!hasCompletePhone(phone)) {
         * setInvalidStyle(txtSdt);
         * showAlert("Số điện thoại chưa hợp lệ", "Vui lòng nhập đủ " + PHONE_LENGTH +
         * " chữ số trước khi tìm khách hàng.");
         * txtSdt.requestFocus();
         * return;
         * }
         * searchKhachHangByPhone(phone, true, true);
         */
    }

    private void searchKhachHang(String sdt) {
        if (searchTask != null && searchTask.isRunning())
            searchTask.cancel();

        searchTask = new Task<KhachHang>() {
            @Override
            protected KhachHang call() {
                return khService.getAll().stream()
                        .filter(kh -> kh.getSoDienThoai().equals(sdt))
                        .findFirst().orElse(null);
            }
        };

        searchTask.setOnSucceeded(e -> {
            if (!sdt.equals(txtSdt.getText()))
                return;
            currentKH = searchTask.getValue();
            if (currentKH != null) {
                if (!currentKH.isDangHoatDong()) {
                    showAlert("Khách hàng đã bị vô hiệu hóa",
                            "Khách hàng đã bị vô hiệu hóa. Vui lòng vào Quản lý khách hàng để kích hoạt lại trước khi lập phiếu.");
                    resetCustomerFormExceptPhone();
                    return;
                }
                fillCustomerForm(currentKH);

                txtSdt.setStyle("-fx-background-radius: 6; -fx-border-color: " + SUCCESS_COLOR + ";" +
                        "-fx-border-width: 2px; -fx-border-radius: 6;");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> {
                    resetStyle(txtSdt);
                    isAutoFilling = false;
                });
                pause.play();
            } else {
                Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                confirmDialog.setTitle("Không tìm thấy khách hàng");
                confirmDialog.setHeaderText("Số điện thoại \"" + sdt + "\" chưa có trong hệ thống.");
                confirmDialog.setContentText("Bạn có muốn thêm khách hàng mới với số điện thoại này không?");

                ButtonType btnThem = new ButtonType("Thêm mới", ButtonBar.ButtonData.YES);
                ButtonType btnHuy = new ButtonType("Để sau", ButtonBar.ButtonData.NO);
                confirmDialog.getButtonTypes().setAll(btnThem, btnHuy);

                confirmDialog.showAndWait().ifPresent(result -> {
                    if (result == btnThem) {
                        KhachHang newCustomer = openAddCustomerDialog(sdt);
                        if (newCustomer != null) {
                            fillCustomerForm(newCustomer);
                        } else {
                            resetCustomerFormExceptPhone();
                        }
                    } else {
                        resetCustomerFormExceptPhone();
                    }
                });
            }
        });

        Thread th = new Thread(searchTask);
        th.setDaemon(true);
        th.start();
    }

    private void searchKhachHangByPhone(String phone, boolean promptCreateWhenNotFound) {
        searchKhachHangByPhone(phone, promptCreateWhenNotFound, false);
    }

    private void searchKhachHangByPhone(String phone, boolean promptCreateWhenNotFound, boolean userInitiated) {
        searchKhachHang(phone, "", promptCreateWhenNotFound, userInitiated);
    }

    private void searchKhachHang(String sdt, String soGiayTo) {
        searchKhachHang(sdt, soGiayTo, true);
    }

    private void searchKhachHang(String sdt, String soGiayTo, boolean promptCreateWhenNotFound) {
        searchKhachHang(sdt, soGiayTo, promptCreateWhenNotFound, true);
    }

    private void searchKhachHang(String sdt, String soGiayTo, boolean promptCreateWhenNotFound, boolean userInitiated) {
        if (searchTask != null && searchTask.isRunning())
            searchTask.cancel();

        String normalizedPhone = sdt == null ? "" : sdt.trim();
        String normalizedDocument = soGiayTo == null ? "" : soGiayTo.trim();
        if (normalizedPhone.isEmpty() && normalizedDocument.isEmpty()) {
            resetCustomerForm();
            return;
        }

        String lookupKey = buildLookupKey(normalizedPhone, normalizedDocument);

        searchTask = new Task<KhachHang>() {
            @Override
            protected KhachHang call() {
                if (!normalizedPhone.isEmpty()) {
                    KhachHang byPhone = khService.findByPhoneOrDocument(normalizedPhone);
                    if (byPhone != null) {
                        return byPhone;
                    }
                }
                if (!normalizedDocument.isEmpty()) {
                    return khService.findByPhoneOrDocument(normalizedDocument);
                }
                return null;
            }
        };

        searchTask.setOnSucceeded(e -> {
            if (!normalizedPhone.equals(txtSdt.getText().trim())
                    || !normalizedDocument.equals(txtSoGiayTo.getText().trim()))
                return;
            currentKH = searchTask.getValue();
            if (currentKH != null) {
                if (!currentKH.isDangHoatDong()) {
                    showAlert("Khách hàng đã bị vô hiệu hóa",
                            "Khách hàng đã bị vô hiệu hóa. Vui lòng vào Quản lý khách hàng để kích hoạt lại trước khi lập phiếu.");
                    resetCustomerFormExceptPhone();
                    return;
                }
                lastPromptedPhone = "";
                fillCustomerForm(currentKH);

                txtSdt.setStyle("-fx-background-radius: 6; -fx-border-color: " + SUCCESS_COLOR + ";"
                        + "-fx-border-width: 2px; -fx-border-radius: 6;");
                txtSoGiayTo.setStyle("-fx-background-radius: 6; -fx-border-color: " + SUCCESS_COLOR + ";"
                        + "-fx-border-width: 2px; -fx-border-radius: 6;");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(2));
                pause.setOnFinished(ev -> {
                    resetStyle(txtSdt);
                    resetStyle(txtSoGiayTo);
                    isAutoFilling = false;
                });
                pause.play();
            } else {
                resetCustomerFormExceptPhone();
                if (!promptCreateWhenNotFound) {
                    return;
                }
                promptCreateInlineCustomer(!normalizedPhone.isEmpty() ? normalizedPhone : normalizedDocument,
                        userInitiated);
                /*
                 * Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
                 * confirmDialog.setTitle("Không tìm thấy khách hàng");
                 * String keyword = !normalizedPhone.isEmpty() ? normalizedPhone :
                 * normalizedDocument;
                 * if (!userInitiated && keyword.equals(lastPromptedPhone)) {
                 * return;
                 * }
                 * lastPromptedPhone = keyword;
                 * confirmDialog.setHeaderText("Không tìm thấy khách hàng với \"" + keyword +
                 * "\".");
                 * confirmDialog.
                 * setContentText("Bạn có muốn thêm khách hàng mới với thông tin này không?");
                 * 
                 * ButtonType btnThem = new ButtonType("Thêm mới", ButtonBar.ButtonData.YES);
                 * ButtonType btnHuy = new ButtonType("Để sau", ButtonBar.ButtonData.NO);
                 * confirmDialog.getButtonTypes().setAll(btnThem, btnHuy);
                 * 
                 * confirmDialog.showAndWait().ifPresent(result -> {
                 * if (result == btnThem) {
                 * KhachHang newCustomer = openAddCustomerDialog(normalizedPhone);
                 * if (newCustomer != null) {
                 * lastPromptedPhone = "";
                 * fillCustomerForm(newCustomer);
                 * } else {
                 * resetStyle(txtSdt);
                 * txtSdt.requestFocus();
                 * }
                 * } else {
                 * resetStyle(txtSdt);
                 * txtSdt.requestFocus();
                 * }
                 * });
                 */
            }
        });

        Thread th = new Thread(searchTask);
        th.setDaemon(true);
        th.start();
    }

    private void showCustomerSearchPopup() {
        try {
            javafx.stage.Window owner = txtSdt.getScene().getWindow();
            CustomerSearchDialog dialog = new CustomerSearchDialog(owner, khService);
            KhachHang selected = dialog.show();
            if (selected != null) {
                fillCustomerForm(selected);

                txtSdt.setStyle("-fx-background-radius: 6; -fx-border-color: #27ae60;" +
                        "-fx-border-width: 2px; -fx-border-radius: 6;");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.seconds(2));
                pause.setOnFinished(e -> {
                    resetStyle(txtSdt);
                    resetStyle(txtSoGiayTo);
                    isAutoFilling = false;
                });
                pause.play();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Lỗi hệ thống", "Không thể mở cửa sổ tìm kiếm!\nChi tiết: " + e);
        }
    }

    public void preSelectRoom(Phong p) {
        clearAllAndReset();
        loadPhongTrong();
        javafx.application.Platform.runLater(() -> {
            if (p != null) {
                selectedRooms.add(p);
                guestCountMap.putIfAbsent(p, 1);
                renderSelectedRooms();
                if (tvPhongTrong != null)
                    tvPhongTrong.refresh();
            }
        });
    }

    public void preFillCustomer(KhachHang kh) {
        clearAllAndReset();
        loadPhongTrong();
        javafx.application.Platform.runLater(() -> {
            if (kh != null) {
                fillCustomerForm(kh);
            }
        });
    }

    private void clearAllAndReset() {
        isAutoFilling = true;
        txtSdt.clear();
        if (txtSoGiayTo != null)
            txtSoGiayTo.clear();
        txtTen.clear();
        txtDiaChi.clear();
        resetStyle(txtSdt);
        if (txtSoGiayTo != null)
            resetStyle(txtSoGiayTo);
        if (cbLoaiGiayTo != null)
            cbLoaiGiayTo.setValue(KhachHangService.GIAY_TO_CCCD);
        resetStyle(txtTen);
        resetStyle(txtDiaChi);
        resetStyle(dpNgayNhan);
        resetStyle(dpNgayTra);
        applySuggestedBookingDateTimeDefaults();
        selectedRooms.clear();
        guestCountMap.clear();
        if (cbHinhThucDatCoc != null)
            cbHinhThucDatCoc.setValue("Tiền mặt");
        if (txtGhiChuDatCoc != null)
            txtGhiChuDatCoc.clear();
        clearCurrentCustomerBinding();
        lastCustomerLookupKey = "";
        if (selectedRoomsContainer != null) {
            renderSelectedRooms();
        }
        if (tvPhongTrong != null) {
            tvPhongTrong.setStyle("");
            tvPhongTrong.refresh();
        }
        if (txtFilterMa != null) {
            clearFilters();
        }
        goToStep1();
        loadPhongTrong();
        refreshBangGiaApDung();
        updateDepositSummary();
        isAutoFilling = false;
    }

    private void syncDocumentPromptAndInput() {
        if (txtSoGiayTo == null || cbLoaiGiayTo == null) {
            return;
        }
        boolean isCccd = KhachHangService.GIAY_TO_CCCD.equals(cbLoaiGiayTo.getValue());
        txtSoGiayTo.setPromptText(isCccd ? "Nhập số CCCD..." : "Nhập số hộ chiếu...");
        String sanitized = sanitizeDocumentInput(txtSoGiayTo.getText());
        if (!sanitized.equals(txtSoGiayTo.getText())) {
            TextInputSafeUtils.safeSetText(txtSoGiayTo, sanitized);
        }
    }

    private void installPhoneTextFormatter() {
        if (txtSdt == null) {
            return;
        }
        txtSdt.setTextFormatter(new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText == null || newText.isEmpty() || newText.matches("\\d{0," + PHONE_LENGTH + "}")) {
                return change;
            }
            return null;
        }));
    }

    private void installDocumentTextFormatter() {
        if (txtSoGiayTo == null) {
            return;
        }
        txtSoGiayTo.setTextFormatter(TextInputSafeUtils.filteringFormatter(this::sanitizeDocumentInput));
    }

    private String sanitizeDocumentInput(String value) {
        String safeValue = value == null ? "" : value;
        boolean isCccd = cbLoaiGiayTo != null
                && KhachHangService.GIAY_TO_CCCD.equals(cbLoaiGiayTo.getValue());
        String sanitized = isCccd
                ? safeValue.replaceAll("\\D", "")
                : safeValue.toUpperCase().replaceAll("[^A-Z0-9]", "");
        return sanitized.length() > 12 ? sanitized.substring(0, 12) : sanitized;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(40);
        resetStyle(tf);
        tf.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                tf.setStyle(FIELD_FOCUS_STYLE);
            } else {
                resetStyle(tf);
            }
        });
        return tf;
    }

    private boolean hasValidCustomerId(KhachHang kh) {
        return kh != null
                && kh.getMaKhachHang() != null
                && kh.getMaKhachHang().matches("^KH\\d{3,}$");
    }

    private void resetStyle(Control c) {
        c.setStyle(FIELD_BASE_STYLE);
    }

    private void styleComboBox(ComboBox<?> comboBox) {
        resetStyle(comboBox);
        comboBox.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                comboBox.setStyle(FIELD_FOCUS_STYLE);
            } else {
                resetStyle(comboBox);
            }
        });
    }

    private void styleDatePicker(DatePicker datePicker) {
        resetStyle(datePicker);
        datePicker.focusedProperty().addListener((obs, oldVal, focused) -> {
            if (focused) {
                datePicker.setStyle(FIELD_FOCUS_STYLE);
            } else {
                resetStyle(datePicker);
            }
        });
    }

    private void styleRadioButton(RadioButton radioButton) {
        radioButton.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12.5));
        radioButton.setTextFill(Color.web("#0f172a"));
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-font-weight: 700; " +
                "-fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 16 10 16;");
        button.setOnMouseEntered(e -> button
                .setStyle("-fx-background-color: " + PRIMARY_HOVER + "; -fx-text-fill: white; -fx-font-weight: 700; " +
                        "-fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 16 10 16;"));
        button.setOnMouseExited(e -> button
                .setStyle("-fx-background-color: " + PRIMARY_COLOR + "; -fx-text-fill: white; -fx-font-weight: 700; " +
                        "-fx-font-size: 14; -fx-cursor: hand; -fx-background-radius: 10; -fx-padding: 10 16 10 16;"));
    }

    private void styleSecondaryButton(Button button) {
        button.setStyle("-fx-background-color: " + SECONDARY_COLOR + "; -fx-text-fill: " + AppTheme.SECONDARY_TEXT
                + "; -fx-font-weight: 700; " +
                "-fx-font-size: 13; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-color: #dbe4ee; -fx-border-radius: 10; -fx-padding: 9 14 9 14;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: " + SECONDARY_HOVER
                + "; -fx-text-fill: #0f172a; -fx-font-weight: 700; " +
                "-fx-font-size: 13; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-color: #cbd5e1; -fx-border-radius: 10; -fx-padding: 9 14 9 14;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + SECONDARY_COLOR + "; -fx-text-fill: "
                + AppTheme.SECONDARY_TEXT + "; -fx-font-weight: 700; " +
                "-fx-font-size: 13; -fx-cursor: hand; -fx-background-radius: 10; -fx-border-color: #dbe4ee; -fx-border-radius: 10; -fx-padding: 9 14 9 14;"));
    }

    private void styleOutlineButton(Button button) {
        String baseStyle = "-fx-background-color: #f8fafc; -fx-text-fill: #0f172a; -fx-font-weight: 700; " +
                "-fx-font-size: 13; -fx-cursor: hand; -fx-border-color: #94a3b8; -fx-border-width: 1.2; -fx-border-radius: 10; "
                +
                "-fx-background-radius: 10; -fx-padding: 8 14 8 14;";
        String hoverStyle = "-fx-background-color: #ecfeff; -fx-text-fill: " + PRIMARY_HOVER
                + "; -fx-font-weight: 700; " +
                "-fx-font-size: 13; -fx-cursor: hand; -fx-border-color: " + PRIMARY_COLOR
                + "; -fx-border-width: 1.2; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-padding: 8 14 8 14;";
        String pressedStyle = "-fx-background-color: #cffafe; -fx-text-fill: " + PRIMARY_HOVER
                + "; -fx-font-weight: 700; " +
                "-fx-font-size: 13; -fx-cursor: hand; -fx-border-color: " + PRIMARY_HOVER
                + "; -fx-border-width: 1.2; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-padding: 8 14 8 14;";
        String disabledStyle = "-fx-background-color: #f8fafc; -fx-text-fill: #94a3b8; -fx-font-weight: 700; " +
                "-fx-font-size: 13; -fx-border-color: #cbd5e1; -fx-border-width: 1.2; -fx-border-radius: 10; " +
                "-fx-background-radius: 10; -fx-padding: 8 14 8 14; -fx-opacity: 1;";

        button.setStyle(baseStyle);
        button.setOpacity(1.0);
        button.setOnMouseEntered(e -> {
            if (!button.isDisabled()) {
                button.setStyle(hoverStyle);
            }
        });
        button.setOnMouseExited(e -> {
            if (!button.isDisabled()) {
                button.setStyle(baseStyle);
            }
        });
        button.setOnMousePressed(e -> {
            if (!button.isDisabled()) {
                button.setStyle(pressedStyle);
            }
        });
        button.setOnMouseReleased(e -> {
            if (!button.isDisabled()) {
                button.setStyle(button.isHover() ? hoverStyle : baseStyle);
            }
        });
        button.disabledProperty()
                .addListener((obs, oldVal, disabled) -> button.setStyle(disabled ? disabledStyle : baseStyle));
    }

    private void styleDangerButton(Button button) {
        button.setStyle(
                "-fx-background-color: #fff1f2; -fx-text-fill: #be123c; -fx-font-weight: 700; -fx-font-size: 12.5; " +
                        "-fx-cursor: hand; -fx-border-color: #fecdd3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 12 8 12;");
        button.setOnMouseEntered(e -> button.setStyle(
                "-fx-background-color: #ffe4e6; -fx-text-fill: #9f1239; -fx-font-weight: 700; -fx-font-size: 12.5; " +
                        "-fx-cursor: hand; -fx-border-color: #fda4af; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 12 8 12;"));
        button.setOnMouseExited(e -> button.setStyle(
                "-fx-background-color: #fff1f2; -fx-text-fill: #be123c; -fx-font-weight: 700; -fx-font-size: 12.5; " +
                        "-fx-cursor: hand; -fx-border-color: #fecdd3; -fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8 12 8 12;"));
    }

    private String buildRemoveButtonStyle(boolean hover) {
        return hover
                ? "-fx-background-color: #fee2e2; -fx-text-fill: #b91c1c; -fx-font-weight: 900; -fx-background-radius: 18; -fx-cursor: hand; -fx-padding: 0; -fx-border-color: #fca5a5; -fx-border-radius: 18; -fx-effect: dropshadow(gaussian, rgba(220,38,38,0.16), 10, 0.2, 0, 2);"
                : "-fx-background-color: #fff5f5; -fx-text-fill: #dc2626; -fx-font-weight: 900; -fx-background-radius: 18; -fx-cursor: hand; -fx-padding: 0; -fx-border-color: #fecaca; -fx-border-radius: 18; -fx-effect: dropshadow(gaussian, rgba(239,68,68,0.10), 8, 0.18, 0, 2);";
    }

    private Spinner<Integer> createTimeSpinner(int max, int current) {
        Spinner<Integer> spinner = new Spinner<>(0, max, current);
        spinner.setEditable(true);
        spinner.setPrefHeight(40);
        spinner.setPrefWidth(85);
        spinner.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10;" +
                        "-fx-border-color: #d7deea; -fx-border-radius: 10; -fx-border-width: 1.2;");
        spinner.getEditor().setStyle(
                "-fx-text-fill: #0f172a; -fx-font-size: 14px; -fx-font-weight: bold;" +
                        "-fx-alignment: center; -fx-background-color: transparent; -fx-padding: 0 4;");
        spinner.focusedProperty().addListener((obs, oldVal, focused) -> {
            spinner.setStyle(focused
                    ? "-fx-background-color: white; -fx-background-radius: 10;" +
                            "-fx-border-color: " + PRIMARY_COLOR + "; -fx-border-radius: 10; -fx-border-width: 1.8;" +
                            "-fx-effect: dropshadow(gaussian, rgba(13,148,136,0.16), 12, 0.2, 0, 2);"
                    : "-fx-background-color: white; -fx-background-radius: 10;" +
                            "-fx-border-color: #d7deea; -fx-border-radius: 10; -fx-border-width: 1.2;");
        });

        spinner.valueProperty().addListener((obs, oldV, newV) -> onDatesChanged());

        spinner.getEditor().setTextFormatter(
                TextInputSafeUtils.filteringFormatter(value -> value == null ? "" : value.replaceAll("\\D", "")));

        return spinner;
    }

    private void setInvalidStyle(Control c) {
        c.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: " + DANGER_COLOR +
                "; -fx-border-radius: 10; -fx-border-width: 2px; -fx-padding: 0 12 0 12;");
    }

    /**
     * Validates mandatory customer fields before allowing the "add new customer"
     * flow.
     * Returns true only when all required fields are present and valid.
     */
    private boolean validateForNewCustomer() {
        List<String> errors = new ArrayList<>();
        Control firstError = null;

        String sdt = txtSdt.getText().trim();
        if (sdt.isEmpty() || !sdt.matches("^0\\d{9}$")) {
            errors.add("- Số điện thoại không hợp lệ (phải bắt đầu bằng 0 và đủ 10 số).");
            setInvalidStyle(txtSdt);
            if (firstError == null)
                firstError = txtSdt;
        }

        String docTypeError = khService.validateLoaiGiayTo(cbLoaiGiayTo.getValue());
        if (docTypeError != null) {
            errors.add("- " + docTypeError);
            setInvalidStyle(cbLoaiGiayTo);
            if (firstError == null)
                firstError = cbLoaiGiayTo;
        }

        String docNumberError = khService.validateSoGiayTo(cbLoaiGiayTo.getValue(), txtSoGiayTo.getText());
        if (docNumberError != null) {
            errors.add("- " + docNumberError);
            setInvalidStyle(txtSoGiayTo);
            if (firstError == null)
                firstError = txtSoGiayTo;
        }

        String tenError = khService.validateTen(txtTen.getText());
        if (tenError != null) {
            errors.add("- " + tenError);
            setInvalidStyle(txtTen);
            if (firstError == null)
                firstError = txtTen;
        }

        if (txtDiaChi.getText().trim().isEmpty()) {
            errors.add("- Vui lòng nhập địa chỉ.");
            setInvalidStyle(txtDiaChi);
            if (firstError == null)
                firstError = txtDiaChi;
        }

        if (!errors.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Thông tin chưa đầy đủ");
            alert.setHeaderText("Vui lòng nhập đầy đủ thông tin trước khi thêm khách mới:");
            alert.setContentText(String.join("\n", errors));
            final Control target = firstError;
            alert.showAndWait().ifPresent(r -> {
                if (target != null)
                    target.requestFocus();
            });
            return false;
        }
        return true;
    }

    private String capitalizeWords(String text) {
        if (text == null || text.trim().isEmpty())
            return text;
        String[] words = text.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase(Locale.ROOT));
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void showAlert(String title, String content) {
        Alert.AlertType type = title.contains("Lỗi") || title.contains("Thiếu") || title.contains("không hợp lệ")
                ? Alert.AlertType.ERROR
                : Alert.AlertType.INFORMATION;
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

}
