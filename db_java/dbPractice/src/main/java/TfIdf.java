import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

/**
 * @author Neil Alishev
 */
public class TfIdf {
    private static final String JDBC_DRIVER = "org.postgresql.Driver";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/db_practice";

    private static final String USER = "neil";
    private static final String PASS = "";

    private static final String ADD_DOCUMENT_SQL = "INSERT INTO documents(file_name) values(?)";

    private static final String ADD_WORD_SQL = "INSERT INTO words(word, document_id) values(?, ?)";

    private static final String WORD_COUNT_IN_DOCUMENT = "SELECT count(*) FROM words " +
            "WHERE document_id = ? AND LOWER(word) = LOWER(?)";

    public static void main(String[] args) throws FileNotFoundException {
//        insertTexts();

        String word1 = "ощущение";
        String word2 = "лишь";
        String word3 = "словно";

        calculate(word1);
        calculate(word2);
        calculate(word3);
    }

    private static void calculate(String word) {
        System.out.println("Calculating TF.IDF for word " + word + ":");

        double idf = idf(word);
        List<Double> tfs = tf(word);

        List<String> documentNames = readDocumentNames();

        for (int i = 0; i < tfs.size(); i++)
            System.out.println(documentNames.get(i) + " : " + tfs.get(i) * idf);

        System.out.println();
    }

    private static List<Double> tf(String word) {
        Connection conn = getConnection();
        PreparedStatement preparedStatement;
        List<Double> tfs = new ArrayList<>();

        try {
            List<Integer> documentIds = readDocumentIds();

            // calculate max occurrence
            int max = 0;
            preparedStatement = conn.prepareStatement(WORD_COUNT_IN_DOCUMENT);
            for (int documentId : documentIds) {
                preparedStatement.setInt(1, documentId);
                preparedStatement.setString(2, word);

                ResultSet resultSet = preparedStatement.executeQuery();
                resultSet.next();
                int wordCount = resultSet.getInt(1);

                if (wordCount > max)
                    max = wordCount;
            }

            // calculate tf for every document
            tfs = new ArrayList<>();
            preparedStatement = conn.prepareStatement(WORD_COUNT_IN_DOCUMENT);
            for (int documentId : documentIds) {
                preparedStatement.setInt(1, documentId);
                preparedStatement.setString(2, word);

                ResultSet resultSet = preparedStatement.executeQuery();
                resultSet.next();

                tfs.add(resultSet.getInt(1) / (double) max);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tfs;
    }

    private static double idf(String word) {
        Connection conn = getConnection();
        PreparedStatement preparedStatement;

        try {
            List<Integer> documentIds = readDocumentIds();

            preparedStatement = conn.prepareStatement(WORD_COUNT_IN_DOCUMENT);
            int count = 0;
            for (int documentId : documentIds) {
                preparedStatement.setInt(1, documentId);
                preparedStatement.setString(2, word);

                ResultSet resultSet = preparedStatement.executeQuery();
                resultSet.next();

                if (resultSet.getInt(1) > 0)
                    count++;
            }

            return Math.log(documentIds.size() / (double) count);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    private static List<Integer> readDocumentIds() {
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        List<Integer> documentIds = null;

        try {
            preparedStatement = getConnection().prepareStatement("SELECT id from documents");
            resultSet = preparedStatement.executeQuery();

            documentIds = new ArrayList<>();
            while (resultSet.next()) {
                documentIds.add(resultSet.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return documentIds;
    }

    private static List<String> readDocumentNames() {
        PreparedStatement preparedStatement;
        ResultSet resultSet;
        List<String> documentNames = null;

        try {
            preparedStatement = getConnection().prepareStatement("SELECT file_name from documents");
            resultSet = preparedStatement.executeQuery();

            documentNames = new ArrayList<>();
            while (resultSet.next()) {
                documentNames.add(resultSet.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return documentNames;
    }

    private static Map<String, List<String>> readFiles() throws FileNotFoundException {
        Map<String, List<String>> result = new HashMap<>();

        File dir = new File("/Users/neil/БД/db_java/dbPractice/textFiles");
        File[] textFiles = dir.listFiles();

        assert textFiles != null;
        for (File file : textFiles) {
            List<String> words = new ArrayList<>();

            Scanner s = new Scanner(file);
            s.useDelimiter("[^\\p{IsCyrillic}]");

            while (s.hasNext()) {
                String wordToInsert = s.next();

                if (!wordToInsert.isEmpty())
                    words.add(wordToInsert);
            }

            result.put(file.getName(), words);

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

    private static void insertTexts() throws FileNotFoundException {
        Map<String, List<String>> textFiles = readFiles();

        Connection conn = getConnection();
        PreparedStatement preparedStatement = null;

        try {
            for (Map.Entry<String, List<String>> document : textFiles.entrySet()) {
                preparedStatement = conn.prepareStatement(ADD_DOCUMENT_SQL, Statement.RETURN_GENERATED_KEYS);
                preparedStatement.setString(1, document.getKey());

                preparedStatement.executeUpdate();

                long documentId;
                try (ResultSet generatedKey = preparedStatement.getGeneratedKeys()) {
                    if (generatedKey.next()) {
                        documentId = generatedKey.getLong(1);
                    } else {
                        throw new SQLException("Creating document failed, no ID obtained.");
                    }
                }

                preparedStatement = conn.prepareStatement(ADD_WORD_SQL);
                preparedStatement.setLong(2, documentId);

                for (String word : document.getValue()) {
                    preparedStatement.setString(1, word);
                    preparedStatement.executeUpdate();
                }
            }

            assert preparedStatement != null;
            preparedStatement.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (preparedStatement != null)
                    preparedStatement.close();
            } catch (SQLException ignored) {
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}