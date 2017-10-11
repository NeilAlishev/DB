import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

/**
 * @author Neil Alishev
 */
public class GenomeMatching {
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/db_practice3";

    private static final String USER = "neil";
    private static final String PASS = "";

    // |intersection| / |union|
    private static final String JACCARD_SIMILARITY_QUERY =
            "SELECT ((SELECT COUNT(*) FROM ((SELECT shingle FROM shingles WHERE genome_id = 1 AND shingle_k = ?) INTERSECT " +
                    "(SELECT shingle FROM shingles WHERE genome_id = 2 AND shingle_k = ?)) AS intersectionCount)::DECIMAL" +
                    " / (SELECT COUNT(*) FROM ((SELECT shingle FROM shingles WHERE genome_id = 1 AND shingle_k = ?) UNION " +
                    "(SELECT shingle FROM shingles WHERE genome_id = 2 AND shingle_k = ?)) AS unionCount)) AS result";

    public static void main(String[] args) throws FileNotFoundException {
        // This method inserts all shingles of size k from both genomes into the database
        // insertShingles(2);
        // insertShingles(5);
        // insertShingles(9);

        // After all shingles are inserted, we can perform calculations for each set of shingles
        System.out.println(calculateSimilarity(2)); // 1.0
        System.out.println(calculateSimilarity(5)); // 1.0
        System.out.println(calculateSimilarity(9)); // 0.6387107895278977
    }

    private static double calculateSimilarity(int k) {
        Connection conn = getConnection();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = conn.prepareStatement(JACCARD_SIMILARITY_QUERY);
            setShingleK(preparedStatement, k);

            ResultSet resultSet = preparedStatement.executeQuery();
            resultSet.next();

            return resultSet.getDouble(1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }

        return -1;
    }

    private static void setShingleK(PreparedStatement preparedStatement, int k) throws SQLException {
        preparedStatement.setInt(1, k);
        preparedStatement.setInt(2, k);
        preparedStatement.setInt(3, k);
        preparedStatement.setInt(4, k);
    }

    /*
        result[0] - Genome_1.txt
        result[1] - Genome_2.txt
     */
    private static String[] readFiles() throws FileNotFoundException {
        String[] result = new String[2];

        File dir = new File("/Users/neil/БД/repo/db_java/dbPractice/genomes");
        File[] textFiles = dir.listFiles();

        assert textFiles != null;
        int idx = 0;
        for (File file : textFiles) {
            if (file.getName().equals(".DS_Store"))
                continue;

            Scanner s = new Scanner(file);
            StringBuilder sb = new StringBuilder("");

            while (s.hasNext())
                sb.append(s.nextLine());

            result[idx++] = sb.toString();
            s.close();
        }

        return result;
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

    private static void insertShingles(int k) throws FileNotFoundException {
        String[] genomes = readFiles();

        Connection conn = getConnection();
        PreparedStatement preparedStatement = null;

        int genomeId = 1;
        try {
            for (String genome : genomes) {
                Set<String> shingles = createShingles(genome, k);

                preparedStatement = conn.prepareStatement("INSERT INTO shingles VALUES(?,?,?)");
                for (String shingle : shingles) {
                    preparedStatement.setInt(1, genomeId);
                    preparedStatement.setString(2, shingle);
                    preparedStatement.setInt(3, k);

                    preparedStatement.addBatch();
                }

                preparedStatement.executeBatch();

                genomeId++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }

    private static Set<String> createShingles(String genome, int k) {
        Set<String> result = new HashSet<>();

        for (int i = 0; i < genome.length() - k; i++)
            result.add(genome.substring(i, i + k));

        return result;
    }
}