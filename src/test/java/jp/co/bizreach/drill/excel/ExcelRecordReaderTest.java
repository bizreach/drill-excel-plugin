package jp.co.bizreach.drill.excel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

class ExcelRecordReaderTest {

    @Test
    void query() throws Exception {
        Class.forName("org.apache.drill.jdbc.Driver");
        try (Connection con = DriverManager.getConnection("jdbc:drill:drillbit=localhost");
            Statement st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM dfs.`/tmp/emp.xlsx` WHERE age > 35.0 ORDER BY age");
        ) {
            assertTrue(rs.next());
            assertEquals("Sara", rs.getString(1));
            assertEquals("36.0", rs.getString(2));
            assertTrue(rs.next());
            assertEquals("Bob", rs.getString(1));
            assertEquals("40.0", rs.getString(2));
            assertFalse(rs.next());
        }
    }

}
