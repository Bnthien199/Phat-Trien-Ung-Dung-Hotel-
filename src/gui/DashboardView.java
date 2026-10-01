package gui;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.chart.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.util.Duration;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import dao.PhongDAO;
import dao.PhieuDatPhongDAO;
import dao.HoaDonDAO;
import entity.Phong;
import entity.PhieuDatPhong;
import connectDB.ConnectDB;
import util.BookingStatus;
import util.RoomStatus;
import util.StatusBadge;
import util.StatusUtils;

public class DashboardView {

    private static final String BG = "#f5f7fb";
    private static final String TEXT_PRIMARY = "#111827";
    private static final String TEXT_SECONDARY = "#334155";
    private static final String TEXT_MUTED = "#334155";
    private static final String BORDER_COLOR = "#e2e8f0";
    private static final String PRIMARY = "#0d9488";

    private final DateTimeFormatter dtfShort = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Dynamic stats components
    private Label valRevenue, valBookings, valOccupancyRate, valServiceRevenue;
    private Label lblRevenueSub, lblBookingsSub, lblOccupancySub, lblServiceSub;
    private StackPane badgeRevenue, badgeBookings, badgeOccupancy, badgeService;

    // Charts components
    private BarChart<String, Number> barChart;
    private PieChart pieChart;
    private Button btn2026, btn2025, btnOtherYear;

    // Data lists
    private final List<PhieuDatPhong> listCheckIn = new ArrayList<>();
    private final List<PhieuDatPhong> listCheckOut = new ArrayList<>();
    private final List<PhieuDatPhong> listOccupied = new ArrayList<>();

    // Smart search cache
    private final List<PhieuDatPhong> allBookings = new ArrayList<>();
    private final List<Phong> allRoomsList = new ArrayList<>();

    private TableView<PhieuDatPhong> mainTable;
    private final List<Button> tabButtons = new ArrayList<>();
    private String currentTab = "checkin";
    private TextField txtSearch;
    private Label lblSearchStatus;
    private DatePicker datePicker;
    private VBox root;

    // Navigator callback
    private Consumer<String> navigator;

    public void setNavigator(Consumer<String> navigator) {
        this.navigator = navigator;
    }

    // ═══════════════════════════════════════════════════════════════
    // BUILD VIEW
    // ═══════════════════════════════════════════════════════════════

    public Node createView() {
        root = new VBox(0);
        root.getStyleClass().add("dashboard-root");
        
        // Load external stylesheet
        try {
            String cssPath = Paths.get("src", "gui", "dashboard.css").toAbsolutePath().toUri().toString();
            root.getStylesheets().add(cssPath);
        } catch (Exception ex) {
            System.err.println("Could not load dashboard.css: " + ex.getMessage());
        }

        root.getChildren().add(createPageHeader());

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(24);
        content.setPadding(new Insets(8, 24, 28, 24));
        content.setStyle("-fx-background-color: transparent;");

        // Add stats, charts and table
        content.getChildren().addAll(createStatRow(), createChartRow(), createTabSection());

        scroll.setContent(content);
        root.getChildren().add(scroll);

        playEntranceAnimation();
        
        // Load default year chart and refresh overall stats
        loadMonthlyRevenueChart(2026);
        refreshData();
        
        return root;
    }

    // ═══════════════════════════════════════════════════════════════
    // TITLE & TOOLBAR
    // ═══════════════════════════════════════════════════════════════

