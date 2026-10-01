package util;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class TableUtils {

    /**
     * Yêu cầu 1: Căn giữa toàn bộ dữ liệu và header của một JTable
     *
     * @param table JTable cần căn giữa
     */
    public static void centerAlignTable(JTable table) {
        if (table == null) return;

        // Căn giữa cho Header
        JTableHeader header = table.getTableHeader();
        if (header != null && header.getDefaultRenderer() instanceof DefaultTableCellRenderer) {
            DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
            headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        }

        // Căn giữa cho toàn bộ Cell trong Table
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        TableColumnModel columnModel = table.getColumnModel();
        for (int i = 0; i < columnModel.getColumnCount(); i++) {
            columnModel.getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    /**
     * Yêu cầu 2: Cố định cột bên phải (Fixed Right Column Techniques)
     * Phương pháp này sẽ tạo một bảng mới với cấu hình đồng bộ cùng bảng chính,
     * đặt vào cùng đường scroll và loại bỏ cột tương ứng trong bảng gốc đi.
     * 
     * Hướng dẫn sử dụng:
     * Chèn table trả về bởi hàm này vào phía *Bên phải* (EAST) của một Container (Panel)
     * chứa JScrollPane gốc của mainTable, và ẩn cột Action ở mainTable đi.
     * 
     * @param mainTable Bảng gốc
     * @param mainScrollPane JScrollPane chứa bảng gốc
     * @param fixedColumnIndex Index của cột muốn cố định nằm bên phải
     * @return 1 JScrollPane mới chứa bảng phụ chỉ hiển thị phần cột bị đóng băng (Cố định phải)
     */
    public static JScrollPane fixRightColumn(JTable mainTable, JScrollPane mainScrollPane, int fixedColumnIndex) {
        // Sao chép model từ bảng chính
        JTable fixedTable = new JTable(mainTable.getModel());
        
        // Đảm bảo không auto-size, không cho chọn đè nhiều ô chồng chéo
        fixedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        fixedTable.setSelectionModel(mainTable.getSelectionModel()); // Đồng bộ selection

        // Đồng bộ chiều cao hàng (Row height)
        fixedTable.setRowHeight(mainTable.getRowHeight());

        // Loại bỏ mọi cột không phải là cột fixed trong fixedTable
        TableColumnModel fixedColumnModel = fixedTable.getColumnModel();
        for (int i = fixedColumnModel.getColumnCount() - 1; i >= 0; i--) {
            if (i != fixedColumnIndex) {
                fixedColumnModel.removeColumn(fixedColumnModel.getColumn(i));
            }
        }

        // Loại bỏ cột fixed ra khỏi mainTable để mainTable không bị lặp lại cột đó
        TableColumn fixedColumnInMain = mainTable.getColumnModel().getColumn(fixedColumnIndex);
        mainTable.getColumnModel().removeColumn(fixedColumnInMain);

        // Tạo JScrollPane cho fixed table
        JScrollPane fixedScrollPane = new JScrollPane(fixedTable);
        fixedScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        fixedScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Loại bỏ border để 2 bảng dính liền mảnh
        fixedScrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Điều chỉnh lại chiều rộng của cái ScrollPane mới vừa đủ với cột cố định
        int preferredWidth = fixedTable.getColumnModel().getColumn(0).getPreferredWidth();
        fixedScrollPane.setPreferredSize(new Dimension(preferredWidth, mainScrollPane.getPreferredSize().height));

        // Tự động kéo qua lại (ngang và dọc)
        mainScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                fixedScrollPane.getVerticalScrollBar().setValue(e.getValue());
            }
        });
        fixedScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                mainScrollPane.getVerticalScrollBar().setValue(e.getValue());
            }
        });

        // Thiết lập layout (bước này thường chạy do bên Main form) 
        // Ví dụ trong Frame cài đặt:
        // container.setLayout(new BorderLayout());
        // container.add(mainScrollPane, BorderLayout.CENTER);
        // container.add(fixedScrollPane, BorderLayout.EAST); <--- BẢNG FOCUSED TRẢ VỀ SẼ NHÉT Ở ĐÂY

        return fixedScrollPane;
    }
}
