import java.sql.*;

/**
 * @author Neil Alishev
 */
public class Etl {
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/db_practice7";

    private static final String USER = "neil";
    private static final String PASS = "";

    public static void main(String[] args) {
        Connection conn = getConnection();

        try {
            Statement statement = conn.createStatement();

            // lookup log table for new modifications
            ResultSet rs = statement.executeQuery("SELECT * FROM log where status = false");

            rs.next();
            String tableName = rs.getString("table_name");
            int modifiedId = rs.getInt("modified_id");
            String action = rs.getString("action");

            switch (action) {
                case "INSERT":
                    // take row from the original table and insert it to the replica
                    ResultSet rs1 = statement.executeQuery("SELECT * FROM " + tableName + " WHERE id = " + modifiedId);
                    rs1.next();

                    String name = rs1.getString("name");
                    boolean someBool = rs1.getBoolean("some_bool");

                    statement.executeUpdate("INSERT INTO replica(name, some_bool) VALUES('" + name + "'," + someBool + ")");

                    // update log status
                    statement.executeUpdate("UPDATE log set status = true where action = 'INSERT' and modified_id = "
                            + modifiedId);
                    break;
                case "UPDATE":
                    // take row from the original table and update the row in the replica
                    ResultSet rs2 = statement.executeQuery("SELECT * FROM " + tableName + " WHERE id = " + modifiedId);
                    rs2.next();

                    String name2 = rs2.getString("name");
                    boolean someBool2 = rs2.getBoolean("some_bool");

                    statement.executeUpdate("UPDATE replica set name = '" + name2 + "', some_bool = " + someBool2
                            + " WHERE id = " + modifiedId);

                    // update log status
                    statement.executeUpdate("UPDATE log set status = true where action = 'UPDATE' and modified_id = "
                            + modifiedId);
                    break;
                case "DELETE":
                    // delete row from the replica
                    statement.executeUpdate("DELETE FROM replica where id = " + modifiedId);

                    // update log status
                    statement.executeUpdate("UPDATE log set status = true where action = 'DELETE' and modified_id = "
                            + modifiedId);
                    break;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static Connection getConnection() {
        Connection conn = null;

        try {
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (ClassNotFoundException | SQLException e1) {
            e1.printStackTrace();
        }

        return conn;
    }
}
