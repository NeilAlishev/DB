import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * @author Neil Alishev
 */
public class InsertWords {
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/db_practice9";

    private static final String USER = "neil";
    private static final String PASS = "";

    public static void main(String[] args) throws FileNotFoundException {
        Connection conn = getConnection();
        PreparedStatement preparedStatement;

        Scanner scanner = new Scanner(new File("random_words"));

        String[] words = new String[1001];
        int counter = 0;
        while (scanner.hasNextLine())
            words[counter++] = scanner.nextLine();

        try {
            preparedStatement = conn.prepareStatement("INSERT INTO words VALUES (?, ?)");

            for (int i = 1; i < 1000; i++) {
                preparedStatement.setInt(1, i + 1);
                preparedStatement.setString(2, words[i]);

                preparedStatement.executeUpdate();
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
