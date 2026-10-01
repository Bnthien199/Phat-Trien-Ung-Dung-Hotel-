package gui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class FormChiTietPhieu extends JDialog {
    private static final Color PAGE_BG = new Color(244, 247, 251);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color BORDER = new Color(219, 230, 243);
    private static final Color TEXT_DARK = new Color(15, 23, 42);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color PRIMARY = new Color(13, 148, 136);

    public FormChiTietPhieu(JFrame parent, BookingDetailData detailData) {
        super(parent, "Chi tiet phieu dat phong", true);
        initUI(detailData);
    }

    private void initUI(BookingDetailData detailData) {
        setLayout(new BorderLayout());
        getContentPane().setBackground(PAGE_BG);

        JPanel shell = new JPanel(new BorderLayout(0, 18));
        shell.setBackground(PAGE_BG);
        shell.setBorder(new EmptyBorder(24, 24, 18, 24));

        shell.add(createHeader(detailData), BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(PAGE_BG);
        body.add(createInfoCard(detailData));
        body.add(Box.createVerticalStrut(16));

        JLabel roomTitle = new JLabel("Danh sach phong chi tiet");
        roomTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        roomTitle.setForeground(TEXT_DARK);
        roomTitle.setAlignmentX(LEFT_ALIGNMENT);
        body.add(roomTitle);
        body.add(Box.createVerticalStrut(10));

        JPanel rooms = new JPanel();
        rooms.setLayout(new BoxLayout(rooms, BoxLayout.Y_AXIS));
        rooms.setBackground(PAGE_BG);
        for (String room : detailData.getRoomList()) {
            rooms.add(createRoomCard(room, detailData));
            rooms.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(rooms);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(PAGE_BG);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setPreferredSize(new Dimension(600, 250));
        body.add(scrollPane);

        shell.add(body, BorderLayout.CENTER);
        shell.add(createFooter(), BorderLayout.SOUTH);
        add(shell, BorderLayout.CENTER);

        setSize(680, 620);
        setMinimumSize(new Dimension(620, 560));
        setResizable(true);
        setLocationRelativeTo(getParent());
    }

    private JPanel createHeader(BookingDetailData detailData) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(PAGE_BG);

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setBackground(PAGE_BG);

        JLabel title = new JLabel("Chi tiet phieu dat " + safe(detailData.getBookingId()));
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel("Thong tin khach hang va tung phong trong phieu dat.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(subtitle);
        header.add(titleBox, BorderLayout.CENTER);
        return header;
    }

    private JPanel createInfoCard(BookingDetailData detailData) {
        JPanel card = createCardPanel();
        card.setLayout(new GridBagLayout());
        card.setAlignmentX(LEFT_ALIGNMENT);

        addInfoRow(card, 0, "Ma dat phong", detailData.getBookingId());
        addInfoRow(card, 1, "Ten khach hang", detailData.getCustomerName());
        addInfoRow(card, 2, "So dien thoai", detailData.getPhone());
        addInfoRow(card, 3, "Ngay check-in", detailData.getCheckInDate());
        addInfoRow(card, 4, "Ngay check-out", detailData.getCheckOutDate());
        return card;
    }

    private void addInfoRow(JPanel panel, int row, String key, String value) {
        GridBagConstraints keyGbc = new GridBagConstraints();
        keyGbc.gridx = 0;
        keyGbc.gridy = row;
        keyGbc.weightx = 0.35;
        keyGbc.fill = GridBagConstraints.HORIZONTAL;
        keyGbc.insets = new Insets(6, 0, 6, 18);

        JLabel keyLabel = new JLabel(key.toUpperCase());
        keyLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        keyLabel.setForeground(TEXT_MUTED);
        panel.add(keyLabel, keyGbc);

        GridBagConstraints valueGbc = new GridBagConstraints();
        valueGbc.gridx = 1;
        valueGbc.gridy = row;
        valueGbc.weightx = 0.65;
        valueGbc.fill = GridBagConstraints.HORIZONTAL;
        valueGbc.insets = new Insets(6, 0, 6, 0);

        JLabel valueLabel = new JLabel(safe(value));
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        valueLabel.setForeground(TEXT_DARK);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(valueLabel, valueGbc);
    }

    private JPanel createRoomCard(String rawRoom, BookingDetailData detailData) {
        RoomCardData room = parseRoom(rawRoom, detailData);
        JPanel card = createCardPanel();
        card.setLayout(new BorderLayout(16, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 116));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(CARD_BG);

        JLabel code = new JLabel(room.code());
        code.setFont(new Font("Segoe UI", Font.BOLD, 18));
        code.setForeground(TEXT_DARK);

        JLabel type = new JLabel(room.type());
        type.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        type.setForeground(TEXT_MUTED);

        JLabel time = new JLabel("Nhan: " + room.checkIn() + "    Tra: " + room.checkOut());
        time.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        time.setForeground(TEXT_MUTED);

        left.add(code);
        left.add(Box.createVerticalStrut(4));
        left.add(type);
        left.add(Box.createVerticalStrut(4));
        left.add(time);

        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBackground(CARD_BG);

        JLabel price = new JLabel(room.price());
        price.setFont(new Font("Segoe UI", Font.BOLD, 14));
        price.setForeground(TEXT_DARK);
        price.setAlignmentX(RIGHT_ALIGNMENT);

        JLabel status = new JLabel(statusLabel(room.status()));
        status.setFont(new Font("Segoe UI", Font.BOLD, 12));
        status.setOpaque(true);
        status.setBackground(statusBackground(room.status()));
        status.setForeground(statusForeground(room.status()));
        status.setBorder(new EmptyBorder(5, 10, 5, 10));
        status.setAlignmentX(RIGHT_ALIGNMENT);

        right.add(price);
        right.add(Box.createVerticalStrut(10));
        right.add(status);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        footer.setBackground(PAGE_BG);

        JButton close = new JButton("Dong");
        close.setFont(new Font("Segoe UI", Font.BOLD, 13));
        close.setForeground(Color.WHITE);
        close.setBackground(PRIMARY);
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.setBorder(new EmptyBorder(10, 22, 10, 22));
        close.addActionListener(e -> dispose());
        footer.add(close);
        return footer;
    }

    private JPanel createCardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD_BG);
        panel.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        return panel;
    }

    private RoomCardData parseRoom(String rawRoom, BookingDetailData detailData) {
        String[] parts = rawRoom != null ? rawRoom.split("\\|", -1) : new String[0];
        String code = parts.length > 0 ? safe(parts[0]) : safe(rawRoom);
        String type = parts.length > 1 ? safe(parts[1]) : "Phong";
        String checkIn = parts.length > 2 ? safe(parts[2]) : safe(detailData.getCheckInDate());
        String checkOut = parts.length > 3 ? safe(parts[3]) : safe(detailData.getCheckOutDate());
        String price = parts.length > 4 ? safe(parts[4]) : "Chua cap nhat";
        String status = parts.length > 5 ? safe(parts[5]) : "CHO_NHAN";
        return new RoomCardData(code, type, checkIn, checkOut, price, status);
    }

    private String statusLabel(String status) {
        String normalized = safe(status).replace("_", "").toUpperCase();
        if (normalized.contains("DANHAN") || normalized.contains("DANGO")) {
            return "Dang o";
        }
        if (normalized.contains("DATRA") || normalized.contains("THANHTOAN")) {
            return "Da tra";
        }
        return "Cho nhan phong";
    }

    private Color statusBackground(String status) {
        String normalized = safe(status).replace("_", "").toUpperCase();
        if (normalized.contains("DANHAN") || normalized.contains("DANGO")) {
            return new Color(220, 252, 231);
        }
        if (normalized.contains("DATRA") || normalized.contains("THANHTOAN")) {
            return new Color(224, 242, 254);
        }
        return new Color(255, 237, 213);
    }

    private Color statusForeground(String status) {
        String normalized = safe(status).replace("_", "").toUpperCase();
        if (normalized.contains("DANHAN") || normalized.contains("DANGO")) {
            return new Color(22, 101, 52);
        }
        if (normalized.contains("DATRA") || normalized.contains("THANHTOAN")) {
            return new Color(7, 89, 133);
        }
        return new Color(154, 52, 18);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    private record RoomCardData(String code, String type, String checkIn, String checkOut, String price, String status) {
    }
}
