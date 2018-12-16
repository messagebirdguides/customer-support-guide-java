import com.messagebird.MessageBirdClient;
import com.messagebird.MessageBirdService;
import com.messagebird.MessageBirdServiceImpl;
import com.mongodb.client.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static spark.Spark.get;
import static spark.Spark.post;

public class SMSCustomerSupport {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        // Create a MessageBirdService
        final MessageBirdService messageBirdService = new MessageBirdServiceImpl(dotenv.get("MESSAGEBIRD_API_KEY"));
        // Add the service to the client
        final MessageBirdClient messageBirdClient = new MessageBirdClient(messageBirdService);

        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        MongoDatabase database = mongoClient.getDatabase("myproject");


        // Handle incoming webhooks
        post("/webhook",
                (req, res) ->
                {
                    JSONParser parser = new JSONParser();
                    JSONObject requestPayload;

                    requestPayload = (JSONObject) parser.parse(req.body());

                    // Read request
                    String number = (String) requestPayload.get("originator");
                    String text = (String) requestPayload.get("body");

                    // Find tickets for number in our database
                    MongoCollection<Document> tickets = database.getCollection("tickets");
                    Document doc = tickets.find(eq("number", number)).first();

                    BigInteger phoneNumber = new BigInteger(number);
                    final List<BigInteger> phones = new ArrayList<BigInteger>();
                    phones.add(phoneNumber);

                    if (doc == null) {
                        // Creating a new ticket
                        JSONObject message = new JSONObject();
                        JSONArray messages = new JSONArray();
                        message.put("direction", "in");
                        message.put("content", text);
                        messages.add(message);

                        Document entry = new Document("number", number)
                                .append("open", true)
                                .append("messages", messages);
                        tickets.insertOne(entry);

                        // After creating a new ticket, send a confirmation
                        String shortId = entry.getObjectId("_id").toString().substring(18, 24);

                        // Notify the user
                        messageBirdClient.sendMessage(dotenv.get("MESSAGEBIRD_ORIGINATOR"), "Thanks for contacting customer support! Your ticket ID is " + shortId, phones);
                    } else {
                        // Add an inbound message to the existing ticket
                        JSONObject message = new JSONObject();
                        message.put("direction", "in");
                        message.put("content", text);
                        ArrayList<JSONObject> messages = (ArrayList<JSONObject>) doc.get("messages");
                        messages.add(message);

                        Bson query = combine(set("open", true), set("messages", messages));
                        tickets.updateOne(eq("number", number), query);
                    }

                    // Return any response, MessageBird won't parse this
                    res.status(200);
                    return "";
                }
        );

        get("/admin",
                (req, res) ->
                {
                    Map<String, Object> model = new HashMap<>();

                    // Find all open tickets
                    MongoCollection<Document> tickets = database.getCollection("tickets");
                    FindIterable<Document> iterable = tickets.find(eq("open", true));
                    MongoCursor<Document> cursor = iterable.iterator();

                    JSONObject ticket = new JSONObject();
                    JSONArray results = new JSONArray();

                    while (cursor.hasNext()) {
                        Document d = cursor.next();
                        ticket.put("shortId", d.get("_id").toString().substring(18, 24));
                        ticket.put("_id", d.get("_id").toString());
                        ticket.put("number", d.get("number"));
                        ticket.put("messages", d.get("messages"));
                        results.add(ticket);
                    }

                    // Show a page with tickets
                    model.put("tickets", results.toArray());
                    return new ModelAndView(model, "admin.handlebars");
                },

                new HandlebarsTemplateEngine()
        );

        post("/reply",
                (req, res) ->
                {
                    String id = req.queryParams("id");
                    String content = req.queryParams("content");

                    // Get number of subscribers to show on the form
                    MongoCollection<Document> tickets = database.getCollection("tickets");

                    Document doc = tickets.find(eq("_id", new ObjectId(id))).first();

                    if (doc != null) {
                        JSONObject message = new JSONObject();
                        message.put("direction", "out");
                        message.put("content", content);
                        ArrayList<JSONObject> messages = (ArrayList<JSONObject>) doc.get("messages");
                        messages.add(message);

                        Bson query = combine(set("open", true), set("messages", messages));
                        tickets.updateOne(eq("_id", new ObjectId(id)), query);

                        // convert String number into acceptable format
                        BigInteger phoneNumber = new BigInteger(doc.get("number").toString());
                        final List<BigInteger> phones = new ArrayList<BigInteger>();
                        phones.add(phoneNumber);

                        // Send reply to customer
                        messageBirdClient.sendMessage(dotenv.get("MESSAGEBIRD_ORIGINATOR"), content, phones);
                    }

                    // Return to previous page
                    res.redirect("/admin");
                    return "";
                }
        );
    }
}