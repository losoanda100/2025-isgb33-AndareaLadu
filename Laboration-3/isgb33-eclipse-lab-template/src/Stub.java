import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.bson.Document;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

public class Stub {

    public static void main(String[] args) throws Exception {

        Properties prop = new Properties();

        try (InputStream input = new FileInputStream("connection.properties")) {
            prop.load(input);
        }

        String connString = prop.getProperty("db.connection_string");
        String dbName = prop.getProperty("db.name");

        ConnectionString connectionString = new ConnectionString(connString);

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();

        MongoClient mongoClient = MongoClients.create(settings);

        MongoDatabase database = mongoClient.getDatabase(dbName);

        MongoCollection<Document> myCollection = database.getCollection("Movies");

        JFrame frame = new JFrame("Filmförslag.nu");
        frame.setSize(400, 500);
        frame.setLayout(null);

        JTextArea textArea = new JTextArea();
        textArea.setBounds(10, 10, 365, 400);
        textArea.setLineWrap(true);

        JTextField textField = new JTextField("");
        textField.setBounds(10, 415, 260, 40);

        JButton searchButton = new JButton("Sök");
        searchButton.setBounds(275, 415, 100, 40);

        frame.add(textArea);
        frame.add(textField);
        frame.add(searchButton);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        searchButton.addActionListener(e -> {

            String genre = textField.getText();

            AggregateIterable<Document> docs = myCollection.aggregate(Arrays.asList(

                    Aggregates.match(
                            Filters.eq("genres", genre)
                    ),

                    Aggregates.project(
                            Projections.fields(
                                    Projections.excludeId(),
                                    Projections.include("title", "year")
                            )
                    ),

                    Aggregates.sort(
                            Sorts.descending("title")
                    ),

                    Aggregates.limit(10)
            ));

            textArea.setText("");

            boolean found = false;

            for (Document d : docs) {

                found = true;

                String title = d.get("title").toString();
                Object yearObj = d.get("year");

                String year = null;

                if (yearObj != null) {
                    year = yearObj.toString();
                }

                textArea.append(title + ", " + year + "\n");
            }

            if (!found) {
                textArea.setText("Ingen film matchade kategorin");
            }
        });
    }
}