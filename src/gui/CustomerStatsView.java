package gui;

import dao.KhachHangDAO;
import dao.PhieuDatPhongDAO;
import entity.KhachHang;
import entity.PhieuDatPhong;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomerStatsView {

    private final KhachHangDAO khachHangDAO = new KhachHangDAO();
    private final PhieuDatPhongDAO phieuDatPhongDAO = new PhieuDatPhongDAO();

    private final ObservableList<TopCustomerStat> topCustomerItems = FXCollections.observableArrayList();
    private final List<TopCustomerStat> allTopCustomerItems = new ArrayList<>();

    private VBox root;
    private Label lblTotal;
    private Label lblNewMonth;
    private Label lblFrequent;
    private Label lblChartHint;
    private TextField txtSearch;
    private TableView<TopCustomerStat> tableTop;
    private BarChart<String, Number> barChart;
    private StackPane chartContainer;

    public Node createView() {
        root = new VBox(20);
        root.getStyleClass().addAll("customer-stats-view", "page-shell");
        root.setPadding(new Insets(24));
        root.getStylesheets().addAll(
                stylesheetUri("global-theme.css"),
                stylesheetUri("analytics-view.css")
        );

        root.getChildren().add(buildHeader());
        root.getChildren().add(buildSummaryGrid());
        Node content = buildContent();
        VBox.setVgrow(content, Priority.ALWAYS);
        root.getChildren().add(content);

        animateEntrance();
        loadData();
        return root;
    }

    public void loadData() {
        Task<CustomerStatsSnapshot> task = new Task<>() {
            @Override
            protected CustomerStatsSnapshot call() {
                List<KhachHang> customers = khachHangDAO.getAllKhachHang();
                List<PhieuDatPhong> bookings = phieuDatPhongDAO.getAllPhieuDatPhong();
                return buildSnapshot(customers, bookings);
            }
        };

        task.setOnSucceeded(e -> applySnapshot(task.getValue()));
        task.setOnFailed(e -> showEmptyState("Không thể tải thống kê khách hàng", "Kiểm tra kết nối CSDL và dữ liệu phiếu đặt."));
        new Thread(task).start();
    }

    private Node buildHeader() {
        HBox header = new HBox(14);
        header.getStyleClass().add("page-header");

        StackPane icon = new StackPane(new Label("\uD83D\uDC65"));
        icon.getStyleClass().add("page-icon-badge");

        VBox textBox = new VBox(4);
        textBox.getStyleClass().add("page-header-text");
        Label title = new Label("Thống kê khách hàng");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Tổng hợp tăng trưởng khách, mức độ trung thành và danh sách khách đặt phòng nhiều nhất.");
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);
        textBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label badge = new Label("Theo lịch sử đặt phòng");
        badge.getStyleClass().add("stats-filter-badge");

        header.getChildren().addAll(icon, textBox, spacer, badge);
        return header;
    }

    private Node buildSummaryGrid() {
        lblTotal = new Label("--");
        lblNewMonth = new Label("--");
        lblFrequent = new Label("--");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("stats-summary-grid");
        for (int i = 0; i < 3; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(33.33);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        grid.add(metricCard("\uD83D\uDC64", "Tổng khách hàng", lblTotal, "Số lượng khách đang lưu trên hệ thống", "#eff6ff", "#1f3a5f"), 0, 0);
        grid.add(metricCard("\u2728", "Khách mới tháng này", lblNewMonth, "Khách có lần đặt đầu tiên trong tháng hiện tại", "#ecfdf5", "#0f766e"), 1, 0);
        grid.add(metricCard("\uD83C\uDFC6", "Khach than thiet", lblFrequent, "Khach co tu 5 luot dat tro len", "#fff7ed", "#d97706"), 2, 0);
        return grid;
    }

    private Node buildContent() {
        HBox content = new HBox(18);

        VBox leftCard = new VBox(16);
        leftCard.getStyleClass().add("panel-card");
        HBox.setHgrow(leftCard, Priority.ALWAYS);

        HBox tableHeader = buildPanelHeader("#1f3a5f", "Top khách hàng đặt nhiều", "Danh sách ưu tiên theo tổng số phiếu đặt và lần đặt gần nhất.");

        HBox searchBox = new HBox(10);
        searchBox.getStyleClass().add("search-box");
        Label searchIcon = new Label("\uD83D\uDD0D");
        searchIcon.getStyleClass().add("search-box-icon");
        txtSearch = new TextField();
        txtSearch.getStyleClass().addAll("search-field", "stats-search-field");
        txtSearch.setPromptText("Tim theo ma khach, ten hoac so dien thoai...");
        txtSearch.textProperty().addListener((obs, oldValue, newValue) -> applySearchFilter());
        HBox.setHgrow(txtSearch, Priority.ALWAYS);
        searchBox.getChildren().addAll(searchIcon, txtSearch);

        tableTop = new TableView<>();
        tableTop.getStyleClass().addAll("app-table", "stats-table");
        tableTop.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableTop.setItems(topCustomerItems);
        tableTop.setPlaceholder(createEmptyState("Không có top khách hàng", "Ngay khi có phiếu đặt, bảng này sẽ hiển thị khách đặt nhiều nhất."));
        configureTopCustomerTable();
        VBox.setVgrow(tableTop, Priority.ALWAYS);

        leftCard.getChildren().addAll(tableHeader, searchBox, tableTop);

        VBox rightCard = new VBox(16);
        rightCard.getStyleClass().add("panel-card");
        rightCard.setPrefWidth(420);

        HBox chartHeader = buildPanelHeader("#0d9488", "Xu hướng khách theo tháng", "Số khách duy nhất phát sinh đặt phòng trong 6 tháng gần nhất.");
        lblChartHint = new Label("Dang dong bo du lieu khach...");
        lblChartHint.getStyleClass().add("panel-subtitle");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setLabel("Khach");

        barChart = new BarChart<>(xAxis, yAxis);
        barChart.getStyleClass().add("analytics-chart");
        barChart.setLegendVisible(false);
        barChart.setCategoryGap(18);
        barChart.setBarGap(8);
        barChart.setAnimated(true);

        chartContainer = new StackPane(barChart, createEmptyState("Chua co du lieu xu huong", "Can it nhat mot phieu dat de ve bieu do khach theo thang."));
        chartContainer.setPrefHeight(420);
        VBox.setVgrow(chartContainer, Priority.ALWAYS);

        rightCard.getChildren().addAll(chartHeader, lblChartHint, chartContainer);

        content.getChildren().addAll(leftCard, rightCard);
        return content;
    }

    private HBox buildPanelHeader(String accent, String title, String subtitle) {
        HBox row = new HBox(10);
        row.getStyleClass().add("panel-header");
        Region dot = new Region();
        dot.getStyleClass().add("panel-dot");
        dot.setStyle("-fx-background-color: " + accent + ";");

        VBox textBox = new VBox(2);
        textBox.getStyleClass().add("panel-header-text");
        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("panel-title");
        Label lblSubtitle = new Label(subtitle);
        lblSubtitle.getStyleClass().add("panel-subtitle");
        lblSubtitle.setWrapText(true);
        textBox.getChildren().addAll(lblTitle, lblSubtitle);

        row.getChildren().addAll(dot, textBox);
        return row;
    }

    private VBox metricCard(String icon, String title, Label value, String caption, String iconBg, String accent) {
        VBox card = new VBox(12);
        card.getStyleClass().add("metric-card");

        HBox header = new HBox(12);
        header.getStyleClass().add("metric-card-header");

        StackPane iconWrap = new StackPane(new Label(icon));
        iconWrap.getStyleClass().add("metric-icon");
        iconWrap.setStyle("-fx-background-color: " + iconBg + ";");

        Label titleLabel = new Label(title.toUpperCase(Locale.ROOT));
        titleLabel.getStyleClass().add("metric-card-title");
        header.getChildren().addAll(iconWrap, titleLabel);

        value.getStyleClass().add("metric-card-value");
        value.setStyle("-fx-text-fill: " + accent + ";");

        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("metric-card-caption");

        card.getChildren().addAll(header, value, captionLabel);
        return card;
    }

    private void configureTopCustomerTable() {
        TableColumn<TopCustomerStat, String> colCode = new TableColumn<>("Ma KH");
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().customerCode));

        TableColumn<TopCustomerStat, String> colName = new TableColumn<>("Khách hàng");
        colName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().customerName));

        TableColumn<TopCustomerStat, String> colPhone = new TableColumn<>("Dien thoai");
        colPhone.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().phone));

        TableColumn<TopCustomerStat, Number> colCount = new TableColumn<>("Luot dat");
        colCount.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().bookingCount));
        colCount.setStyle("-fx-alignment: CENTER;");

        TableColumn<TopCustomerStat, String> colLast = new TableColumn<>("Gan nhat");
        colLast.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().lastBookingLabel));
        colLast.setStyle("-fx-alignment: CENTER;");

        tableTop.getColumns().setAll(colCode, colName, colPhone, colCount, colLast);
    }

    private CustomerStatsSnapshot buildSnapshot(List<KhachHang> customers, List<PhieuDatPhong> bookings) {
        List<KhachHang> safeCustomers = customers != null ? customers : List.of();
        List<PhieuDatPhong> safeBookings = bookings != null ? bookings : List.of();

        Map<String, KhachHang> customerById = safeCustomers.stream()
                .collect(Collectors.toMap(KhachHang::getMaKhachHang, customer -> customer, (a, b) -> a));

        Map<String, Integer> bookingCountByCustomer = new HashMap<>();
        Map<String, LocalDate> firstBookingByCustomer = new HashMap<>();
        Map<String, LocalDate> lastBookingByCustomer = new HashMap<>();

        for (PhieuDatPhong booking : safeBookings) {
            if (booking.getKhachHang() == null || booking.getKhachHang().getMaKhachHang() == null || booking.getNgayDat() == null) {
                continue;
            }
            String customerId = booking.getKhachHang().getMaKhachHang();
            LocalDate bookingDate = booking.getNgayDat().toLocalDate();
            bookingCountByCustomer.merge(customerId, 1, Integer::sum);
            firstBookingByCustomer.merge(customerId, bookingDate, (oldValue, newValue) -> oldValue.isBefore(newValue) ? oldValue : newValue);
            lastBookingByCustomer.merge(customerId, bookingDate, (oldValue, newValue) -> oldValue.isAfter(newValue) ? oldValue : newValue);
        }

        YearMonth currentMonth = YearMonth.now();
        long newCustomersThisMonth = firstBookingByCustomer.values().stream()
                .filter(date -> YearMonth.from(date).equals(currentMonth))
                .count();
        long frequentCustomers = bookingCountByCustomer.values().stream()
                .filter(count -> count >= 5)
                .count();

        List<TopCustomerStat> topCustomers = bookingCountByCustomer.entrySet().stream()
                .map(entry -> {
                    KhachHang customer = customerById.get(entry.getKey());
                    String displayCode = customer != null ? customer.getMaKhachHang() : entry.getKey();
                    String name = customer != null ? customer.getTenKhachHang() : "Khách hàng";
                    String phone = customer != null ? customer.getSoDienThoai() : "";
                    LocalDate lastBooking = lastBookingByCustomer.get(entry.getKey());
                    String lastLabel = lastBooking != null
                            ? lastBooking.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            : "--";
                    return new TopCustomerStat(displayCode, name, phone, entry.getValue(), lastLabel);
                })
                .sorted(Comparator.comparingInt(TopCustomerStat::bookingCount).reversed()
                        .thenComparing(TopCustomerStat::customerName))
                .limit(10)
                .toList();

        LinkedHashMap<String, Number> monthlyCounts = new LinkedHashMap<>();
        YearMonth startMonth = currentMonth.minusMonths(5);
        for (int i = 0; i < 6; i++) {
            YearMonth month = startMonth.plusMonths(i);
            long uniqueCustomers = firstBookingByCustomer.entrySet().stream()
                    .filter(entry -> {
                        YearMonth ym = YearMonth.from(entry.getValue());
                        return ym.equals(month);
                    })
                    .count();
            monthlyCounts.put("T" + month.getMonthValue(), uniqueCustomers);
        }

        return new CustomerStatsSnapshot(safeCustomers.size(), (int) newCustomersThisMonth, (int) frequentCustomers, topCustomers, monthlyCounts);
    }

    private void applySnapshot(CustomerStatsSnapshot snapshot) {
        if (snapshot == null) {
            showEmptyState("Không thể tải thống kê khách hàng", "Snapshot khách hàng đang rỗng.");
            return;
        }

        lblTotal.setText(String.valueOf(snapshot.totalCustomers));
        lblNewMonth.setText(String.valueOf(snapshot.newCustomers));
        lblFrequent.setText(String.valueOf(snapshot.frequentCustomers));

        allTopCustomerItems.clear();
        allTopCustomerItems.addAll(snapshot.topCustomers);
        applySearchFilter();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        snapshot.monthlyCustomers.forEach((label, value) -> series.getData().add(new XYChart.Data<>(label, value)));
        barChart.setData(FXCollections.observableArrayList(series));

        boolean hasChartData = snapshot.monthlyCustomers.values().stream().mapToLong(Number::longValue).sum() > 0;
        barChart.setVisible(hasChartData);
        barChart.setManaged(hasChartData);
        Node placeholder = chartContainer.getChildren().get(1);
        placeholder.setVisible(!hasChartData);
        placeholder.setManaged(!hasChartData);

        lblChartHint.setText("Tổng cộng " + snapshot.totalCustomers + " khách | " + snapshot.newCustomers + " khách mới trong tháng này");

        Platform.runLater(() -> {
            for (XYChart.Series<String, Number> chartSeries : barChart.getData()) {
                for (XYChart.Data<String, Number> data : chartSeries.getData()) {
                    if (data.getNode() != null) {
                        Tooltip.install(data.getNode(), new Tooltip(data.getXValue() + ": " + data.getYValue() + " khach"));
                    }
                }
            }
        });
    }

    private void applySearchFilter() {
        String keyword = txtSearch == null ? "" : txtSearch.getText().trim().toLowerCase(Locale.ROOT);
        List<TopCustomerStat> filtered = allTopCustomerItems.stream()
                .filter(item -> keyword.isBlank()
                        || item.customerCode.toLowerCase(Locale.ROOT).contains(keyword)
                        || item.customerName.toLowerCase(Locale.ROOT).contains(keyword)
                        || item.phone.toLowerCase(Locale.ROOT).contains(keyword))
                .toList();
        topCustomerItems.setAll(filtered);
    }

    private void showEmptyState(String title, String subtitle) {
        lblTotal.setText("--");
        lblNewMonth.setText("--");
        lblFrequent.setText("--");
        lblChartHint.setText(subtitle);
        allTopCustomerItems.clear();
        topCustomerItems.clear();
        barChart.setData(FXCollections.observableArrayList());
        Node placeholder = chartContainer.getChildren().get(1);
        if (placeholder instanceof VBox box) {
            ((Label) box.getChildren().get(1)).setText(title);
            ((Label) box.getChildren().get(2)).setText(subtitle);
        }
        barChart.setVisible(false);
        barChart.setManaged(false);
        placeholder.setVisible(true);
        placeholder.setManaged(true);
    }

    private VBox createEmptyState(String title, String subtitle) {
        VBox empty = new VBox(8);
        empty.getStyleClass().addAll("empty-state", "stats-chart-placeholder");
        empty.setAlignment(Pos.CENTER);
        Label icon = new Label("\uD83D\uDCCA");
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

    private void animateEntrance() {
        for (int i = 0; i < root.getChildren().size(); i++) {
            Node node = root.getChildren().get(i);
            node.setOpacity(0);
            node.setTranslateY(18);

            FadeTransition fade = new FadeTransition(Duration.millis(360), node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(Duration.millis(i * 90L));

            TranslateTransition slide = new TranslateTransition(Duration.millis(360), node);
            slide.setFromY(18);
            slide.setToY(0);
            slide.setDelay(Duration.millis(i * 90L));

            fade.play();
            slide.play();
        }
    }

    private String stylesheetUri(String fileName) {
        return Paths.get("src", "gui", fileName).toAbsolutePath().toUri().toString();
    }

    private record TopCustomerStat(String customerCode, String customerName, String phone, int bookingCount, String lastBookingLabel) {
    }

    private record CustomerStatsSnapshot(
            int totalCustomers,
            int newCustomers,
            int frequentCustomers,
            List<TopCustomerStat> topCustomers,
            LinkedHashMap<String, Number> monthlyCustomers
    ) {
    }
}
