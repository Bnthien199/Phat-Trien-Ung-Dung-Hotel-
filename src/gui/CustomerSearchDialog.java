package gui;

import entity.KhachHang;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import util.AppTheme;

import java.util.List;

public class CustomerSearchDialog {
    private final Stage stage;
    private final KhachHangService service;
    private KhachHang selectedCustomer;

    public CustomerSearchDialog(Window owner, KhachHangService service) {
        this.service = service;
        stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }
        stage.setTitle("Tìm kiếm khách hàng");
        stage.setResizable(false);
        stage.setScene(new Scene(buildUi(), 860, 560));
        util.ModalUtils.setupModalBehavior(stage, true, false, null);
    }

    public KhachHang show() {
        stage.showAndWait();
        return selectedCustomer;
    }

    private BorderPane buildUi() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + AppTheme.BG_PAGE + ";");
        root.setTop(buildHeader());
        root.setCenter(buildContent());
        return root;
    }

    private HBox buildHeader() {
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 18, 24));
        header.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: transparent transparent " + AppTheme.BORDER + " transparent;" +
                        "-fx-border-width: 0 0 1 0;");

        StackPane iconWrap = new StackPane();
        iconWrap.setPrefSize(42, 42);
        iconWrap.setStyle(
                "-fx-background-color: " + AppTheme.TEAL_LIGHT + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #c7ece8;" +
                        "-fx-border-radius: 14;");
        Label icon = new Label("🔎");
        icon.setFont(Font.font(18));
        iconWrap.getChildren().add(icon);

        VBox titleBox = new VBox(4);
        Label title = new Label("Tìm kiếm khách hàng");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 19));
        title.setTextFill(Color.web(AppTheme.TEXT));

        Label sub = new Label("Tra cứu nhanh theo mã khách hàng, tên, số điện thoại hoặc giấy tờ.");
        sub.setFont(Font.font("Segoe UI", 12));
        sub.setTextFill(Color.web(AppTheme.TEXT_SUB));
        titleBox.getChildren().addAll(title, sub);

        header.getChildren().addAll(iconWrap, titleBox);
        return header;
    }

    private VBox buildContent() {
        List<KhachHang> customers = service.getAllActive();
        ObservableList<KhachHang> items = FXCollections.observableArrayList(customers);
        FilteredList<KhachHang> filtered = new FilteredList<>(items, kh -> true);

        HBox searchWrap = new HBox(10);
        searchWrap.setAlignment(Pos.CENTER_LEFT);
        searchWrap.setPadding(new Insets(0, 14, 0, 14));
        searchWrap.setPrefHeight(44);
        searchWrap.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: " + AppTheme.BORDER + ";" +
                        "-fx-border-radius: 14;" +
                        "-fx-effect: " + AppTheme.SHADOW_SM + ";");

        Label searchIcon = new Label("🔍");
        searchIcon.setFont(Font.font(14));
        searchIcon.setTextFill(Color.web(AppTheme.MUTED));

        TextField txtKeyword = new TextField();
        txtKeyword.setPromptText("Nhập mã khách hàng, tên, số điện thoại hoặc giấy tờ...");
        txtKeyword.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-padding: 0;" +
                        "-fx-font-size: 13;");
        HBox.setHgrow(txtKeyword, Priority.ALWAYS);
        searchWrap.getChildren().addAll(searchIcon, txtKeyword);

        TableView<KhachHang> table = new TableView<>(filtered);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("Không tìm thấy khách hàng phù hợp."));
        table.setStyle(buildTableStyle());
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<KhachHang, String> colMa = new TableColumn<>("Mã khách hàng");
        colMa.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getMaKhachHang())));

        TableColumn<KhachHang, String> colTen = new TableColumn<>("Tên khách hàng");
        colTen.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getTenKhachHang())));

        TableColumn<KhachHang, String> colSdt = new TableColumn<>("So dien thoai");
        colSdt.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getSoDienThoai())));

        TableColumn<KhachHang, String> colGiayTo = new TableColumn<>("Giay to");
        colGiayTo.setCellValueFactory(d -> new SimpleStringProperty(
                safe(d.getValue().getLoaiGiayTo()) +
                        (safe(d.getValue().getSoGiayTo()).isEmpty() ? "" : " • " + safe(d.getValue().getSoGiayTo()))));

        TableColumn<KhachHang, String> colDiaChi = new TableColumn<>("Dia chi");
        colDiaChi.setCellValueFactory(d -> new SimpleStringProperty(safe(d.getValue().getDiaChi())));

        table.getColumns().addAll(colMa, colTen, colSdt, colGiayTo, colDiaChi);

        txtKeyword.textProperty().addListener((obs, oldValue, newValue) -> {
            String keyword = newValue == null ? "" : newValue.trim().toLowerCase();
            filtered.setPredicate(kh -> {
                if (keyword.isEmpty()) {
                    return true;
                }
                return safe(kh.getMaKhachHang()).toLowerCase().contains(keyword)
                        || safe(kh.getTenKhachHang()).toLowerCase().contains(keyword)
                        || safe(kh.getSoDienThoai()).contains(keyword)
                        || safe(kh.getSoGiayTo()).toLowerCase().contains(keyword)
                        || safe(kh.getDiaChi()).toLowerCase().contains(keyword);
            });
        });

        table.setRowFactory(tv -> {
            TableRow<KhachHang> row = new TableRow<>();
            row.setStyle("-fx-cell-size: 46px;");
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    selectedCustomer = row.getItem();
                    stage.close();
                }
            });
            return row;
        });

        Button btnClose = new Button("Đóng");
        styleSecondaryButton(btnClose);
        btnClose.setOnAction(e -> stage.close());

        Button btnSelect = new Button("Chọn khách");
        stylePrimaryButton(btnSelect);
        btnSelect.setOnAction(e -> {
            selectedCustomer = table.getSelectionModel().getSelectedItem();
            if (selectedCustomer != null) {
                stage.close();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, spacer, btnClose, btnSelect);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(16, searchWrap, table, actions);
        content.setPadding(new Insets(18, 24, 24, 24));
        return content;
    }

    private String buildTableStyle() {
        return "-fx-background-color: white;" +
                "-fx-background-radius: 16;" +
                "-fx-border-color: " + AppTheme.BORDER + ";" +
                "-fx-border-radius: 16;" +
                "-fx-padding: 8;" +
                "-fx-table-cell-border-color: transparent;" +
                "-fx-selection-bar: #dff7f3;" +
                "-fx-selection-bar-non-focused: #ecfdf9;" +
                "-fx-control-inner-background: white;";
    }

    private void stylePrimaryButton(Button button) {
        button.setPrefHeight(40);
        button.setStyle(
                "-fx-background-color: " + AppTheme.TEAL + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 12;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 18;" +
                        "-fx-effect: " + AppTheme.SHADOW_SM + ";");
    }

    private void styleSecondaryButton(Button button) {
        button.setPrefHeight(40);
        button.setStyle(
                "-fx-background-color: #eef2f7;" +
                        "-fx-text-fill: " + AppTheme.TEXT_SUB + ";" +
                        "-fx-font-weight: 700;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + AppTheme.BORDER + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 0 18;");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
