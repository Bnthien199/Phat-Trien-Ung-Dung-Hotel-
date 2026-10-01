package gui;

import dao.BangGiaHeaderDAO;
import dao.ChiTietBangGiaDAO;
import dao.LoaiNgayDAO;
import dao.PhongDAO;
import entity.BangGiaHeader;
import entity.ChiTietBangGia;
import entity.LoaiNgay;
import entity.Phong;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;
import util.AppTheme;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class PricingView {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DETAIL_INVALID_MESSAGE = "Thông tin chi tiết bảng giá không hợp lệ. Vui lòng kiểm tra lại.";

    private final ObservableList<BangGiaHeader> headerList = FXCollections.observableArrayList();
    private final ObservableList<ChiTietBangGia> detailList = FXCollections.observableArrayList();
    private final BangGiaHeaderDAO bangGiaHeaderDAO = new BangGiaHeaderDAO();
    private final ChiTietBangGiaDAO chiTietBangGiaDAO = new ChiTietBangGiaDAO();
    private final LoaiNgayDAO loaiNgayDAO = new LoaiNgayDAO();
    private final PhongDAO phongDAO = new PhongDAO();

    private final Map<String, String> roomTypeNames = new HashMap<>();
    private final Map<String, String> basePriceLabels = new HashMap<>();
    private final Map<String, String> customDayTypeLabels = new LinkedHashMap<>();

    private List<BangGiaHeader> allHeaders = new ArrayList<>();

    private TableView<BangGiaHeader> tableHeader;
    private TableView<ChiTietBangGia> tableDetail;
    private Label lblDetailTitle;
    private Label lblDetailHint;
    private Label lblHeaderCount;
    private Label lblDetailCount;
    private TextField txtSearch;
    private boolean loadingHeaders;
    private String pendingSelectedMaBangGia;
    private Node pendingFocusTarget;
    private boolean suppressComboSelectionEvents;

    public Node createView() {
        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("pricing-view");
        root.getStylesheets().add(getStylesheetUri());

        Node content = buildContent();
        root.getChildren().addAll(buildHeader(), buildToolbar(), content);
        VBox.setVgrow(content, Priority.ALWAYS);

        loadData();
        return root;
    }

    private VBox buildHeader() {
        HBox titleRow = new HBox(14);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        StackPane marker = new StackPane(new Label("₫"));
        marker.getStyleClass().add("pricing-hero-icon");

        VBox textBox = new VBox(4);
        Label lblTitle = new Label("QUẢN LÝ BẢNG GIÁ");
        lblTitle.getStyleClass().add("pricing-page-title");

        Label lblSub = new Label("Thiết lập bảng giá theo loại ngày, thời gian hiệu lực và giá áp dụng cho từng loại phòng.");
        lblSub.getStyleClass().add("pricing-page-subtitle");
        lblSub.setWrapText(true);
        textBox.getChildren().addAll(lblTitle, lblSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox statHeader = createStatCard("Bảng giá hiệu lực", "0 bảng");
        VBox statDetail = createStatCard("Chi tiết đang chọn", "0 dòng");
        lblHeaderCount = (Label) statHeader.getChildren().get(1);
        lblDetailCount = (Label) statDetail.getChildren().get(1);

        HBox stats = new HBox(10, statHeader, statDetail);
        stats.setAlignment(Pos.CENTER_RIGHT);

        titleRow.getChildren().addAll(marker, textBox, spacer, stats);
        return new VBox(titleRow);
    }

    private VBox createStatCard(String title, String value) {
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("pricing-stat-title");

        Label lblValue = new Label(value);
        lblValue.getStyleClass().add("pricing-stat-value");

        VBox card = new VBox(4, lblTitle, lblValue);
        card.getStyleClass().add("pricing-stat-card");
        card.setMinWidth(164);
        card.setAlignment(Pos.CENTER_LEFT);
        return card;
    }

    private HBox buildToolbar() {
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("pricing-toolbar");

        Label searchIcon = new Label("⌕");
        searchIcon.getStyleClass().add("pricing-search-icon");

        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm mã, tên bảng giá hoặc loại ngày...");
        txtSearch.setPrefHeight(40);
        txtSearch.setPrefWidth(390);
        txtSearch.getStyleClass().add("pricing-search");
        txtSearch.textProperty().addListener((obs, oldVal, newVal) -> handleSearch());

        HBox searchBox = new HBox(8, searchIcon, txtSearch);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.getStyleClass().add("pricing-search-shell");

        Button btnAdd = createButton("+ Thêm bảng giá", AppTheme.TEAL, 154);
        Button btnRefresh = createGhostButton("Làm mới", 116);
        btnAdd.setOnAction(e -> showHeaderDialog(null));
        btnRefresh.setOnAction(e -> refreshPriceList());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().addAll(searchBox, spacer, btnAdd, btnRefresh);
        return toolbar;
    }

    private Node buildContent() {
        VBox leftCard = buildHeaderCard();
        VBox rightCard = buildDetailCard();

        SplitPane splitPane = new SplitPane(leftCard, rightCard);
        splitPane.getStyleClass().add("pricing-split");
        splitPane.setDividerPositions(0.42);
        VBox.setVgrow(splitPane, Priority.ALWAYS);
        return splitPane;
    }

    private VBox buildHeaderCard() {
        Label title = new Label("Danh sách bảng giá");
        title.getStyleClass().add("pricing-section-title");

        Label sub = new Label("Chọn một bảng giá để xem hoặc thêm chi tiết theo loại phòng.");
        sub.getStyleClass().add("pricing-section-subtitle");
        sub.setWrapText(true);

        tableHeader = createHeaderTable();
        VBox.setVgrow(tableHeader, Priority.ALWAYS);

        Label plusIcon = new Label("+");
        plusIcon.getStyleClass().add("pricing-add-type-icon");

        Button btnAddType = new Button("Thêm loại bảng giá", plusIcon);
        btnAddType.getStyleClass().add("pricing-add-type-button");
        btnAddType.setMaxWidth(Double.MAX_VALUE);
        btnAddType.setOnAction(e -> showPriceTypeDialog());

        HBox bottomAction = new HBox(btnAddType);
        bottomAction.setAlignment(Pos.CENTER_LEFT);
        bottomAction.getStyleClass().add("pricing-bottom-action");
        HBox.setHgrow(btnAddType, Priority.ALWAYS);

        VBox card = new VBox(10, title, sub, tableHeader, bottomAction);
        card.getStyleClass().add("pricing-card");
        card.setPadding(new Insets(16));
        card.setMinWidth(480);
        return card;
    }

    private VBox buildDetailCard() {
        lblDetailTitle = new Label("Chi tiết bảng giá");
        lblDetailTitle.getStyleClass().add("pricing-section-title");

        lblDetailHint = new Label("Giá áp dụng được tính theo loại ngày của bảng giá đang chọn.");
        lblDetailHint.getStyleClass().add("pricing-detail-hint");
        lblDetailHint.setWrapText(true);

        Label lblFallbackHint = new Label("Nếu chưa có chi tiết, hệ thống dùng giá niêm yết của phòng làm dữ liệu tham chiếu.");
        lblFallbackHint.getStyleClass().add("pricing-fallback-note");
        lblFallbackHint.setWrapText(true);
        HBox.setHgrow(lblFallbackHint, Priority.ALWAYS);

        Button btnAddDetail = createButton("+ Thêm chi tiết", AppTheme.TEAL, 146);
        btnAddDetail.setOnAction(e -> handleAddDetail());

        HBox toolbar = new HBox(10, lblFallbackHint, btnAddDetail);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        tableDetail = createDetailTable();
        VBox.setVgrow(tableDetail, Priority.ALWAYS);

        VBox card = new VBox(10, lblDetailTitle, lblDetailHint, toolbar, tableDetail);
        card.getStyleClass().add("pricing-card");
        card.setPadding(new Insets(16));
        card.setMinWidth(660);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private TableView<BangGiaHeader> createHeaderTable() {
        TableView<BangGiaHeader> table = new TableView<>(headerList);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("pricing-table");
        table.setFixedCellSize(48);
        table.setMinHeight(300);
        table.setPrefHeight(460);
        table.setPlaceholder(createEmptyState("Chưa có bảng giá", "Bấm Thêm bảng giá để tạo dữ liệu áp dụng cho khách sạn."));

        table.setRowFactory(tv -> {
            TableRow<BangGiaHeader> row = new TableRow<>();
            row.getStyleClass().add("pricing-row");
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showHeaderDialog(row.getItem());
                }
            });
            return row;
        });

        TableColumn<BangGiaHeader, String> colMa = headerColumn("Mã", 84, BangGiaHeader::getMaBangGia);
        TableColumn<BangGiaHeader, String> colTen = headerColumn("Tên bảng giá", 190, BangGiaHeader::getTenBangGia);
        TableColumn<BangGiaHeader, String> colLoaiNgay = headerColumn("Loại ngày", 118, bg -> displayLoaiNgay(bg.getLoaiNgay()));
        TableColumn<BangGiaHeader, String> colHieuLuc = headerColumn("Hiệu lực", 160, bg -> formatRange(bg.getNgayBatDau(), bg.getNgayKetThuc()));

        TableColumn<BangGiaHeader, Boolean> colTrangThai = new TableColumn<>("Trạng thái");
        colTrangThai.setPrefWidth(132);
        colTrangThai.setCellValueFactory(d -> new javafx.beans.property.SimpleBooleanProperty(d.getValue().isDangHoatDong()).asObject());
        colTrangThai.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label badge = new Label(active ? "Đang hoạt động" : "Tạm khóa");
                badge.getStyleClass().add(active ? "pricing-badge-active" : "pricing-badge-inactive");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        TableColumn<BangGiaHeader, Void> colAction = new TableColumn<>("Xóa");
        colAction.setPrefWidth(70);
        colAction.setSortable(false);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = createIconButton("X", AppTheme.DANGER);

            {
                btnDelete.setOnAction(e -> quickDeleteHeader(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
                setAlignment(Pos.CENTER);
            }
        });

        table.getColumns().setAll(colMa, colTen, colLoaiNgay, colHieuLuc, colTrangThai, colAction);
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            loadPriceListDetails(newV);
            lblDetailTitle.setText(newV == null ? "Chi tiết bảng giá" : "Chi tiết bảng giá: " + newV.getMaBangGia());
            lblDetailHint.setText(newV == null
                    ? "Giá áp dụng được tính theo loại ngày của bảng giá đang chọn."
                    : "Đang xem " + newV.getTenBangGia() + " - " + displayLoaiNgay(newV.getLoaiNgay()) + ".");
        });
        return table;
    }

    private TableColumn<BangGiaHeader, String> headerColumn(String title, double width,
                                                            java.util.function.Function<BangGiaHeader, String> mapper) {
        TableColumn<BangGiaHeader, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(d -> new SimpleStringProperty(displayText(mapper.apply(d.getValue()))));
        return column;
    }

    private TableView<ChiTietBangGia> createDetailTable() {
        TableView<ChiTietBangGia> table = new TableView<>(detailList);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("pricing-table");
        table.setFixedCellSize(48);
        table.setMinHeight(300);
        table.setPrefHeight(460);
        table.setPlaceholder(createEmptyState("Chưa có chi tiết bảng giá", "Bấm Thêm chi tiết để khai báo giá cho từng loại phòng."));

        table.setRowFactory(tv -> {
            TableRow<ChiTietBangGia> row = new TableRow<>();
            row.getStyleClass().add("pricing-row");
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showDetailDialog(tableHeader.getSelectionModel().getSelectedItem(), row.getItem());
                }
            });
            return row;
        });

        TableColumn<ChiTietBangGia, String> colMaLoai = textColumn("Mã loại", 86, ChiTietBangGia::getMaLoai);
        TableColumn<ChiTietBangGia, String> colTenLoai = textColumn("Loại phòng", 145,
                ct -> roomTypeNames.getOrDefault(ct.getMaLoai(), ct.getMaLoai()));
        TableColumn<ChiTietBangGia, String> colGiaNiemYet = textColumn("Giá niêm yết", 126,
                ct -> basePriceLabels.getOrDefault(ct.getMaLoai(), "Theo phòng"));
        TableColumn<ChiTietBangGia, String> colGiaApDung = textColumn("Giá áp dụng", 126, this::formatAppliedPrice);
        TableColumn<ChiTietBangGia, String> colGiaTheoGio = moneyColumn("Theo giờ", 108, ChiTietBangGia::getGiaTheoGio);
        TableColumn<ChiTietBangGia, String> colGiaQuaDem = moneyColumn("Qua đêm", 108, ChiTietBangGia::getGiaQuaDem);
        TableColumn<ChiTietBangGia, String> colGiaTheoNgay = moneyColumn("Ngày thường", 124, ChiTietBangGia::getGiaTheoNgay);
        TableColumn<ChiTietBangGia, String> colGiaCuoiTuan = moneyColumn("Cuối tuần", 120, ChiTietBangGia::getGiaCuoiTuan);
        TableColumn<ChiTietBangGia, String> colGiaLe = moneyColumn("Ngày lễ", 112, ChiTietBangGia::getGiaLe);
        TableColumn<ChiTietBangGia, String> colPhuThu = moneyColumn("Phụ thu", 104, ChiTietBangGia::getPhuThu);

        TableColumn<ChiTietBangGia, Void> colAction = new TableColumn<>("Xóa");
        colAction.setPrefWidth(66);
        colAction.setSortable(false);
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnDelete = createIconButton("X", AppTheme.DANGER);

            {
                btnDelete.setOnAction(e -> quickDeleteDetail(tableHeader.getSelectionModel().getSelectedItem(),
                        getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnDelete);
                setAlignment(Pos.CENTER);
            }
        });

        table.getColumns().setAll(
                colMaLoai, colTenLoai, colGiaNiemYet, colGiaApDung,
                colGiaTheoGio, colGiaQuaDem, colGiaTheoNgay, colGiaCuoiTuan, colGiaLe, colPhuThu, colAction
        );
        return table;
    }

    private TableColumn<ChiTietBangGia, String> textColumn(String title, double width,
                                                            java.util.function.Function<ChiTietBangGia, String> mapper) {
        TableColumn<ChiTietBangGia, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(d -> new SimpleStringProperty(displayText(mapper.apply(d.getValue()))));
        return column;
    }

    private TableColumn<ChiTietBangGia, String> moneyColumn(String title, double width,
                                                            java.util.function.ToDoubleFunction<ChiTietBangGia> mapper) {
        TableColumn<ChiTietBangGia, String> column = textColumn(title, width, ct -> formatMoney(mapper.applyAsDouble(ct)));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(Pos.CENTER_RIGHT);
            }
        });
        return column;
    }

    public void loadData() {
        if (loadingHeaders) {
            return;
        }
        loadingHeaders = true;
        setTableLoading(true);
        lblHeaderCount.setText("...");
        lblDetailCount.setText("...");

        Task<List<BangGiaHeader>> task = new Task<>() {
            @Override
            protected List<BangGiaHeader> call() {
                cacheLoaiNgayLabels();
                cacheBaseRoomPrices();
                return bangGiaHeaderDAO.getAllBangGiaHeader();
            }
        };

        task.setOnSucceeded(e -> {
            loadingHeaders = false;
            allHeaders = task.getValue() != null ? task.getValue() : new ArrayList<>();
            allHeaders.sort(Comparator.comparing(BangGiaHeader::getNgayBatDau, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(BangGiaHeader::getMaBangGia, Comparator.nullsLast(String::compareToIgnoreCase)));
            setTableLoading(false);
            headerList.setAll(allHeaders);
            updateHeaderStats();
            detailList.clear();
            updateDetailStats();

            if (!headerList.isEmpty()) {
                selectHeaderAfterReload();
            } else {
                lblDetailTitle.setText("Chi tiết bảng giá");
            }
        });

        task.setOnFailed(e -> {
            loadingHeaders = false;
            headerList.clear();
            detailList.clear();
            setTableLoading(false);
            updateHeaderStats();
            updateDetailStats();
            Throwable ex = task.getException();
            showAlert(Alert.AlertType.ERROR, "Lỗi tải bảng giá",
                    userFriendlyError(ex, "Không thể tải dữ liệu bảng giá."));
        });

        Thread thread = new Thread(task, "pricing-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshPriceList() {
        if (txtSearch != null) {
            txtSearch.clear();
        }
        loadData();
    }

    private void selectHeaderAfterReload() {
        String maBangGia = pendingSelectedMaBangGia;
        pendingSelectedMaBangGia = null;

        if (maBangGia != null && !maBangGia.isBlank()) {
            headerList.stream()
                    .filter(bg -> maBangGia.equalsIgnoreCase(bg.getMaBangGia()))
                    .findFirst()
                    .ifPresentOrElse(
                            bg -> tableHeader.getSelectionModel().select(bg),
                            () -> tableHeader.getSelectionModel().selectFirst()
                    );
            return;
        }

        tableHeader.getSelectionModel().selectFirst();
    }

    private void setTableLoading(boolean loading) {
        if (tableHeader != null) {
            tableHeader.setPlaceholder(loading
                    ? createEmptyState("Đang tải dữ liệu", "Vui lòng chờ trong giây lát.")
                    : createEmptyState("Chưa có bảng giá", "Bấm Thêm bảng giá để tạo dữ liệu áp dụng cho khách sạn."));
        }
        if (tableDetail != null) {
            tableDetail.setPlaceholder(loading
                    ? createEmptyState("Đang tải dữ liệu", "Vui lòng chờ trong giây lát.")
                    : createEmptyState("Chưa có chi tiết bảng giá", "Bấm Thêm chi tiết để khai báo giá cho từng loại phòng."));
        }
        if (loading) {
            headerList.clear();
            detailList.clear();
        }
    }

    private Node createEmptyState(String title, String subtitle) {
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("pricing-empty-title");
        Label lblSub = new Label(subtitle);
        lblSub.getStyleClass().add("pricing-empty-subtitle");
        lblSub.setWrapText(true);
        VBox box = new VBox(6, lblTitle, lblSub);
        box.getStyleClass().add("pricing-empty-state");
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(28));
        return box;
    }

    private void updateHeaderStats() {
        long activeCount = headerList.stream().filter(BangGiaHeader::isDangHoatDong).count();
        lblHeaderCount.setText(activeCount + " bảng");
    }

    private void updateDetailStats() {
        lblDetailCount.setText(detailList.size() + " dòng");
    }

    private void cacheLoaiNgayLabels() {
        customDayTypeLabels.clear();
        for (LoaiNgay item : loaiNgayDAO.loadAllLoaiNgay()) {
            if (item.getMaLoaiNgay() != null && !item.getMaLoaiNgay().isBlank()) {
                customDayTypeLabels.put(
                        bangGiaHeaderDAO.normalizeLoaiNgay(item.getMaLoaiNgay()),
                        item.getTenLoaiNgay()
                );
            }
        }
    }

    private void cacheBaseRoomPrices() {
        roomTypeNames.clear();
        basePriceLabels.clear();

        List<Phong> rooms = phongDAO.getAllPhong();
        Map<String, List<Phong>> byType = rooms.stream()
                .filter(room -> room.getMaLoai() != null && !room.getMaLoai().isBlank())
                .collect(Collectors.groupingBy(Phong::getMaLoai));

        byType.forEach((maLoai, phongList) -> {
            roomTypeNames.put(maLoai, phongList.get(0).getLoaiPhong());
            List<Double> prices = phongList.stream().map(Phong::getGiaPhong).distinct().sorted().toList();
            if (prices.isEmpty()) {
                return;
            }
            if (prices.size() == 1) {
                basePriceLabels.put(maLoai, formatMoney(prices.get(0)));
            } else {
                basePriceLabels.put(maLoai, formatMoney(prices.get(0)) + " - " + formatMoney(prices.get(prices.size() - 1)));
            }
        });
    }

    private void loadPriceListDetails(BangGiaHeader header) {
        detailList.clear();
        updateDetailStats();
        if (header == null) {
            return;
        }

        Task<List<ChiTietBangGia>> task = new Task<>() {
            @Override
            protected List<ChiTietBangGia> call() {
                return chiTietBangGiaDAO.getDetailsByMaBangGia(header.getMaBangGia());
            }
        };

        task.setOnSucceeded(e -> {
            detailList.setAll(task.getValue());
            updateDetailStats();
        });
        task.setOnFailed(e -> {
            updateDetailStats();
            Throwable ex = task.getException();
            showAlert(Alert.AlertType.ERROR, "Lỗi tải chi tiết bảng giá",
                    ex != null ? ex.getMessage() : "Không thể tải dữ liệu chi tiết.");
        });

        Thread thread = new Thread(task, "pricing-detail-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleSearch() {
        String keyword = txtSearch.getText() == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        if (keyword.isBlank()) {
            headerList.setAll(allHeaders);
            updateHeaderStats();
            return;
        }

        headerList.setAll(allHeaders.stream()
                .filter(bg -> contains(bg.getMaBangGia(), keyword)
                        || contains(bg.getTenBangGia(), keyword)
                        || contains(displayLoaiNgay(bg.getLoaiNgay()), keyword))
                .collect(Collectors.toList()));
        updateHeaderStats();
    }

    private void handleAddDetail() {
        BangGiaHeader selectedHeader = tableHeader.getSelectionModel().getSelectedItem();
        if (selectedHeader == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng chọn bảng giá trước khi thêm chi tiết.");
            return;
        }
        if (!canEditPriceListDetails(selectedHeader)) {
            showAlert(Alert.AlertType.WARNING, "Không thể thêm chi tiết",
                    "Bảng giá đang tạm khóa hoặc đã hết hiệu lực. Vui lòng mở lại trạng thái hoặc chọn bảng giá khác.");
            return;
        }
        showDetailDialog(selectedHeader, null);
    }

    private void quickDeleteHeader(BangGiaHeader selected) {
        if (selected == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa bảng giá " + selected.getMaBangGia() + "?");
        confirm.setContentText("Toàn bộ chi tiết liên quan cũng sẽ bị xóa.");

        ButtonType btnYes = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        confirm.showAndWait().ifPresent(type -> {
            if (type == btnYes) {
                chiTietBangGiaDAO.deleteByMaBangGia(selected.getMaBangGia());
                if (bangGiaHeaderDAO.delete(selected.getMaBangGia())) {
                    loadData();
                }
            }
        });
    }

    private void quickDeleteDetail(BangGiaHeader header, ChiTietBangGia selectedDetail) {
        if (header == null || selectedDetail == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText("Xóa chi tiết loại phòng " + selectedDetail.getMaLoai() + "?");
        confirm.setContentText("Dữ liệu sẽ không thể hoàn tác.");

        ButtonType btnYes = new ButtonType("Xóa", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnYes, btnNo);

        confirm.showAndWait().ifPresent(type -> {
            if (type == btnYes && chiTietBangGiaDAO.delete(header.getMaBangGia(), selectedDetail.getMaLoai())) {
                loadPriceListDetails(header);
            }
        });
    }

    private void showHeaderDialog(BangGiaHeader existing) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Thêm bảng giá" : "Cập nhật bảng giá");
        configureDialog(dialog, 560);

        Label formTitle = dialogTitle(existing == null ? "Thêm bảng giá" : "Cập nhật bảng giá");
        Label formHint = dialogHint("Nhập thông tin hiệu lực và loại ngày. Mã loại ngày vẫn được lưu theo code hệ thống.");

        TextField txtMa = styledField(existing != null ? existing.getMaBangGia() : "");
        txtMa.setPromptText("VD: BG001");
        txtMa.setDisable(existing != null);

        TextField txtTen = styledField(existing != null ? existing.getTenBangGia() : "");
        txtTen.setPromptText("VD: Bảng giá mùa hè");

        ComboBox<LoaiNgay> cboLoaiNgay = buildLoaiNgayCombo(existing != null ? existing.getLoaiNgay() : BangGiaHeader.LOAI_NGAY_THUONG);

        DatePicker dpBatDau = styledDatePicker(existing != null ? existing.getNgayBatDau() : LocalDate.now());
        DatePicker dpKetThuc = styledDatePicker(existing != null ? existing.getNgayKetThuc() : LocalDate.now().plusMonths(1));

        CheckBox chkTrangThai = new CheckBox("Đang hoạt động");
        chkTrangThai.getStyleClass().add("pricing-check");
        chkTrangThai.setSelected(existing == null || existing.isDangHoatDong());

        GridPane grid = createFormGrid();
        addFormRow(grid, 0, "Mã bảng giá", txtMa);
        addFormRow(grid, 1, "Tên bảng giá", txtTen);
        addFormRow(grid, 2, "Loại ngày", cboLoaiNgay);
        addFormRow(grid, 3, "Ngày bắt đầu", dpBatDau);
        addFormRow(grid, 4, "Ngày kết thúc", dpKetThuc);
        addFormRow(grid, 5, "Trạng thái", chkTrangThai);

        VBox content = new VBox(14, formTitle, formHint, grid);
        content.getStyleClass().add("pricing-form-shell");
        dialog.getDialogPane().setContent(content);

        ButtonType btnSave = new ButtonType(existing == null ? "Thêm" : "Cập nhật", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(btnSave, btnCancel);
        styleDialogButtons(dialog.getDialogPane(), btnSave, btnCancel);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(btnSave);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            ValidationResult validation;
            try {
                validation = validatePriceList(txtMa, txtTen, cboLoaiNgay, dpBatDau, dpKetThuc, chkTrangThai, existing);
            } catch (RuntimeException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi kiểm tra bảng giá", rootMessage(ex));
                return;
            }
            if (!validation.success()) {
                pendingFocusTarget = validation.focusTarget();
                showAlert(Alert.AlertType.WARNING, "Thông tin bảng giá không hợp lệ", validation.message());
                return;
            }

            String loaiNgayCode = bangGiaHeaderDAO.normalizeLoaiNgay(cboLoaiNgay.getValue().getMaLoaiNgay());
            String maBangGia = txtMa.getText().trim();
            String tenBangGia = txtTen.getText().trim();
            txtMa.setText(maBangGia);
            txtTen.setText(tenBangGia);
            BangGiaHeader bg = new BangGiaHeader(
                    maBangGia,
                    tenBangGia,
                    displayLoaiNgay(loaiNgayCode),
                    loaiNgayCode,
                    dpBatDau.getValue(),
                    dpKetThuc.getValue(),
                    chkTrangThai.isSelected(),
                    0
            );

            try {
                boolean ok = existing == null ? bangGiaHeaderDAO.create(bg) : bangGiaHeaderDAO.update(bg);
                if (!ok) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi lưu bảng giá", "Không thể lưu bảng giá. Vui lòng thử lại.");
                    return;
                }
                pendingSelectedMaBangGia = bg.getMaBangGia();
                if (txtSearch != null) {
                    txtSearch.clear();
                }
                dialog.close();
                loadData();
            } catch (RuntimeException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi lưu bảng giá", rootMessage(ex));
            }
        });

        dialog.showAndWait();
    }

    private ComboBox<LoaiNgay> buildLoaiNgayCombo(String selectedCode) {
        ComboBox<LoaiNgay> combo = new ComboBox<>();
        combo.getStyleClass().add("pricing-input");
        combo.setPrefHeight(40);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(LoaiNgay value) {
                return value == null || value.isAddNewItem() ? "" : formatLoaiNgayItem(value);
            }

            @Override
            public LoaiNgay fromString(String value) {
                return null;
            }
        });
        configureLoaiNgayComboCells(combo);
        loadLoaiNgayCombo(combo, selectedCode);
        combo.valueProperty().addListener((obs, oldValue, newValue) -> handleLoaiNgaySelectionChanged(combo, oldValue, newValue));
        return combo;
    }

    private void loadLoaiNgayCombo(ComboBox<LoaiNgay> combo, String selectedCode) {
        List<LoaiNgay> items = new ArrayList<>(loaiNgayDAO.loadAllLoaiNgay());
        customDayTypeLabels.clear();
        for (LoaiNgay item : items) {
            customDayTypeLabels.put(bangGiaHeaderDAO.normalizeLoaiNgay(item.getMaLoaiNgay()), item.getTenLoaiNgay());
        }
        items.add(LoaiNgay.addNewItem());

        suppressComboSelectionEvents = true;
        try {
            combo.getItems().setAll(items);
            combo.setVisibleRowCount(Math.min(Math.max(items.size(), 4), 8));
            selectLoaiNgay(combo, selectedCode);
        } finally {
            suppressComboSelectionEvents = false;
        }
    }

    private void configureLoaiNgayComboCells(ComboBox<LoaiNgay> combo) {
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(LoaiNgay item, boolean empty) {
                super.updateItem(item, empty);
                renderLoaiNgayCell(this, item, empty);
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(LoaiNgay item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isAddNewItem()) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(formatLoaiNgayItem(item));
                setStyle("");
            }
        });
    }

    private void renderLoaiNgayCell(ListCell<LoaiNgay> cell, LoaiNgay item, boolean empty) {
        cell.setGraphic(null);
        if (empty || item == null) {
            cell.setText(null);
            cell.setStyle("");
            return;
        }
        cell.setText(item.isAddNewItem() ? "+ Thêm loại ngày" : formatLoaiNgayItem(item));
        cell.setStyle(item.isAddNewItem()
                ? "-fx-text-fill:#0d9488;-fx-font-weight:800;-fx-border-color:#e2e8f0 transparent transparent transparent;-fx-border-width:1 0 0 0;"
                : "");
    }

    private void selectLoaiNgay(ComboBox<LoaiNgay> combo, String selectedCode) {
        String normalizedCode = bangGiaHeaderDAO.normalizeLoaiNgay(selectedCode);
        combo.getItems().stream()
                .filter(item -> !item.isAddNewItem())
                .filter(item -> normalizedCode.equalsIgnoreCase(bangGiaHeaderDAO.normalizeLoaiNgay(item.getMaLoaiNgay())))
                .findFirst()
                .ifPresentOrElse(combo::setValue, () -> combo.getSelectionModel().clearSelection());
    }

    private void handleLoaiNgaySelectionChanged(ComboBox<LoaiNgay> combo, LoaiNgay previousItem, LoaiNgay selectedItem) {
        if (suppressComboSelectionEvents || selectedItem == null || !selectedItem.isAddNewItem()) {
            return;
        }

        LoaiNgay created = openAddLoaiNgayForm(combo);
        if (created != null) {
            loadLoaiNgayCombo(combo, created.getMaLoaiNgay());
            return;
        }

        suppressComboSelectionEvents = true;
        try {
            if (previousItem != null && !previousItem.isAddNewItem()) {
                combo.setValue(previousItem);
            } else {
                combo.getSelectionModel().clearSelection();
            }
        } finally {
            suppressComboSelectionEvents = false;
        }
    }

    private LoaiNgay openAddLoaiNgayForm(Node ownerNode) {
        Dialog<LoaiNgay> dialog = new Dialog<>();
        dialog.setTitle("Thêm loại ngày");
        if (ownerNode != null && ownerNode.getScene() != null) {
            dialog.initOwner(ownerNode.getScene().getWindow());
        }
        configureDialog(dialog, 520);

        Label formTitle = dialogTitle("Thêm loại ngày");
        Label formHint = dialogHint("Tạo loại ngày mới để sử dụng trong bảng giá. Mã loại ngày sẽ được lưu vào cột loaiNgay của bảng giá.");

        TextField txtMaLoaiNgay = styledField("");
        txtMaLoaiNgay.setPromptText("VD: MUA_CAO_DIEM");
        TextField txtTenLoaiNgay = styledField("");
        txtTenLoaiNgay.setPromptText("VD: Mùa cao điểm");

        CheckBox chkTrangThai = new CheckBox("Đang hoạt động");
        chkTrangThai.getStyleClass().add("pricing-check");
        chkTrangThai.setSelected(true);

        GridPane grid = createFormGrid();
        addFormRow(grid, 0, "Mã loại ngày", txtMaLoaiNgay);
        addFormRow(grid, 1, "Tên loại ngày", txtTenLoaiNgay);
        addFormRow(grid, 2, "Trạng thái", chkTrangThai);

        VBox content = new VBox(14, formTitle, formHint, grid);
        content.getStyleClass().add("pricing-form-shell");
        dialog.getDialogPane().setContent(content);

        ButtonType btnSave = new ButtonType("Thêm", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(btnSave, btnCancel);
        styleDialogButtons(dialog.getDialogPane(), btnSave, btnCancel);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(btnSave);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();

            String maLoaiNgay = loaiNgayDAO.normalizeCode(txtMaLoaiNgay.getText());
            String tenLoaiNgay = loaiNgayDAO.normalizeName(txtTenLoaiNgay.getText());

            if (maLoaiNgay == null) {
                pendingFocusTarget = txtMaLoaiNgay;
                showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", "Vui lòng nhập mã loại ngày.");
                return;
            }
            if (!maLoaiNgay.matches("[A-Z0-9_-]+")) {
                pendingFocusTarget = txtMaLoaiNgay;
                showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", "Mã loại ngày chỉ được chứa chữ, số, dấu gạch dưới (_) hoặc gạch ngang (-).");
                return;
            }
            if (tenLoaiNgay == null) {
                pendingFocusTarget = txtTenLoaiNgay;
                showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", "Vui lòng nhập tên loại ngày.");
                return;
            }
            if (loaiNgayDAO.existsByMaLoaiNgay(maLoaiNgay)) {
                pendingFocusTarget = txtMaLoaiNgay;
                showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", "Loại ngày này đã tồn tại.");
                return;
            }
            if (loaiNgayDAO.existsByTenLoaiNgay(tenLoaiNgay)) {
                pendingFocusTarget = txtTenLoaiNgay;
                showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", "Loại ngày này đã tồn tại.");
                return;
            }

            LoaiNgay item = new LoaiNgay(maLoaiNgay, tenLoaiNgay, chkTrangThai.isSelected());
            try {
                if (!loaiNgayDAO.addLoaiNgay(item)) {
                    pendingFocusTarget = txtTenLoaiNgay;
                    showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", "Loại ngày này đã tồn tại.");
                    return;
                }
                customDayTypeLabels.put(bangGiaHeaderDAO.normalizeLoaiNgay(item.getMaLoaiNgay()), item.getTenLoaiNgay());
                dialog.setResult(item);
                dialog.close();
            } catch (RuntimeException ex) {
                if (loaiNgayDAO.isDuplicateKeyException(ex)) {
                    pendingFocusTarget = txtTenLoaiNgay;
                    showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", "Loại ngày này đã tồn tại.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi lưu loại ngày", "Không thể thêm loại ngày. Vui lòng thử lại.");
                }
            }
        });

        return dialog.showAndWait().orElse(null);
    }

    private ValidationResult validatePriceList(TextField txtMa,
                                               TextField txtTen,
                                               ComboBox<LoaiNgay> cboLoaiNgay,
                                               DatePicker dpBatDau,
                                               DatePicker dpKetThuc,
                                               CheckBox chkTrangThai,
                                               BangGiaHeader existing) {
        String ma = txtMa.getText() == null ? "" : txtMa.getText().trim();
        String ten = txtTen.getText() == null ? "" : txtTen.getText().trim();

        if (ma.isBlank()) {
            return ValidationResult.invalid("Vui lòng nhập mã bảng giá.", txtMa);
        }
        if (!ma.matches("[A-Za-z0-9_-]+")) {
            return ValidationResult.invalid("Mã bảng giá chỉ được chứa chữ, số, dấu gạch dưới (_) hoặc gạch ngang (-).", txtMa);
        }
        if (existing != null && !ma.equalsIgnoreCase(existing.getMaBangGia())) {
            return ValidationResult.invalid("Không được sửa mã bảng giá khi cập nhật.", txtMa);
        }
        if (existing == null && bangGiaHeaderDAO.existsPriceListCode(ma)) {
            return ValidationResult.invalid("Mã bảng giá đã tồn tại. Vui lòng nhập mã khác.", txtMa);
        }
        if (ten.isBlank()) {
            return ValidationResult.invalid("Vui lòng nhập tên bảng giá.", txtTen);
        }
        if (ten.length() < 3) {
            return ValidationResult.invalid("Tên bảng giá phải có tối thiểu 3 ký tự.", txtTen);
        }
        LoaiNgay selectedLoaiNgay = cboLoaiNgay.getValue();
        if (selectedLoaiNgay == null || selectedLoaiNgay.getMaLoaiNgay() == null || selectedLoaiNgay.getMaLoaiNgay().isBlank()) {
            return ValidationResult.invalid("Vui lòng chọn loại ngày.", cboLoaiNgay);
        }
        if (selectedLoaiNgay.isAddNewItem()) {
            return ValidationResult.invalid("Vui lòng chọn một loại ngày hợp lệ.", cboLoaiNgay);
        }
        String loaiNgayCode = bangGiaHeaderDAO.normalizeLoaiNgay(selectedLoaiNgay.getMaLoaiNgay());
        if (dpBatDau.getValue() == null) {
            return ValidationResult.invalid("Vui lòng chọn ngày bắt đầu.", dpBatDau);
        }
        if (dpKetThuc.getValue() == null) {
            return ValidationResult.invalid("Vui lòng chọn ngày kết thúc.", dpKetThuc);
        }
        if (dpKetThuc.getValue().isBefore(dpBatDau.getValue())) {
            return ValidationResult.invalid("Ngày kết thúc phải lớn hơn hoặc bằng ngày bắt đầu.", dpKetThuc);
        }
        LocalDate today = LocalDate.now();
        if (dpBatDau.getValue().isBefore(today)) {
            return ValidationResult.invalid("Ngày bắt đầu phải lớn hơn hoặc bằng ngày hiện tại.", dpBatDau);
        }
        String excludeMa = existing != null ? existing.getMaBangGia() : null;
        if (bangGiaHeaderDAO.existsPriceListNameInSameDayTypeAndPeriod(ten, loaiNgayCode, dpBatDau.getValue(), dpKetThuc.getValue(), excludeMa)) {
            return ValidationResult.invalid("Tên bảng giá đã tồn tại trong cùng loại ngày và khoảng thời gian hiệu lực.", txtTen);
        }
        boolean savingActive = chkTrangThai == null || chkTrangThai.isSelected();
        if (savingActive && bangGiaHeaderDAO.hasOverlappingActivePriceList(excludeMa, loaiNgayCode, dpBatDau.getValue(), dpKetThuc.getValue())) {
            return ValidationResult.invalid("Khoảng thời gian áp dụng bị giao với một bảng giá đang hoạt động cùng loại ngày.", dpBatDau);
        }
        return ValidationResult.ok();
    }

    private void showPriceTypeDialog() {
        LoaiNgay created = openAddLoaiNgayForm(tableHeader);
        if (created != null) {
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Thêm loại ngày thành công.");
        }
    }

    private void showDetailDialog(BangGiaHeader header, ChiTietBangGia existing) {
        if (header == null) {
            showAlert(Alert.AlertType.WARNING, "Thiếu dữ liệu", "Vui lòng chọn bảng giá trước.");
            return;
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Thêm chi tiết bảng giá" : "Cập nhật chi tiết bảng giá");
        configureDialog(dialog, 590);

        Label formTitle = dialogTitle(existing == null ? "Thêm chi tiết bảng giá" : "Cập nhật chi tiết bảng giá");
        Label formHint = dialogHint("Chọn loại phòng và nhập các mức giá áp dụng. Có thể để trống các ô chưa dùng.");

        ComboBox<PhongDAO.LoaiPhongItem> cboLoaiPhong = new ComboBox<>();
        cboLoaiPhong.getStyleClass().add("pricing-input");
        cboLoaiPhong.setPrefHeight(40);
        cboLoaiPhong.setMaxWidth(Double.MAX_VALUE);
        configureRoomTypeCombo(cboLoaiPhong);
        loadLoaiPhongCombo(cboLoaiPhong, existing != null ? existing.getMaLoai() : null);
        cboLoaiPhong.valueProperty().addListener((obs, oldValue, newValue) -> handleLoaiPhongSelectionChanged(cboLoaiPhong, oldValue, newValue));

        if (existing != null) {
            cboLoaiPhong.setDisable(true);
        }

        TextField txtGiaTheoGio = styledMoneyField(existing != null ? existing.getGiaTheoGio() : 0);
        TextField txtGiaQuaDem = styledMoneyField(existing != null ? existing.getGiaQuaDem() : 0);
        TextField txtGiaTheoNgay = styledMoneyField(existing != null ? existing.getGiaTheoNgay() : 0);
        TextField txtGiaCuoiTuan = styledMoneyField(existing != null ? existing.getGiaCuoiTuan() : 0);
        TextField txtGiaLe = styledMoneyField(existing != null ? existing.getGiaLe() : 0);
        TextField txtPhuThu = styledMoneyField(existing != null ? existing.getPhuThu() : 0);

        GridPane grid = createFormGrid();
        addFormRow(grid, 0, "Loại phòng", cboLoaiPhong);
        addFormRow(grid, 1, "Giá theo giờ", txtGiaTheoGio);
        addFormRow(grid, 2, "Giá qua đêm", txtGiaQuaDem);
        addFormRow(grid, 3, "Giá ngày thường", txtGiaTheoNgay);
        addFormRow(grid, 4, "Giá cuối tuần", txtGiaCuoiTuan);
        addFormRow(grid, 5, "Giá ngày lễ", txtGiaLe);
        addFormRow(grid, 6, "Phụ thu", txtPhuThu);

        VBox content = new VBox(14, formTitle, formHint, grid);
        content.getStyleClass().add("pricing-form-shell");
        dialog.getDialogPane().setContent(content);

        ButtonType btnSave = new ButtonType(existing == null ? "Thêm" : "Cập nhật", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(btnSave, btnCancel);
        styleDialogButtons(dialog.getDialogPane(), btnSave, btnCancel);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(btnSave);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            ValidationResult validation;
            try {
                validation = validatePriceListDetail(
                        header,
                        existing,
                        cboLoaiPhong.getValue(),
                        txtGiaTheoGio,
                        txtGiaQuaDem,
                        txtGiaTheoNgay,
                        txtGiaCuoiTuan,
                        txtGiaLe,
                        txtPhuThu
                );
            } catch (RuntimeException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi kiểm tra chi tiết bảng giá", rootMessage(ex));
                return;
            }
            if (!validation.success()) {
                pendingFocusTarget = validation.focusTarget();
                showDetailValidationAlert(validation.message());
                return;
            }

            try {
                DetailPrices prices = readDetailPrices(txtGiaTheoGio, txtGiaQuaDem, txtGiaTheoNgay, txtGiaCuoiTuan, txtGiaLe, txtPhuThu);
                ChiTietBangGia ct = new ChiTietBangGia(
                        header,
                        cboLoaiPhong.getValue().getMaLoai(),
                        prices.giaTheoGio(),
                        prices.giaQuaDem(),
                        prices.giaTheoNgay(),
                        prices.giaCuoiTuan(),
                        prices.giaLe(),
                        prices.phuThu()
                );
                boolean ok = existing == null ? chiTietBangGiaDAO.create(ct) : chiTietBangGiaDAO.update(ct);
                if (!ok) {
                    showAlert(Alert.AlertType.ERROR, "Lỗi lưu chi tiết bảng giá", "Không thể lưu chi tiết bảng giá. Vui lòng thử lại.");
                    return;
                }
                dialog.close();
                loadPriceListDetails(header);
            } catch (RuntimeException ex) {
                showAlert(Alert.AlertType.ERROR, "Lỗi lưu chi tiết bảng giá", rootMessage(ex));
            }
        });

        dialog.showAndWait();
    }

    private void configureRoomTypeCombo(ComboBox<PhongDAO.LoaiPhongItem> combo) {
        combo.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PhongDAO.LoaiPhongItem item, boolean empty) {
                super.updateItem(item, empty);
                renderLoaiPhongCell(this, item, empty);
            }
        });
        combo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PhongDAO.LoaiPhongItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isAddNewItem()) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(formatRoomType(item));
                setStyle("");
            }
        });
        combo.setPromptText("Chọn loại phòng");
    }

    private void loadLoaiPhongCombo(ComboBox<PhongDAO.LoaiPhongItem> combo, String selectedMaLoai) {
        List<PhongDAO.LoaiPhongItem> items = new ArrayList<>(phongDAO.getDistinctLoaiPhong());
        items.add(PhongDAO.LoaiPhongItem.addNewItem());

        suppressComboSelectionEvents = true;
        try {
            combo.getItems().setAll(items);
            combo.setVisibleRowCount(Math.min(Math.max(items.size(), 4), 8));
            selectLoaiPhong(combo, selectedMaLoai);
        } finally {
            suppressComboSelectionEvents = false;
        }
    }

    private void renderLoaiPhongCell(ListCell<PhongDAO.LoaiPhongItem> cell, PhongDAO.LoaiPhongItem item, boolean empty) {
        cell.setGraphic(null);
        if (empty || item == null) {
            cell.setText(null);
            cell.setStyle("");
            return;
        }
        cell.setText(item.isAddNewItem() ? "+ Thêm loại phòng" : formatRoomType(item));
        cell.setStyle(item.isAddNewItem()
                ? "-fx-text-fill:#0d9488;-fx-font-weight:800;-fx-border-color:#e2e8f0 transparent transparent transparent;-fx-border-width:1 0 0 0;"
                : "");
    }

    private void selectLoaiPhong(ComboBox<PhongDAO.LoaiPhongItem> combo, String selectedMaLoai) {
        if (selectedMaLoai == null || selectedMaLoai.isBlank()) {
            combo.getSelectionModel().clearSelection();
            return;
        }
        combo.getItems().stream()
                .filter(item -> !item.isAddNewItem())
                .filter(item -> selectedMaLoai.equalsIgnoreCase(item.getMaLoai()))
                .findFirst()
                .ifPresentOrElse(combo::setValue, () -> combo.getSelectionModel().clearSelection());
    }

    private void handleLoaiPhongSelectionChanged(ComboBox<PhongDAO.LoaiPhongItem> combo,
                                                 PhongDAO.LoaiPhongItem previousItem,
                                                 PhongDAO.LoaiPhongItem selectedItem) {
        if (suppressComboSelectionEvents || selectedItem == null || !selectedItem.isAddNewItem()) {
            return;
        }

        PhongDAO.LoaiPhongItem created = openAddLoaiPhongForm(combo);
        if (created != null) {
            loadLoaiPhongCombo(combo, created.getMaLoai());
            return;
        }

        suppressComboSelectionEvents = true;
        try {
            if (previousItem != null && !previousItem.isAddNewItem()) {
                combo.setValue(previousItem);
            } else {
                combo.getSelectionModel().clearSelection();
            }
        } finally {
            suppressComboSelectionEvents = false;
        }
    }

    private PhongDAO.LoaiPhongItem openAddLoaiPhongForm(Node ownerNode) {
        RoomTypeManagementDialog dialog = new RoomTypeManagementDialog(
                ownerNode != null && ownerNode.getScene() != null ? ownerNode.getScene().getWindow() : null
        );
        RoomTypeManagementDialog.Result result = dialog.showAndWait();
        if (result.changed() && result.preferredSelection() != null) {
            return result.preferredSelection();
        }
        return null;
    }

    private ValidationResult validatePriceListDetail(BangGiaHeader header,
                                                     ChiTietBangGia existing,
                                                     PhongDAO.LoaiPhongItem selectedRoomType,
                                                     TextField txtGiaTheoGio,
                                                     TextField txtGiaQuaDem,
                                                     TextField txtGiaTheoNgay,
                                                     TextField txtGiaCuoiTuan,
                                                     TextField txtGiaLe,
                                                     TextField txtPhuThu) {
        if (header == null) {
            return ValidationResult.invalid("Vui lòng chọn bảng giá trước khi thêm chi tiết.");
        }
        if (!canEditPriceListDetails(header)) {
            return ValidationResult.invalid("Bảng giá đang tạm khóa hoặc đã hết hiệu lực, không thể thêm chi tiết.");
        }
        if (selectedRoomType == null || selectedRoomType.getMaLoai() == null || selectedRoomType.getMaLoai().isBlank()) {
            return ValidationResult.invalid("Vui lòng chọn loại phòng.", null);
        }
        if (selectedRoomType.isAddNewItem()) {
            return ValidationResult.invalid("Vui lòng chọn một loại phòng hợp lệ.", null);
        }

        DetailPrices prices;
        try {
            prices = readDetailPrices(txtGiaTheoGio, txtGiaQuaDem, txtGiaTheoNgay, txtGiaCuoiTuan, txtGiaLe, txtPhuThu);
        } catch (NumberFormatException ex) {
            return ValidationResult.invalid("Các ô tiền phải là số hợp lệ và không âm.", txtGiaTheoGio);
        }

        if (prices.hasNegativeValue()) {
            return ValidationResult.invalid("Các giá và phụ thu phải lớn hơn hoặc bằng 0.", txtGiaTheoGio);
        }
        if (!prices.hasAnyPositiveMainPrice()) {
            return ValidationResult.invalid("Ít nhất một loại giá phải lớn hơn 0.", txtGiaTheoGio);
        }
        if (prices.giaTheoNgay() > 0 && prices.giaCuoiTuan() > 0 && prices.giaCuoiTuan() < prices.giaTheoNgay()) {
            return ValidationResult.invalid("Giá cuối tuần nên lớn hơn hoặc bằng giá ngày thường.", txtGiaCuoiTuan);
        }
        if (prices.giaCuoiTuan() > 0 && prices.giaLe() > 0 && prices.giaLe() < prices.giaCuoiTuan()) {
            return ValidationResult.invalid("Giá ngày lễ nên lớn hơn hoặc bằng giá cuối tuần.", txtGiaLe);
        }
        if (isDuplicateDetail(header, selectedRoomType.getMaLoai(), existing)) {
            return ValidationResult.invalid("Loại phòng này đã có trong bảng giá đang chọn.", null);
        }

        return ValidationResult.ok();
    }

    private boolean isDuplicateDetail(BangGiaHeader header, String maLoai, ChiTietBangGia existing) {
        if (existing != null && maLoai.equalsIgnoreCase(existing.getMaLoai())) {
            return false;
        }

        boolean duplicateInView = detailList.stream()
                .anyMatch(ct -> maLoai.equalsIgnoreCase(ct.getMaLoai())
                        && header.getMaBangGia().equalsIgnoreCase(ct.getMaBangGia()));
        if (duplicateInView) {
            return true;
        }

        return chiTietBangGiaDAO.existsByBangGiaAndLoai(header.getMaBangGia(), maLoai);
    }

    private boolean canEditPriceListDetails(BangGiaHeader header) {
        if (header == null || !header.isDangHoatDong()) {
            return false;
        }
        return header.getNgayKetThuc() == null || !header.getNgayKetThuc().isBefore(LocalDate.now());
    }

    private DetailPrices readDetailPrices(TextField txtGiaTheoGio,
                                          TextField txtGiaQuaDem,
                                          TextField txtGiaTheoNgay,
                                          TextField txtGiaCuoiTuan,
                                          TextField txtGiaLe,
                                          TextField txtPhuThu) {
        return new DetailPrices(
                parseMoney(txtGiaTheoGio.getText()),
                parseMoney(txtGiaQuaDem.getText()),
                parseMoney(txtGiaTheoNgay.getText()),
                parseMoney(txtGiaCuoiTuan.getText()),
                parseMoney(txtGiaLe.getText()),
                parseMoney(txtPhuThu.getText())
        );
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setPadding(new Insets(4, 0, 0, 0));

        ColumnConstraints labelCol = new ColumnConstraints(140);
        ColumnConstraints inputCol = new ColumnConstraints(320);
        inputCol.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().setAll(labelCol, inputCol);
        return grid;
    }

    private void addFormRow(GridPane grid, int rowIndex, String labelText, Node field) {
        Label label = formLabel(labelText);
        grid.add(label, 0, rowIndex);
        grid.add(field, 1, rowIndex);
        GridPane.setHgrow(field, Priority.ALWAYS);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private Label dialogTitle(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("pricing-dialog-title");
        return label;
    }

    private Label dialogHint(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("pricing-dialog-hint");
        label.setWrapText(true);
        return label;
    }

    private Label formLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("pricing-form-label");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(Pos.CENTER_RIGHT);
        return label;
    }

    private TextField styledField(String value) {
        TextField field = new TextField(value);
        field.getStyleClass().add("pricing-input");
        field.setPrefHeight(40);
        return field;
    }

    private TextField styledMoneyField(double value) {
        TextField field = styledField(value > 0 ? formatMoneyInput(value) : "0");
        field.setPromptText("0");
        field.setAlignment(Pos.CENTER_LEFT);
        field.getStyleClass().add("pricing-money-input");
        attachMoneyBehavior(field);
        return field;
    }

    private DatePicker styledDatePicker(LocalDate value) {
        DatePicker picker = new DatePicker(value);
        picker.getStyleClass().add("pricing-input");
        picker.setPrefHeight(40);
        picker.setMaxWidth(Double.MAX_VALUE);
        return picker;
    }

    private void attachMoneyBehavior(TextField field) {
        field.textProperty().addListener((obs, oldValue, newValue) -> {
            String digitsOnly = newValue == null ? "" : newValue.replaceAll("[^0-9]", "");
            if (!digitsOnly.equals(newValue)) {
                field.setText(digitsOnly);
            }
        });
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                String editingValue = stripMoneyForEditing(field.getText());
                field.setText("0".equals(editingValue) ? "" : editingValue);
                return;
            }
            try {
                double value = parseMoney(field.getText());
                field.setText(value > 0 ? formatMoneyInput(value) : "0");
            } catch (NumberFormatException ignored) {
                field.setText("0");
            }
        });
    }

    private Button createButton(String text, String color, double width) {
        Button button = new Button(text);
        button.setMinWidth(width);
        button.setPrefHeight(40);
        button.setTextFill(Color.WHITE);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.getStyleClass().add("pricing-button");
        button.setStyle("-fx-background-color:" + color + ";");
        return button;
    }

    private Button createGhostButton(String text, double width) {
        Button button = new Button(text);
        button.setMinWidth(width);
        button.setPrefHeight(40);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        button.getStyleClass().add("pricing-ghost-button");
        return button;
    }

    private Button createIconButton(String text, String color) {
        Button button = new Button(text);
        button.setMinSize(30, 30);
        button.setPrefSize(30, 30);
        button.setMaxSize(30, 30);
        button.setTextFill(Color.WHITE);
        button.setFont(Font.font("Segoe UI", FontWeight.BOLD, 11));
        button.getStyleClass().add("pricing-icon-button");
        button.setStyle("-fx-background-color:" + color + ";");
        return button;
    }

    private void configureDialog(Dialog<?> dialog, double width) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(getStylesheetUri());
        pane.getStyleClass().add("pricing-dialog");
        pane.setMinWidth(width);
        pane.setPrefWidth(width);
    }

    private void styleDialogButtons(DialogPane pane, ButtonType btnSave, ButtonType btnCancel) {
        Button save = (Button) pane.lookupButton(btnSave);
        Button cancel = (Button) pane.lookupButton(btnCancel);
        if (save != null) {
            save.getStyleClass().add("pricing-dialog-primary");
            save.setDefaultButton(true);
        }
        if (cancel != null) {
            cancel.getStyleClass().add("pricing-dialog-secondary");
            cancel.setCancelButton(true);
        }
    }

    private String formatAppliedPrice(ChiTietBangGia detail) {
        BangGiaHeader header = tableHeader != null ? tableHeader.getSelectionModel().getSelectedItem() : null;
        if (header == null) {
            return "-";
        }
        double value = chiTietBangGiaDAO.resolveAppliedDailyRate(header, detail, header.getNgayBatDau());
        return formatMoney(value);
    }

    private String displayText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String displayLoaiNgay(String loaiNgay) {
        String normalized = bangGiaHeaderDAO.normalizeLoaiNgay(loaiNgay);
        if (customDayTypeLabels.containsKey(normalized)) {
            return customDayTypeLabels.get(normalized);
        }
        if (BangGiaHeader.LOAI_CUOI_TUAN.equals(normalized)) {
            return "Cuối tuần";
        }
        if (BangGiaHeader.LOAI_NGAY_LE.equals(normalized)) {
            return "Ngày lễ";
        }
        if (BangGiaHeader.LOAI_NGAY_THUONG.equals(normalized)) {
            return "Ngày thường";
        }
        return customDayTypeLabels.getOrDefault(normalized, normalized.replace('_', ' '));
    }

    private String formatLoaiNgayItem(LoaiNgay item) {
        if (item == null) {
            return "";
        }
        String ten = item.getTenLoaiNgay() == null || item.getTenLoaiNgay().isBlank()
                ? displayLoaiNgay(item.getMaLoaiNgay())
                : item.getTenLoaiNgay();
        return ten;
    }

    private String formatRoomType(PhongDAO.LoaiPhongItem item) {
        if (item == null) {
            return "";
        }
        String name = item.getLoaiPhong() == null || item.getLoaiPhong().isBlank() ? "Chưa đặt tên" : item.getLoaiPhong();
        String code = item.getMaLoai() == null || item.getMaLoai().isBlank() ? "" : " (" + item.getMaLoai() + ")";
        return name + code;
    }

    private String formatRange(LocalDate fromDate, LocalDate toDate) {
        String from = fromDate != null ? DATE_FORMAT.format(fromDate) : "-";
        String to = toDate != null ? DATE_FORMAT.format(toDate) : "Không giới hạn";
        return from + " - " + to;
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String formatMoney(double value) {
        return value > 0 ? String.format("%,.0f VNĐ", value) : "-";
    }

    private String formatMoneyInput(double value) {
        return String.format("%.0f", value);
    }

    private String stripMoneyForEditing(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace("VNĐ", "")
                .replace("VND", "")
                .replace("Đ", "")
                .replace("đ", "")
                .replace(",", "")
                .replace(".", "")
                .replace(" ", "")
                .trim();
    }

    private double parseMoney(String value) {
        if (value == null || value.trim().isBlank()) {
            return 0;
        }
        String normalized = value.trim()
                .replace("VNĐ", "")
                .replace("VND", "")
                .replace("Đ", "")
                .replace("đ", "")
                .replace(" ", "");
        if (!normalized.matches("[0-9,.]+")) {
            throw new NumberFormatException("Invalid money value: " + value);
        }
        String digits = normalized.replace(",", "").replace(".", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Double.parseDouble(digits);
    }

    private void showDetailValidationAlert(String specificMessage) {
        String message = DETAIL_INVALID_MESSAGE;
        if (specificMessage != null && !specificMessage.isBlank()) {
            message += "\n\n" + specificMessage;
        }
        showAlert(Alert.AlertType.WARNING, "Dữ liệu không hợp lệ", message);
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
        focusPendingTarget();
    }

    private void focusPendingTarget() {
        Node target = pendingFocusTarget;
        pendingFocusTarget = null;
        if (target == null || target.isDisabled()) {
            return;
        }
        target.requestFocus();
        if (target instanceof TextField textField) {
            textField.selectAll();
        }
    }

    private String rootMessage(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage() != null ? current.getMessage() : ex.getMessage();
        if (current instanceof java.sql.SQLException
                || (message != null && (message.contains("SQLState=") || message.contains("ErrorCode=")))) {
            return "Không thể xử lý dữ liệu. Vui lòng thử lại.";
        }
        return message != null ? message : "Không thể xử lý dữ liệu. Vui lòng thử lại.";
    }

    private String userFriendlyError(Throwable ex, String fallbackMessage) {
        if (ex == null) {
            return fallbackMessage;
        }
        if (loaiNgayDAO.isDuplicateKeyException(ex)) {
            return "Loại ngày này đã tồn tại.";
        }
        return fallbackMessage;
    }

    private String getStylesheetUri() {
        return Paths.get("src", "gui", "pricing-view.css").toAbsolutePath().toUri().toString();
    }

    private record ValidationResult(boolean success, String message, Node focusTarget) {
        static ValidationResult ok() {
            return new ValidationResult(true, "", null);
        }

        static ValidationResult invalid(String message) {
            return invalid(message, null);
        }

        static ValidationResult invalid(String message, Node focusTarget) {
            return new ValidationResult(false, message, focusTarget);
        }
    }

    private record DetailPrices(double giaTheoGio,
                                double giaQuaDem,
                                double giaTheoNgay,
                                double giaCuoiTuan,
                                double giaLe,
                                double phuThu) {
        boolean hasNegativeValue() {
            return giaTheoGio < 0 || giaQuaDem < 0 || giaTheoNgay < 0 || giaCuoiTuan < 0 || giaLe < 0 || phuThu < 0;
        }

        boolean hasAnyPositiveMainPrice() {
            return giaTheoGio > 0 || giaQuaDem > 0 || giaTheoNgay > 0 || giaCuoiTuan > 0 || giaLe > 0;
        }
    }
}
