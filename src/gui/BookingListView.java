package gui;

import dao.ChiTietPhieuDatDAO;
import dao.PhieuDatPhongDAO;
import entity.ChiTietPhieuDat;
import entity.HuyDatPhong;
import entity.KhachHang;
import entity.NhanVien;
import entity.PhieuDatPhong;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import util.BookingStatus;
import util.DepositReceiptPdfExporter;
import util.StatusBadge;
import util.StatusUtils;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BookingListView {
    private static final String ALL_FILTER = "T\u1ea5t c\u1ea3";

    private final TableView<PhieuDatPhong> tableView = new TableView<>();
    private final ObservableList<PhieuDatPhong> dataList = FXCollections.observableArrayList();
    private final PhieuDatPhongDAO phieuDatPhongDAO = new PhieuDatPhongDAO();
    private final ChiTietPhieuDatDAO chiTietPhieuDatDAO = new ChiTietPhieuDatDAO();
    private final NhanVien loggedInNhanVien;

    private TextField txtSearch;
    private ComboBox<String> cboFilter;
    private Label lblCount;
    private Button btnExportDeposit;
    private List<PhieuDatPhong> allBookings = new ArrayList<>();
    private Runnable onBookingCancelled;

    public BookingListView() {
        this(null);
    }

    public BookingListView(NhanVien loggedInNhanVien) {
        this.loggedInNhanVien = loggedInNhanVien;
    }

    public void setOnBookingCancelled(Runnable onBookingCancelled) {
        this.onBookingCancelled = onBookingCancelled;
    }

    public Node createView() {
        VBox root = new VBox(18);
        root.getStyleClass().addAll("booking-list-view", "page-shell");
        root.getStylesheets().addAll(
                stylesheetUri("global-theme.css"),
                stylesheetUri("booking-list-view.css"));
        root.setPadding(new Insets(24));

        root.getChildren().add(buildHeader());
        root.getChildren().add(buildToolbar());

        VBox tableCard = new VBox(16);
        tableCard.getStyleClass().add("panel-card");
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        configureTable();
        tableView.setItems(dataList);
        VBox.setVgrow(tableView, Priority.ALWAYS);

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setMaxWidth(Double.MAX_VALUE);
        btnExportDeposit = createActionButton("Xuất phiếu đặt cọc", "btn-secondary");
        btnExportDeposit.setOnAction(e -> handlePrimaryExportAction());
        Button btnCancel = createActionButton("🗑  Hủy phiếu", "btn-danger");
        btnCancel.setOnAction(e -> handleCancel());
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> updateExportDepositButtonState(newV));
        updateExportDepositButtonState(null);
        actions.getChildren().addAll(btnExportDeposit, btnCancel);

        tableCard.getChildren().addAll(tableView, actions);
        root.getChildren().add(tableCard);

        loadData();
        return root;
    }

    public void loadData() {
        setTableLoading(true);
        Task<List<PhieuDatPhong>> task = new Task<>() {
            @Override
            protected List<PhieuDatPhong> call() {
                return phieuDatPhongDAO.getAllPhieuDatPhong();
            }
        };

        task.setOnSucceeded(e -> {
            allBookings = groupBookings(task.getValue() != null ? task.getValue() : new ArrayList<>());
            setTableLoading(false);
            applyFilters();
        });
        task.setOnFailed(e -> {
            allBookings = new ArrayList<>();
            dataList.clear();
            if (lblCount != null) {
                lblCount.setText("0 phi\u1ebfu");
            }
            setTableLoading(false);
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải danh sách phiếu đặt phòng.");
        });
        Thread loader = new Thread(task, "booking-list-load");
        loader.setDaemon(true);
        loader.start();
    }

    private void handleRefresh() {
        if (txtSearch != null) {
            txtSearch.clear();
        }
        if (cboFilter != null) {
            cboFilter.setValue(ALL_FILTER);
        }
        loadData();
    }

    private void setTableLoading(boolean loading) {
        tableView.setPlaceholder(loading
                ? new Label("\u0110ang t\u1ea3i d\u1eef li\u1ec7u...")
                : createEmptyState("Kh\u00f4ng c\u00f3 d\u1eef li\u1ec7u \u0111\u1eb7t ph\u00f2ng",
                        "Danh s\u00e1ch s\u1ebd hi\u1ec3n th\u1ecb ngay khi c\u00f3 phi\u1ebfu \u0111\u1eb7t m\u1edbi."));
        if (loading) {
            dataList.clear();
            if (lblCount != null) {
                lblCount.setText("\u0110ang t\u1ea3i...");
            }
        }
    }

    private Node buildHeader() {
        HBox header = new HBox(14);
        header.getStyleClass().add("page-header");

        StackPane icon = new StackPane(new Label("\uD83D\uDECF"));
        icon.getStyleClass().add("page-icon-badge");

        VBox textBox = new VBox(4);
        textBox.getStyleClass().add("page-header-text");
        Label title = new Label("Danh sách đặt phòng");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label(
                "Theo dõi toàn bộ phiếu đặt, xem chi tiết từng phòng và thao tác hủy phiếu ngay trên bảng.");
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);
        textBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblCount = new Label("0 phiếu");
        lblCount.getStyleClass().add("booking-list-count-chip");

        header.getChildren().addAll(icon, textBox, spacer, lblCount);
        return header;
    }

    private Node buildToolbar() {
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("toolbar-card");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        HBox searchBox = new HBox(10);
        searchBox.getStyleClass().add("search-box");
        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.getStyleClass().add("search-box-icon");
        txtSearch = new TextField();
        txtSearch.getStyleClass().add("search-field");
        txtSearch.setPromptText("Tìm theo mã phiếu, khách hàng hoặc số điện thoại...");
        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, txtSearch);
        HBox.setHgrow(searchBox, Priority.ALWAYS);

        cboFilter = new ComboBox<>(FXCollections.observableArrayList(
                "Tất cả",
                BookingStatus.CHO_COC.getDisplayName(),
                BookingStatus.DA_DAT_COC.getDisplayName(),
                BookingStatus.CHO_XAC_NHAN.getDisplayName(),
                BookingStatus.DA_NHAN_PHONG.getDisplayName(),
                BookingStatus.DA_THANH_TOAN.getDisplayName(),
                BookingStatus.DA_HUY.getDisplayName()));
        cboFilter.getStyleClass().add("combo-field");
        cboFilter.setValue("Tất cả");
        cboFilter.setPrefHeight(40);
        cboFilter.setOnAction(e -> applyFilters());

        Button btnRefresh = createActionButton("Làm mới", "btn-secondary");
        btnRefresh.setOnAction(e -> handleRefresh());

        toolbar.getChildren().addAll(searchBox, cboFilter, btnRefresh);
        return toolbar;
    }

    private void configureTable() {
        tableView.getStyleClass().add("app-table");
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setPlaceholder(
                createEmptyState("Không có dữ liệu đặt phòng", "Danh sách sẽ hiển thị ngay khi có phiếu đặt mới."));

        TableColumn<PhieuDatPhong, String> colMa = new TableColumn<>("Mã phiếu");
        colMa.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getMaDatPhong()));

        TableColumn<PhieuDatPhong, String> colKhach = new TableColumn<>("Khách hàng");
        colKhach.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getKhachHang() != null ? nullSafe(data.getValue().getKhachHang().getTenKhachHang())
                        : "Khách lẻ"));

        TableColumn<PhieuDatPhong, String> colSdt = new TableColumn<>("Điện thoại");
        colSdt.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getKhachHang() != null ? nullSafe(data.getValue().getKhachHang().getSoDienThoai())
                        : ""));

        TableColumn<PhieuDatPhong, String> colNgayDat = new TableColumn<>("Ngày đặt");
        colNgayDat.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getNgayDat() != null
                        ? data.getValue().getNgayDat().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                        : ""));
        colNgayDat.setStyle("-fx-alignment: CENTER;");

        TableColumn<PhieuDatPhong, String> colPhong = new TableColumn<>("Danh sách phòng");
        colPhong.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(nullSafe(data.getValue().getDsMaPhong())));

        TableColumn<PhieuDatPhong, String> colSoLuong = new TableColumn<>("Số lượng phòng");
        colSoLuong.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getSoLuongPhong())));
        colSoLuong.setStyle("-fx-alignment: CENTER;");

        TableColumn<PhieuDatPhong, String> colTienCoc = new TableColumn<>("Tiền cọc");
        colTienCoc.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                formatMoney(data.getValue().getTienDatCoc())));
        colTienCoc.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<PhieuDatPhong, String> colConLai = new TableColumn<>("Còn lại");
        colConLai.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                formatMoney(data.getValue().getTienConLai())));
        colConLai.setStyle("-fx-alignment: CENTER-RIGHT;");

        TableColumn<PhieuDatPhong, String> colTrangThai = new TableColumn<>("Trạng thái");
        colTrangThai.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getTrangThai()));
        colTrangThai.setCellFactory(StatusBadge.bookingCellFactory());
        colTrangThai.setStyle("-fx-alignment: CENTER;");

        tableView.getColumns().setAll(colMa, colKhach, colSdt, colNgayDat, colPhong, colSoLuong, colTienCoc, colConLai,
                colTrangThai);
        tableView.setRowFactory(tv -> {
            TableRow<PhieuDatPhong> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    tableView.getSelectionModel().select(row.getItem());
                    handleViewDetail();
                }
            });
            return row;
        });
    }

    private void applyFilters() {
        String keyword = nullSafe(txtSearch != null ? txtSearch.getText() : "").trim().toLowerCase(Locale.ROOT);
        String selectedLabel = cboFilter != null ? cboFilter.getValue() : ALL_FILTER;

        List<PhieuDatPhong> filtered = allBookings.stream()
                .filter(p -> {
                    String ma = nullSafe(p.getMaDatPhong()).toLowerCase(Locale.ROOT);
                    String ten = p.getKhachHang() != null
                            ? nullSafe(p.getKhachHang().getTenKhachHang()).toLowerCase(Locale.ROOT)
                            : "";
                    String sdt = p.getKhachHang() != null
                            ? nullSafe(p.getKhachHang().getSoDienThoai()).toLowerCase(Locale.ROOT)
                            : "";
                    boolean matchKeyword = keyword.isBlank() || ma.contains(keyword) || ten.contains(keyword)
                            || sdt.contains(keyword);

                    boolean matchStatus = ALL_FILTER.equals(selectedLabel)
                            || StatusUtils.bookingLabel(p.getTrangThai()).equalsIgnoreCase(selectedLabel);
                    return matchKeyword && matchStatus;
                })
                .collect(Collectors.toList());

        dataList.setAll(filtered);
        if (lblCount != null) {
            lblCount.setText(filtered.size() + " phiếu");
        }
    }

    private List<PhieuDatPhong> groupBookings(List<PhieuDatPhong> rawBookings) {
        Map<String, PhieuDatPhong> grouped = new LinkedHashMap<>();

        for (PhieuDatPhong raw : rawBookings) {
            if (raw == null || raw.getMaDatPhong() == null || raw.getMaDatPhong().isBlank()) {
                continue;
            }

            PhieuDatPhong existing = grouped.get(raw.getMaDatPhong());
            if (existing == null) {
                PhieuDatPhong booking = new PhieuDatPhong(
                        raw.getMaDatPhong(),
                        raw.getNgayDat(),
                        raw.getTrangThai(),
                        raw.getNhanVien(),
                        raw.getKhachHang());
                booking.setNgayNhan(raw.getNgayNhan());
                booking.setNgayTra(raw.getNgayTra());
                booking.setDsMaPhong(joinRoomCodes(raw.getDsMaPhong()));
                booking.setSoLuongPhong(countRoomCodes(raw.getDsMaPhong(), raw.getSoLuongPhong()));
                copyDepositFields(raw, booking);
                grouped.put(raw.getMaDatPhong(), booking);
                continue;
            }

            if (existing.getNgayDat() == null && raw.getNgayDat() != null) {
                existing.setNgayDat(raw.getNgayDat());
            }
            if (existing.getKhachHang() == null && raw.getKhachHang() != null) {
                existing.setKhachHang(raw.getKhachHang());
            }
            if ((existing.getTrangThai() == null || existing.getTrangThai().isBlank())
                    && raw.getTrangThai() != null && !raw.getTrangThai().isBlank()) {
                // Keep the first booking-level status by default; only backfill if the first
                // row was missing.
                existing.setTrangThai(raw.getTrangThai());
            }

            Set<String> roomCodes = new LinkedHashSet<>(splitRoomCodes(existing.getDsMaPhong()));
            roomCodes.addAll(splitRoomCodes(raw.getDsMaPhong()));
            existing.setDsMaPhong(String.join(", ", roomCodes));
            existing.setSoLuongPhong(roomCodes.size());
        }

        return new ArrayList<>(grouped.values());
    }

    private void copyDepositFields(PhieuDatPhong source, PhieuDatPhong target) {
        target.setSoDemLuuTru(source.getSoDemLuuTru());
        target.setTongTienPhong(source.getTongTienPhong());
        target.setTienDatCoc(source.getTienDatCoc());
        target.setTienConLai(source.getTienConLai());
        target.setTyLeDatCoc(source.getTyLeDatCoc());
        target.setHinhThucDatCoc(source.getHinhThucDatCoc());
        target.setNgayDatCoc(source.getNgayDatCoc());
        target.setNhanVienNhanCoc(source.getNhanVienNhanCoc());
        target.setGhiChuDatCoc(source.getGhiChuDatCoc());
    }

    private List<String> splitRoomCodes(String roomCodes) {
        List<String> result = new ArrayList<>();
        String normalized = nullSafe(roomCodes).trim();
        if (normalized.isEmpty()) {
            return result;
        }

        for (String roomCode : normalized.split(",")) {
            String value = roomCode == null ? "" : roomCode.trim();
            if (!value.isEmpty() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private String joinRoomCodes(String roomCodes) {
        return String.join(", ", splitRoomCodes(roomCodes));
    }

    private int countRoomCodes(String roomCodes, int fallbackCount) {
        List<String> roomList = splitRoomCodes(roomCodes);
        return roomList.isEmpty() ? Math.max(fallbackCount, 0) : roomList.size();
    }

    private void handleViewDetail() {
        PhieuDatPhong selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một phiếu đặt phòng.");
            return;
        }

        Task<List<ChiTietPhieuDat>> task = new Task<>() {
            @Override
            protected List<ChiTietPhieuDat> call() {
                return chiTietPhieuDatDAO.getDSChiTietByMaPhieu(selected.getMaDatPhong());
            }
        };

        task.setOnSucceeded(e -> showDetailDialog(selected, task.getValue() != null ? task.getValue() : List.of()));
        task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể tải chi tiết phiếu đặt."));
        new Thread(task).start();
    }

    private void showDetailDialog(PhieuDatPhong booking, List<ChiTietPhieuDat> details) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Chi tiết phiếu đặt");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getStylesheets().addAll(
                stylesheetUri("global-theme.css"),
                stylesheetUri("booking-list-view.css"));
        dialog.getDialogPane().getStyleClass().addAll("dialog-surface", "booking-dialog");
        dialog.getDialogPane().setPrefWidth(760);

        VBox container = new VBox(18);
        container.getStyleClass().add("booking-detail-dialog-content");
        container.setPadding(new Insets(24, 24, 18, 24));

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        StackPane icon = new StackPane(new Label("\uD83D\uDCC4"));
        icon.getStyleClass().add("page-icon-badge");

        VBox titleBox = new VBox(4);
        Label title = new Label("Chi tiết phiếu đặt " + booking.getMaDatPhong());
        title.getStyleClass().add("panel-title");
        Label subtitle = new Label("Thông tin khách hàng, trạng thái và danh sách phòng đã đặt.");
        subtitle.getStyleClass().add("panel-subtitle");
        subtitle.setWrapText(true);
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(icon, titleBox);

        javafx.scene.layout.GridPane summaryGrid = new javafx.scene.layout.GridPane();
        summaryGrid.getStyleClass().add("detail-grid");
        summaryGrid.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.ColumnConstraints keyColumn = new javafx.scene.layout.ColumnConstraints();
        keyColumn.setPercentWidth(34);
        javafx.scene.layout.ColumnConstraints valueColumn = new javafx.scene.layout.ColumnConstraints();
        valueColumn.setPercentWidth(66);
        summaryGrid.getColumnConstraints().setAll(keyColumn, valueColumn);

        KhachHang kh = booking.getKhachHang();
        String giayToLabel = (kh != null && kh.getLoaiGiayTo() != null && !kh.getLoaiGiayTo().isBlank())
                ? kh.getLoaiGiayTo()
                : "CCCD / Hộ chiếu";
        String soGiayTo = (kh != null && kh.getSoGiayTo() != null && !kh.getSoGiayTo().isBlank())
                ? kh.getSoGiayTo()
                : "Chưa cập nhật";

        addDetailRow(summaryGrid, 0, "Mã phiếu", booking.getMaDatPhong());
        addDetailRow(summaryGrid, 1, "Khách hàng", kh != null ? nullSafe(kh.getTenKhachHang()) : "Khách lẻ");
        addDetailRow(summaryGrid, 2, "Điện thoại", kh != null ? nullSafe(kh.getSoDienThoai()) : "--");
        addDetailRow(summaryGrid, 3, giayToLabel, soGiayTo);
        addDetailRow(summaryGrid, 4, "Trạng thái", StatusUtils.bookingLabel(booking.getTrangThai()));
        addDetailRow(summaryGrid, 5, "Số phòng", String.valueOf(booking.getSoLuongPhong()));
        addDetailRow(summaryGrid, 6, "Danh sách phòng", nullSafe(booking.getDsMaPhong()));
        addDetailRow(summaryGrid, 7, "Tổng tiền phòng", formatMoney(booking.getTongTienPhong()));
        addDetailRow(summaryGrid, 8, "Tiền đặt cọc", formatMoney(booking.getTienDatCoc()));
        addDetailRow(summaryGrid, 9, "Còn lại", formatMoney(booking.getTienConLai()));
        addDetailRow(summaryGrid, 10, "Hình thức cọc", nullSafe(booking.getHinhThucDatCoc()));

        Label roomSectionTitle = new Label("Danh sách phòng chi tiết");
        VBox summaryCard = new VBox(12, summaryGrid);
        summaryCard.getStyleClass().add("booking-detail-summary-card");

        roomSectionTitle.getStyleClass().add("panel-title");

        VBox roomList = new VBox(10);
        roomList.getStyleClass().add("booking-detail-room-list");
        if (details.isEmpty()) {
            roomList.getChildren().add(
                    createEmptyState("Chưa có chi tiết phòng", "Không tìm thấy dòng chi tiết thuộc phiếu đặt này."));
        } else {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (ChiTietPhieuDat detail : details) {
                VBox chip = new VBox(4);
                chip.getStyleClass().add("detail-room-chip");
                Label roomCode = new Label((detail.getPhong() != null ? detail.getPhong().getMaPhong() : "--")
                        + "  |  " + (detail.getPhong() != null ? detail.getPhong().getLoaiPhong() : "Phòng"));
                roomCode.getStyleClass().add("detail-room-title");
                Label roomTime = new Label(
                        (detail.getNgayNhan() != null ? detail.getNgayNhan().format(dtf) : "--")
                                + "  ->  "
                                + (detail.getNgayTra() != null ? detail.getNgayTra().format(dtf) : "--"));
                roomTime.getStyleClass().add("detail-room-caption");
                Label roomPrice = new Label(formatMoney(detail.getGiaThuePhong()));
                roomPrice.getStyleClass().add("detail-room-caption");
                Label roomStatus = new Label(detailRoomStatusText(detail));
                roomStatus.getStyleClass().addAll("detail-room-status", detailRoomStatusClass(detail));
                chip.getChildren().addAll(roomCode, roomTime, roomPrice, roomStatus);
                roomList.getChildren().add(chip);
            }
        }

        ScrollPane scrollPane = new ScrollPane(roomList);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("clean-scroll");
        scrollPane.setPrefViewportHeight(260);

        container.getChildren().addAll(header, summaryCard, roomSectionTitle, scrollPane);
        dialog.getDialogPane().setContent(container);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.getStyleClass().add("btn-primary");
        okButton.setText("Đóng");

        util.ModalUtils.setupDialogBehavior(dialog, ownerWindow(), ButtonType.OK, true, false, null);

        dialog.showAndWait();
    }

    private void addDetailRow(javafx.scene.layout.GridPane grid, int rowIndex, String key, String value) {
        Label lblKey = new Label(key.toUpperCase(Locale.ROOT));
        lblKey.getStyleClass().add("detail-key");
        Label lblValue = new Label(value);
        lblValue.getStyleClass().add("detail-value");
        lblValue.setWrapText(true);
        grid.add(lblKey, 0, rowIndex);
        grid.add(lblValue, 1, rowIndex);
    }

    private String detailRoomStatusText(ChiTietPhieuDat detail) {
        if (detail == null) {
            return "";
        }
        String status = StatusUtils.normalize(detail.getTrangThai());
        if (ChiTietPhieuDat.TRANG_THAI_DA_NHAN.equalsIgnoreCase(status)) {
            return "Đang ở";
        }
        if (ChiTietPhieuDat.TRANG_THAI_DA_TRA.equalsIgnoreCase(status)) {
            return "Đã trả";
        }
        return "Chờ nhận phòng";
    }

    private String detailRoomStatusClass(ChiTietPhieuDat detail) {
        String status = detail != null ? StatusUtils.normalize(detail.getTrangThai()) : "";
        if (ChiTietPhieuDat.TRANG_THAI_DA_NHAN.equalsIgnoreCase(status)) {
            return "detail-room-status-occupied";
        }
        if (ChiTietPhieuDat.TRANG_THAI_DA_TRA.equalsIgnoreCase(status)) {
            return "detail-room-status-paid";
        }
        return "detail-room-status-pending";
    }

    private void handlePrimaryExportAction() {
        PhieuDatPhong selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn một phiếu đặt phòng.");
            return;
        }
        if (isBookingFullyPaid(selected)) {
            new InvoiceView().exportInvoiceForBooking(selected.getMaDatPhong());
            return;
        }
        handleExportDepositReceipt(selected);
    }

    private void handleExportDepositReceipt(PhieuDatPhong selected) {
        if (selected.getTienDatCoc() <= 0) {
            showAlert(Alert.AlertType.WARNING, "Chưa có tiền cọc", "Phiếu chưa có tiền đặt cọc nên không thể xuất phiếu đặt cọc.");
            return;
        }
        if (isBookingFullyPaid(selected)) {
            showAlert(Alert.AlertType.INFORMATION, "Đã thanh toán",
                    "Phiếu này đã thanh toán hoàn tất. Hãy dùng chức năng xuất hóa đơn hoặc in phiếu thanh toán.");
            return;
        }

        String bookingId = selected.getMaDatPhong();
        Task<DepositReceiptSource> task = new Task<>() {
            @Override
            protected DepositReceiptSource call() {
                PhieuDatPhong booking = phieuDatPhongDAO.getPhieuDatPhongByMa(bookingId);
                if (booking == null) {
                    throw new IllegalStateException("Không tìm thấy phiếu đặt phòng " + bookingId + ".");
                }
                List<ChiTietPhieuDat> details = chiTietPhieuDatDAO.getDSChiTietByMaPhieu(bookingId);
                return new DepositReceiptSource(booking, details != null ? details : List.of());
            }
        };

        task.setOnSucceeded(e -> exportLoadedDepositReceipt(task.getValue()));
        task.setOnFailed(e -> showAlert(Alert.AlertType.ERROR, "Lỗi xuất phiếu",
                task.getException() != null ? task.getException().getMessage() : "Không thể tải dữ liệu phiếu đặt cọc."));
        Thread thread = new Thread(task, "booking-list-deposit-receipt-export");
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
            showAlert(Alert.AlertType.WARNING, "Không thể xuất phiếu", ex.getMessage());
        } catch (Exception ex) {
            showAlert(Alert.AlertType.ERROR, "Lỗi xuất phiếu", "Không thể xuất phiếu đặt cọc: " + ex.getMessage());
        }
    }

    private record DepositReceiptSource(PhieuDatPhong booking, List<ChiTietPhieuDat> details) {
    }

    private void updateExportDepositButtonState(PhieuDatPhong booking) {
        if (btnExportDeposit == null) {
            return;
        }
        boolean fullyPaid = isBookingFullyPaid(booking);
        boolean canExportDeposit = booking != null && booking.getTienDatCoc() > 0 && !fullyPaid;
        btnExportDeposit.setText(fullyPaid ? "Xuất hóa đơn" : "Xuất phiếu đặt cọc");
        btnExportDeposit.setDisable(booking == null || (!canExportDeposit && booking.getTienDatCoc() <= 0));
    }

    private boolean isBookingFullyPaid(PhieuDatPhong booking) {
        return booking != null
                && (StatusUtils.isBookingStatus(booking.getTrangThai(), BookingStatus.DA_THANH_TOAN)
                || booking.getTienConLai() <= 0.0001);
    }

    private void handleCancel() {
        PhieuDatPhong selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Chưa chọn", "Vui lòng chọn phiếu cần hủy.");
            return;
        }
        if (StatusUtils.isBookingStatus(selected.getTrangThai(), BookingStatus.DA_HUY)
                || StatusUtils.isBookingStatus(selected.getTrangThai(), BookingStatus.DA_THANH_TOAN)
                || StatusUtils.isBookingStatus(selected.getTrangThai(), BookingStatus.DA_NHAN_PHONG)) {
            showAlert(Alert.AlertType.WARNING, "Không thể hủy", "Phiếu đã hủy, đã nhận phòng hoặc đã trả phòng.");
            return;
        }

        showCancelDialog(selected);
    }

    private void showCancelDialog(PhieuDatPhong booking) {
        HuyDatPhong initialPreview = phieuDatPhongDAO.tinhHoanCoc(booking.getMaDatPhong(), LocalDateTime.now());
        if (initialPreview == null) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không tìm thấy phiếu đặt phòng.");
            return;
        }

        Dialog<HuyDatPhong> dialog = new Dialog<>();
        dialog.setTitle("Hủy phòng");
        ButtonType confirmType = new ButtonType("Xác nhận hủy phòng", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().setAll(confirmType, ButtonType.CANCEL);
        styleDialog(dialog.getDialogPane());

        TextArea txtLyDo = new TextArea();
        txtLyDo.setPromptText("Nhập lý do hủy phòng...");
        txtLyDo.setPrefRowCount(3);
        txtLyDo.setWrapText(true);

        util.ModalUtils.setupDialogBehavior(dialog, ownerWindow(), ButtonType.CANCEL, true, true, () -> {
            return txtLyDo.getText() != null && !txtLyDo.getText().trim().isEmpty();
        });

        DatePicker dpNgayHuy = new DatePicker(LocalDate.now());
        dpNgayHuy.getStyleClass().add("combo-field");
        ComboBox<String> cboHinhThucHoan = new ComboBox<>(FXCollections.observableArrayList(
                "Tiền mặt", "Chuyển khoản", "Thẻ", "Ví điện tử"));
        cboHinhThucHoan.setValue("Tiền mặt");
        cboHinhThucHoan.getStyleClass().add("combo-field");

        Label lblKhach = new Label(customerName(booking));
        Label lblMaPhieu = new Label(booking.getMaDatPhong());
        Label lblNgayDatCoc = new Label(formatDateTime(initialPreview.getNgayDatCoc()));
        Label lblTienCoc = new Label(formatMoney(initialPreview.getTienCocBanDau()));
        Label lblSoNgay = new Label(String.valueOf(initialPreview.getSoNgaySauDatCoc()));
        Label lblTyLe = new Label(formatPercent(initialPreview.getTyLeHoanCoc()));
        Label lblTienHoan = new Label(formatMoney(initialPreview.getTienHoanLai()));
        Label lblTienGiu = new Label(formatMoney(initialPreview.getTienKhachSanGiu()));
        HuyDatPhong[] previewRef = { initialPreview };

        Runnable refreshPreview = () -> {
            HuyDatPhong preview = phieuDatPhongDAO.tinhHoanCoc(booking.getMaDatPhong(), cancelDateTime(dpNgayHuy));
            if (preview == null) {
                return;
            }
            previewRef[0] = preview;
            lblNgayDatCoc.setText(formatDateTime(preview.getNgayDatCoc()));
            lblTienCoc.setText(formatMoney(preview.getTienCocBanDau()));
            lblSoNgay.setText(String.valueOf(preview.getSoNgaySauDatCoc()));
            lblTyLe.setText(formatPercent(preview.getTyLeHoanCoc()));
            lblTienHoan.setText(formatMoney(preview.getTienHoanLai()));
            lblTienGiu.setText(formatMoney(preview.getTienKhachSanGiu()));
        };
        dpNgayHuy.setOnAction(e -> refreshPreview.run());

        GridPane grid = new GridPane();
        grid.getStyleClass().add("detail-grid");
        grid.setHgap(12);
        grid.setVgap(10);
        addDialogRow(grid, 0, "Tên khách hàng", lblKhach);
        addDialogRow(grid, 1, "Mã phiếu", lblMaPhieu);
        addDialogRow(grid, 2, "Ngày đặt cọc", lblNgayDatCoc);
        addDialogRow(grid, 3, "Ngày hủy", dpNgayHuy);
        addDialogRow(grid, 4, "Tiền cọc đã nhận", lblTienCoc);
        addDialogRow(grid, 5, "Số ngày sau đặt cọc", lblSoNgay);
        addDialogRow(grid, 6, "Tỷ lệ hoàn cọc", lblTyLe);
        addDialogRow(grid, 7, "Số tiền hoàn lại", lblTienHoan);
        addDialogRow(grid, 8, "Khách sạn giữ lại", lblTienGiu);
        addDialogRow(grid, 9, "Hình thức hoàn tiền", cboHinhThucHoan);
        addDialogRow(grid, 10, "Lý do hủy", txtLyDo);

        VBox content = new VBox(14, grid);
        content.setPadding(new Insets(8));
        dialog.getDialogPane().setContent(content);

        Button confirmButton = (Button) dialog.getDialogPane().lookupButton(confirmType);
        confirmButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (txtLyDo.getText() == null || txtLyDo.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập lý do hủy phòng.");
                event.consume();
            }
        });

        dialog.setResultConverter(button -> {
            if (button != confirmType) {
                return null;
            }
            HuyDatPhong request = previewRef[0];
            request.setNgayHuy(cancelDateTime(dpNgayHuy));
            request.setLyDoHuy(txtLyDo.getText().trim());
            request.setHinhThucHoanTien(cboHinhThucHoan.getValue());
            request.setNhanVienXuLy(loggedInNhanVien != null ? loggedInNhanVien : booking.getNhanVien());
            return request;
        });

        dialog.showAndWait().ifPresent(this::executeCancelBooking);
    }

    private void executeCancelBooking(HuyDatPhong request) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return phieuDatPhongDAO.huyPhieu(request);
            }
        };
        task.setOnSucceeded(e -> {
            if (Boolean.TRUE.equals(task.getValue())) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công",
                        "Hủy phòng thành công. Số tiền hoàn lại cho khách là "
                                + formatMoney(request.getTienHoanLai())
                                + ". Số tiền khách sạn giữ lại là "
                                + formatMoney(request.getTienKhachSanGiu()) + ".");
                loadData();
                if (onBookingCancelled != null) {
                    onBookingCancelled.run();
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể hủy phiếu.");
            }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex != null)
                ex.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi hủy phiếu",
                    ex != null ? ex.getMessage() : "Không thể hủy phiếu.");
        });
        Thread worker = new Thread(task, "booking-cancel");
        worker.setDaemon(true);
        worker.start();
    }

    private void addDialogRow(GridPane grid, int rowIndex, String key, Node value) {
        Label lblKey = new Label(key.toUpperCase(Locale.ROOT));
        lblKey.getStyleClass().add("detail-key");
        grid.add(lblKey, 0, rowIndex);
        grid.add(value, 1, rowIndex);
        GridPane.setHgrow(value, Priority.ALWAYS);
    }

    private LocalDateTime cancelDateTime(DatePicker datePicker) {
        LocalDate date = datePicker.getValue() != null ? datePicker.getValue() : LocalDate.now();
        return LocalDateTime.of(date, LocalTime.now());
    }

    private Button createActionButton(String text, String styleClass) {
        Button btn = new Button(text);
        btn.getStyleClass().add(styleClass);
        btn.setPrefHeight(38);
        return btn;
    }

    private VBox createEmptyState(String title, String subtitle) {
        VBox empty = new VBox(8);
        empty.getStyleClass().add("empty-state");
        empty.setAlignment(Pos.CENTER);
        Label icon = new Label("\u2630");
        icon.getStyleClass().add("empty-state-icon");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("empty-state-title");
        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.getStyleClass().add("empty-state-subtitle");
        lblSubtitle.setWrapText(true);
        lblSubtitle.setMaxWidth(260);
        lblSubtitle.setAlignment(Pos.CENTER);
        empty.getChildren().addAll(icon, lblTitle, lblSubtitle);
        return empty;
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        styleDialog(alert.getDialogPane());
        alert.showAndWait();
    }

    private void styleDialog(DialogPane pane) {
        pane.getStylesheets().addAll(
                stylesheetUri("global-theme.css"),
                stylesheetUri("booking-list-view.css"));
        pane.getStyleClass().add("dialog-surface");
        if (pane.lookupButton(ButtonType.OK) instanceof Button button) {
            button.getStyleClass().add("btn-primary");
        }
        if (pane.lookupButton(ButtonType.YES) instanceof Button yesButton) {
            yesButton.getStyleClass().add("btn-primary");
        }
        if (pane.lookupButton(ButtonType.NO) instanceof Button noButton) {
            noButton.getStyleClass().add("btn-secondary");
        }
    }

    private String stylesheetUri(String fileName) {
        return Paths.get("src", "gui", fileName).toAbsolutePath().toUri().toString();
    }

    private Window ownerWindow() {
        return tableView.getScene() != null ? tableView.getScene().getWindow() : null;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String customerName(PhieuDatPhong booking) {
        KhachHang kh = booking != null ? booking.getKhachHang() : null;
        return kh != null ? nullSafe(kh.getTenKhachHang()) : "";
    }

    private String formatDateTime(LocalDateTime value) {
        return value != null ? value.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }

    private String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.0f%%", value);
    }

    private String formatMoney(double value) {
        return String.format("%,.0f VND", value);
    }
}
