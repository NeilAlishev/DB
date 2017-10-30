import java.sql.*;

/**
 * @author Neil Alishev
 */
public class Practice {
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/db_practice5";

    private static final String USER = "neil";
    private static final String PASS = "";

    public static void main(String[] args) {
        Connection conn = getConnection();
        PreparedStatement preparedStatement;

        try {
            preparedStatement = conn.prepareStatement("DELETE FROM simple_table WHERE id = ?");

            for (int i = 1; i < 1000000; i++) {
                preparedStatement.setLong(1, i);

                preparedStatement.addBatch();
                System.out.println(i);
            }

            preparedStatement.executeBatch();

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

/*
create table simple_table (
  id bigint,
  name varchar
);

alter system set AUTOVACUUM = off;
select pg_reload_conf();

select pg_size_pretty(pg_total_relation_size('simple_table'));

 */