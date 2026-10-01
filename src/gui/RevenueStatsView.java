package gui;

import dao.HoaDonDAO;
import dao.HoaDonDAO.RevenueChartData;
import dao.HoaDonDAO.RevenuePeriod;
import dao.HoaDonDAO.RevenueStatistics;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import javafx.util.StringConverter;
import util.ReportExportUtils;
import util.ReportExportUtils.SaveTarget;

import java.io.File;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RevenueStatsView {
    private static final Logger LOGGER = Logger.getLogger(RevenueStatsView.class.getName());

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0");
    private static final double MIN_VISIBLE_REVENUE = 0.0001;
    private static final String REVENUE_BAR_STYLE = """
            -fx-background-color: linear-gradient(to top, #3b82f6, #8b5cf6);
            -fx-background-radius: 8 8 0 0;
            -fx-background-insets: 0;
            """;

    private static final String PERIOD_DAY = "Theo ngày";
    private static final String PERIOD_WEEK = "Theo tuần";
    private static final String PERIOD_MONTH = "Theo tháng";
    private static final String PERIOD_YEAR = "Theo năm";

    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final List<RevenueChartData> currentBuckets = new ArrayList<>();

    private StackPane root;
    private VBox toastContainer;
    private StackPane loadingOverlay;
    private StackPane chartStack;
    private VBox chartEmptyState;

    private Label lblTotalRevenue;
    private Label lblTotalInvoices;
    private Label lblAvgRevenue;
    private Label lblRangeHint;
    private Label lblChartHint;
    private Label lblEmptyTitle;
    private Label lblEmptySubtitle;

    private ComboBox<String> cboPeriod;
    private DatePicker dpFrom;
    private DatePicker dpTo;
    private Button btnStats;
    private Button btnExport;

    private BarChart<String, Number> revenueChart;
    private CategoryAxis revenueXAxis;
    private NumberAxis revenueYAxis;
    private RevenueStatistics currentStatistics = RevenueStatistics.empty();
    private boolean adjustingFilters;

    public Node createView() {
        root = new StackPane();
        root.getStyleClass().add("revenue-view");
        root.getStylesheets().addAll(
                stylesheetUri("global-theme.css"),
                stylesheetUri("revenue-stats.css")
        );

        VBox page = new VBox(18);
        page.setPadding(new Insets(24));

        page.getChildren().addAll(
                buildHeader(),
                buildFilterBar(),
                buildSummaryCards(),
                buildChartCard()
        );
        VBox.setVgrow(page.getChildren().get(page.getChildren().size() - 1), Priority.ALWAYS);

        toastContainer = new VBox(10);
        toastContainer.setPickOnBounds(false);
        toastContainer.setMouseTransparent(true);
        StackPane.setAlignment(toastContainer, Pos.TOP_RIGHT);
        StackPane.setMargin(toastContainer, new Insets(18));

        loadingOverlay = new StackPane(new ProgressIndicator());
        loadingOverlay.getStyleClass().add("revenue-loading-overlay");
        loadingOverlay.setVisible(false);
        loadingOverlay.setManaged(false);

        root.getChildren().addAll(page, loadingOverlay, toastContainer);
        initializeDefaults();
        loadData();
        return root;
    }

    public void loadData() {
        loadRevenueStatistics(false);
    }

    private Node buildHeader() {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        StackPane iconWrap = new StackPane();
        iconWrap.getStyleClass().add("revenue-hero-icon");
        Label icon = new Label("₫");
        icon.setFont(Font.font("Segoe UI Symbol", FontWeight.EXTRA_BOLD, 22));
        iconWrap.getChildren().add(icon);

        VBox texts = new VBox(6);
        Label title = new Label("Thống kê doanh thu");
        title.getStyleClass().add("revenue-page-title");

        Label subtitle = new Label("Tổng hợp hóa đơn hợp lệ theo thời gian, theo dõi xu hướng doanh thu và xuất báo cáo nhanh.");
        subtitle.getStyleClass().add("revenue-page-subtitle");
        subtitle.setWrapText(true);

        texts.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblRangeHint = new Label();
        lblRangeHint.getStyleClass().add("revenue-range-chip");

        row.getChildren().addAll(iconWrap, texts, spacer, lblRangeHint);
        return row;
    }

    private Node buildFilterBar() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("app-card", "revenue-filter-card");

        cboPeriod = new ComboBox<>(FXCollections.observableArrayList(
                PERIOD_DAY, PERIOD_WEEK, PERIOD_MONTH, PERIOD_YEAR
        ));
        cboPeriod.getStyleClass().add("revenue-filter-combo");
        cboPeriod.setPrefWidth(158);
        cboPeriod.setVisibleRowCount(4);
        cboPeriod.valueProperty().addListener((obs, oldValue, newValue) -> handlePeriodChanged(newValue));

        dpFrom = new DatePicker();
        dpFrom.getStyleClass().add("revenue-date-picker");
        dpFrom.setPromptText("Từ ngày");
        dpFrom.setPrefWidth(160);
        dpFrom.valueProperty().addListener((obs, oldValue, newValue) -> handleDateChanged());

        dpTo = new DatePicker();
        dpTo.getStyleClass().add("revenue-date-picker");
        dpTo.setPromptText("Đến ngày");
        dpTo.setPrefWidth(160);
        dpTo.valueProperty().addListener((obs, oldValue, newValue) -> handleDateChanged());

        VBox periodBox = filterItem("Kỳ", cboPeriod);
        VBox fromBox = filterItem("Từ ngày", wrapDatePicker(dpFrom));
        VBox toBox = filterItem("Đến ngày", wrapDatePicker(dpTo));

        btnStats = new Button("▦  Thống kê");
        btnStats.getStyleClass().addAll("btn-primary", "revenue-btn-primary");
        btnStats.setOnAction(event -> loadRevenueStatistics(true));

        btnExport = new Button("⇩  Xuất báo cáo");
        btnExport.getStyleClass().addAll("btn-outline", "revenue-btn-secondary");
        btnExport.setOnAction(event -> handleExport());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(periodBox, fromBox, toBox, spacer, btnStats, btnExport);
        return row;
    }

    private VBox buildSummaryCards() {
        HBox row = new HBox(16);

        lblTotalRevenue = statValueLabel();
        lblTotalInvoices = statValueLabel();
        lblAvgRevenue = statValueLabel();

        row.getChildren().addAll(
                createStatCard("Tổng doanh thu", "Hóa đơn đã thanh toán", lblTotalRevenue, "₫", "revenue-stat-teal"),
                createStatCard("Số hóa đơn", "Hóa đơn hợp lệ", lblTotalInvoices, "#", "revenue-stat-blue"),
                createStatCard("Doanh thu trung bình", "Theo mỗi hóa đơn", lblAvgRevenue, "Ø", "revenue-stat-amber")
        );

        for (Node card : row.getChildren()) {
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        return new VBox(row);
    }

    private VBox buildChartCard() {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("app-card", "revenue-chart-card");

        HBox head = new HBox(10);
        head.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Biểu đồ doanh thu");
        title.getStyleClass().add("revenue-section-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        lblChartHint = new Label("Dữ liệu lấy từ hóa đơn hợp lệ");
        lblChartHint.getStyleClass().add("label-muted");

        head.getChildren().addAll(title, spacer, lblChartHint);

        revenueXAxis = new CategoryAxis();
        CategoryAxis xAxis = revenueXAxis;
        xAxis.setLabel("Kỳ thống kê");
        xAxis.setTickLabelRotation(0);
        xAxis.setGapStartAndEnd(true);

        revenueYAxis = new NumberAxis();
        NumberAxis yAxis = revenueYAxis;
        yAxis.setLabel("Doanh thu (VND)");
        yAxis.setForceZeroInRange(true);
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(0);
        yAxis.setUpperBound(1);
        yAxis.setTickUnit(1);
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return compactMoney(value == null ? 0 : value.doubleValue());
            }

            @Override
            public Number fromString(String value) {
                return 0;
            }
        });

        revenueChart = new BarChart<>(xAxis, yAxis);
        revenueChart.setAnimated(false);
        revenueChart.setLegendVisible(true);
        revenueChart.setHorizontalGridLinesVisible(true);
        revenueChart.setVerticalGridLinesVisible(false);
        revenueChart.setCategoryGap(18);
        revenueChart.setBarGap(8);
        revenueChart.setMinHeight(360);
        revenueChart.getStyleClass().add("revenue-chart");
        revenueChart.setTitle("Doanh thu theo kỳ");

        chartEmptyState = buildEmptyState();
        chartStack = new StackPane(revenueChart, chartEmptyState);
        chartStack.getStyleClass().add("revenue-chart-stack");
        VBox.setVgrow(chartStack, Priority.ALWAYS);

        card.getChildren().addAll(head, chartStack);
        VBox.setVgrow(card, Priority.ALWAYS);
        return card;
    }

    private void initializeDefaults() {
        adjustingFilters = true;
        cboPeriod.setValue(PERIOD_MONTH);
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        dpFrom.setValue(currentMonth.atDay(1));
        dpTo.setValue(currentMonth.atEndOfMonth());
        adjustingFilters = false;
        updateRangeHint();
        setEmptyState("Chưa có dữ liệu hiển thị", "Chọn kỳ thống kê rồi nhấn Thống kê để tải doanh thu.");
    }

    private void handlePeriodChanged(String period) {
        if (adjustingFilters || period == null) {
            return;
        }

        adjustingFilters = true;
        LocalDate today = LocalDate.now();
        switch (period) {
            case PERIOD_WEEK -> {
                dpFrom.setValue(today.minusDays(6));
                dpTo.setValue(today);
            }
            case PERIOD_MONTH -> {
                YearMonth currentMonth = YearMonth.from(today);
                dpFrom.setValue(currentMonth.atDay(1));
                dpTo.setValue(currentMonth.atEndOfMonth());
            }
            case PERIOD_YEAR -> {
                dpFrom.setValue(LocalDate.of(today.getYear(), 1, 1));
                dpTo.setValue(LocalDate.of(today.getYear(), 12, 31));
            }
            default -> {
                // Theo ngày giữ nguyên khoảng thời gian người dùng đang chọn.
            }
        }
        adjustingFilters = false;
        updateRangeHint();
        loadRevenueStatistics(false);
    }

    private void handleDateChanged() {
        if (adjustingFilters) {
            return;
        }
        updateRangeHint();
    }

    private void loadRevenueStatistics(boolean notifyResult) {
        if (!validateDateRange(true)) {
            return;
        }

        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        RevenuePeriod period = selectedPeriod();

        Task<RevenueStatistics> task = new Task<>() {
            @Override
            protected RevenueStatistics call() {
                return hoaDonDAO.getRevenueStatistics(from, to, period);
            }
        };

        setLoading(true);

        task.setOnSucceeded(event -> {
            setLoading(false);
            RevenueStatistics statistics = task.getValue() == null ? RevenueStatistics.empty() : task.getValue();
            applyStatistics(statistics);
            updateRangeHint();

            if (statistics.getTotalInvoices() == 0) {
                if (notifyResult) {
                    showToast("Không có hóa đơn hợp lệ trong khoảng thời gian đã chọn.", ToastType.INFO);
                }
            } else if (notifyResult) {
                showToast("Đã cập nhật báo cáo doanh thu.", ToastType.SUCCESS);
            }
        });

        task.setOnFailed(event -> {
            setLoading(false);
            currentStatistics = RevenueStatistics.empty();
            currentBuckets.clear();
            updateSummaryCards(currentStatistics);
            updateChart(currentBuckets);

            Throwable ex = task.getException();
            showToast("Không thể thống kê doanh thu" + (ex != null ? ": " + ex.getMessage() : ""), ToastType.ERROR);
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Thread loader = new Thread(task, "revenue-stats-load");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyStatistics(RevenueStatistics statistics) {
        currentStatistics = statistics;
        currentBuckets.clear();
        currentBuckets.addAll(statistics.getChartData());

        updateSummaryCards(statistics);
        updateChart(currentBuckets);
    }

    private boolean validateDateRange(boolean showMessage) {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        if (from == null || to == null) {
            if (showMessage) {
                showToast("Vui lòng chọn đầy đủ từ ngày và đến ngày.", ToastType.WARNING);
            }
            return false;
        }
        if (from.isAfter(to)) {
            if (showMessage) {
                showToast("Từ ngày không được lớn hơn Đến ngày.", ToastType.WARNING);
            }
            return false;
        }
        return true;
    }

    private void updateSummaryCards(RevenueStatistics statistics) {
        lblTotalRevenue.setText(formatMoney(statistics.getTotalRevenue()));
        lblTotalInvoices.setText(String.valueOf(statistics.getTotalInvoices()));
        lblAvgRevenue.setText(formatMoney(statistics.getAverageRevenue()));
    }

    private void updateChart(List<RevenueChartData> buckets) {
        clearRevenueChart();

        boolean isMonthly = selectedPeriod() == RevenuePeriod.MONTH;
        List<RevenueChartData> drawableBuckets = new ArrayList<>();

        if (isMonthly) {
            double[] monthlyRevenue = new double[12];
            int[] monthlyInvoices = new int[12];

            if (buckets != null) {
                for (RevenueChartData bucket : buckets) {
                    if (bucket == null || bucket.getLabel() == null) {
                        continue;
                    }
                    try {
                        String label = bucket.getLabel().trim();
                        int monthIdx = -1;
                        if (label.contains("/")) {
                            String[] parts = label.split("/");
                            monthIdx = Integer.parseInt(parts[0]) - 1;
                        } else if (label.startsWith("T") && label.length() > 1 && Character.isDigit(label.charAt(1))) {
                            monthIdx = Integer.parseInt(label.substring(1)) - 1;
                        } else {
                            String digits = label.replaceAll("\\D+", "");
                            if (!digits.isEmpty()) {
                                monthIdx = Integer.parseInt(digits) - 1;
                            }
                        }

                        if (monthIdx >= 0 && monthIdx < 12) {
                            monthlyRevenue[monthIdx] += bucket.getRevenue();
                            monthlyInvoices[monthIdx] += bucket.getInvoiceCount();
                        }
                    } catch (Exception ex) {
                        LOGGER.log(Level.WARNING, "Failed to parse month from label: " + bucket.getLabel(), ex);
                    }
                }
            }

            for (int m = 0; m < 12; m++) {
                drawableBuckets.add(new RevenueChartData("T" + (m + 1), monthlyRevenue[m], monthlyInvoices[m]));
            }
        } else {
            drawableBuckets = ensureDrawableRevenueBuckets(drawableRevenueBuckets(buckets));
        }

        if (drawableBuckets.isEmpty() || (!isMonthly && !hasPositiveRevenue(drawableBuckets))) {
            showRevenueChartEmpty();
            return;
        }

        drawableBuckets.forEach(this::logRevenueChartBucket);

        chartEmptyState.setVisible(false);
        chartEmptyState.setManaged(false);
        revenueChart.setVisible(true);
        revenueChart.setManaged(true);

        configureRevenueAxes(drawableBuckets);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Doanh Thu");

        int bucketCount = drawableBuckets.size();
        int dataIndex = 0;
        for (RevenueChartData bucket : drawableBuckets) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(bucket.getLabel(), bucket.getRevenue());
            data.setNode(createRevenueBarNode(bucket, bucketCount, dataIndex++));
            series.getData().add(data);
        }

        if (series.getData().isEmpty()) {
            showRevenueChartEmpty();
            return;
        }

        revenueChart.setData(FXCollections.observableArrayList(series));
        refreshRevenueChartLayout();
        lblChartHint.setText("Hiển thị " + series.getData().size() + " mốc thống kê theo "
                + selectedPeriodLabelForHint());
    }

    private void clearRevenueChart() {
        if (revenueChart != null) {
            revenueChart.getData().clear();
        }
        if (revenueXAxis != null) {
            revenueXAxis.getCategories().clear();
        }
        resetRevenueAxis();
    }

    private void showRevenueChartEmpty() {
        setEmptyState("Chưa có dữ liệu để vẽ biểu đồ", "Không có hóa đơn hợp lệ trong khoảng thời gian đã chọn.");
        revenueChart.setVisible(false);
        revenueChart.setManaged(false);
        chartEmptyState.setVisible(true);
        chartEmptyState.setManaged(true);
        lblChartHint.setText("Chưa có dữ liệu để vẽ biểu đồ");
    }

    private List<RevenueChartData> ensureDrawableRevenueBuckets(List<RevenueChartData> drawableBuckets) {
        if (hasPositiveRevenue(drawableBuckets) || currentStatistics.getTotalRevenue() <= MIN_VISIBLE_REVENUE) {
            return drawableBuckets;
        }

        LOGGER.warning("Revenue summary is positive but chart buckets are empty/zero. Rendering one fallback chart bucket.");
        return List.of(new RevenueChartData(
                fallbackChartLabel(),
                currentStatistics.getTotalRevenue(),
                currentStatistics.getTotalInvoices()));
    }

    private boolean hasPositiveRevenue(List<RevenueChartData> buckets) {
        if (buckets == null) {
            return false;
        }
        for (RevenueChartData bucket : buckets) {
            if (bucket != null && bucket.getRevenue() > MIN_VISIBLE_REVENUE) {
                return true;
            }
        }
        return false;
    }

    private List<RevenueChartData> drawableRevenueBuckets(List<RevenueChartData> buckets) {
        List<RevenueChartData> drawableBuckets = new ArrayList<>();
        if (buckets == null) {
            return drawableBuckets;
        }

        for (RevenueChartData bucket : buckets) {
            if (bucket == null || bucket.getLabel() == null || bucket.getLabel().isBlank()) {
                continue;
            }

            double revenue = bucket.getRevenue();
            if (Double.isNaN(revenue) || Double.isInfinite(revenue) || revenue <= MIN_VISIBLE_REVENUE) {
                continue;
            }

            drawableBuckets.add(new RevenueChartData(bucket.getLabel().trim(), revenue, bucket.getInvoiceCount()));
        }
        return drawableBuckets;
    }

    private void configureRevenueAxes(List<RevenueChartData> buckets) {
        boolean isMonthly = selectedPeriod() == RevenuePeriod.MONTH;
        List<String> categories = new ArrayList<>();
        double maxRevenue = 0;
        for (RevenueChartData bucket : buckets) {
            categories.add(bucket.getLabel());
            maxRevenue = Math.max(maxRevenue, bucket.getRevenue());
        }

        revenueXAxis.setCategories(FXCollections.observableArrayList(categories));
        revenueXAxis.setLabel(isMonthly ? "Tháng" : "Kỳ thống kê");
        revenueXAxis.setTickLabelRotation(isMonthly ? 0 : (categories.size() > 5 ? -30 : 0));
        revenueXAxis.setGapStartAndEnd(true);
        double edgeMargin = (isMonthly) ? 12 : (categories.size() == 1 ? 96 : 12);
        revenueXAxis.setStartMargin(edgeMargin);
        revenueXAxis.setEndMargin(edgeMargin);

        double upperBound = niceRevenueUpperBound(maxRevenue);
        revenueYAxis.setAutoRanging(false);
        revenueYAxis.setLowerBound(0);
        revenueYAxis.setUpperBound(upperBound);
        revenueYAxis.setTickUnit(niceRevenueTickUnit(upperBound));
        revenueYAxis.setLabel("Doanh thu (VND)");

        if (isMonthly) {
            revenueChart.setCategoryGap(24);
            revenueChart.setBarGap(0);
        } else {
            revenueChart.setCategoryGap(categories.size() == 1 ? 24 : categories.size() <= 3 ? 40 : 12);
            revenueChart.setBarGap(categories.size() == 1 ? 0 : 8);
        }
    }

    private void resetRevenueAxis() {
        if (revenueYAxis == null) {
            return;
        }
        revenueYAxis.setAutoRanging(false);
        revenueYAxis.setLowerBound(0);
        revenueYAxis.setUpperBound(1);
        revenueYAxis.setTickUnit(1);
    }

    private double niceRevenueUpperBound(double maxRevenue) {
        if (maxRevenue <= 0) {
            return 1;
        }

        double padded = maxRevenue * 1.18;
        double magnitude = Math.pow(10, Math.floor(Math.log10(padded)));
        double normalized = padded / magnitude;

        double niceNormalized;
        if (normalized <= 2) {
            niceNormalized = 2;
        } else if (normalized <= 5) {
            niceNormalized = 5;
        } else {
            niceNormalized = 10;
        }

        return niceNormalized * magnitude;
    }

    private double niceRevenueTickUnit(double upperBound) {
        if (upperBound <= 1) {
            return 0.2;
        }
        return upperBound / 5;
    }

    private String selectedPeriodLabelForHint() {
        return switch (selectedPeriod()) {
            case DAY -> "ngày";
            case WEEK -> "tuần";
            case YEAR -> "năm";
            default -> "tháng";
        };
    }

    private void handleExport() {
        if (currentBuckets.isEmpty()) {
            showToast("Không có dữ liệu để xuất báo cáo.", ToastType.WARNING);
            return;
        }

        SaveTarget target = ReportExportUtils.chooseReportSaveTarget(
                root.getScene() != null ? root.getScene().getWindow() : null,
                "Xuất báo cáo doanh thu",
                ReportExportUtils.defaultFileName("BaoCao_DoanhThu"));
        if (target == null) {
            return;
        }

        try {
            File file = target.file();
            if (target.format() == ReportExportUtils.ReportFormat.PDF) {
                ReportExportUtils.exportRevenuePdf(
                        file,
                        cboPeriod.getValue(),
                        dpFrom.getValue(),
                        dpTo.getValue(),
                        currentStatistics,
                        currentBuckets);
            } else {
                ReportExportUtils.exportRevenueExcel(
                        file,
                        cboPeriod.getValue(),
                        dpFrom.getValue(),
                        dpTo.getValue(),
                        currentStatistics,
                        currentBuckets);
            }
            showToast("Xuất báo cáo thành công.", ToastType.SUCCESS);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Không thể xuất báo cáo doanh thu", ex);
            showToast("Không thể xuất báo cáo: " + ex.getMessage(), ToastType.ERROR);
        }
    }

    private VBox filterItem(String label, Node field) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("filter-label");
        VBox box = new VBox(6, lbl, field);
        box.getStyleClass().add("revenue-filter-item");
        return box;
    }

    private Node wrapDatePicker(DatePicker picker) {
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("revenue-date-box");

        Label icon = new Label("◷");
        icon.getStyleClass().add("revenue-date-icon");
        HBox.setHgrow(picker, Priority.ALWAYS);

        box.getChildren().addAll(icon, picker);
        return box;
    }

    private VBox createStatCard(String title, String caption, Label valueLabel, String iconText, String styleClass) {
        VBox card = new VBox(14);
        card.getStyleClass().addAll("app-card", "revenue-stat-card", styleClass);
        card.setMaxWidth(Double.MAX_VALUE);

        HBox top = new HBox(10);
        top.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label(iconText);
        icon.setFont(Font.font("Segoe UI Symbol", FontWeight.EXTRA_BOLD, 17));
        StackPane iconWrap = new StackPane(icon);
        iconWrap.getStyleClass().add("revenue-stat-icon");

        VBox textBox = new VBox(4);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("revenue-stat-title");
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("revenue-stat-caption");
        textBox.getChildren().addAll(titleLabel, captionLabel);

        top.getChildren().addAll(iconWrap, textBox);

        card.getChildren().addAll(top, valueLabel);
        return card;
    }

    private VBox buildEmptyState() {
        Label icon = new Label("▥");
        icon.getStyleClass().add("revenue-empty-icon");

        lblEmptyTitle = new Label();
        lblEmptyTitle.getStyleClass().add("revenue-empty-title");

        lblEmptySubtitle = new Label();
        lblEmptySubtitle.getStyleClass().add("revenue-empty-subtitle");
        lblEmptySubtitle.setWrapText(true);

        VBox box = new VBox(10, icon, lblEmptyTitle, lblEmptySubtitle);
        box.getStyleClass().add("revenue-empty-state");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Label statValueLabel() {
        Label label = new Label("--");
        label.getStyleClass().add("revenue-stat-value");
        return label;
    }

    private RevenuePeriod selectedPeriod() {
        String period = cboPeriod.getValue();
        if (PERIOD_DAY.equals(period)) {
            return RevenuePeriod.DAY;
        }
        if (PERIOD_WEEK.equals(period)) {
            return RevenuePeriod.WEEK;
        }
        if (PERIOD_YEAR.equals(period)) {
            return RevenuePeriod.YEAR;
        }
        return RevenuePeriod.MONTH;
    }

    private String buildTooltip(RevenueChartData bucket) {
        return bucket.getLabel()
                + "\nDoanh thu: " + formatMoney(bucket.getRevenue())
                + "\nSố hóa đơn: " + bucket.getInvoiceCount();
    }

    private Node createRevenueBarNode(RevenueChartData bucket, int bucketCount, int dataIndex) {
        StackPane bar = new StackPane();
        bar.getStyleClass().addAll("chart-bar", "default-color0", "series0", "data" + dataIndex, "revenue-bar");
        bar.setStyle(REVENUE_BAR_STYLE);
        bar.setMinWidth(bucketCount == 1 ? 72 : 28);
        bar.setPrefWidth(bucketCount == 1 ? 88 : 44);
        bar.setMinHeight(2);

        if (bucket.getRevenue() > MIN_VISIBLE_REVENUE) {
            Label valueLabel = new Label(formatMoney(bucket.getRevenue()));
            valueLabel.getStyleClass().add("revenue-bar-value");
            valueLabel.setMouseTransparent(true);
            StackPane.setAlignment(valueLabel, Pos.TOP_CENTER);
            StackPane.setMargin(valueLabel, new Insets(-28, 0, 0, 0));
            bar.getChildren().add(valueLabel);
        }

        Tooltip tooltip = new Tooltip(buildTooltip(bucket));
        tooltip.getStyleClass().add("revenue-tooltip");
        Tooltip.install(bar, tooltip);
        playBarAnimation(bar);
        return bar;
    }

    private String fallbackChartLabel() {
        LocalDate from = dpFrom.getValue();
        if (from == null) {
            return "Tong";
        }

        return switch (selectedPeriod()) {
            case DAY -> from.format(DATE_FORMAT);
            case YEAR -> String.valueOf(from.getYear());
            default -> String.format(Locale.ROOT, "%02d/%d", from.getMonthValue(), from.getYear());
        };
    }

    private void logRevenueChartBucket(RevenueChartData bucket) {
        LOGGER.log(Level.INFO, "Revenue chart bucket before draw: label={0}, value={1}, invoices={2}",
                new Object[]{bucket.getLabel(), bucket.getRevenue(), bucket.getInvoiceCount()});
    }

    private void refreshRevenueChartLayout() {
        revenueChart.applyCss();
        revenueChart.requestLayout();
        chartStack.requestLayout();
    }

    private String formatMoney(double value) {
        return MONEY_FORMAT.format(value) + " VND";
    }

    private String compactMoney(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000) {
            return MONEY_FORMAT.format(value / 1_000_000_000) + " tỷ";
        }
        if (abs >= 1_000_000) {
            return MONEY_FORMAT.format(value / 1_000_000) + "tr";
        }
        if (abs >= 1_000) {
            return MONEY_FORMAT.format(value / 1_000) + "K";
        }
        return MONEY_FORMAT.format(value);
    }

    private void updateRangeHint() {
        if (dpFrom.getValue() == null || dpTo.getValue() == null) {
            lblRangeHint.setText("Khoảng thời gian chưa xác định");
            return;
        }
        lblRangeHint.setText(dpFrom.getValue().format(DATE_FORMAT) + " - " + dpTo.getValue().format(DATE_FORMAT));
    }

    private void setEmptyState(String title, String subtitle) {
        if (lblEmptyTitle != null) {
            lblEmptyTitle.setText(title);
        }
        if (lblEmptySubtitle != null) {
            lblEmptySubtitle.setText(subtitle);
        }
    }

    private void setLoading(boolean loading) {
        loadingOverlay.setVisible(loading);
        loadingOverlay.setManaged(loading);
        if (btnStats != null) {
            btnStats.setDisable(loading);
        }
        if (btnExport != null) {
            btnExport.setDisable(loading);
        }
    }

    private void playBarAnimation(Node node) {
        node.setScaleY(0.4);
        node.setOpacity(0.2);
        TranslateTransition translate = new TranslateTransition(Duration.millis(380), node);
        translate.setFromY(18);
        translate.setToY(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(380), node);
        scale.setFromY(0.4);
        scale.setToY(1);

        FadeTransition fade = new FadeTransition(Duration.millis(380), node);
        fade.setFromValue(0.2);
        fade.setToValue(1);

        new ParallelTransition(translate, scale, fade).play();
    }

    private void showToast(String message, ToastType type) {
        Label toast = new Label(message);
        toast.getStyleClass().addAll("revenue-toast", type.styleClass);
        toast.setWrapText(true);
        toast.setMaxWidth(380);
        toast.setOpacity(0);
        toastContainer.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(180), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        TranslateTransition slideIn = new TranslateTransition(Duration.millis(180), toast);
        slideIn.setFromY(-10);
        slideIn.setToY(0);

        ParallelTransition show = new ParallelTransition(fadeIn, slideIn);
        show.play();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(260), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setDelay(Duration.seconds(2.8));
        fadeOut.setOnFinished(event -> toastContainer.getChildren().remove(toast));
        fadeOut.play();
    }

    private String stylesheetUri(String fileName) {
        return Paths.get("src", "gui", fileName).toAbsolutePath().toUri().toString();
    }

    private enum ToastType {
        SUCCESS("revenue-toast-success"),
        WARNING("revenue-toast-warning"),
        ERROR("revenue-toast-error"),
        INFO("revenue-toast-info");

        private final String styleClass;

        ToastType(String styleClass) {
            this.styleClass = styleClass;
        }
    }
}
