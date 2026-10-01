package gui;

import dao.ChiTietHoaDonDAO;
import dao.ChiTietPhieuDatDAO;
import dao.HoaDonDAO;
import dao.PhieuDatPhongDAO;
import dao.PhongDAO;
import entity.ChiTietHoaDon;
import entity.ChiTietPhieuDat;
import entity.HoaDon;
import entity.NhanVien;
import entity.PhieuDatPhong;
import entity.Phong;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import util.PricingService;
import util.DepositPolicy;
import util.RoomStatus;
import util.StatusUtils;
import util.TextInputSafeUtils;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CheckoutView {
    private final NhanVien loggedInNhanVien;
    private final PricingService pricingService = new PricingService();
    private final ChiTietPhieuDatDAO chiTietPhieuDatDAO = new ChiTietPhieuDatDAO();
    private final ChiTietHoaDonDAO chiTietHoaDonDAO = new ChiTietHoaDonDAO();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final PhieuDatPhongDAO phieuDatPhongDAO = new PhieuDatPhongDAO();
    private final PhongDAO phongDAO = new PhongDAO();

    private final TableView<PhieuDatPhong> tvBookings = new TableView<>();
    private final VBox roomsContainer = new VBox(12);
    private final Map<String, RoomCheckoutState> roomStates = new LinkedHashMap<>();

    private TextField txtSearch;
    private Label lblBookingCode;
    private Label lblGuestName;
    private Label lblGuestPhone;
    private Label lblRoomCount;
    private Label lblRoomSubtotal;
    private Label lblServiceSubtotal;
    private Label lblDepositApplied;
    private Label lblTotal;
    private TextField txtCashReceived;
    private javafx.scene.control.ComboBox<String> cmbPaymentMethod;
    private Label lblCashStatusTitle;
    private Label lblCashStatusValue;
    private Label lblPaymentHint;
    private VBox detailPane;

    private List<PhieuDatPhong> allBookings = new ArrayList<>();
    private List<ChiTietPhieuDat> allDetailsForBooking = new ArrayList<>();
    private PhieuDatPhong selectedPDP;
    private HoaDon currentHoaDon;
    private Runnable onCheckoutSaved;
    private boolean isPaidAmountAuto = true;
    private boolean isUpdatingPaidAmount = false;

    public CheckoutView(NhanVien nhanVien) {
        this.loggedInNhanVien = nhanVien;
    }

    public void setOnCheckoutSaved(Runnable onCheckoutSaved) {
        this.onCheckoutSaved = onCheckoutSaved;
    }

    public Node createView() {
        HBox root = new HBox(18);
        root.setPadding(new Insets(18));
        root.getStyleClass().add("checkout-view");
        root.getStylesheets().addAll(
                stylesheetUri("global-theme.css"),
                stylesheetUri("checkout-flow.css"));

        VBox leftPane = buildBookingListPane();
        ScrollPane rightPane = buildDetailPane();

        HBox.setHgrow(rightPane, Priority.ALWAYS);
        root.getChildren().addAll(leftPane, rightPane);

        clearDetail();
        loadData();
        return root;
    }

    public void selectRoom(String maPhong) {
        if (txtSearch != null) {
            txtSearch.setText(maPhong);
            filterBookings();
        }
    }

    public void loadData() {
        setBookingListLoading(true);
        Task<List<PhieuDatPhong>> task = new Task<>() {
            @Override
            protected List<PhieuDatPhong> call() {
                return phieuDatPhongDAO.getDanhSachChoCheckout();
            }
        };

        task.setOnSucceeded(e -> {
            allBookings = task.getValue() != null ? task.getValue() : new ArrayList<>();
            setBookingListLoading(false);
            filterBookings();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            allBookings = new ArrayList<>();
            tvBookings.getItems().clear();
            setBookingListLoading(false);
            showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu",
                    ex != null ? ex.getMessage() : "Không thể tải danh sách phiếu cần trả phòng.");
        });

        Thread loader = new Thread(task, "checkout-load");
        loader.setDaemon(true);
        loader.start();
    }

    private void handleRefresh() {
        if (txtSearch != null) {
            txtSearch.clear();
        }
        if (tvBookings != null) {
            tvBookings.getSelectionModel().clearSelection();
        }
        clearDetail();
        loadData();
    }

    private void setBookingListLoading(boolean loading) {
        if (tvBookings != null) {
            tvBookings.setPlaceholder(new Label(loading
                    ? "\u0110ang t\u1ea3i d\u1eef li\u1ec7u..."
                    : "Kh\u00f4ng c\u00f3 phi\u1ebfu n\u00e0o c\u00f3 ph\u00f2ng \u0111ang \u1edf."));
            if (loading) {
                tvBookings.getItems().clear();
            }
        }
    }

    private VBox buildBookingListPane() {
        VBox wrapper = new VBox(12);
        wrapper.setPrefWidth(470);
        wrapper.setMinWidth(440);

        Label title = new Label("TRẢ PHÒNG");
        title.getStyleClass().add("checkout-title");

        Label subtitle = new Label(
                "Chọn phiếu đang có phòng đã nhận phòng để xem từng phòng, thêm dịch vụ riêng và xác nhận trả phòng.");
        subtitle.getStyleClass().add("checkout-subtitle");
        subtitle.setWrapText(true);

        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm theo mã phiếu, khách hàng hoặc số phòng đang ở...");
        txtSearch.getStyleClass().add("checkout-search");
        txtSearch.setPrefHeight(40);
        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> filterBookings());

        Button btnRefresh = createButton("Làm mới", "btn-secondary");
        btnRefresh.setOnAction(e -> handleRefresh());

        HBox toolbar = new HBox(10, txtSearch, btnRefresh);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(txtSearch, Priority.ALWAYS);

        Label tableTitle = new Label("Danh sách phiếu cần trả");
        tableTitle.getStyleClass().add("checkout-section-title");

        configureTable();
        VBox tableCard = new VBox(10, tableTitle, tvBookings);
        tableCard.getStyleClass().add("checkout-card");
        tableCard.setPadding(new Insets(14));
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        VBox.setVgrow(tvBookings, Priority.ALWAYS);

        wrapper.getChildren().addAll(title, subtitle, toolbar, tableCard);
        return wrapper;
    }

    private ScrollPane buildDetailPane() {
        detailPane = new VBox(12);
        detailPane.getStyleClass().add("checkout-card");
        detailPane.setPadding(new Insets(16));
        detailPane.setFillWidth(true);

        lblRoomSubtotal = createMoneyLabel();
        lblServiceSubtotal = createMoneyLabel();
        lblDepositApplied = createMoneyLabel();
        lblTotal = new Label("0 VNĐ");
        lblTotal.getStyleClass().add("checkout-total-value");

        lblCashStatusTitle = new Label("Tiền thừa trả khách");
        lblCashStatusTitle.getStyleClass().add("checkout-info-title");
        lblCashStatusValue = createMoneyLabel();
        lblPaymentHint = new Label("Nhập số tiền khách đưa để hệ thống tự tính tiền thừa hoặc còn thiếu.");
        lblPaymentHint.getStyleClass().add("checkout-subtitle");
        lblPaymentHint.setWrapText(true);

        Label title = new Label("CHI TIẾT THANH TOÁN");
        title.getStyleClass().add("checkout-title");

        Label desc = new Label(
                "Phòng trả và dịch vụ được xử lý theo từng phòng đã nhận phòng, không dùng chung cho toàn phiếu.");
        desc.getStyleClass().add("checkout-subtitle");
        desc.setWrapText(true);


        HBox topRow = new HBox(12, buildCustomerCard(), buildSummaryCard());
        HBox.setHgrow(topRow.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(topRow.getChildren().get(1), Priority.ALWAYS);

        VBox roomCard = buildRoomPanel();
        VBox.setVgrow(roomCard, Priority.ALWAYS);

        HBox actionBar = buildActionBar();
        detailPane.getChildren().addAll(title, desc, topRow, roomCard, actionBar);
        VBox.setVgrow(roomCard, Priority.ALWAYS);

        ScrollPane detailScroll = new ScrollPane(detailPane);
        detailScroll.setFitToWidth(true);
        detailScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        detailScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        detailScroll.getStyleClass().add("checkout-scroll");
        return detailScroll;
    }

    private VBox buildCustomerCard() {
        lblBookingCode = createValueLabel();
        lblGuestName = createValueLabel();
        lblGuestPhone = createValueLabel();
        lblRoomCount = createValueLabel();

        VBox card = createSection("Thông tin phiếu");
        card.setSpacing(12);
        card.setFillWidth(true);

        VBox item1 = compactInfo("Mã phiếu", lblBookingCode);
        VBox item2 = compactInfo("Phòng đang ở", lblRoomCount);
        VBox item3 = compactInfo("Khách hàng", lblGuestName);
        VBox item4 = compactInfo("Điện thoại", lblGuestPhone);

        card.getChildren().addAll(item1, item2, item3, item4);
        return card;
    }

    private VBox buildRoomPanel() {
        ScrollPane roomScroll = createInnerScroll(roomsContainer, 430);
        VBox card = createSection("Phòng trả và dịch vụ theo phòng");
        card.getChildren().addAll(roomScroll, new Separator(), footerRow("Tiền phòng đã chọn", lblRoomSubtotal));
        VBox.setVgrow(roomScroll, Priority.ALWAYS);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildSummaryCard() {
        Label caption = new Label("Cần thanh toán");
        caption.getStyleClass().add("checkout-summary-caption");

        Label hint = new Label("Đã trừ tiền cọc được phân bổ cho các phòng đang trả.");
        hint.getStyleClass().add("checkout-subtitle");
        hint.setWrapText(true);

        VBox totalBox = new VBox(4, caption, lblTotal, hint);

        cmbPaymentMethod = new javafx.scene.control.ComboBox<>();
        cmbPaymentMethod.getItems().addAll("Tiền mặt", "Chuyển khoản", "Thẻ");
        cmbPaymentMethod.setValue("Tiền mặt");
        cmbPaymentMethod.setMaxWidth(Double.MAX_VALUE);
        cmbPaymentMethod.getStyleClass().add("checkout-combo");

        cmbPaymentMethod.valueProperty().addListener((obs, oldV, newV) -> {
            if ("Tiền mặt".equals(newV)) {
                txtCashReceived.setDisable(false);
            } else {
                txtCashReceived.setDisable(true);
                double total = parseMoneyLabel(lblTotal.getText());
                isUpdatingPaidAmount = true;
                txtCashReceived.setText(formatMoney(total));
                isUpdatingPaidAmount = false;
            }
            refreshPaymentSummary();
        });

        txtCashReceived = new TextField();
        txtCashReceived.setPromptText("Nhập số tiền khách đưa");
        txtCashReceived.getStyleClass().add("checkout-search");
        txtCashReceived.setPrefHeight(38);

        txtCashReceived.setTextFormatter(new javafx.scene.control.TextFormatter<>(change -> {
            if (!change.isContentChange()) return change;
            if (txtCashReceived.isFocused()) {
                if (change.isAdded() || change.isReplaced()) {
                    String addedText = change.getText();
                    if (!addedText.matches("\\d*")) {
                        change.setText(addedText.replaceAll("[^\\d]", ""));
                    }
                }
                String newText = change.getControlNewText();
                if (newText.isEmpty() || newText.matches("\\d+")) {
                    return change;
                }
                return null;
            }
            return change;
        }));

        txtCashReceived.focusedProperty().addListener((obs, oldFocus, newFocus) -> {
            if (newFocus) {
                String raw = txtCashReceived.getText().replaceAll("[^\\d]", "");
                isUpdatingPaidAmount = true;
                txtCashReceived.setText(raw);
                isUpdatingPaidAmount = false;
                javafx.application.Platform.runLater(() -> txtCashReceived.positionCaret(txtCashReceived.getText().length()));
            } else {
                String text = txtCashReceived.getText();
                if (text == null || text.trim().isEmpty()) {
                    isPaidAmountAuto = true;
                    double total = parseMoneyLabel(lblTotal.getText());
                    isUpdatingPaidAmount = true;
                    txtCashReceived.setText(formatMoney(total));
                    isUpdatingPaidAmount = false;
                } else {
                    String raw = text.replaceAll("[^\\d]", "");
                    if (!raw.isEmpty()) {
                        try {
                            double val = Double.parseDouble(raw);
                            isUpdatingPaidAmount = true;
                            txtCashReceived.setText(formatMoney(val));
                            isUpdatingPaidAmount = false;
                        } catch (Exception ignored) {}
                    } else {
                        isPaidAmountAuto = true;
                        double total = parseMoneyLabel(lblTotal.getText());
                        isUpdatingPaidAmount = true;
                        txtCashReceived.setText(formatMoney(total));
                        isUpdatingPaidAmount = false;
                    }
                }
                refreshPaymentSummary();
            }
        });

        txtCashReceived.textProperty().addListener((obs, oldValue, newValue) -> {
            if (isUpdatingPaidAmount) return;
            if (txtCashReceived.isFocused()) {
                isPaidAmountAuto = false;
            }
            refreshPaymentSummary();
        });

        VBox paymentBox = new VBox(10,
                compactInput("Phương thức thanh toán", cmbPaymentMethod),
                compactInput("Tiền khách đưa", txtCashReceived),
                footerRow("Cần thu", createLinkedLabel(lblTotal)),
                footerRowDynamicLabel(lblCashStatusTitle, lblCashStatusValue),
                lblPaymentHint);
        paymentBox.getStyleClass().add("checkout-payment-box");

        VBox card = createSection("Tổng kết thanh toán");
        card.getChildren().addAll(
                footerRow("Tiền phòng", createLinkedLabel(lblRoomSubtotal)),
                footerRow("Tiền dịch vụ", createLinkedLabel(lblServiceSubtotal)),
                footerRow("Tiền cọc đã nhận", createLinkedLabel(lblDepositApplied)),
                new Separator(),
                totalBox,
                new Separator(),
                paymentBox);
        return card;
    }

    private HBox buildActionBar() {
        Button btnClear = createButton("Xóa lựa chọn", "btn-secondary");
        btnClear.setOnAction(e -> {
            tvBookings.getSelectionModel().clearSelection();
            clearDetail();
        });

        Button btnCheckout = createButton("Xác nhận trả phòng", "btn-primary");
        btnCheckout.setOnAction(e -> handleCheckout());

        HBox box = new HBox(10, btnClear, btnCheckout);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    private void configureTable() {
        tvBookings.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tvBookings.getStyleClass().add("checkout-table");
        tvBookings.setPlaceholder(new Label("Không có phiếu nào có phòng đang ở."));

        tvBookings.setRowFactory(tv -> {
            TableRow<PhieuDatPhong> row = new TableRow<>();
            row.getStyleClass().add("checkout-row");
            return row;
        });

        TableColumn<PhieuDatPhong, String> colMa = new TableColumn<>("Mã phiếu");
        colMa.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMaDatPhong()));

        TableColumn<PhieuDatPhong, String> colKhach = new TableColumn<>("Khách hàng");
        colKhach.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getKhachHang() != null ? data.getValue().getKhachHang().getTenKhachHang()
                        : "Khách lẻ"));

        TableColumn<PhieuDatPhong, String> colPhong = new TableColumn<>("Phòng đang ở");
        colPhong.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDsMaPhong()));

        TableColumn<PhieuDatPhong, String> colSoLuong = new TableColumn<>("Số lượng");
        colSoLuong.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getSoLuongPhong())));
        colSoLuong.setStyle("-fx-alignment: CENTER;");

        tvBookings.getColumns().setAll(colMa, colKhach, colPhong, colSoLuong);
        tvBookings.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> showDetail(newV));
    }

    private void filterBookings() {
        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        tvBookings.getItems().setAll(allBookings.stream()
                .filter(p -> keyword.isBlank()
                        || p.getMaDatPhong().toLowerCase(Locale.ROOT).contains(keyword)
                        || (p.getKhachHang() != null
                                && p.getKhachHang().getTenKhachHang().toLowerCase(Locale.ROOT).contains(keyword))
                        || (p.getDsMaPhong() != null && p.getDsMaPhong().toLowerCase(Locale.ROOT).contains(keyword)))
                .collect(Collectors.toList()));
    }

    private void showDetail(PhieuDatPhong pdp) {
        selectedPDP = pdp;
        currentHoaDon = null;
        allDetailsForBooking = new ArrayList<>();
        roomStates.clear();
        isPaidAmountAuto = true;

        if (pdp == null) {
            clearDetail();
            return;
        }

        try {
            lblBookingCode.setText(pdp.getMaDatPhong());
            lblGuestName.setText(pdp.getKhachHang() != null ? pdp.getKhachHang().getTenKhachHang() : "Khách lẻ");
            lblGuestPhone.setText(pdp.getKhachHang() != null ? pdp.getKhachHang().getSoDienThoai() : "--");

            allDetailsForBooking = chiTietPhieuDatDAO.getDSChiTietByMaPhieu(pdp.getMaDatPhong());
            normalizeDetailTimes(allDetailsForBooking, pdp);
            List<ChiTietPhieuDat> activeDetails = chiTietPhieuDatDAO.getCheckoutRoomsByBooking(pdp.getMaDatPhong());
            normalizeDetailTimes(activeDetails, pdp);

            currentHoaDon = hoaDonDAO.getHoaDonByMaPhieu(pdp.getMaDatPhong());
            renderRoomLines(activeDetails);
            preloadPersistedServicesForActiveRooms();

            lblRoomCount.setText(String.valueOf(roomStates.size()));
            refreshTotals(true);
        } catch (RuntimeException ex) {
            clearDetail();
            showAlert(Alert.AlertType.ERROR, "Lỗi tải chi tiết", ex.getMessage());
        }
    }

    private void renderRoomLines(List<ChiTietPhieuDat> details) {
        roomsContainer.getChildren().clear();
        roomStates.clear();

        if (details == null || details.isEmpty()) {
            roomsContainer.getChildren()
                    .add(createEmptyState("Chỉ các phòng đã nhận phòng mới hiển thị ở đây để thao tác trả phòng."));
            return;
        }

        for (ChiTietPhieuDat ct : details) {
            Phong room = phongDAO.getPhongByMa(ct.getPhong().getMaPhong());
            if (room == null) {
                room = ct.getPhong();
            }
            if (room == null || room.getMaPhong() == null) {
                continue;
            }
            ct.setPhong(room);
            if (!isCurrentlyOccupied(ct)) {
                continue;
            }

            RoomCheckoutState state = new RoomCheckoutState(ct);
            roomStates.put(state.roomId, state);
            roomsContainer.getChildren().add(buildRoomCard(state));
        }

        if (roomStates.isEmpty()) {
            roomsContainer.getChildren().add(createEmptyState("Không có phòng nào đang ở để trả trong phiếu này."));
        }
    }

    private Node buildRoomCard(RoomCheckoutState state) {
        Label lblTitle = new Label(state.roomId + " | " + safeRoomType(state.detail));
        lblTitle.getStyleClass().add("checkout-card-title");

        Label lblTime = new Label(buildRoomTimeLabel(state.detail));
        lblTime.getStyleClass().add("checkout-card-meta");
        lblTime.setWrapText(true);

        Label lblStatus = new Label("Trạng thái: " + resolveRoomStatusText(state.detail));
        lblStatus.getStyleClass().add("checkout-room-status");

        Label lblRoomCharge = new Label(formatMoney(state.calculateRoomCharge()));
        lblRoomCharge.getStyleClass().add("checkout-card-value");

        Button btnAddService = createButton("\u2615 Thêm dịch vụ", "btn-primary");
        btnAddService.getStyleClass().add("checkout-add-service");
        btnAddService.setOnAction(e -> openAddServiceDialog(state));

        state.checkbox = new CheckBox();
        state.checkbox.setSelected(true);
        state.checkbox.getStyleClass().add("checkout-room-checkbox");
        state.checkbox.selectedProperty().addListener((obs, oldValue, newValue) -> refreshTotals());

        VBox info = new VBox(4, lblTitle, lblTime, lblStatus);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox header = new HBox(12, state.checkbox, info, lblRoomCharge, btnAddService);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblServiceTitle = new Label("Dịch vụ");
        lblServiceTitle.getStyleClass().add("checkout-info-title");

        state.serviceSubtotalLabel = createValueLabel();
        state.roomSummaryLabel = createValueLabel();

        HBox footer = new HBox(
                18,
                footerRow("Tiền dịch vụ", state.serviceSubtotalLabel),
                footerRow("Tổng phòng này", state.roomSummaryLabel));
        footer.setAlignment(Pos.CENTER_LEFT);

        VBox wrapper = new VBox(10, header, lblServiceTitle, state.servicesBox, footer);
        wrapper.getStyleClass().add("checkout-room-card");
        wrapper.setPadding(new Insets(14));

        renderServiceSection(state);
        return wrapper;
    }

    private void preloadPersistedServicesForActiveRooms() {
        if (currentHoaDon == null) {
            return;
        }

        Set<String> activeRoomIds = roomStates.keySet();
        RoomCheckoutState firstActiveState = roomStates.values().stream().findFirst().orElse(null);
        for (ChiTietHoaDon chiTiet : chiTietHoaDonDAO.getByMaHoaDon(currentHoaDon.getMaHoaDon())) {
            String roomId = chiTiet.getPhong() != null ? chiTiet.getPhong().getMaPhong() : null;
            RoomCheckoutState state;
            if (roomId != null && activeRoomIds.contains(roomId)) {
                state = roomStates.get(roomId);
            } else {
                // Legacy detail rows without room binding are assigned to first active room for settlement.
                state = firstActiveState;
            }
            if (state == null) {
                continue;
            }
            state.services.add(CheckoutServiceItem.fromChiTietHoaDon(chiTiet));
            renderServiceSection(state);
        }
    }

    private void renderServiceSection(RoomCheckoutState state) {
        state.servicesBox.getChildren().clear();

        if (state.services.isEmpty()) {
            state.servicesBox.getChildren().add(createEmptyState("Chưa có dịch vụ"));
        } else {
            for (CheckoutServiceItem item : state.services) {
                state.servicesBox.getChildren().add(buildServiceRow(state, item));
            }
        }

        state.serviceSubtotalLabel.setText(formatMoney(state.calculateServiceTotal()));
        state.roomSummaryLabel.setText(formatMoney(state.calculateRoomCharge() + state.calculateServiceTotal()));
        refreshTotals();
    }

    private Node buildServiceRow(RoomCheckoutState state, CheckoutServiceItem item) {
        Label lblName = new Label(item.getDichVu().getTenDichVu());
        lblName.getStyleClass().add("checkout-card-title");

        Label lblMeta = new Label(item.getDichVu().getMaDichVu() + " | " + formatMoney(item.getDonGia()) + " / lượt");
        lblMeta.getStyleClass().add("checkout-card-meta");

        Label lblQty = new Label("Số lượng");
        lblQty.getStyleClass().add("checkout-card-meta");

        Spinner<Integer> spQty = new Spinner<>();
        spQty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, item.getSoLuong()));
        spQty.setEditable(true);
        spQty.setPrefWidth(88);
        spQty.getStyleClass().add("checkout-spinner");
        spQty.valueProperty().addListener((obs, oldValue, newValue) -> {
            item.setSoLuong(newValue);
            if (currentHoaDon != null) {
                try {
                    chiTietHoaDonDAO.updateServiceQuantity(
                            currentHoaDon.getMaHoaDon(),
                            state.roomId,
                            item.getDichVu().getMaDichVu(),
                            newValue);
                } catch (RuntimeException ignored) {
                    // best-effort; UI totals already updated
                }
            }
            renderServiceSection(state);
        });

        Label lblLineTotal = new Label(formatMoney(item.getThanhTien()));
        lblLineTotal.getStyleClass().add("checkout-card-value");

        Button btnRemove = createButton("Xóa", "btn-danger");
        btnRemove.setPrefHeight(34);
        btnRemove.setOnAction(e -> {
            if (currentHoaDon != null) {
                try {
                    chiTietHoaDonDAO.deleteService(
                            currentHoaDon.getMaHoaDon(),
                            state.roomId,
                            item.getDichVu().getMaDichVu());
                } catch (RuntimeException ex) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi xóa dịch vụ", ex.getMessage());
                    return;
                }
            }
            state.services.remove(item);
            renderServiceSection(state);
        });

        VBox info = new VBox(4, lblName, lblMeta);
        HBox.setHgrow(info, Priority.ALWAYS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(10, info, spacer, lblQty, spQty, lblLineTotal, btnRemove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("checkout-service-card");
        row.setPadding(new Insets(12));
        return row;
    }

    private void openAddServiceDialog(RoomCheckoutState state) {
        if (selectedPDP == null || detailPane.getScene() == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng chọn phiếu checkout trước khi thêm dịch vụ.");
            return;
        }

        ensureHoaDonExists();
        if (currentHoaDon == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tạo hóa đơn tạm. Vui lòng thử lại.");
            return;
        }

        CheckoutServiceDialog dialog = new CheckoutServiceDialog(detailPane.getScene().getWindow());
        dialog.show(state.roomId, item -> {
            try {
                chiTietHoaDonDAO.saveOrUpdateService(
                        currentHoaDon.getMaHoaDon(),
                        state.roomId,
                        item.getDichVu().getMaDichVu(),
                        item.getSoLuong(),
                        item.getDonGia());
            } catch (RuntimeException ex) {
                throw new RuntimeException("Lỗi lưu DB: " + ex.getMessage(), ex);
            }

            CheckoutServiceItem existing = state.services.stream()
                    .filter(s -> s.getDichVu().getMaDichVu().equals(item.getDichVu().getMaDichVu()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                existing.setSoLuong(existing.getSoLuong() + item.getSoLuong());
            } else {
                state.services.add(item);
            }
            renderServiceSection(state);
        });
    }

    private void ensureHoaDonExists() {
        if (currentHoaDon != null) {
            return;
        }
        try {
            currentHoaDon = hoaDonDAO.getHoaDonByMaPhieu(selectedPDP.getMaDatPhong());
            if (currentHoaDon == null) {
                HoaDon draft = new HoaDon(
                        hoaDonDAO.generateNextId(),
                        LocalDateTime.now(),
                        0.08, 0, 0,
                        selectedPDP,
                        loggedInNhanVien);
                if (hoaDonDAO.create(draft)) {
                    currentHoaDon = draft;
                }
            }
        } catch (RuntimeException ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi tạo hóa đơn", ex.getMessage());
        }
    }

    private void refreshTotals() {
        refreshTotals(false);
    }

    private void refreshTotals(boolean isInitialLoad) {
        List<RoomCheckoutState> selectedStates = roomStates.values().stream()
                .filter(RoomCheckoutState::isSelected)
                .collect(Collectors.toList());

        double roomTotal = selectedStates.stream()
                .mapToDouble(this::calculateRoomPrice)
                .sum();

        double serviceTotal = selectedStates.stream()
                .mapToDouble(this::calculateServicePrice)
                .sum();
        double depositCredit = calculateDepositCredit(selectedStates);
        double newTotal = DepositPolicy.roundMoney(Math.max(0, roomTotal + serviceTotal - depositCredit));

        lblRoomSubtotal.setText(formatMoney(roomTotal));
        lblServiceSubtotal.setText(formatMoney(serviceTotal));
        lblDepositApplied.setText(formatMoney(depositCredit));
        lblTotal.setText(formatMoney(newTotal));

        if (cmbPaymentMethod != null) {
            String method = cmbPaymentMethod.getValue();
            if (!"Tiền mặt".equals(method)) {
                isUpdatingPaidAmount = true;
                txtCashReceived.setText(formatMoney(newTotal));
                isUpdatingPaidAmount = false;
            } else if (isInitialLoad || isPaidAmountAuto) {
                isUpdatingPaidAmount = true;
                txtCashReceived.setText(formatMoney(newTotal));
                isUpdatingPaidAmount = false;
            }
        }
        refreshPaymentSummary();
    }

    private double calculateDepositCredit(List<RoomCheckoutState> selectedStates) {
        if (selectedPDP == null || selectedStates == null || selectedStates.isEmpty()) {
            return 0;
        }

        double totalDeposit = selectedPDP.getTienDatCoc();
        if (totalDeposit <= 0) {
            return 0;
        }

        double bookingRoomTotal = selectedPDP.getTongTienPhong();
        double selectedBookedTotal = selectedStates.stream()
                .mapToDouble(state -> pricingService.getStoredBookingPrice(state.detail))
                .sum();
        if (bookingRoomTotal > 0 && selectedBookedTotal > 0) {
            double ratio = Math.min(1, selectedBookedTotal / bookingRoomTotal);
            return DepositPolicy.roundMoney(Math.min(totalDeposit, totalDeposit * ratio));
        }

        double selectedCurrentRoomTotal = selectedStates.stream()
                .mapToDouble(this::calculateRoomPrice)
                .sum();
        double rate = selectedPDP.getTyLeDatCoc() > 0
                ? selectedPDP.getTyLeDatCoc()
                : DepositPolicy.DEPOSIT_RATE_PERCENT;
        return DepositPolicy.roundMoney(Math.min(totalDeposit, selectedCurrentRoomTotal * rate / 100.0));
    }

    private void refreshPaymentSummary() {
        if (lblCashStatusValue == null || lblCashStatusTitle == null || lblPaymentHint == null) {
            return;
        }

        double total = parseMoneyLabel(lblTotal != null ? lblTotal.getText() : "0");
        double cashReceived = parseCashReceived();
        double delta = cashReceived - total;

        if (delta >= 0) {
            lblCashStatusTitle.setText("Tiền thừa trả khách");
            lblCashStatusValue.setText(formatMoney(delta));
            lblCashStatusValue.getStyleClass().setAll("checkout-money-label", "checkout-money-positive");
            if (cashReceived == 0 && total == 0) {
                lblPaymentHint.setText("Nhập số tiền khách đưa để hệ thống tự tính tiền thừa hoặc còn thiếu.");
            } else if (delta == 0) {
                lblPaymentHint.setText("Khách đã thanh toán đủ.");
            } else {
                lblPaymentHint.setText("Khách đưa dư, cần trả lại đúng số tiền thừa.");
            }
        } else {
            lblCashStatusTitle.setText("Còn thiếu");
            lblCashStatusValue.setText(formatMoney(Math.abs(delta)));
            lblCashStatusValue.getStyleClass().setAll("checkout-money-label", "checkout-money-danger");
            lblPaymentHint.setText("Khách chưa thanh toán đủ. Không thể hoàn tất trả phòng.");
        }
    }

    private void handleCheckout() {
        List<RoomCheckoutState> selectedStates = roomStates.values().stream()
                .filter(RoomCheckoutState::isSelected)
                .collect(Collectors.toList());

        if (selectedPDP == null || selectedStates.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng chọn phiếu và ít nhất một phòng để trả.");
            return;
        }

        double currentRoomCharge = selectedStates.stream()
                .mapToDouble(this::calculateRoomPrice)
                .sum();
        double currentServiceCharge = selectedStates.stream()
                .mapToDouble(this::calculateServicePrice)
                .sum();
        double depositCredit = calculateDepositCredit(selectedStates);
        double grossTotal = DepositPolicy.roundMoney(currentRoomCharge + currentServiceCharge);
        double payableTotal = DepositPolicy.roundMoney(Math.max(0, grossTotal - depositCredit));
        double cashReceived = parseCashReceived();

        if (cashReceived < payableTotal) {
            showAlert(Alert.AlertType.WARNING, "Thanh toán chưa đủ",
                    "Tiền khách đưa chưa đủ để hoàn tất trả phòng.\nCòn thiếu: "
                            + formatMoney(payableTotal - cashReceived));
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        HoaDon hoaDon = currentHoaDon != null
                ? currentHoaDon
                : new HoaDon(hoaDonDAO.generateNextId(), now, 0.08, 0, 0, selectedPDP, loggedInNhanVien);

        double accumulatedRoom = currentHoaDon != null ? currentHoaDon.getTongTienPhong() + currentRoomCharge
                : currentRoomCharge;
        double accumulatedService = currentHoaDon != null ? currentHoaDon.getTongTienDichVu() + currentServiceCharge
                : currentServiceCharge;
        hoaDon.setNgayLap(now);
        hoaDon.setThue(0.08);
        hoaDon.setTongTienPhong(accumulatedRoom);
        hoaDon.setTongTienDichVu(accumulatedService);
        hoaDon.setPhieuDatPhong(selectedPDP);
        hoaDon.setNhanVien(loggedInNhanVien);

        List<ChiTietHoaDon> serviceDetails = selectedStates.stream()
                .flatMap(state -> state.services.stream().map(item -> item.toChiTietHoaDon(hoaDon, state.roomId)))
                .collect(Collectors.toList());

        List<String> selectedRoomIds = selectedStates.stream()
                .map(state -> state.roomId)
                .collect(Collectors.toList());
        selectedRoomIds.forEach(maPhong -> System.out.println("[CHECKOUT] maPhong = " + maPhong));

        Set<String> selectedRoomSet = Set.copyOf(selectedRoomIds);
        boolean hasRemaining = allDetailsForBooking.stream()
                .filter(ct -> ct.getPhong() != null && ct.getPhong().getMaPhong() != null)
                .anyMatch(ct -> !selectedRoomSet.contains(ct.getPhong().getMaPhong()) && !isAlreadyCheckedOut(ct));

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return hoaDonDAO.saveCheckout(hoaDon, serviceDetails, selectedRoomIds, !hasRemaining);
            }
        };

        task.setOnSucceeded(e -> {
            if (Boolean.TRUE.equals(task.getValue())) {
                clearDetail();
                loadData();
                if (onCheckoutSaved != null) {
                    onCheckoutSaved.run();
                }
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Trả phòng thành công cho " + selectedRoomIds.size() + " phòng.\nTổng phát sinh: "
                                + formatMoney(grossTotal)
                                + "\nĐã trừ tiền cọc: " + formatMoney(depositCredit)
                                + "\nCần thu đợt này: " + formatMoney(payableTotal)
                                + "\nTiền khách đưa: " + formatMoney(cashReceived));
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu dữ liệu checkout.");
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            logException("Checkout failed", ex);
            showAlert(Alert.AlertType.ERROR, "Lỗi checkout",
                    buildUserErrorMessage(ex, "Không thể thực hiện checkout."));
        });

        Thread thread = new Thread(task, "checkout-save");
        thread.setDaemon(true);
        thread.start();
    }

    private void clearDetail() {
        selectedPDP = null;
        currentHoaDon = null;
        allDetailsForBooking = new ArrayList<>();
        roomStates.clear();
        isPaidAmountAuto = true;

        if (lblBookingCode != null) {
            lblBookingCode.setText("--");
            lblGuestName.setText("Chưa chọn phiếu");
            lblGuestPhone.setText("--");
            lblRoomCount.setText("0");
            lblRoomSubtotal.setText("0 VNĐ");
            lblServiceSubtotal.setText("0 VNĐ");
            lblDepositApplied.setText("0 VNĐ");
            lblTotal.setText("0 VNĐ");
        }
        roomsContainer.getChildren().clear();
        roomsContainer.getChildren().add(createEmptyState("Chọn phiếu có phòng đang ở để thao tác trả phòng."));
        refreshPaymentSummary();
    }

    private Label createStaticValue(String text) {
        Label label = createValueLabel();
        label.setText(text);
        return label;
    }

    private Label createValueLabel() {
        Label label = new Label();
        label.getStyleClass().add("checkout-value-label");
        label.setWrapText(true);
        return label;
    }

    private ScrollPane createInnerScroll(VBox content, double prefHeight) {
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(prefHeight);
        scroll.setMinHeight(prefHeight);
        scroll.getStyleClass().add("checkout-scroll");
        return scroll;
    }

    private Label createMoneyLabel() {
        Label label = new Label("0 VNĐ");
        label.getStyleClass().add("checkout-money-label");
        return label;
    }

    private HBox footerRow(String title, Label value) {
        Label lbl = new Label(title);
        lbl.getStyleClass().add("checkout-info-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, lbl, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(row, Priority.ALWAYS);
        return row;
    }

    private HBox footerRowDynamicLabel(Label titleLabel, Label value) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, titleLabel, spacer, value);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(row, Priority.ALWAYS);
        return row;
    }

    private Label createLinkedLabel(Label source) {
        Label clone = new Label(source.getText());
        clone.textProperty().bind(source.textProperty());
        clone.getStyleClass().setAll(source.getStyleClass());
        return clone;
    }

    private Label createEmptyState(String text) {
        Label empty = new Label(text);
        empty.getStyleClass().add("checkout-empty-label");
        empty.setWrapText(true);
        return empty;
    }

    private Button createButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setPrefHeight(36);
        return button;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private String buildUserErrorMessage(Throwable ex, String fallback) {
        Throwable current = ex;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                return message;
            }
            current = current.getCause();
        }
        return fallback;
    }

    private void logException(String context, Throwable ex) {
        if (ex == null) {
            System.err.println(context);
            return;
        }
        System.err.println(context + ": " + buildUserErrorMessage(ex, "Unknown error"));
        ex.printStackTrace();
    }

    private String stylesheetUri(String fileName) {
        return Paths.get("src", "gui", fileName).toAbsolutePath().toUri().toString();
    }

    private boolean isCurrentlyOccupied(ChiTietPhieuDat detail) {
        if (detail == null || detail.getPhong() == null) {
            return false;
        }
        if (ChiTietPhieuDat.TRANG_THAI_DA_TRA.equalsIgnoreCase(detail.getTrangThai())) {
            return false;
        }
        return ChiTietPhieuDat.TRANG_THAI_DA_NHAN.equalsIgnoreCase(detail.getTrangThai());
    }

    private String resolveRoomStatusText(ChiTietPhieuDat detail) {
        if (detail == null) {
            return "";
        }
        if (ChiTietPhieuDat.TRANG_THAI_DA_NHAN.equalsIgnoreCase(detail.getTrangThai())) {
            return "Đang ở";
        }
        if (ChiTietPhieuDat.TRANG_THAI_DA_TRA.equalsIgnoreCase(detail.getTrangThai())) {
            return "Đã trả phòng";
        }
        return "Không hợp lệ";
    }

    private void normalizeDetailTimes(List<ChiTietPhieuDat> details, PhieuDatPhong pdp) {
        if (details == null || details.isEmpty()) {
            return;
        }
        for (ChiTietPhieuDat detail : details) {
            if (detail == null) {
                continue;
            }
            if (detail.getNgayNhan() == null && pdp != null && pdp.getNgayNhan() != null) {
                detail.setNgayNhan(pdp.getNgayNhan());
            }
            if (detail.getNgayTra() == null && pdp != null && pdp.getNgayTra() != null) {
                detail.setNgayTra(pdp.getNgayTra());
            }
        }
    }

    private double calculateRoomPrice(RoomCheckoutState state) {
        if (state == null || state.detail == null) {
            return 0;
        }
        ChiTietPhieuDat detail = state.detail;
        LocalDateTime checkIn = detail.getNgayNhan();
        if (checkIn == null) {
            return detail.getGiaThuePhong() > 0 ? detail.getGiaThuePhong() : 0;
        }

        LocalDateTime effectiveCheckout = LocalDateTime.now();
        if (detail.getNgayTra() != null && detail.getNgayTra().isAfter(checkIn)) {
            effectiveCheckout = detail.getNgayTra().isBefore(effectiveCheckout) ? detail.getNgayTra() : effectiveCheckout;
        }

        double calculated = pricingService.calculateRoomCharge(detail, effectiveCheckout);
        if (calculated <= 0 && detail.getGiaThuePhong() > 0) {
            return detail.getGiaThuePhong();
        }
        return calculated;
    }

    private double calculateServicePrice(RoomCheckoutState state) {
        if (state == null) {
            return 0;
        }
        return state.calculateServiceTotal();
    }

    private boolean isAlreadyCheckedOut(ChiTietPhieuDat detail) {
        return detail == null
                || ChiTietPhieuDat.TRANG_THAI_DA_TRA.equalsIgnoreCase(detail.getTrangThai());
    }

    private String safeRoomType(ChiTietPhieuDat detail) {
        return detail.getPhong() != null && detail.getPhong().getLoaiPhong() != null
                ? detail.getPhong().getLoaiPhong()
                : "Loại phòng";
    }

    private String buildRoomTimeLabel(ChiTietPhieuDat detail) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String checkIn = detail.getNgayNhan() != null ? detail.getNgayNhan().format(formatter) : "--";
        String expectedCheckout = detail.getNgayTra() != null ? detail.getNgayTra().format(formatter) : "--";
        return "Nhận: " + checkIn + " | Trả dự kiến: " + expectedCheckout;
    }

    private double parseCashReceived() {
        if (txtCashReceived == null || txtCashReceived.getText() == null || txtCashReceived.getText().trim().isEmpty()) {
            return 0;
        }
        String text = txtCashReceived.getText();
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private double parseMoneyLabel(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        String normalized = text.replaceAll("[^\\d]", "");
        if (normalized.isBlank()) {
            return 0;
        }
        return Double.parseDouble(normalized);
    }

    private String formatMoney(double value) {
        return String.format("%,.0f VNĐ", value);
    }

    private final class RoomCheckoutState {
        private final ChiTietPhieuDat detail;
        private final String roomId;
        private final VBox servicesBox = new VBox(8);
        private final List<CheckoutServiceItem> services = new ArrayList<>();
        private CheckBox checkbox;
        private Label serviceSubtotalLabel;
        private Label roomSummaryLabel;

        private RoomCheckoutState(ChiTietPhieuDat detail) {
            this.detail = detail;
            this.roomId = detail.getPhong().getMaPhong();
        }

        private boolean isSelected() {
            return checkbox != null && checkbox.isSelected();
        }

        private double calculateRoomCharge() {
            return calculateRoomPrice(this);
        }

        private double calculateServiceTotal() {
            return services.stream().mapToDouble(CheckoutServiceItem::getThanhTien).sum();
        }
    }

    private VBox createSection(String title) {
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("checkout-section-title");
        VBox box = new VBox(10, lblTitle);
        box.getStyleClass().add("checkout-section");
        box.setFillWidth(true);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPadding(new Insets(14));
        return box;
    }

    private VBox compactInfo(String labelText, Label valueLabel) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("checkout-info-title");
        VBox box = new VBox(4, lbl, valueLabel);
        box.getStyleClass().add("checkout-info-item");
        box.setMinHeight(64);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setFillWidth(true);
        valueLabel.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private VBox compactInput(String labelText, Control inputControl) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("checkout-input-title");
        VBox box = new VBox(4, lbl, inputControl);
        box.getStyleClass().add("checkout-input-item");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }
}
