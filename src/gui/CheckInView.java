package gui;



import dao.ChiTietPhieuDatDAO;

import entity.ChiTietPhieuDat;

import javafx.beans.property.SimpleStringProperty;

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

import javafx.scene.control.CheckBox;

import javafx.scene.control.ContentDisplay;

import javafx.scene.control.Dialog;

import javafx.scene.control.Label;

import javafx.scene.control.ScrollPane;

import javafx.scene.control.TableCell;

import javafx.scene.control.TableColumn;

import javafx.scene.control.TableRow;

import javafx.scene.control.TableView;

import javafx.scene.control.TextField;

import javafx.scene.layout.HBox;

import javafx.scene.layout.Priority;

import javafx.scene.layout.Region;

import javafx.scene.layout.StackPane;

import javafx.scene.layout.VBox;

import javafx.scene.shape.Circle;

import util.BookingStatus;

import util.RoomStatus;

import util.StatusUtils;



import java.nio.file.Paths;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;

import java.util.LinkedHashMap;

import java.util.LinkedHashSet;

import java.util.List;

import java.util.Locale;

import java.util.Map;

import java.util.Set;

import java.util.stream.Collectors;



public class CheckInView {



    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");



    private final ObservableList<CheckInBookingItem> dataList = FXCollections.observableArrayList();

    private final ChiTietPhieuDatDAO chiTietPhieuDatDAO = new ChiTietPhieuDatDAO();

    private final Set<String> selectedBookingKeys = new LinkedHashSet<>();



    private List<ChiTietPhieuDat> allRoomDetails = new ArrayList<>();

    private List<CheckInBookingItem> allData = new ArrayList<>();



    private TableView<CheckInBookingItem> tableView;

    private TextField txtSearch;

    private Label lblCount;

    private Button btnCheckInSelected;

    private Runnable onCheckInSaved;



    public void setOnCheckInSaved(Runnable onCheckInSaved) {

        this.onCheckInSaved = onCheckInSaved;

    }



    public Node createView() {

        VBox root = new VBox(18);

        root.setPadding(new Insets(24));

        root.getStyleClass().add("checkin-view");

        root.getStylesheets().addAll(stylesheetUri("global-theme.css"), stylesheetUri("checkin-view.css"));



        VBox header = buildHeader();

        HBox toolbar = buildToolbar();

        VBox content = buildContentCard();



        VBox.setVgrow(content, Priority.ALWAYS);

        root.getChildren().addAll(header, toolbar, content);

        loadData();

        return root;

    }



    private VBox buildHeader() {

        VBox wrapper = new VBox(14);

        HBox titleRow = new HBox(14);

        titleRow.setAlignment(Pos.CENTER_LEFT);



        StackPane iconWrap = new StackPane();

        iconWrap.getStyleClass().add("checkin-hero-icon");

        Label icon = new Label("\u2713");

        iconWrap.getChildren().add(icon);



        VBox textBox = new VBox(6);

        Label lblTitle = new Label("NHẬN PHÒNG KHÁCH HÀNG");

        lblTitle.getStyleClass().add("checkin-title");



        Label lblSub = new Label("Mỗi dòng đại diện cho một phiếu đặt. Double-click hoặc bấm nút nhận phòng để chọn các phòng cần check-in.");

        lblSub.getStyleClass().add("checkin-subtitle");     

        lblSub.setWrapText(true);

        textBox.getChildren().addAll(lblTitle, lblSub);



        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);



        VBox statCard = new VBox(4);

        statCard.getStyleClass().addAll("app-card", "checkin-stat-card");

        Label lblStatCaption = new Label("Phiếu chờ nhận phòng");

        lblStatCaption.getStyleClass().add("label-caption");

        lblCount = new Label("0 phiếu");

        lblCount.getStyleClass().add("checkin-stat-value");

        statCard.getChildren().addAll(lblStatCaption, lblCount);

        titleRow.getChildren().addAll(iconWrap, textBox, spacer, statCard);



        HBox guide = new HBox(10);

        guide.setAlignment(Pos.CENTER_LEFT);

        guide.getStyleClass().add("alert-info");

        Label guideIcon = new Label("\u25F7");

        guideIcon.getStyleClass().add("checkin-guide-icon");

        Label guideText = new Label("Bảng chính đã được gom theo mã phiếu. Khi thao tác, hệ thống mở popup để chọn đúng các phòng chưa nhận phòng trong phiếu.");

        guideText.setWrapText(true);

        guideText.getStyleClass().add("checkin-guide-text");

        guide.getChildren().addAll(guideIcon, guideText);

        wrapper.getChildren().addAll(titleRow, guide);

