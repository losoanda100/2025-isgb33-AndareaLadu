import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;

import io.javalin.Javalin;
import io.javalin.http.Context;

public class Stub {

    private static final Logger logger = LoggerFactory.getLogger(Stub.class);

    private static MongoCollection<Document> myCollection;

    public static void main(String[] args) throws Exception {
        initMongo();
        startServer();
    }

    private static void initMongo() throws Exception {

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

        myCollection = database.getCollection("Movies");

        logger.info("Connected to MongoDB, using database '{}'", dbName);
    }

    private static void startServer() {

        Javalin app = Javalin.create().start(4567);

        app.get("/title/{title}", Stub::movieTitle);
        app.get("/fullplot/{title}", Stub::fullplot);
        app.get("/cast/{title}", Stub::cast);
        app.get("/genre/{genre}", Stub::genre);
        app.get("/actor/{actor}", Stub::actor);
    }

    private static void movieTitle(Context ctx) {

        String title = ctx.pathParam("title");

        Document doc = myCollection
                .find(Filters.eq("title", title))
                .projection(
                        Projections.exclude("_id", "poster", "cast", "fullplot")
                )
                .first();

        if (doc != null) {
            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(doc.toJson());
        } else {
            ctx.status(404);
            ctx.contentType("application/json");
            ctx.result(jsonError("Movie not found.").toString());
        }
    }

    private static void fullplot(Context ctx) {

        String title = ctx.pathParam("title");

        Document doc = myCollection
                .find(Filters.eq("title", title))
                .projection(
                        Projections.fields(
                                Projections.include("title", "fullplot"),
                                Projections.excludeId()
                        )
                )
                .first();

        if (doc != null) {
            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(doc.toJson());
        } else {
            ctx.status(404);
            ctx.contentType("application/json");
            ctx.result(jsonError("Movie not found.").toString());
        }
    }

    private static void cast(Context ctx) {

        String title = ctx.pathParam("title");

        Document doc = myCollection
                .find(Filters.eq("title", title))
                .projection(
                        Projections.fields(
                                Projections.include("title", "cast"),
                                Projections.excludeId()
                        )
                )
                .first();

        if (doc != null) {
            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(doc.toJson());
        } else {
            ctx.status(404);
            ctx.contentType("application/json");
            ctx.result(jsonError("Movie not found.").toString());
        }
    }

    private static void genre(Context ctx) {

        String genre = ctx.pathParam("genre");

        JsonArray result = new JsonArray();

        MongoCursor<Document> iterator = myCollection
                .find(Filters.eq("genres", genre))
                .projection(
                        Projections.exclude("_id", "poster", "cast", "fullplot")
                )
                .limit(10)
                .iterator();

        while (iterator.hasNext()) {

            Document doc = iterator.next();

            result.add(
                    JsonParser.parseString(doc.toJson())
            );
        }

        iterator.close();

        if (result.size() > 0) {
            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(result.toString());
        } else {
            ctx.status(404);
            ctx.contentType("application/json");
            ctx.result(jsonError("No movies found for genre.").toString());
        }
    }

    private static void actor(Context ctx) {

        String actor = ctx.pathParam("actor");

        JsonArray result = new JsonArray();

        MongoCursor<Document> iterator = myCollection
                .find(Filters.eq("cast", actor))
                .projection(
                        Projections.fields(
                                Projections.include("title"),
                                Projections.excludeId()
                        )
                )
                .limit(10)
                .iterator();

        while (iterator.hasNext()) {

            Document doc = iterator.next();

            String movieTitle = doc.getString("title");

            result.add(movieTitle);
        }

        iterator.close();

        if (result.size() > 0) {
            ctx.status(200);
            ctx.contentType("application/json");
            ctx.result(result.toString());
        } else {
            ctx.status(404);
            ctx.contentType("application/json");
            ctx.result(jsonError("Actor not found.").toString());
        }
    }

    private static JsonObject jsonError(String error) {

        JsonObject obj = new JsonObject();

        obj.addProperty("error", error);

        return obj;
    }
}