    private HBox createPageHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(24, 24, 16, 24));

        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(48, 48);
        iconWrap.getStyleClass().add("header-icon-wrapper");
        
        Label icon = new Label("🏠");
        icon.setFont(Font.font("Segoe UI Emoji", 20));
        iconWrap.getChildren().add(icon);

        VBox titleBox = new VBox(4);
        Label title = new Label("Tổng quan");
        title.getStyleClass().add("page-title");

        Label sub = new Label("Tổng quan tình hình đặt phòng, doanh thu và vận hành của khách sạn.");
        sub.getStyleClass().add("page-subtitle");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Modern DatePicker
        datePicker = new DatePicker(LocalDate.now());
        datePicker.getStyleClass().add("dashboard-date-picker");
        datePicker.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");
        datePicker.setPrefWidth(90);
        
        datePicker.setConverter(new javafx.util.StringConverter<LocalDate>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            @Override
            public String toString(LocalDate date) {
                return date != null ? formatter.format(date) : "";
            }
            @Override
            public LocalDate fromString(String string) {
                return (string != null && !string.isEmpty()) ? LocalDate.parse(string, formatter) : null;
            }
        });
        
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) {
                refreshData();
                loadMonthlyRevenueChart(newDate.getYear());
                updateYearButtonsState(newDate.getYear());
            }
        });
        
        HBox datePickerBox = new HBox(datePicker);
        datePickerBox.setAlignment(Pos.CENTER);
        datePickerBox.setPrefHeight(44);
        datePickerBox.setStyle(
                "-fx-background-color: white;"
                        + "-fx-padding: 0 12;"
                        + "-fx-background-radius: 12;"
                        + "-fx-border-color: " + BORDER_COLOR + ";"
                        + "-fx-border-radius: 12;"
                        + "-fx-border-width: 1.5;");

        Button btnRefresh = new Button("↻  Làm mới");
        btnRefresh.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        btnRefresh.setPrefHeight(44);
        String btnNormal = "-fx-background-color: white;"
                + "-fx-text-fill: " + TEXT_PRIMARY + ";"
                + "-fx-border-color: " + BORDER_COLOR + ";"
                + "-fx-border-radius: 12; -fx-background-radius: 12;"
                + "-fx-border-width: 1.5;"
                + "-fx-cursor: hand; -fx-padding: 0 18;";
        String btnHover = "-fx-background-color: #f1f5f9;"
                + "-fx-text-fill: " + TEXT_PRIMARY + ";"
                + "-fx-border-color: #cbd5e1;"
                + "-fx-border-radius: 12; -fx-background-radius: 12;"
                + "-fx-border-width: 1.5;"
                + "-fx-cursor: hand; -fx-padding: 0 18;";
        btnRefresh.setStyle(btnNormal);
        btnRefresh.setOnMouseEntered(e -> {
            if (!btnRefresh.isDisabled())
                btnRefresh.setStyle(btnHover);
        });
        btnRefresh.setOnMouseExited(e -> {
            if (!btnRefresh.isDisabled())
                btnRefresh.setStyle(btnNormal);
        });
        btnRefresh.setOnAction(e -> {
            btnRefresh.setDisable(true);
            btnRefresh.setText("⏳  Đang tải...");
            resetRefreshFilters();
            refreshData();
            new Thread(() -> {
                try {
                    Thread.sleep(800);
                } catch (InterruptedException ignored) {
                }
                javafx.application.Platform.runLater(() -> {
                    btnRefresh.setText("↻  Làm mới");
                    btnRefresh.setStyle(btnNormal);
                    btnRefresh.setDisable(false);
                });
            }).start();
        });

        HBox controls = new HBox(12, datePickerBox, btnRefresh);
        controls.setAlignment(Pos.CENTER_RIGHT);

        header.getChildren().addAll(iconWrap, titleBox, spacer, controls);
        return header;
    }

    // ═══════════════════════════════════════════════════════════════
    // SMART SEARCH
    // ═══════════════════════════════════════════════════════════════

    private void triggerSmartSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            clearSearchStatus();
            return;
        }
        String raw = keyword.trim();
        String kw = raw.toLowerCase(Locale.ROOT);

        String normalizedRoom = normalizeRoomCode(kw);

        if (kw.startsWith("dp")) {
            boolean found = allBookings.stream().anyMatch(p -> p.getMaDatPhong() != null
                    && p.getMaDatPhong().toLowerCase().contains(kw));
            if (found) {
                showSearchStatus("✓ Tìm thấy phiếu đặt — chuyển sang Danh sách đặt phòng", false);
                if (navigator != null)
                    navigator.accept("BookingList");
                return;
            }
            showSearchStatus("Không tìm thấy phiếu \"" + raw + "\"", true);
            return;
        }

        if (looksLikeRoom(kw)) {
            boolean found = allRoomsList.stream()
                    .anyMatch(p -> p.getMaPhong() != null && (p.getMaPhong().toLowerCase().contains(kw)
                            || p.getMaPhong().equalsIgnoreCase(normalizedRoom)));
            if (found) {
                showSearchStatus("✓ Tìm thấy phòng " + normalizedRoom + " — chuyển sang Quản lý phòng", false);
                if (navigator != null)
                    navigator.accept("Room");
                return;
            }
            showSearchStatus("Không tìm thấy phòng \"" + normalizedRoom + "\"", true);
            return;
        }

        boolean foundCustomer = allBookings.stream().anyMatch(p -> p.getKhachHang() != null
                && p.getKhachHang().getTenKhachHang() != null
                && p.getKhachHang().getTenKhachHang().toLowerCase().contains(kw));

        if (foundCustomer) {
            showSearchStatus("✓ Tìm thấy khách hàng — chuyển sang Danh sách khách hàng", false);
            if (navigator != null)
                navigator.accept("Customer");
            return;
        }

        showSearchStatus("Không tìm thấy dữ liệu cho \"" + raw + "\"", true);
    }

    private boolean looksLikeRoom(String kw) {
        if (kw.matches("\\d+"))
            return true;
        if (kw.matches("p\\d+"))
            return true;
        return false;
    }

    private String normalizeRoomCode(String kw) {
        if (kw.matches("\\d+"))
            return "P" + kw.toUpperCase();
        if (kw.matches("p\\d+"))
            return kw.toUpperCase();
        return kw.toUpperCase();
    }

    private void showSearchStatus(String message, boolean isError) {
        lblSearchStatus.setText(message);
        lblSearchStatus.setTextFill(isError ? Color.web("#ef4444") : Color.web("#10b981"));
        lblSearchStatus.setVisible(true);
        lblSearchStatus.setManaged(true);
    }

    private void clearSearchStatus() {
        lblSearchStatus.setText("");
        lblSearchStatus.setVisible(false);
        lblSearchStatus.setManaged(false);
    }

    // ═══════════════════════════════════════════════════════════════
    // 4 STATISTICS CARDS
    // ═══════════════════════════════════════════════════════════════

    private HBox createStatRow() {
        valRevenue = new Label("—");
        valBookings = new Label("—");
        valOccupancyRate = new Label("—");
        valServiceRevenue = new Label("—");

        lblRevenueSub = new Label("Tháng hiện tại");
        lblBookingsSub = new Label("Lượt đặt tháng này");
        lblOccupancySub = new Label("Công suất phòng");
        lblServiceSub = new Label("Doanh thu F&B tháng này");

        badgeRevenue = new StackPane();
        badgeBookings = new StackPane();
        badgeOccupancy = new StackPane();
        badgeService = new StackPane();

        HBox row = new HBox(20);
        row.getChildren().addAll(
                buildStatCard("TỔNG DOANH THU", "₫", valRevenue, lblRevenueSub, badgeRevenue, "#0ea5e9", "#eff6ff"),
                buildStatCard("SỐ PHÒNG ĐÃ ĐẶT", "🏢", valBookings, lblBookingsSub, badgeBookings, "#8b5cf6", "#f5f3ff"),
                buildStatCard("TỶ LỆ LẤP PHÒNG", "📈", valOccupancyRate, lblOccupancySub, badgeOccupancy, "#10b981", "#f0fdfa"),
                buildStatCard("DOANH THU F&B", "🍽", valServiceRevenue, lblServiceSub, badgeService, "#f59e0b", "#fff7ed")
        );
        for (Node n : row.getChildren())
            HBox.setHgrow(n, Priority.ALWAYS);
            
        return row;
    }

    private VBox buildStatCard(String title, String icon, Label valueLabel, Label subLabel, StackPane badgePercent,
                               String accentColor, String iconBgColor) {
        VBox card = new VBox(8);
        card.getStyleClass().add("dashboard-card");
        card.setAlignment(Pos.TOP_LEFT);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        StackPane iconBg = new StackPane();
        iconBg.setPrefSize(42, 42);
        iconBg.setStyle("-fx-background-color: " + iconBgColor + "; -fx-background-radius: 12px;");
        Label iconL = new Label(icon);
        iconL.setFont(Font.font("Segoe UI Symbol", FontWeight.BOLD, 17));
        iconL.setTextFill(Color.web(accentColor));
        iconBg.getChildren().add(iconL);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topRow.getChildren().addAll(iconBg, spacer, badgePercent);

        Label titleL = new Label(title);
        titleL.getStyleClass().add("stat-card-title");

        valueLabel.getStyleClass().add("stat-card-value");
        subLabel.getStyleClass().add("stat-card-subtitle");

        card.getChildren().addAll(topRow, titleL, valueLabel, subLabel);
        return card;
    }

    private void updatePercentBadge(StackPane badgePane, double val) {
        badgePane.getChildren().clear();
        Label lbl = new Label();
        lbl.getStyleClass().add("badge-percent");
        if (val > 0) {
            lbl.setText(String.format(Locale.ROOT, "+%.1f%%", val));
            lbl.getStyleClass().add("badge-percent-up");
        } else if (val < 0) {
            lbl.setText(String.format(Locale.ROOT, "%.1f%%", val));
            lbl.getStyleClass().add("badge-percent-down");
        } else {
            lbl.setText("0.0%");
            lbl.getStyleClass().add("badge-percent-neutral");
        }
        badgePane.getChildren().add(lbl);
    }

    // ═══════════════════════════════════════════════════════════════
    // INTERACTIVE CHARTS ROW (60/40)
    // ═══════════════════════════════════════════════════════════════

    private HBox createChartRow() {
        HBox row = new HBox(24); // Gap of 24px between the 2 cards
        row.setAlignment(Pos.CENTER);

        // LEFT CARD: MONTHLY REVENUE COLUMN CHART (revenueChartCard)
        VBox revenueChartCard = new VBox(16);
        revenueChartCard.getStyleClass().add("dashboard-card");
        HBox.setHgrow(revenueChartCard, Priority.ALWAYS);

        HBox leftHeader = new HBox(12);
        leftHeader.setAlignment(Pos.CENTER_LEFT);
        
        // Large title and small gray subtitle
        VBox leftTitleBox = new VBox(4);
        Label lblLeftTitle = new Label("Doanh thu theo tháng");
        lblLeftTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblLeftTitle.setTextFill(Color.web(TEXT_PRIMARY));

        Label lblLeftSub = new Label("Biểu đồ cột (thực thu hóa đơn, triệu đồng)");
        lblLeftSub.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        lblLeftSub.setTextFill(Color.web(TEXT_SECONDARY));
        leftTitleBox.getChildren().addAll(lblLeftTitle, lblLeftSub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Buttons group
        btn2026 = new Button("2026");
        btn2026.getStyleClass().addAll("btn-pill", "btn-pill-active");
        btn2026.setOnAction(e -> {
            setActiveYearButton(btn2026);
            loadMonthlyRevenueChart(2026);
        });

        btn2025 = new Button("2025");
        btn2025.getStyleClass().addAll("btn-pill", "btn-pill-inactive");
        btn2025.setOnAction(e -> {
            setActiveYearButton(btn2025);
            loadMonthlyRevenueChart(2025);
        });

        btnOtherYear = new Button("Năm khác");
        btnOtherYear.getStyleClass().addAll("btn-pill", "btn-pill-inactive");
        btnOtherYear.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("2024");
            dialog.setTitle("Chọn năm thống kê");
            dialog.setHeaderText("Nhập năm cần xem doanh thu:");
            dialog.setContentText("Năm:");
            dialog.showAndWait().ifPresent(yearStr -> {
                try {
                    int y = Integer.parseInt(yearStr.trim());
                    btnOtherYear.setText(String.valueOf(y));
                    setActiveYearButton(btnOtherYear);
                    loadMonthlyRevenueChart(y);
                } catch (NumberFormatException ex) {
                    // Ignore invalid numbers
                }
            });
        });

        Button btnViewReport = new Button("Xem báo cáo");
        btnViewReport.getStyleClass().addAll("btn-pill", "btn-pill-inactive");
        btnViewReport.setOnAction(e -> {
            if (navigator != null) navigator.accept("RevenueStats");
        });

        Button btnChartRefresh = new Button("Làm mới");
        btnChartRefresh.getStyleClass().addAll("btn-pill", "btn-pill-inactive");
        btnChartRefresh.setOnAction(e -> {
            int selectedYear = 2026;
            if (btn2025.getStyleClass().contains("btn-pill-active")) {
                selectedYear = 2025;
            } else if (btnOtherYear.getStyleClass().contains("btn-pill-active")) {
                try {
                    selectedYear = Integer.parseInt(btnOtherYear.getText());
                } catch (NumberFormatException ignored) {}
            }
            loadMonthlyRevenueChart(selectedYear);
        });

        // Pill buttons spaced evenly in their own HBox container
        HBox filterButtonGroup = new HBox(8);
        filterButtonGroup.setAlignment(Pos.CENTER_RIGHT);
        filterButtonGroup.getChildren().addAll(btn2026, btn2025, btnOtherYear, btnViewReport, btnChartRefresh);

        leftHeader.getChildren().addAll(leftTitleBox, spacer, filterButtonGroup);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Tháng trong năm");
        xAxis.setCategories(FXCollections.observableArrayList(
            "T1", "T2", "T3", "T4", "T5", "T6", "T7", "T8", "T9", "T10", "T11", "T12"
        ));
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Doanh thu (triệu đồng)");
        
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(true);
        barChart.setLegendSide(Side.BOTTOM);
        barChart.setAnimated(false);
        barChart.setHorizontalGridLinesVisible(true);
        barChart.setVerticalGridLinesVisible(false);
        // Smaller height to look sleek and balanced
        barChart.setMinHeight(200);
        barChart.setPrefHeight(220);
        barChart.setMaxHeight(240);
        // Set bar/category gaps to distribute columns beautifully
        barChart.setBarGap(0);
        barChart.setCategoryGap(20);
        barChart.setPadding(new Insets(10, 16, 10, 16));

        revenueChartCard.getChildren().addAll(leftHeader, barChart);

        // RIGHT CARD: PIE CHART (occupancyChartCard)
        VBox occupancyChartCard = new VBox(16);
        occupancyChartCard.getStyleClass().add("dashboard-card");
        
        // Large title and small gray subtitle
        VBox rightTitleBox = new VBox(4);
        Label lblRightTitle = new Label("Công suất phòng");
        lblRightTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        lblRightTitle.setTextFill(Color.web(TEXT_PRIMARY));

        Label lblRightSub = new Label("Tỷ trọng loại phòng trong hóa đơn đã thanh toán");
        lblRightSub.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        lblRightSub.setTextFill(Color.web(TEXT_SECONDARY));
        rightTitleBox.getChildren().addAll(lblRightTitle, lblRightSub);

        pieChart = new PieChart();
        pieChart.setLegendSide(Side.BOTTOM); // Legend below chart
        pieChart.setLabelsVisible(false); // Hide outer labels to avoid layout distortion and text overlap
        pieChart.setAnimated(false);
        // Equal height with bar chart to align perfectly
        pieChart.setMinHeight(200);
        pieChart.setPrefHeight(220);
        pieChart.setMaxHeight(240);

        occupancyChartCard.getChildren().addAll(rightTitleBox, pieChart);

        row.getChildren().addAll(revenueChartCard, occupancyChartCard);

        // Dynamic 70% / 30% width binding with spacing offset to avoid scrollbars
        revenueChartCard.prefWidthProperty().bind(root.widthProperty().subtract(72).multiply(0.7));
        occupancyChartCard.prefWidthProperty().bind(root.widthProperty().subtract(72).multiply(0.3));

        return row;
    }

    private void setActiveYearButton(Button activeBtn) {
        List<Button> btns = List.of(btn2026, btn2025, btnOtherYear);
        for (Button b : btns) {
            b.getStyleClass().removeAll("btn-pill-active", "btn-pill-inactive");
            if (b == activeBtn) {
                b.getStyleClass().add("btn-pill-active");
            } else {
                b.getStyleClass().add("btn-pill-inactive");
            }
        }
    }

    private void updateYearButtonsState(int year) {
        if (year == 2026) {
            setActiveYearButton(btn2026);
            btnOtherYear.setText("Năm khác");
        } else if (year == 2025) {
            setActiveYearButton(btn2025);
            btnOtherYear.setText("Năm khác");
        } else {
            btnOtherYear.setText(String.valueOf(year));
            setActiveYearButton(btnOtherYear);
        }
    }

    private void loadMonthlyRevenueChart(int year) {
        Task<List<HoaDonDAO.RevenueChartData>> task = new Task<>() {
            @Override
            protected List<HoaDonDAO.RevenueChartData> call() {
                LocalDate from = LocalDate.of(year, 1, 1);
                LocalDate to = LocalDate.of(year, 12, 31);
                return new HoaDonDAO().getRevenueChartData(from, to, HoaDonDAO.RevenuePeriod.MONTH);
            }
        };

        task.setOnSucceeded(e -> {
            List<HoaDonDAO.RevenueChartData> data = task.getValue();
            Platform.runLater(() -> {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName("Doanh Thu");

                double[] monthlyRevenue = new double[12];
                for (HoaDonDAO.RevenueChartData d : data) {
                    try {
                        String[] parts = d.getLabel().split("/");
                        int monthIdx = Integer.parseInt(parts[0]) - 1;
                        if (monthIdx >= 0 && monthIdx < 12) {
                            monthlyRevenue[monthIdx] = d.getRevenue();
                        }
                    } catch (Exception ex) {
                        // ignore parsing error
                    }
                }

                for (int m = 0; m < 12; m++) {
                    String monthLabel = "T" + (m + 1);
                    XYChart.Data<String, Number> item = new XYChart.Data<>(monthLabel, monthlyRevenue[m] / 1_000_000.0);
                    series.getData().add(item);
                }

                barChart.getData().setAll(series);
            });
        });

        new Thread(task).start();
    }

    // ═══════════════════════════════════════════════════════════════
    // MODERN DATA TABLE & TABS
    // ═══════════════════════════════════════════════════════════════

    private VBox createTabSection() {
        Button btnIn = createTabButton("📥  Phòng đến", "checkin");
        Button btnOut = createTabButton("📤  Phòng đi", "checkout");
        Button btnOcc = createTabButton("🛏   Đang sử dụng", "occupied");
        tabButtons.addAll(List.of(btnIn, btnOut, btnOcc));

        // Create integrated search box on the right of tabs
        Label searchIcon = new Label("🔍");
        searchIcon.setFont(Font.font(14));
        searchIcon.setTextFill(Color.web(TEXT_MUTED));
        searchIcon.setPadding(new Insets(0, 0, 0, 12));

        txtSearch = new TextField();
        txtSearch.setPromptText("Tìm kiếm mã phiếu, số phòng...");
        txtSearch.setPrefWidth(260);
        txtSearch.setStyle(
                "-fx-background-color: transparent;"
                        + "-fx-border-color: transparent;"
                        + "-fx-font-size: 13;"
                        + "-fx-padding: 0;"
                        + "-fx-prompt-text-fill: " + TEXT_MUTED + ";");

        HBox searchBox = new HBox(6, searchIcon, txtSearch);
        searchBox.setAlignment(Pos.CENTER_LEFT);
        searchBox.setPrefHeight(40);
        searchBox.setStyle(
                "-fx-background-color: white;"
                        + "-fx-background-radius: 10;"
                        + "-fx-border-color: " + BORDER_COLOR + ";"
                        + "-fx-border-radius: 10;"
                        + "-fx-border-width: 1.5;");

        lblSearchStatus = new Label();
        lblSearchStatus.setFont(Font.font("Segoe UI", 11.5));
        lblSearchStatus.setVisible(false);
        lblSearchStatus.setManaged(false);
        lblSearchStatus.setPadding(new Insets(0, 8, 0, 4));

        txtSearch.textProperty().addListener((obs, oldV, newV) -> {
            applySearch(newV);
            if (newV == null || newV.trim().isEmpty())
                clearSearchStatus();
        });

        txtSearch.setOnAction(e -> triggerSmartSearch(txtSearch.getText()));

        Region tabBarSpacer = new Region();
        HBox.setHgrow(tabBarSpacer, Priority.ALWAYS);

        HBox tabBar = new HBox(12, btnIn, btnOut, btnOcc, tabBarSpacer, searchBox, lblSearchStatus);
        tabBar.setAlignment(Pos.CENTER_LEFT);

        mainTable = new TableView<>();
        mainTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        mainTable.setPlaceholder(createEmptyPlaceholder("Không có dữ liệu"));
        mainTable.getStyleClass().add("modern-table");
        
        setupMainTable();

        VBox header = new VBox(0);
        header.setPadding(new Insets(0, 0, 16, 0));
        header.getChildren().add(tabBar);

        VBox card = new VBox(0);
        card.getStyleClass().add("dashboard-card");
        card.getChildren().addAll(header, mainTable);

        applyTabActiveStyle(btnIn);
        currentTab = "checkin";

        VBox wrapper = new VBox(card);
        VBox.setVgrow(wrapper, Priority.ALWAYS);
        return wrapper;
    }

    private Button createTabButton(String text, String tabKey) {
        Button btn = new Button(text);
        btn.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 13));
        btn.setPrefHeight(40);
        btn.setUserData(tabKey);
        applyTabInactiveStyle(btn);
        btn.setOnAction(e -> {
            tabButtons.forEach(this::applyTabInactiveStyle);
            applyTabActiveStyle(btn);
            currentTab = tabKey;
            applySearch(txtSearch != null ? txtSearch.getText() : "");
        });
        return btn;
    }

    private void applyTabActiveStyle(Button btn) {
        btn.setStyle(
                "-fx-background-color: " + PRIMARY + "; -fx-text-fill: white;"
                        + "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 0 20;");
    }

    private void applyTabInactiveStyle(Button btn) {
        btn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: " + TEXT_SECONDARY + ";"
                        + "-fx-background-radius: 10; -fx-cursor: hand; -fx-padding: 0 20;");
    }

    private void applySearch(String keyword) {
        List<PhieuDatPhong> source = switch (currentTab) {
            case "checkout" -> listCheckOut;
            case "occupied" -> listOccupied;
            default -> listCheckIn;
        };

        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        List<PhieuDatPhong> filtered = kw.isBlank()
                ? new ArrayList<>(source)
                : source.stream()
                        .filter(p -> (p.getMaDatPhong() != null && p.getMaDatPhong().toLowerCase().contains(kw))
                                || (p.getKhachHang() != null && p.getKhachHang().getTenKhachHang() != null
                                        && p.getKhachHang().getTenKhachHang().toLowerCase().contains(kw))
                                || (p.getDsMaPhong() != null && p.getDsMaPhong().toLowerCase().contains(kw)))
                        .collect(Collectors.toList());

        if (mainTable != null)
            mainTable.getItems().setAll(filtered);
    }

    private void resetRefreshFilters() {
        if (txtSearch != null) {
            txtSearch.clear();
        }
        if (datePicker != null) {
            datePicker.setValue(LocalDate.now());
        }
        clearSearchStatus();
    }

    private void setupMainTable() {
        TableColumn<PhieuDatPhong, String> colStt = new TableColumn<>("STT");
        colStt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });
        colStt.setStyle("-fx-alignment: CENTER;");
        colStt.setPrefWidth(60);
        colStt.setMinWidth(60);
        colStt.setMaxWidth(60);

        TableColumn<PhieuDatPhong, String> colMa = new TableColumn<>("Mã phiếu");
        colMa.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getMaDatPhong()));
        colMa.setStyle("-fx-alignment: CENTER;");
        colMa.setPrefWidth(140);
        colMa.setMinWidth(140);
        colMa.setMaxWidth(140);

        TableColumn<PhieuDatPhong, String> colKhach = new TableColumn<>("Khách hàng");
        colKhach.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getKhachHang() != null ? d.getValue().getKhachHang().getTenKhachHang() : "—"));
        colKhach.setStyle("-fx-alignment: CENTER-LEFT;");
        colKhach.setPrefWidth(240);
        colKhach.setMinWidth(200);

        TableColumn<PhieuDatPhong, String> colPhong = new TableColumn<>("Số phòng");
        colPhong.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getDsMaPhong() != null ? d.getValue().getDsMaPhong() : "—"));
        colPhong.setStyle("-fx-alignment: CENTER;");
        colPhong.setPrefWidth(120);
        colPhong.setMinWidth(120);
        colPhong.setMaxWidth(120);

        TableColumn<PhieuDatPhong, String> colSoLuong = new TableColumn<>("SL");
        colSoLuong.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(d.getValue().getSoLuongPhong())));
        colSoLuong.setStyle("-fx-alignment: CENTER;");
        colSoLuong.setPrefWidth(70);
        colSoLuong.setMinWidth(70);
        colSoLuong.setMaxWidth(70);

        TableColumn<PhieuDatPhong, String> colNgayDen = new TableColumn<>("Ngày đến");
        colNgayDen.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getNgayNhan() != null ? d.getValue().getNgayNhan().format(dtfShort) : "—"));
        colNgayDen.setStyle("-fx-alignment: CENTER;");
        colNgayDen.setPrefWidth(160);
        colNgayDen.setMinWidth(160);
        colNgayDen.setMaxWidth(160);

        TableColumn<PhieuDatPhong, String> colNgayDi = new TableColumn<>("Ngày đi");
        colNgayDi.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
                d.getValue().getNgayTra() != null ? d.getValue().getNgayTra().format(dtfShort) : "—"));
        colNgayDi.setStyle("-fx-alignment: CENTER;");
        colNgayDi.setPrefWidth(160);
        colNgayDi.setMinWidth(160);
        colNgayDi.setMaxWidth(160);

        TableColumn<PhieuDatPhong, String> colTT = new TableColumn<>("Trạng thái");
        colTT.setCellValueFactory(d -> {
            String status = d.getValue().getTrangThai();
            String displayName;
            if ("DA_THANH_TOAN".equalsIgnoreCase(status)) {
                displayName = "Đã thanh toán";
            } else if ("DA_TRA_PHONG".equalsIgnoreCase(status)) {
                displayName = "Đã trả phòng";
            } else if ("CHO_TRA_PHONG".equalsIgnoreCase(status)) {
                displayName = "Chờ trả phòng";
            } else {
                displayName = BookingStatus.fromCode(status).getDisplayName();
            }
            return new javafx.beans.property.SimpleStringProperty(displayName);
        });
        colTT.setStyle("-fx-alignment: CENTER;");
        
        // CUSTOM PILL BADGE FACTORY FOR STATUS
        colTT.setCellFactory(col -> new TableCell<PhieuDatPhong, String>() {
            private final Label badge = new Label();
            {
                badge.getStyleClass().add("badge-status");
                setAlignment(Pos.CENTER);
            }
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setText(item);
                    badge.getStyleClass().removeAll("badge-status-paid", "badge-status-pending", "badge-status-staying");
                    
                    String compact = item.trim().toLowerCase();
                    if (compact.contains("thanh toán") || compact.contains("hoàn tất") || compact.contains("hoàn thành") || compact.contains("paid") || compact.contains("da_thanh_toan") || compact.contains("trả phòng") || compact.contains("da_tra_phong")) {
                        badge.getStyleClass().add("badge-status-paid");
                    } else if (compact.contains("chờ") || compact.contains("đặt") || compact.contains("chơ") || compact.contains("pending") || compact.contains("cho_nhan")) {
                        badge.getStyleClass().add("badge-status-pending");
                    } else {
                        badge.getStyleClass().add("badge-status-staying");
                    }
                    setGraphic(badge);
                }
            }
        });
        
        colTT.setPrefWidth(160);
        colTT.setMinWidth(160);
        colTT.setMaxWidth(160);

        mainTable.getColumns().clear();
        mainTable.getColumns().addAll(colStt, colMa, colKhach, colPhong, colSoLuong, colNgayDen, colNgayDi, colTT);
    }

    private Label createEmptyPlaceholder(String text) {
        Label lbl = new Label(text);
        lbl.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14));
        lbl.setTextFill(Color.web("#111827"));

        Label icon = new Label("📭");
        icon.setFont(Font.font(24));
        icon.setTextFill(Color.web(PRIMARY));
        icon.setAlignment(Pos.CENTER);
        icon.setMinSize(56, 56);
        icon.setStyle("-fx-background-color: #f0fdfa; -fx-background-radius: 28;");

        VBox box = new VBox(12, icon, lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Label wrapper = new Label();
        wrapper.setGraphic(box);
        wrapper.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        wrapper.setAlignment(Pos.CENTER);
        wrapper.setMaxWidth(Double.MAX_VALUE);
        wrapper.setMaxHeight(Double.MAX_VALUE);
        return wrapper;
    }

    // ═══════════════════════════════════════════════════════════════
    // ANIMATIONS
    // ═══════════════════════════════════════════════════════════════

    private void playEntranceAnimation() {
        for (int i = 0; i < root.getChildren().size(); i++) {
            Node node = root.getChildren().get(i);
            node.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(350), node);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 80));
            ft.play();
        }
    }

    private void animateValue(Label label, String newValue) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(120), label);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0.3);
        fadeOut.setOnFinished(e -> {
            label.setText(newValue);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), label);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
    }

    // ═══════════════════════════════════════════════════════════════
    // DATABASE & REFRESH LOGIC
    // ═══════════════════════════════════════════════════════════════

    public void refreshData() {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    PhongDAO phongDAO = new PhongDAO();
                    PhieuDatPhongDAO pdpDAO = new PhieuDatPhongDAO();

                    List<Phong> allRooms = phongDAO.getAllPhong();
                    List<PhieuDatPhong> all = pdpDAO.getAllPhieuDatPhong();
                    if (all == null) all = new ArrayList<>();
                    if (allRooms == null) allRooms = new ArrayList<>();

                    LocalDate today = (datePicker != null && datePicker.getValue() != null) ? datePicker.getValue() : LocalDate.now();

                    // Calculate bookings dates
                    List<PhieuDatPhong> checkInToday = all.stream()
                            .filter(p -> p.getNgayNhan() != null
                                    && p.getNgayNhan().toLocalDate().equals(today))
                            .sorted((a, b) -> a.getNgayNhan().compareTo(b.getNgayNhan()))
                            .collect(Collectors.toList());

                    List<PhieuDatPhong> checkOutToday = pdpDAO.getDanhSachPhongDiTrongNgay(today);

                    List<PhieuDatPhong> occupied = all.stream()
                            .filter(p -> StatusUtils.isBookingStatus(p.getTrangThai(), BookingStatus.DA_NHAN_PHONG))
                            .collect(Collectors.toList());

                    // CALCULATE METRICS
                    // 1. Occupancy Rate
                    final long totalRoomsCount = allRooms.size();
                    final long occupiedRoomCount = allRooms.stream()
                            .filter(p -> StatusUtils.isRoomStatus(p.getTrangThai(), RoomStatus.DANG_O))
                            .count();
                    final double occupancyRate = totalRoomsCount == 0 ? 0.0 : (occupiedRoomCount * 100.0) / totalRoomsCount;

                    // 2. Booked Rooms (This Month & Last Month)
                    LocalDate startOfThisMonth = today.withDayOfMonth(1);
                    LocalDate startOfNextMonth = startOfThisMonth.plusMonths(1);
                    LocalDate startOfLastMonth = startOfThisMonth.minusMonths(1);

                    final long bookedRoomsThisMonth = all.stream()
                            .filter(p -> p.getNgayNhan() != null &&
                                         !p.getNgayNhan().toLocalDate().isBefore(startOfThisMonth) &&
                                         p.getNgayNhan().toLocalDate().isBefore(startOfNextMonth))
                            .mapToLong(p -> Math.max(1, p.getSoLuongPhong()))
                            .sum();

                    final long bookedRoomsLastMonth = all.stream()
                            .filter(p -> p.getNgayNhan() != null &&
                                         !p.getNgayNhan().toLocalDate().isBefore(startOfLastMonth) &&
                                         p.getNgayNhan().toLocalDate().isBefore(startOfThisMonth))
                            .mapToLong(p -> Math.max(1, p.getSoLuongPhong()))
                            .sum();

                    final double bookingsChange = bookedRoomsLastMonth == 0 ? 100.0 :
                            ((bookedRoomsThisMonth - bookedRoomsLastMonth) / (double) bookedRoomsLastMonth) * 100.0;

                    // 3. Database Revenue and Service Revenue Queries
                    double thisMonthRevenue = 0;
                    double thisMonthService = 0;
                    double lastMonthRevenue = 0;
                    double lastMonthService = 0;

                    String sqlRev = "SELECT COALESCE(SUM(hd.tongTienPhong + hd.tongTienDichVu), 0) AS total, " +
                                    "COALESCE(SUM(hd.tongTienDichVu), 0) AS service " +
                                    "FROM HoaDon hd " +
                                    "WHERE hd.ngayLap >= ? AND hd.ngayLap < ?";

                    try (Connection con = ConnectDB.getConnection()) {
                        // Current month
                        try (PreparedStatement stmt = con.prepareStatement(sqlRev)) {
                            stmt.setTimestamp(1, Timestamp.valueOf(startOfThisMonth.atStartOfDay()));
                            stmt.setTimestamp(2, Timestamp.valueOf(startOfNextMonth.atStartOfDay()));
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    thisMonthRevenue = rs.getDouble("total");
                                    thisMonthService = rs.getDouble("service");
                                }
                            }
                        }
                        // Last month
                        try (PreparedStatement stmt = con.prepareStatement(sqlRev)) {
                            stmt.setTimestamp(1, Timestamp.valueOf(startOfLastMonth.atStartOfDay()));
                            stmt.setTimestamp(2, Timestamp.valueOf(startOfThisMonth.atStartOfDay()));
                            try (ResultSet rs = stmt.executeQuery()) {
                                if (rs.next()) {
                                    lastMonthRevenue = rs.getDouble("total");
                                    lastMonthService = rs.getDouble("service");
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Error fetching revenue stats for dashboard: " + ex.getMessage());
                    }

                    final double finalThisMonthRevenue = thisMonthRevenue;
                    final double finalThisMonthService = thisMonthService;
                    final double revenueChange = lastMonthRevenue == 0 ? 100.0 :
                            ((thisMonthRevenue - lastMonthRevenue) / lastMonthRevenue) * 100.0;
                    final double serviceChange = lastMonthService == 0 ? 100.0 :
                            ((thisMonthService - lastMonthService) / lastMonthService) * 100.0;

                    // Pie Chart data variables
                    final long finalOccupiedCount = occupiedRoomCount;
                    final long finalVacantCount = allRooms.stream()
                            .filter(p -> StatusUtils.isRoomStatus(p.getTrangThai(), RoomStatus.TRONG))
                            .count();
                    final long finalOthersCount = totalRoomsCount - finalOccupiedCount - finalVacantCount;

                    final List<Phong> allRoomsFinal = allRooms;
                    final List<PhieuDatPhong> allFinal = all;

                    javafx.application.Platform.runLater(() -> {
                        listCheckIn.clear();
                        listCheckIn.addAll(checkInToday);
                        listCheckOut.clear();
                        listCheckOut.addAll(checkOutToday);
                        listOccupied.clear();
                        listOccupied.addAll(occupied);

                        allBookings.clear();
                        allBookings.addAll(allFinal);
                        allRoomsList.clear();
                        allRoomsList.addAll(allRoomsFinal);

                        // Populate stats labels
                        java.text.DecimalFormat df = new java.text.DecimalFormat("#,##0");
                        animateValue(valRevenue, df.format(finalThisMonthRevenue) + " ₫");
                        animateValue(valBookings, String.valueOf(bookedRoomsThisMonth) + " phòng");
                        animateValue(valOccupancyRate, String.format(Locale.ROOT, "%.1f%%", occupancyRate));
                        animateValue(valServiceRevenue, df.format(finalThisMonthService) + " ₫");

                        // Subtitles and badges
                        lblRevenueSub.setText("Tháng " + today.getMonthValue() + "/" + today.getYear());
                        lblBookingsSub.setText("Tổng lượt phòng tháng này");
                        lblOccupancySub.setText(occupiedRoomCount + " / " + totalRoomsCount + " phòng đang sử dụng");
                        lblServiceSub.setText("Tháng " + today.getMonthValue() + "/" + today.getYear());

                        updatePercentBadge(badgeRevenue, revenueChange);
                        updatePercentBadge(badgeBookings, bookingsChange);
                        updatePercentBadge(badgeService, serviceChange);
                        
                        // Occupancy badge compared to standard 65% benchmark
                        updatePercentBadge(badgeOccupancy, occupancyRate - 65.0);

                        // Update PieChart
                        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                                new PieChart.Data("Đang ở (" + finalOccupiedCount + ")", finalOccupiedCount),
                                new PieChart.Data("Trống (" + finalVacantCount + ")", finalVacantCount),
                                new PieChart.Data("Khác (" + finalOthersCount + ")", finalOthersCount)
                        );
                        pieChart.setData(pieData);

                        // Style PieChart slices
                        pieChart.getData().forEach(d -> {
                            Node node = d.getNode();
                            if (node != null) {
                                if (d.getName().startsWith("Đang ở")) {
                                    node.setStyle("-fx-pie-color: #3b82f6;"); // Blue
                                } else if (d.getName().startsWith("Trống")) {
                                    node.setStyle("-fx-pie-color: #10b981;"); // Green
                                } else {
                                    node.setStyle("-fx-pie-color: #f59e0b;"); // Orange
                                }
                            }
                        });

                        applySearch(txtSearch != null ? txtSearch.getText() : "");
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        };
        new Thread(task).start();
    }
}