        return wrapper;

    }



    private HBox buildToolbar() {

        HBox toolbar = new HBox(12);

        toolbar.setAlignment(Pos.CENTER_LEFT);

        Label searchIcon = new Label("\u2315");

        searchIcon.getStyleClass().add("checkin-search-icon");



        txtSearch = new TextField();

        txtSearch.setPromptText("Tìm theo mã phiếu, khách hàng, danh sách phòng hoặc loại phòng...");

        txtSearch.getStyleClass().addAll("search-field", "checkin-search-field");

        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> handleSearch());



        HBox searchBox = new HBox(10, searchIcon, txtSearch);

        searchBox.setAlignment(Pos.CENTER_LEFT);

        searchBox.getStyleClass().add("checkin-search-box");

        txtSearch.focusedProperty().addListener((obs, oldValue, focused) -> {

            if (focused) {

                if (!searchBox.getStyleClass().contains("checkin-search-box-focused")) {

                    searchBox.getStyleClass().add("checkin-search-box-focused");

                }

            } else {

                searchBox.getStyleClass().remove("checkin-search-box-focused");

            }

        });

        HBox.setHgrow(searchBox, Priority.ALWAYS);

        HBox.setHgrow(txtSearch, Priority.ALWAYS);



        btnCheckInSelected = new Button("Nh\u1EADn ph\u00F2ng");

        btnCheckInSelected.getStyleClass().addAll("btn-primary", "checkin-confirm-button");

        btnCheckInSelected.setDisable(true);

        btnCheckInSelected.setOnAction(e -> confirmBatchCheckIn());



        Button btnRefresh = new Button("\u21BB Làm mới");

        btnRefresh.getStyleClass().addAll("btn-secondary", "checkin-refresh-button");

        btnRefresh.setOnAction(e -> handleRefresh());



        toolbar.getChildren().addAll(searchBox, btnCheckInSelected, btnRefresh);

        return toolbar;

    }



    private VBox buildContentCard() {

        VBox card = new VBox(14);

        card.setPadding(new Insets(18));

        card.getStyleClass().addAll("app-card", "checkin-card");



        HBox header = new HBox(10);

        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("\u2302");

        icon.getStyleClass().add("checkin-section-icon");

        Label title = new Label("Danh sách phòng chờ nhận phòng");

        title.getStyleClass().add("checkin-section-title");



        Region spacer = new Region();

        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label hint = new Label("Mỗi dòng là một phiếu. Double-click hoặc bấm nút để chọn phòng cần nhận trong popup");

        hint.getStyleClass().add("label-muted");

        header.getChildren().addAll(icon, title, spacer, hint);



        tableView = new TableView<>(dataList);

        tableView.getStyleClass().addAll("app-table", "checkin-table");

        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        tableView.setPlaceholder(createEmptyState());

        tableView.setPrefHeight(620);

        tableView.setRowFactory(tv -> buildRow());

        buildColumns();



        VBox.setVgrow(tableView, Priority.ALWAYS);

        card.getChildren().addAll(header, tableView);

        return card;

    }



    private TableRow<CheckInBookingItem> buildRow() {

        TableRow<CheckInBookingItem> row = new TableRow<>();

        row.setOnMouseClicked(event -> {

            if (event.getClickCount() == 2 && !row.isEmpty()) {

                openCheckInSelectionDialog(row.getItem());

            }

        });

        return row;

    }

    private void buildColumns() {

        tableView.getColumns().clear();



        TableColumn<CheckInBookingItem, Void> colSelect = new TableColumn<>("");

        colSelect.setSortable(false);

        colSelect.setMaxWidth(58);

        colSelect.setStyle("-fx-alignment: CENTER;");

        colSelect.setCellFactory(column -> new TableCell<>() {

            private final CheckBox checkBox = new CheckBox();



            {

                checkBox.setOnAction(e -> {

                    CheckInBookingItem item = getTableRow() != null ? getTableRow().getItem() : null;

                    if (item == null) {

                        return;

                    }

                    String key = bookingKey(item);

                    if (checkBox.isSelected()) {

                        selectedBookingKeys.add(key);

                    } else {

                        selectedBookingKeys.remove(key);

                    }

                    refreshBatchButtonState();

                });

            }



            @Override

            protected void updateItem(Void item, boolean empty) {

                super.updateItem(item, empty);

                if (empty || getTableRow() == null || getTableRow().getItem() == null) {

                    setGraphic(null);

                    return;

                }



                CheckInBookingItem bookingItem = getTableRow().getItem();

                boolean selectable = canCheckIn(bookingItem);

                checkBox.setDisable(!selectable);

                checkBox.setSelected(selectable && selectedBookingKeys.contains(bookingKey(bookingItem)));

                setGraphic(new StackPane(checkBox));

                setAlignment(Pos.CENTER);

            }

        });



        TableColumn<CheckInBookingItem, String> colMaPhieu = textColumn("Mã phiếu", CheckInBookingItem::getMaPhieu);

        colMaPhieu.setMaxWidth(140);

        TableColumn<CheckInBookingItem, String> colKhach = textColumn("Khách hàng", CheckInBookingItem::getTenKhachHang);

        TableColumn<CheckInBookingItem, String> colPhong = textColumn("Danh sách phòng", CheckInBookingItem::getJoinedRoomCodes);

        TableColumn<CheckInBookingItem, String> colLoai = textColumn("Loại phòng", CheckInBookingItem::getJoinedRoomTypes);

        TableColumn<CheckInBookingItem, String> colNgayNhan = centeredColumn("Ngày nhận", item -> item.getNgayNhan() != null ? item.getNgayNhan().format(DATE_TIME_FORMATTER) : "");

        TableColumn<CheckInBookingItem, String> colNgayTra = centeredColumn("Ngày trả", item -> item.getNgayTra() != null ? item.getNgayTra().format(DATE_TIME_FORMATTER) : "");



        TableColumn<CheckInBookingItem, String> colTrangThai = new TableColumn<>("Trạng thái");

        colTrangThai.setSortable(false);

        colTrangThai.setCellValueFactory(data -> new SimpleStringProperty(resolveStatusCode(data.getValue())));

        colTrangThai.setCellFactory(column -> new TableCell<>() {

            @Override

            protected void updateItem(String item, boolean empty) {

                super.updateItem(item, empty);

                setText(null);

                setGraphic(empty ? null : createStatusBadge(getTableRow() != null ? getTableRow().getItem() : null));

                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                setAlignment(Pos.CENTER);

            }

        });

        colTrangThai.setStyle("-fx-alignment: CENTER;");

        colTrangThai.setMaxWidth(180);



        TableColumn<CheckInBookingItem, Void> colAction = new TableColumn<>("Nhận phòng");

        colAction.setSortable(false);

        colAction.setStyle("-fx-alignment: CENTER;");

        colAction.setCellFactory(column -> new TableCell<>() {

            private final Button btnCheckIn = new Button("\u2713 nhận phòng");



            {

                btnCheckIn.getStyleClass().addAll("btn-primary", "checkin-confirm-button");

                btnCheckIn.setOnAction(e -> {

                    if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {

                        return;

                    }

                    openCheckInSelectionDialog(getTableView().getItems().get(getIndex()));

                });

            }



            @Override

            protected void updateItem(Void item, boolean empty) {

                super.updateItem(item, empty);

                if (empty || getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {

                    setGraphic(null);

                    return;

                }

                CheckInBookingItem bookingItem = getTableView().getItems().get(getIndex());

                btnCheckIn.setDisable(!canCheckIn(bookingItem));

                setGraphic(new StackPane(btnCheckIn));

                setAlignment(Pos.CENTER);

            }

        });

        colAction.setMaxWidth(180);

        tableView.getColumns().addAll(colSelect, colMaPhieu, colKhach, colPhong, colLoai, colNgayNhan, colNgayTra, colTrangThai, colAction);

    }



    public void loadData() {

        setTableLoading(true);

        Task<List<ChiTietPhieuDat>> task = new Task<>() {

            @Override

            protected List<ChiTietPhieuDat> call() {

                return chiTietPhieuDatDAO.getDanhSachChoCheckIn();

            }

        };



        task.setOnSucceeded(e -> {

            allRoomDetails = task.getValue() != null ? task.getValue() : new ArrayList<>();

            allData = groupBookings(allRoomDetails);

            pruneInvalidSelections();

            setTableLoading(false);

            applyFilter();

            updateCountLabel(dataList.size());

            refreshBatchButtonState();

        });



        task.setOnFailed(e -> {

            Throwable ex = task.getException();

            ex.printStackTrace();

            allRoomDetails = new ArrayList<>();

            allData = new ArrayList<>();

            dataList.clear();

            updateCountLabel(0);

            setTableLoading(false);

            refreshBatchButtonState();

            showAlert(Alert.AlertType.ERROR, "Lỗi tải dữ liệu", ex != null ? "Không thể tải danh sách nhận phòng.\n" + ex.getMessage() : "Không thể tải danh sách nhận phòng.");

        });



        Thread loader = new Thread(task, "checkin-load");

        loader.setDaemon(true);

        loader.start();

    }

    private void handleRefresh() {
        if (txtSearch != null) {
            txtSearch.clear();
        }
        selectedBookingKeys.clear();
        refreshBatchButtonState();
        loadData();
    }

    private void setTableLoading(boolean loading) {
        if (tableView != null) {
            tableView.setPlaceholder(loading ? new Label("\u0110ang t\u1ea3i d\u1eef li\u1ec7u...") : createEmptyState());
        }
        if (loading) {
            allRoomDetails = new ArrayList<>();
            allData = new ArrayList<>();
            dataList.clear();
            updateCountLabel(0);
        }
    }



    private void handleSearch() {

        applyFilter();

        updateCountLabel(dataList.size());

        refreshBatchButtonState();

    }



    private void applyFilter() {

        String keyword = normalizeKeyword(txtSearch != null ? txtSearch.getText() : "");

        if (keyword.isBlank()) {

            dataList.setAll(allData);

            tableView.refresh();

            return;

        }



        dataList.setAll(allData.stream().filter(item -> matches(item, keyword)).collect(Collectors.toList()));

        tableView.refresh();

    }



    private List<CheckInBookingItem> groupBookings(List<ChiTietPhieuDat> rawRows) {

        Map<String, CheckInBookingItem> grouped = new LinkedHashMap<>();

        for (ChiTietPhieuDat detail : rawRows) {

            if (detail == null || detail.getPhieuDatPhong() == null || detail.getPhong() == null) {

                continue;

            }

            String maPhieu = detail.getPhieuDatPhong().getMaDatPhong();

            if (maPhieu == null || maPhieu.isBlank()) {

                continue;

            }



            CheckInBookingItem item = grouped.get(maPhieu);

            if (item == null) {

                item = new CheckInBookingItem(maPhieu);

                item.setTenKhachHang(detail.getPhieuDatPhong().getKhachHang() != null ? nullSafe(detail.getPhieuDatPhong().getKhachHang().getTenKhachHang()) : "");
                item.setKhachHangDangHoatDong(detail.getPhieuDatPhong().getKhachHang() == null
                        || detail.getPhieuDatPhong().getKhachHang().isDangHoatDong());

                item.setNgayNhan(detail.getNgayNhan());

                item.setNgayTra(detail.getNgayTra());

                item.setBookingStatus(detail.getPhieuDatPhong().getTrangThai());

                grouped.put(maPhieu, item);

            }

            item.addDetail(detail);

        }

        return new ArrayList<>(grouped.values());

    }



    private boolean matches(CheckInBookingItem item, String keyword) {

        return contains(item.getMaPhieu(), keyword)

                || contains(item.getTenKhachHang(), keyword)

                || contains(item.getJoinedRoomCodes(), keyword)

                || contains(item.getJoinedRoomTypes(), keyword)

                || contains(item.getNgayNhan() != null ? item.getNgayNhan().format(DATE_TIME_FORMATTER) : null, keyword)

                || contains(item.getNgayTra() != null ? item.getNgayTra().format(DATE_TIME_FORMATTER) : null, keyword);

    }

    private void confirmBatchCheckIn() {

        List<CheckInBookingItem> selectedBookings = getSelectedBookings();



        if (selectedBookings.isEmpty()) {

            showAlert(Alert.AlertType.WARNING, "Chưa chọn phiếu", "Vui lòng chọn ít nhất 1 phiếu");

            return;

        }

        if (selectedBookings.size() == 1) {

            openCheckInSelectionDialog(selectedBookings.get(0));

            return;

        }



        List<BatchBookingSelection> batchSelections = buildBatchSelections(selectedBookings);

        if (batchSelections.isEmpty()) {

            showAlert(Alert.AlertType.INFORMATION, "Không còn phòng chờ nhận", "Các phiếu được chọn không còn phòng nào hợp lệ để nhận.");

            loadData();

            return;

        }



        if (showBatchCheckInDialog(batchSelections)) {

            executeBatchBookingCheckIn(batchSelections);

        }

    }



    private List<CheckInBookingItem> getSelectedBookings() {

        return allData.stream()

                .filter(this::canCheckIn)

                .filter(item -> selectedBookingKeys.contains(bookingKey(item)))

                .collect(Collectors.toList());

    }



    private List<BatchBookingSelection> buildBatchSelections(List<CheckInBookingItem> selectedBookings) {

        List<BatchBookingSelection> batchSelections = new ArrayList<>();

        for (CheckInBookingItem bookingItem : selectedBookings) {

            if (bookingItem == null || bookingItem.getMaPhieu() == null || bookingItem.getMaPhieu().isBlank()) {

                continue;

            }

            List<ChiTietPhieuDat> pendingRooms = loadPendingRoomsForBooking(bookingItem.getMaPhieu());

            if (pendingRooms.isEmpty()) {

                continue;

            }

            batchSelections.add(new BatchBookingSelection(bookingItem, pendingRooms));

        }

        return batchSelections;

    }



    private boolean showBatchCheckInDialog(List<BatchBookingSelection> batchSelections) {

        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("Xác nhận nhận phòng");

        dialog.setHeaderText("Nhận phòng nhiều phiếu");



        ButtonType confirmType = new ButtonType("Xác nhận nhận phòng", ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(stylesheetUri("global-theme.css"), stylesheetUri("checkin-view.css"));
        dialog.getDialogPane().getStyleClass().addAll("dialog-surface", "checkin-room-dialog");
        dialog.getDialogPane().setPrefWidth(680);



        VBox content = new VBox(12);

        content.getStyleClass().add("checkin-room-dialog-content");
        content.setPadding(new Insets(20, 20, 12, 20));



        String bookingIds = batchSelections.stream()

                .map(selection -> selection.bookingItem().getMaPhieu())

                .collect(Collectors.joining(", "));

        Label bookingLabel = new Label("Phiếu được chọn: " + bookingIds);

        bookingLabel.setWrapText(true);

        bookingLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #0f172a;");



        Label roomTitle = new Label("Danh sách phòng sẽ nhận");

        roomTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");



        VBox roomList = new VBox(10);

        Map<String, ChiTietPhieuDat> aggregatedRooms = aggregateRooms(batchSelections);



        Label summaryLabel = new Label("T\u1ED5ng phi\u1EBFu: " + batchSelections.size() + " | T\u1ED5ng ph\u00F2ng: " + aggregatedRooms.size());

        summaryLabel.setWrapText(true);

        summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");



        for (Map.Entry<String, ChiTietPhieuDat> entry : aggregatedRooms.entrySet()) {

            ChiTietPhieuDat detail = entry.getValue();

            Label roomCode = new Label(entry.getKey());

            roomCode.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

            Label roomType = new Label(detail.getPhong() != null ? nullSafe(detail.getPhong().getLoaiPhong()) : "");

            roomType.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            Label roomTime = new Label(formatDateRange(detail));

            roomTime.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");



            VBox infoBox = new VBox(4, roomCode, roomType, roomTime);

            infoBox.setPadding(new Insets(10, 12, 10, 12));

            infoBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12; -fx-border-color: #dbeafe; -fx-border-radius: 12;");

            roomList.getChildren().add(infoBox);

        }



        ScrollPane scrollPane = new ScrollPane(roomList);

        scrollPane.setFitToWidth(true);

        scrollPane.setPrefViewportHeight(Math.min(360, 120 + aggregatedRooms.size() * 70.0));

        scrollPane.getStyleClass().add("checkin-room-choice-scroll");



        content.getChildren().addAll(bookingLabel, summaryLabel, roomTitle, scrollPane);

        dialog.getDialogPane().setContent(content);



        return dialog.showAndWait().filter(result -> result == confirmType).isPresent();

    }



    private Map<String, ChiTietPhieuDat> aggregateRooms(List<BatchBookingSelection> batchSelections) {

        Map<String, ChiTietPhieuDat> aggregatedRooms = new LinkedHashMap<>();

        for (BatchBookingSelection selection : batchSelections) {

            if (selection == null || selection.pendingRooms() == null) {

                continue;

            }

            for (ChiTietPhieuDat detail : selection.pendingRooms()) {

                if (detail == null || detail.getPhong() == null || detail.getPhong().getMaPhong() == null || detail.getPhong().getMaPhong().isBlank()) {

                    continue;

                }

                aggregatedRooms.putIfAbsent(detail.getPhong().getMaPhong(), detail);

            }

        }

        return aggregatedRooms;

    }



    private void openCheckInSelectionDialog(CheckInBookingItem bookingItem) {
        if (bookingItem != null && !bookingItem.isKhachHangDangHoatDong()) {
            showAlert(Alert.AlertType.WARNING, "Khách hàng đã bị vô hiệu hóa",
                    "Khách hàng đã bị vô hiệu hóa. Không thể check-in cho phiếu này.");
            return;
        }

        if (!canCheckIn(bookingItem)) {

            showAlert(Alert.AlertType.WARNING, "Không hợp lệ", "Phiếu này không còn phòng nào ở trạng thái chờ nhận.");

            return;

        }



        List<ChiTietPhieuDat> pendingRooms = loadPendingRoomsForBooking(bookingItem.getMaPhieu());

        if (pendingRooms.isEmpty()) {

            showAlert(Alert.AlertType.INFORMATION, "Không còn phòng chờ nhận", "Phiếu này không còn phòng cần nhận.");

            loadData();

            return;

        }



        List<ChiTietPhieuDat> selectedRooms = showRoomSelectionDialog("Chọn phòng cần nhận", bookingItem, pendingRooms, "Xác nhận nhận phòng");

        if (selectedRooms == null || selectedRooms.isEmpty()) {

            return;

        }



        String roomLabels = selectedRooms.stream().map(detail -> detail.getPhong().getMaPhong()).collect(Collectors.joining(", "));

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,

                "Xác nhận nhận phòng " + selectedRooms.size() + " phòng của phiếu " + bookingItem.getMaPhieu() + "?\n" + roomLabels,

                ButtonType.YES, ButtonType.NO);

        confirm.setTitle("Xác nhận nhận phòng");

        confirm.setHeaderText(null);

        confirm.showAndWait().ifPresent(type -> {

            if (type == ButtonType.YES) {

                executeCheckIn(selectedRooms, "Đã nhận phòng thành công " + selectedRooms.size() + " phòng của phiếu " + bookingItem.getMaPhieu() + ".");

            }

        });

    }



    private List<ChiTietPhieuDat> loadPendingRoomsForBooking(String maPhieu) {

        return chiTietPhieuDatDAO.getPendingRoomsByBooking(maPhieu).stream().filter(this::canCheckIn).collect(Collectors.toList());

    }



    private List<ChiTietPhieuDat> showRoomSelectionDialog(String title, CheckInBookingItem bookingItem, List<ChiTietPhieuDat> pendingRooms, String confirmLabel) {

        Dialog<List<ChiTietPhieuDat>> dialog = new Dialog<>();

        dialog.setTitle(title);

        dialog.setHeaderText("Phiếu " + bookingItem.getMaPhieu() + " - " + bookingItem.getTenKhachHang());



        ButtonType confirmType = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);

        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().addAll(stylesheetUri("global-theme.css"), stylesheetUri("checkin-view.css"));
        dialog.getDialogPane().getStyleClass().addAll("dialog-surface", "checkin-room-dialog");
        dialog.getDialogPane().setPrefWidth(680);



        VBox content = new VBox(12);

        content.getStyleClass().add("checkin-room-dialog-content");
        content.setPadding(new Insets(20, 20, 12, 20));

        Label description = new Label("Chọn 1 hoặc nhiều phòng chưa nhận phòng trong phiếu này.");

        description.setWrapText(true);
        description.getStyleClass().add("checkin-room-dialog-description");



        VBox roomList = new VBox(12);

        Map<ChiTietPhieuDat, CheckBox> checkBoxes = new LinkedHashMap<>();

        for (ChiTietPhieuDat detail : pendingRooms) {

            CheckBox checkBox = new CheckBox();

            checkBox.setMinWidth(24);
            checkBox.getStyleClass().add("checkin-room-choice-checkbox");
            checkBox.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> event.consume());



            Label roomLabel = new Label(detail.getPhong() != null ? nullSafe(detail.getPhong().getMaPhong()) : "");

            roomLabel.getStyleClass().add("checkin-room-choice-code");
            roomLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

            Label typeLabel = new Label(detail.getPhong() != null ? nullSafe(detail.getPhong().getLoaiPhong()) : "");

            typeLabel.getStyleClass().add("checkin-room-choice-meta");
            typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            Label timeLabel = new Label(formatDateRange(detail));

            timeLabel.getStyleClass().add("checkin-room-choice-meta");
            timeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");

            Label statusLabel = new Label(resolveRoomStatusLabel(detail));

            statusLabel.getStyleClass().add("checkin-room-choice-status");
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #0f766e; -fx-font-weight: 700;");



            VBox infoBox = new VBox(4, roomLabel, typeLabel, timeLabel, statusLabel);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            HBox itemRow = new HBox(12, checkBox, infoBox);

            itemRow.setAlignment(Pos.CENTER_LEFT);

            itemRow.setPadding(new Insets(10, 12, 10, 12));

            itemRow.getStyleClass().add("checkin-room-choice-card");
            itemRow.setOnMouseClicked(event -> {
                if (event.getTarget() != checkBox) {
                    checkBox.setSelected(!checkBox.isSelected());
                }
            });
            checkBox.selectedProperty().addListener((obs, oldVal, selected) -> {
                if (selected) {
                    if (!itemRow.getStyleClass().contains("checkin-room-choice-card-selected")) {
                        itemRow.getStyleClass().add("checkin-room-choice-card-selected");
                    }
                } else {
                    itemRow.getStyleClass().remove("checkin-room-choice-card-selected");
                }
            });



            roomList.getChildren().add(itemRow);

            checkBoxes.put(detail, checkBox);

        }



        ScrollPane scrollPane = new ScrollPane(roomList);

        scrollPane.setFitToWidth(true);

        scrollPane.setPrefViewportHeight(Math.min(360, 110 + pendingRooms.size() * 74.0));

        scrollPane.getStyleClass().add("checkin-room-choice-scroll");



        content.getChildren().addAll(description, scrollPane);

        dialog.getDialogPane().setContent(content);



        Node confirmButton = dialog.getDialogPane().lookupButton(confirmType);

        confirmButton.setDisable(true);
        if (confirmButton instanceof Button button) {
            button.getStyleClass().addAll("btn-primary", "checkin-room-confirm-button");
        }
        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (cancelButton instanceof Button button) {
            button.getStyleClass().add("btn-secondary");
        }

        checkBoxes.values().forEach(checkBox -> checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> confirmButton.setDisable(checkBoxes.values().stream().noneMatch(CheckBox::isSelected))));



        dialog.setResultConverter(buttonType -> {

            if (buttonType != confirmType) {

                return null;

            }

            return checkBoxes.entrySet().stream().filter(entry -> entry.getValue().isSelected()).map(Map.Entry::getKey).collect(Collectors.toList());

        });

        return dialog.showAndWait().orElse(null);

    }

    private void executeCheckIn(List<ChiTietPhieuDat> details, String successMessage) {

        Task<Boolean> task = new Task<>() {

            @Override

            protected Boolean call() {

                return chiTietPhieuDatDAO.checkInNhieuPhong(details);

            }

        };



        task.setOnSucceeded(e -> {

            if (Boolean.TRUE.equals(task.getValue())) {

                details.stream()

                        .map(detail -> detail.getPhieuDatPhong() != null ? detail.getPhieuDatPhong().getMaDatPhong() : null)

                        .filter(maPhieu -> maPhieu != null && !maPhieu.isBlank())

                        .forEach(selectedBookingKeys::remove);

                refreshBatchButtonState();

                showAlert(Alert.AlertType.INFORMATION, "Nhận phòng thành công", successMessage);

                loadData();

                if (onCheckInSaved != null) {

                    onCheckInSaved.run();

                }

            } else {

                showAlert(Alert.AlertType.ERROR, "Nhận phòng thất bại", "Không cập nhật được trạng thái phòng hoặc phiếu đặt.");

            }

        });



        task.setOnFailed(e -> {

            Throwable ex = task.getException();

            ex.printStackTrace();

            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", ex != null ? "Không thể thực hiện nhận phòng.\n" + ex.getMessage() : "Không thể thực hiện nhận phòng.");

        });



        Thread worker = new Thread(task, "checkin-confirm");

        worker.setDaemon(true);

        worker.start();

    }



    private void executeBatchBookingCheckIn(List<BatchBookingSelection> batchSelections) {

        Task<Boolean> task = new Task<>() {

            @Override

            protected Boolean call() {

                List<ChiTietPhieuDat> selectedRooms = batchSelections.stream()

                        .filter(selection -> selection != null && selection.pendingRooms() != null)

                        .flatMap(selection -> selection.pendingRooms().stream())

                        .filter(CheckInView.this::canCheckIn)

                        .collect(Collectors.toList());

                return chiTietPhieuDatDAO.checkInNhieuPhong(selectedRooms);

            }

        };



        task.setOnSucceeded(e -> {

            if (Boolean.TRUE.equals(task.getValue())) {

                batchSelections.stream()

                        .map(selection -> selection.bookingItem() != null ? selection.bookingItem().getMaPhieu() : null)

                        .filter(maPhieu -> maPhieu != null && !maPhieu.isBlank())

                        .forEach(selectedBookingKeys::remove);

                refreshBatchButtonState();

                showAlert(Alert.AlertType.INFORMATION, "Nhận phòng thành công",

                        "Đã nhận phòng " + batchSelections.size() + " phiếu được chọn.");

                loadData();

                if (onCheckInSaved != null) {

                    onCheckInSaved.run();

                }

            } else {

                showAlert(Alert.AlertType.ERROR, "Nhận phòng thất bại", "Không cập nhật được tất cả phiếu được chọn.");

            }

        });



        task.setOnFailed(e -> {

            Throwable ex = task.getException();

            ex.printStackTrace();

            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống",

                    ex != null ? "Không thể thực hiện nhận phòng nhiều phiếu.\n" + ex.getMessage()

                            : "Không thể thực hiện nhận phòng nhiều phiếu.");

        });



        Thread worker = new Thread(task, "checkin-batch-confirm");

        worker.setDaemon(true);

        worker.start();

    }



    private boolean canCheckIn(CheckInBookingItem item) {

        return item != null && item.isKhachHangDangHoatDong() && !item.getPendingDetails().isEmpty();

    }



    private boolean canCheckIn(ChiTietPhieuDat detail) {

        if (detail == null || detail.getPhieuDatPhong() == null || detail.getPhong() == null) {

            return false;

        }
        if (detail.getPhieuDatPhong().getKhachHang() != null && !detail.getPhieuDatPhong().getKhachHang().isDangHoatDong()) {
            return false;
        }

        if (StatusUtils.isBookingStatus(detail.getPhieuDatPhong().getTrangThai(), BookingStatus.DA_HUY)

                || StatusUtils.isBookingStatus(detail.getPhieuDatPhong().getTrangThai(), BookingStatus.DA_THANH_TOAN)) {

            return false;

        }

        return isPendingCheckInStatus(detail.getTrangThai())
                && !StatusUtils.isRoomStatus(detail.getPhong().getTrangThai(), RoomStatus.DANG_O);

    }

    private boolean isPendingCheckInStatus(String status) {

        String compact = StatusUtils.normalize(status).replace("_", "");

        return compact.isBlank()

                || "CHONHAN".equals(compact)

                || "CHONHANPHONG".equals(compact)

                || "CHOCHECKIN".equals(compact);

    }



    private Node createStatusBadge(CheckInBookingItem item) {

        if (item == null) {

            return null;

        }



        String text;

        String styleClass;

        String iconText;

        if (item.getPendingDetails().isEmpty()) {

            text = "Đã nhận phòng";

            styleClass = "badge-checkin";

            iconText = "\u2713";

        } else if (StatusUtils.isBookingStatus(item.getBookingStatus(), BookingStatus.DA_HUY)) {

            text = "Đã hủy";

            styleClass = "badge-cancel";

            iconText = "!";

        } else if (StatusUtils.isBookingStatus(item.getBookingStatus(), BookingStatus.DA_NHAN_PHONG)) {

            text = "Đã nhận một phần";

            styleClass = "badge-checkin";

            iconText = "\u25F7";

        } else {

            text = "Chờ nhận phòng";

            styleClass = "badge-pending";

            iconText = "\u25F7";

        }



        Label icon = new Label(iconText);

        icon.getStyleClass().add("checkin-badge-icon");

        Label textLabel = new Label(text);

        HBox badge = new HBox(6, icon, textLabel);

        badge.setAlignment(Pos.CENTER);

        badge.getStyleClass().addAll("badge", styleClass, "checkin-status-badge");

        return badge;

    }



    private String resolveStatusCode(CheckInBookingItem item) {

        if (item == null) {

            return "";

        }

        if (item.getPendingDetails().isEmpty()) {

            return BookingStatus.DA_NHAN_PHONG.getCode();

        }

        if (StatusUtils.isBookingStatus(item.getBookingStatus(), BookingStatus.DA_HUY)) {

            return BookingStatus.DA_HUY.getCode();

        }

        if (StatusUtils.isBookingStatus(item.getBookingStatus(), BookingStatus.DA_NHAN_PHONG)) {

            return BookingStatus.DA_NHAN_PHONG.getCode();

        }

        return BookingStatus.CHO_XAC_NHAN.getCode();

    }



    private Node createEmptyState() {

        VBox empty = new VBox(12);

        empty.setAlignment(Pos.CENTER);

        empty.setPadding(new Insets(48, 24, 48, 24));



        Circle circle = new Circle(30);

        circle.getStyleClass().add("checkin-empty-circle");

        Label glyph = new Label("\u2302");

        glyph.getStyleClass().add("checkin-empty-icon");

        StackPane iconWrap = new StackPane(circle, glyph);



        Label title = new Label("Không có phiếu cần nhận phòng");

        title.getStyleClass().add("checkin-empty-title");

        Label subtitle = new Label("Chỉ hiển thị các phiếu còn phòng chưa nhận phòng. Thử làm mới nếu vừa thao tác nhận phòng.");

        subtitle.getStyleClass().add("checkin-empty-subtitle");

        subtitle.setWrapText(true);

        subtitle.setMaxWidth(320);



        empty.getChildren().addAll(iconWrap, title, subtitle);

        return empty;

    }



    private TableColumn<CheckInBookingItem, String> textColumn(String title, ValueProvider provider) {

        TableColumn<CheckInBookingItem, String> column = new TableColumn<>(title);

        column.setCellValueFactory(data -> new SimpleStringProperty(provider.get(data.getValue())));

        column.setCellFactory(col -> defaultCell(Pos.CENTER));

        column.setStyle("-fx-alignment: CENTER;");

        return column;

    }



    private TableColumn<CheckInBookingItem, String> centeredColumn(String title, ValueProvider provider) {

        TableColumn<CheckInBookingItem, String> column = new TableColumn<>(title);

        column.setCellValueFactory(data -> new SimpleStringProperty(provider.get(data.getValue())));

        column.setCellFactory(col -> defaultCell(Pos.CENTER));

        column.setStyle("-fx-alignment: CENTER;");

        return column;

    }



    private TableCell<CheckInBookingItem, String> defaultCell(Pos alignment) {

        return new TableCell<>() {

            @Override

            protected void updateItem(String item, boolean empty) {

                super.updateItem(item, empty);

                setText(empty ? null : item);

                setGraphic(null);

                setAlignment(alignment);

            }

        };

    }

    private void updateCountLabel(int size) {

        if (lblCount == null) {

            return;

        }

        lblCount.setText(size + " phiếu");

    }



    private void refreshBatchButtonState() {

        if (btnCheckInSelected == null) {

            return;

        }

        btnCheckInSelected.setDisable(getSelectedBookings().isEmpty());

    }



    private void pruneInvalidSelections() {

        Set<String> validKeys = allData.stream().map(this::bookingKey).collect(Collectors.toSet());

        selectedBookingKeys.removeIf(key -> !validKeys.contains(key));

    }



    private String bookingKey(CheckInBookingItem item) {

        return item != null ? nullSafe(item.getMaPhieu()) : "";

    }



    private String formatDateRange(ChiTietPhieuDat detail) {

        String ngayNhan = detail.getNgayNhan() != null ? detail.getNgayNhan().format(DATE_TIME_FORMATTER) : "--";

        String ngayTra = detail.getNgayTra() != null ? detail.getNgayTra().format(DATE_TIME_FORMATTER) : "--";

        return "Nhận: " + ngayNhan + " | Trả: " + ngayTra;

    }



    private String resolveRoomStatusLabel(ChiTietPhieuDat detail) {

        if (detail == null) {

            return "";

        }

        if (ChiTietPhieuDat.TRANG_THAI_DA_NHAN.equalsIgnoreCase(detail.getTrangThai())
                || StatusUtils.isRoomStatus(detail.getPhong() != null ? detail.getPhong().getTrangThai() : null, RoomStatus.DANG_O)) {

            return "Đã nhận phòng";

        }

        if (ChiTietPhieuDat.TRANG_THAI_DA_TRA.equalsIgnoreCase(detail.getTrangThai())) {
            return "Đã trả phòng";
        }

        return "Chờ nhận phòng";

    }



    private String nullSafe(String value) {

        return value == null ? "" : value;

    }



    private String stylesheetUri(String fileName) {

        return Paths.get("src", "gui", fileName).toAbsolutePath().toUri().toString();

    }



    private boolean contains(String value, String keyword) {

        return normalizeKeyword(value).contains(keyword);

    }



    private String normalizeKeyword(String value) {

        return value == null ? "" : StatusUtils.normalize(value).toLowerCase(Locale.ROOT);

    }



    private void showAlert(Alert.AlertType type, String title, String message) {

        Alert alert = new Alert(type);

        alert.setTitle(title);

        alert.setHeaderText(null);

        alert.setContentText(message);

        alert.showAndWait();

    }



    @FunctionalInterface

    private interface ValueProvider {

        String get(CheckInBookingItem item);

    }



    private static final class CheckInBookingItem {

        private final String maPhieu;

        private final List<ChiTietPhieuDat> roomDetails = new ArrayList<>();

        private final Set<String> roomCodes = new LinkedHashSet<>();

        private final Set<String> roomTypes = new LinkedHashSet<>();

        private String tenKhachHang;

        private String bookingStatus;

        private boolean khachHangDangHoatDong = true;

        private LocalDateTime ngayNhan;

        private LocalDateTime ngayTra;



        private CheckInBookingItem(String maPhieu) {

            this.maPhieu = maPhieu;

        }



        public String getMaPhieu() {

            return maPhieu;

        }



        public String getTenKhachHang() {

            return tenKhachHang;

        }



        public void setTenKhachHang(String tenKhachHang) {

            this.tenKhachHang = tenKhachHang;

        }



        public String getBookingStatus() {

            return bookingStatus;

        }



        public void setBookingStatus(String bookingStatus) {

            this.bookingStatus = bookingStatus;

        }

        public boolean isKhachHangDangHoatDong() {

            return khachHangDangHoatDong;

        }

        public void setKhachHangDangHoatDong(boolean khachHangDangHoatDong) {

            this.khachHangDangHoatDong = khachHangDangHoatDong;

        }



        public LocalDateTime getNgayNhan() {

            return ngayNhan;

        }



        public void setNgayNhan(LocalDateTime ngayNhan) {

            this.ngayNhan = ngayNhan;

        }



        public LocalDateTime getNgayTra() {

            return ngayTra;

        }



        public void setNgayTra(LocalDateTime ngayTra) {

            this.ngayTra = ngayTra;

        }



        public void addDetail(ChiTietPhieuDat detail) {

            roomDetails.add(detail);

            if (detail.getNgayNhan() != null && (ngayNhan == null || detail.getNgayNhan().isBefore(ngayNhan))) {

                ngayNhan = detail.getNgayNhan();

            }

            if (detail.getNgayTra() != null && (ngayTra == null || detail.getNgayTra().isAfter(ngayTra))) {

                ngayTra = detail.getNgayTra();

            }

            if (detail.getPhong() != null) {

                if (detail.getPhong().getMaPhong() != null && !detail.getPhong().getMaPhong().isBlank()) {

                    roomCodes.add(detail.getPhong().getMaPhong());

                }

                if (detail.getPhong().getLoaiPhong() != null && !detail.getPhong().getLoaiPhong().isBlank()) {

                    roomTypes.add(detail.getPhong().getLoaiPhong());

                }

            }

        }



        public List<ChiTietPhieuDat> getPendingDetails() {

            return roomDetails.stream().filter(detail -> detail != null && detail.getPhong() != null).collect(Collectors.toList());

        }



        public String getJoinedRoomCodes() {

            return roomCodes.isEmpty() ? "" : String.join(", ", roomCodes);

        }



        public String getJoinedRoomTypes() {

            if (roomTypes.isEmpty()) {

                return "";

            }

            return roomTypes.size() == 1 ? roomTypes.iterator().next() : String.join(", ", roomTypes);

        }

    }



    private record BatchBookingSelection(CheckInBookingItem bookingItem, List<ChiTietPhieuDat> pendingRooms) {

    }

}

