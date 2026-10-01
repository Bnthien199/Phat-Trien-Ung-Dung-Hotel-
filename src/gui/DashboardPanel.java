package gui;

import dao.HoaDonDAO;
import dao.PhieuDatPhongDAO;
import dao.PhongDAO;
import entity.HoaDon;
import entity.PhieuDatPhong;
import entity.Phong;
import util.StatusUtils;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.List;

public class DashboardPanel extends JPanel {

    private final Color colorTotal = new Color(52, 152, 219);
    private final Color colorOccupied = new Color(231, 76, 60);
    private final Color colorVacant = new Color(46, 204, 113);
    private final Color colorRevenue = new Color(155, 89, 182);

    private final PhongDAO phongDAO = new PhongDAO();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAO();
    private final PhieuDatPhongDAO phieuDatPhongDAO = new PhieuDatPhongDAO();
    private final BookingDetailController bookingDetailController = new BookingDetailController();
    private final DecimalFormat df = new DecimalFormat("#,### VND");

    private JTable tableDanhSachDatPhong;

    public DashboardPanel() {
        setLayout(new BorderLayout(0, 30));
        setBackground(new Color(245, 246, 250));
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel lblTitle = new JLabel("TONG QUAN HE THONG");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(44, 62, 80));
        add(lblTitle, BorderLayout.NORTH);

        JPanel pnlMain = new JPanel(new BorderLayout(0, 30));
        pnlMain.setOpaque(false);

        JPanel pnlCards = new JPanel(new GridLayout(1, 4, 25, 0));
        pnlCards.setOpaque(false);
        loadStatistics(pnlCards);
        pnlMain.add(pnlCards, BorderLayout.NORTH);

        JPanel pnlLists = new JPanel(new GridLayout(1, 2, 25, 0));
        pnlLists.setOpaque(false);
        pnlLists.add(createTablePanel("PHONG VUA DAT (GAN DAY)", createRecentBookingTable()));
        pnlLists.add(createTablePanel("DANH SACH PHONG DANG TRONG", createVacantRoomTable()));
        pnlMain.add(pnlLists, BorderLayout.CENTER);

        add(pnlMain, BorderLayout.CENTER);
    }

    private void loadStatistics(JPanel container) {
        try {
            List<Phong> dsPhong = phongDAO.getAllPhong();
            int total = dsPhong.size();
            long occupied = dsPhong.stream()
                    .filter(p -> p.getTrangThai() != null && p.getTrangThai().equalsIgnoreCase("DANG_O"))
                    .count();
            long vacant = dsPhong.stream()
                    .filter(p -> p.getTrangThai() != null && p.getTrangThai().equalsIgnoreCase("TRONG"))
                    .count();

            List<HoaDon> dsHoaDon = hoaDonDAO.getAllHoaDon();
            double todayRevenue = dsHoaDon.stream()
                    .filter(hd -> hd.getNgayLap() != null && hd.getNgayLap().toLocalDate().equals(LocalDate.now()))
                    .mapToDouble(hd -> hd.getTongTienPhong() + hd.getTongTienDichVu())
                    .sum();

            container.add(new StatCard("TONG SO PHONG", String.valueOf(total), "Phong khach san", colorTotal));
            container.add(new StatCard("DANG CO KHACH", String.valueOf(occupied), "Phong dang o", colorOccupied));
            container.add(new StatCard("PHONG TRONG", String.valueOf(vacant), "San sang don khach", colorVacant));
            container.add(new StatCard("DOANH THU HOM NAY", df.format(todayRevenue), "Tong tien thu ve", colorRevenue));
        } catch (Exception e) {
            container.add(new StatCard("LOI DU LIEU", "0", "Vui long kiem tra ket noi", Color.GRAY));
        }
    }

    private JPanel createTablePanel(String title, JTable table) {
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setOpaque(false);

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(new Color(52, 73, 94));
        pnl.add(lbl, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230)));
        pnl.add(scroll, BorderLayout.CENTER);

        return pnl;
    }

    private JTable createRecentBookingTable() {
        String[] columns = {"Ma Phieu", "Khach Hang", "Ngay Dat", "Trang Thai"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        try {
            List<PhieuDatPhong> ds = phieuDatPhongDAO.getAllPhieuDatPhong();
            ds.stream()
                    .sorted((p1, p2) -> p2.getNgayDat().compareTo(p1.getNgayDat()))
                    .limit(10)
                    .forEach(p -> model.addRow(new Object[]{
                            p.getMaDatPhong(),
                            p.getKhachHang() != null ? p.getKhachHang().getMaKhachHang() : "",
                            p.getNgayDat() != null ? p.getNgayDat().toLocalDate() : "",
                            StatusUtils.bookingLabel(p.getTrangThai())
                    }));
        } catch (Exception ignored) {
        }

        tableDanhSachDatPhong = setupModernTable(model);
        tableDanhSachDatPhong.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int selectedRow = tableDanhSachDatPhong.rowAtPoint(e.getPoint());
                    if (selectedRow < 0) {
                        return;
                    }
                    String bookingId = String.valueOf(tableDanhSachDatPhong.getValueAt(selectedRow, 0));
                    Window parentWindow = SwingUtilities.getWindowAncestor(DashboardPanel.this);
                    JFrame parentFrame = parentWindow instanceof JFrame ? (JFrame) parentWindow : null;
                    bookingDetailController.openBookingDetail(parentFrame, bookingId);
                }
            }
        });
        return tableDanhSachDatPhong;
    }

    private JTable createVacantRoomTable() {
        String[] columns = {"Ma Phong", "Loai Phong", "So Nguoi", "Gia Phong"};
        DefaultTableModel model = new DefaultTableModel(columns, 0);

        try {
            List<Phong> ds = phongDAO.filterPhong("TRONG", null);
            ds.forEach(p -> model.addRow(new Object[]{
                    p.getMaPhong(),
                    p.getLoaiPhong(),
                    p.getSoNguoi(),
                    df.format(p.getGiaPhong())
            }));
        } catch (Exception ignored) {
        }

        return setupModernTable(model);
    }

    private JTable setupModernTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(240, 240, 240));
        table.setGridColor(new Color(245, 245, 245));
        table.setSelectionBackground(new Color(52, 152, 219, 50));
        table.setSelectionForeground(Color.BLACK);
        table.setShowVerticalLines(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        return table;
    }

    private static class StatCard extends JPanel {
        private final String title;
        private final String value;
        private final String subtitle;
        private final Color color;

        private StatCard(String title, String value, String subtitle, Color color) {
            this.title = title;
            this.value = value;
            this.subtitle = subtitle;
            this.color = color;
            setPreferredSize(new Dimension(250, 160));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20));
            g2.setColor(color);
            g2.fill(new RoundRectangle2D.Double(0, 0, 8, getHeight(), 0, 0));
            g2.setColor(new Color(127, 140, 141));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString(title, 25, 40);
            g2.setColor(new Color(44, 62, 80));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
            g2.drawString(value, 25, 80);
            g2.setColor(new Color(189, 195, 199));
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            g2.drawString(subtitle, 25, 110);
        }
    }
}
