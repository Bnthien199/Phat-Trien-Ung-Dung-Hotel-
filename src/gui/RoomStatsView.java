package gui;

import dao.PhieuDatPhongDAO;
import dao.PhongDAO;
import entity.PhieuDatPhong;
import entity.Phong;
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
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import util.BookingStatus;
import util.RoomStatus;
import util.StatusBadge;
import util.StatusUtils;

import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoomStatsView {

    private final PhongDAO phongDAO = new PhongDAO();
    private final PhieuDatPhongDAO phieuDatPhongDAO = new PhieuDatPhongDAO();

    private final ObservableList<TopRoomStat> topRoomItems = FXCollections.observableArrayList();

    private VBox root;
    private Label lblTotalRooms;
    private Label lblOccupied;
    private Label lblVacant;
    private Label lblMaintenance;
    private Label lblOccupancyRate;
    private PieChart pieChart;
    private TableView<TopRoomStat> tableTopRooms;
    private StackPane chartContainer;
    private Label chartSummaryLabel;

    public Node createView() {
        root = new VBox(20);
        root.getStyleClass().addAll("room-stats-view", "page-shell");
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
        Task<RoomStatsSnapshot> task = new Task<>() {
            @Override
            protected RoomStatsSnapshot call() {
                List<Phong> rooms = phongDAO.getAllPhong();
                List<PhieuDatPhong> bookings = phieuDatPhongDAO.getAllPhieuDatPhong();
                return buildSnapshot(rooms, bookings);
            }
        };

        task.setOnSucceeded(e -> applySnapshot(task.getValue()));
        task.setOnFailed(e -> showEmptyState("Không thể tải thống kê phòng", "Kiểm tra lại kết nối database và dữ liệu phòng."));
        new Thread(task).start();
    }

    private Node buildHeader() {
        HBox header = new HBox(14);
        header.getStyleClass().add("page-header");

        StackPane icon = new StackPane(new Label("\uD83D\uDECF"));
        icon.getStyleClass().add("page-icon-badge");

        VBox textBox = new VBox(4);
        textBox.getStyleClass().add("page-header-text");
        Label title = new Label("Thống kê phòng");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Theo dõi mức sử dụng phòng, tình trạng vận hành và các phòng được đặt nhiều nhất.");
        subtitle.getStyleClass().add("page-subtitle");
        subtitle.setWrapText(true);
        textBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox occupancyChip = new VBox();
        occupancyChip.getStyleClass().add("stats-occupancy-pill");
        occupancyChip.setAlignment(Pos.CENTER);
        Label chipLabel = new Label("Cập nhật theo dữ liệu đặt phòng hiện tại");
        occupancyChip.getChildren().add(chipLabel);

        header.getChildren().addAll(icon, textBox, spacer, occupancyChip);
        return header;
    }

    private Node buildSummaryGrid() {
        lblTotalRooms = new Label("--");
        lblOccupied = new Label("--");
        lblVacant = new Label("--");
        lblMaintenance = new Label("--");
        lblOccupancyRate = new Label("--%");

        GridPane grid = new GridPane();
        grid.getStyleClass().add("stats-summary-grid");
        for (int i = 0; i < 5; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(20);
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        grid.add(metricCard("\uD83C\uDFE8", "Tổng số phòng", lblTotalRooms, "Tất cả phòng trong hệ thống", "#eff6ff", "#1f3a5f"), 0, 0);
        grid.add(metricCard("\uD83D\uDD11", "Đang có khách", lblOccupied, "Phòng đang ở hoặc đã nhận phòng", "#fff7ed", "#d97706"), 1, 0);
        grid.add(metricCard("\u2705", "Phòng trống", lblVacant, "Sẵn sàng nhận khách mới", "#ecfdf5", "#0f766e"), 2, 0);
        grid.add(metricCard("\uD83D\uDEE0", "Bảo trì", lblMaintenance, "Phòng tạm ngưng khai thác", "#fef2f2", "#dc2626"), 3, 0);
        grid.add(metricCard("\uD83D\uDCC8", "Tỷ lệ lấp đầy", lblOccupancyRate, "Tỷ lệ phòng đang khai thác", "#f5f3ff", "#7c3aed"), 4, 0);
        return grid;
    }

    private Node buildContent() {
        HBox content = new HBox(18);

        VBox chartCard = new VBox(16);
        chartCard.getStyleClass().add("panel-card");
        HBox.setHgrow(chartCard, Priority.ALWAYS);

        HBox chartHeader = buildPanelHeader("#0d9488", "Biểu đồ trạng thái phòng", "Phân bố phòng trống, đang có khách và bảo trì theo thời điểm hiện tại.");
        chartSummaryLabel = new Label("Đang đồng bộ dữ liệu phòng...");
        chartSummaryLabel.getStyleClass().add("panel-subtitle");

        pieChart = new PieChart();
        pieChart.getStyleClass().addAll("analytics-chart");
        pieChart.setLegendVisible(true);
        pieChart.setLabelsVisible(true);
        pieChart.setClockwise(true);
        pieChart.setLabelLineLength(10);

        chartContainer = new StackPane(pieChart, createEmptyState("Không có dữ liệu thống kê phòng", "Thêm phòng hoặc kiểm tra trạng thái phòng để xem biểu đồ."));
        chartContainer.setPrefHeight(420);
        VBox.setVgrow(chartContainer, Priority.ALWAYS);

        chartCard.getChildren().addAll(chartHeader, chartSummaryLabel, chartContainer);

        VBox tableCard = new VBox(16);
        tableCard.getStyleClass().add("panel-card");
        tableCard.setPrefWidth(430);

        HBox tableHeader = buildPanelHeader("#1f3a5f", "Top phòng được đặt nhiều", "Sắp xếp theo số lần xuất hiện trong phiếu đặt.");

        tableTopRooms = new TableView<>();
        tableTopRooms.getStyleClass().addAll("app-table", "stats-table");
        tableTopRooms.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableTopRooms.setItems(topRoomItems);
        tableTopRooms.setPlaceholder(createEmptyState("Chưa có lịch sử đặt phòng", "Top phòng sẽ hiển thị ngay khi có phiếu đặt."));
        configureTopRoomTable();
        VBox.setVgrow(tableTopRooms, Priority.ALWAYS);

        tableCard.getChildren().addAll(tableHeader, tableTopRooms);

        content.getChildren().addAll(chartCard, tableCard);
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

    private void configureTopRoomTable() {
        TableColumn<TopRoomStat, String> colRoom = new TableColumn<>("Phòng");
        colRoom.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().roomCode));

        TableColumn<TopRoomStat, String> colType = new TableColumn<>("Loại phòng");
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().roomType));

        TableColumn<TopRoomStat, Number> colBookings = new TableColumn<>("Lượt đặt");
        colBookings.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().bookingCount));
        colBookings.setStyle("-fx-alignment: CENTER;");

        TableColumn<TopRoomStat, String> colStatus = new TableColumn<>("Tình trạng");
        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().statusRaw));
        colStatus.setCellFactory(StatusBadge.roomCellFactory());
        colStatus.setStyle("-fx-alignment: CENTER;");

        tableTopRooms.getColumns().setAll(colRoom, colType, colBookings, colStatus);
    }

    private RoomStatsSnapshot buildSnapshot(List<Phong> rooms, List<PhieuDatPhong> bookings) {
        List<Phong> safeRooms = rooms != null ? rooms : List.of();
        List<PhieuDatPhong> safeBookings = bookings != null ? bookings : List.of();

        int total = safeRooms.size();
        long maintenance = safeRooms.stream()
                .filter(room -> StatusUtils.isRoomStatus(room.getTrangThai(), RoomStatus.SUA_CHUA))
                .count();

        int vacant;
        try {
            vacant = phongDAO.findAvailableRooms(LocalDateTime.now(), LocalDateTime.now().plusMinutes(1)).size();
        } catch (RuntimeException ex) {
            vacant = (int) safeRooms.stream()
                    .filter(room -> StatusUtils.isRoomStatus(room.getTrangThai(), RoomStatus.TRONG))
                    .count();
        }
        int occupied = Math.max(total - vacant - (int) maintenance, 0);
        int pending = Math.max(total - occupied - vacant - (int) maintenance, 0);
        double occupancyRate = total > 0 ? (occupied * 100.0 / total) : 0;

        List<PieChart.Data> pieData = new ArrayList<>();
        if (occupied > 0) {
            pieData.add(new PieChart.Data("Đang có khách", occupied));
        }
        if (vacant > 0) {
            pieData.add(new PieChart.Data("Phòng trống", vacant));
        }
        if (maintenance > 0) {
            pieData.add(new PieChart.Data("Bảo trì", maintenance));
        }
        if (pending > 0) {
            pieData.add(new PieChart.Data("Chờ xử lý", pending));
        }

        Map<String, Phong> roomByCode = new HashMap<>();
        for (Phong room : safeRooms) {
            roomByCode.put(room.getMaPhong(), room);
        }

        Map<String, Integer> bookingCountByRoom = new HashMap<>();
        for (PhieuDatPhong booking : safeBookings) {
            if (booking == null || booking.getDsMaPhong() == null || booking.getDsMaPhong().isBlank()) {
                continue;
            }
            String normalizedStatus = StatusUtils.bookingCode(booking.getTrangThai());
            if (StatusUtils.isBookingStatus(normalizedStatus, BookingStatus.DA_HUY)) {
                continue;
            }
            String[] roomsInBooking = booking.getDsMaPhong().split(",");
            for (String roomCode : roomsInBooking) {
                String normalized = roomCode.trim();
                if (!normalized.isEmpty()) {
                    bookingCountByRoom.merge(normalized, 1, Integer::sum);
                }
            }
        }

        List<TopRoomStat> topRooms = bookingCountByRoom.entrySet().stream()
                .map(entry -> {
                    Phong room = roomByCode.get(entry.getKey());
                    return new TopRoomStat(
                            entry.getKey(),
                            room != null ? room.getLoaiPhong() : "Chưa xác định",
                            entry.getValue(),
                            room != null ? room.getTrangThai() : RoomStatus.TRONG.getCode()
                    );
                })
                .sorted(Comparator.comparingInt(TopRoomStat::bookingCount).reversed()
                        .thenComparing(TopRoomStat::roomCode))
                .limit(8)
                .toList();

        return new RoomStatsSnapshot(total, occupied, vacant, (int) maintenance, occupancyRate, pieData, topRooms);
    }

    private void applySnapshot(RoomStatsSnapshot snapshot) {
        if (snapshot == null) {
            showEmptyState("Không có dữ liệu thống kê phòng", "Không nhận được snapshot thống kê phòng.");
            return;
        }

        lblTotalRooms.setText(String.valueOf(snapshot.totalRooms));
        lblOccupied.setText(String.valueOf(snapshot.occupiedRooms));
        lblVacant.setText(String.valueOf(snapshot.vacantRooms));
        lblMaintenance.setText(String.valueOf(snapshot.maintenanceRooms));
        lblOccupancyRate.setText(new DecimalFormat("0.0").format(snapshot.occupancyRate) + "%");

        topRoomItems.setAll(snapshot.topRooms);
        pieChart.setData(FXCollections.observableArrayList(snapshot.pieData));

        boolean hasPieData = !snapshot.pieData.isEmpty();
        pieChart.setVisible(hasPieData);
        pieChart.setManaged(hasPieData);
        chartContainer.getChildren().get(1).setVisible(!hasPieData);
        chartContainer.getChildren().get(1).setManaged(!hasPieData);

        chartSummaryLabel.setText("Cong suat hien tai: " + new DecimalFormat("0.0").format(snapshot.occupancyRate)
                + "% | " + snapshot.occupiedRooms + "/" + snapshot.totalRooms + " phong dang duoc su dung");

        Platform.runLater(() -> {
            for (PieChart.Data data : pieChart.getData()) {
                Tooltip.install(data.getNode(), new Tooltip(data.getName() + ": " + (int) data.getPieValue() + " phong"));
            }
        });
    }

    private void showEmptyState(String title, String subtitle) {
        lblTotalRooms.setText("--");
        lblOccupied.setText("--");
        lblVacant.setText("--");
        lblMaintenance.setText("--");
        lblOccupancyRate.setText("--%");
        topRoomItems.clear();
        pieChart.setData(FXCollections.observableArrayList());
        chartSummaryLabel.setText(subtitle);
        Node placeholder = chartContainer.getChildren().get(1);
        if (placeholder instanceof VBox box) {
            ((Label) box.getChildren().get(1)).setText(title);
            ((Label) box.getChildren().get(2)).setText(subtitle);
        }
        pieChart.setVisible(false);
        pieChart.setManaged(false);
        placeholder.setVisible(true);
        placeholder.setManaged(true);
    }

    private VBox createEmptyState(String title, String subtitle) {
        VBox empty = new VBox(8);
        empty.getStyleClass().addAll("empty-state", "stats-chart-placeholder");
        empty.setAlignment(Pos.CENTER);
        Label icon = new Label("\uD83D\uDCC9");
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

    private record TopRoomStat(String roomCode, String roomType, int bookingCount, String statusRaw) {
    }

    private record RoomStatsSnapshot(
            int totalRooms,
            int occupiedRooms,
            int vacantRooms,
            int maintenanceRooms,
            double occupancyRate,
            List<PieChart.Data> pieData,
            List<TopRoomStat> topRooms
    ) {
    }
}
