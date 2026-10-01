import connectDB.ConnectDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class DebugCheckout {
    public static void main(String[] args) {
        try {
            System.out.println("Connecting to database...");
            Connection conn = ConnectDB.getConnection();
            System.out.println("Connected successfully!");

            String[] queries = {
                "SELECT * FROM Phong WHERE MaPhong = 'P301'",
                "SELECT * FROM ChiTietPhieuDat WHERE MaPhong = 'P301'",
                "SELECT * FROM PhieuDatPhong WHERE MaDatPhong IN (SELECT MaDatPhong FROM ChiTietPhieuDat WHERE MaPhong = 'P301')",
                "SELECT * FROM HoaDon WHERE MaDatPhong IN (SELECT MaDatPhong FROM ChiTietPhieuDat WHERE MaPhong = 'P301')"
            };

            for (String sql : queries) {
                System.out.println("\n=== RUNNING: " + sql + " ===");
                try (PreparedStatement stmt = conn.prepareStatement(sql);
                     ResultSet rs = stmt.executeQuery()) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    for (int i = 1; i <= cols; i++) {
                        System.out.print(md.getColumnName(i) + "\t");
                    }
                    System.out.println();
                    int count = 0;
                    while (rs.next()) {
                        count++;
                        for (int i = 1; i <= cols; i++) {
                            System.out.print(rs.getObject(i) + "\t");
                        }
                        System.out.println();
                    }
                    System.out.println("Total rows: " + count);
                } catch (Exception ex) {
                    System.err.println("Error running query: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
