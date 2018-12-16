# Building an SMS-Based Customer Support System with MessageBird

### ⏱ 30 min build time

## Why build SMS customer support?

In this MessageBird Developer Tutorial, we'll show you how to provide an excellent user experience by managing your inbound support tickets with this real-time SMS communication application between consumers and companies powered by the [MessageBird SMS Messaging API](https://developers.messagebird.com/docs/sms-messaging).

People love communicating in real time, regardless of whether it’s their friends or to a business. Real time support in a comfortable medium helps to create an excellent support experience that can contribute to retaining users for life.

On the business side, Support teams need to organize communication with their customers, often using ticket systems to combine all messages for specific cases in a shared view for support agents.

We'll walk you through the following steps:

* Customers can send any message to a virtual mobile number (VMN) created and published by the company. Their message becomes a support ticket, and they receive an automated confirmation with a ticket ID for their reference.
* Any subsequent message from the same number is added to the same support ticket; there's no additional confirmation.
* Support agents can view all messages in a web view and reply to them.

## Getting Started

Our sample application is built in Java using the [Spark framework](http://sparkjava.com/). You can download or clone the complete source code from the [MessageBird Developer Tutorials GitHub repository](https://github.com/messagebirdguides/customer-support-guide-java) to run the application on your computer and follow along with the tutorial. To run the application, you will need [Java 1.8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) and [Maven](https://maven.apache.org/) installed.

The `pom.xml` file has all the dependencies the project needs. Your IDE should be configured to automatically install them.

We use [MongoDB](https://mongodb.github.io/mongo-java-driver/) to provide an in-memory database for testing, so you don't need to configure an external database.

## Prerequisites for Receiving Messages

### Overview

The support system receives incoming messages. From a high-level viewpoint, receiving with MessageBird is relatively simple: an application defines a _webhook URL_, which you assign to a number purchased in the MessageBird Dashboard using a flow. A [webhook](https://en.wikipedia.org/wiki/Webhook) is a URL on your site that doesn't render a page to users but is like an API endpoint that can be triggered by other servers. Every time someone sends a message to that number, MessageBird collects it and forwards it to the webhook URL where you can process it.

When working with webhooks, an external service like MessageBird needs to access your application, so the URL must be public. During development, though, you're typically working in a local development environment that is not publicly available. There are various tools and services available that allow you to quickly expose your development environment to the Internet by providing a tunnel from a public URL to your local machine. One of the most popular tools is [ngrok](https://ngrok.com/).

You can [download ngrok here for free](https://ngrok.com/download) as a single-file binary for almost every operating system, or optionally sign up for an account to access additional features.

You can start a tunnel by providing a local port number on which your application runs. We will run our Java server on port 4567, so you can launch your tunnel with this command:

```
ngrok http 4567
```

After you've launched the tunnel, ngrok displays your temporary public URL along with some other information. We'll need that URL in a minute.

Another common tool for tunneling your local machine is [localtunnel.me](https://localtunnel.me/), which you can have a look at if you're facing problems with ngrok. It works in virtually the same way but requires you to install [NPM](https://www.npmjs.com/) first.

### Getting an Inbound Number

A requirement for receiving messages is a dedicated inbound number. Virtual mobile numbers look and work in a similar way to regular mobile numbers, however, instead of being attached to a mobile device via a SIM card, they live in the cloud and can process incoming SMS and voice calls. MessageBird offers numbers from different countries for a low monthly fee; [feel free to explore our low-cost programmable and configurable numbers](https://www.messagebird.com/en/numbers).

Purchasing a number is quite easy:

Purchasing a number is quite easy:

1. Go to the '[Numbers](https://dashboard.messagebird.com/en/numbers)' section in the left-hand side of your Dashboard and click the blue button '[Buy a number](https://dashboard.messagebird.com/en/vmn/buy-number)' in the top-right side of your screen.
2. Pick the country in which you and your customers are located, and make sure both the SMS capability is selected.
3. Choose one number from the selection and the duration for which you want to pay now.
4. Confirm by clicking 'Buy Number' in the bottom-right of your screen.
![Buy a number](https://developers.messagebird.com/assets/images/screenshots/subscription-node/buy-a-number.png)

Awesome, you’ve set up your first virtual mobile number! 🎉

**Pro-Tip**: Check out our Help Center for more information about [virtual mobile numbers])https://support.messagebird.com/hc/en-us/sections/201958489-Virtual-Numbers and [country

### Connect Number to the Webhook

So you have a number now, but MessageBird has no idea what to do with it. That's why now you need to define a Flow that links your number to your webhook. This is how you do it:

#### STEP ONE
Go to [Flow Builder](https://dashboard.messagebird.com/en/flow-builder), choose the template ‘Call HTTP endpoint with SMS’ and click ‘Try this flow’.

![Call HTTP with SMS](https://developers.messagebird.com/assets/images/screenshots/support-node/call-HTTP-with-SMS.png)

#### STEP TWO
This template has two steps. Click on the first step ‘SMS’ and select the number or numbers you’d like to attach the flow to. Now, click on the second step ‘Forward to URL’ and choose POST as the method; copy the output from the ngrok command in the URL and add `/webhook` at the end—this is the name of the route we use to handle incoming messages in our sample application. Click on ‘Save’ when ready.

![Forward to URL](https://developers.messagebird.com/assets/images/screenshots/support-node/forward-to-URL.png)

#### STEP THREE
**Ready!** Hit ‘Publish’ on the right top of the screen to activate your flow. Well done, another step closer to building a customer support system for SMS-based communication!

![Support Receiver](https://developers.messagebird.com/assets/images/screenshots/support-node/support-receiver.png)

**Pro-Tip:** You can edit the name of the flow by clicking on the icon next to button ‘Back to Overview’ and pressing ‘Rename flow’.

![Rename Flow](https://developers.messagebird.com/assets/images/screenshots/support-node/rename-flow.png)

## Configuring the MessageBird SDK

The MessageBird SDK and an API key are not required to receive messages; however, since we want to send replies, we need to add and configure it. The SDK is defined in `pom.xml` and loaded with a statement in `SMSCustomerSupport.java`:

``` java
// Create a MessageBirdService
final MessageBirdService messageBirdService = new MessageBirdServiceImpl("YOUR-API-KEY");
// Add the service to the client
final MessageBirdClient messageBirdClient = new MessageBirdClient(messageBirdService);
```

You need to provide a MessageBird API key, as well as the phone number you registered so that you can use it as the originator, via environment variables loaded with [dotenv](https://mvnrepository.com/artifact/io.github.cdimascio/java-dotenv). We've prepared an env.example file in the repository, which you should rename to .env and add the required information. Here's an example:

```
MESSAGEBIRD_API_KEY=YOUR-API-KEY
MESSAGEBIRD_ORIGINATOR=+31970XXXXXXX
```

You can create or retrieve a live API key from the [API access (REST) tab](https://dashboard.messagebird.com/en/developers/access) in the [Developers section](https://dashboard.messagebird.com/en/developers/settings) of the MessageBird Dashboard.

## Receiving Messages

Now that the preparations for receiving messages are complete, we'll implement the `post '/webhook'` route:

```java
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

```

MessageBird sends a few fields for incoming messages. We're interested in two of them: the `originator`, which is the number that the message came from (don't confuse it with the _originator_ you configured, which is for _outgoing_ messages), and the `body`, which is the content of the text message.

``` java
// Find tickets for number in our database
MongoCollection<Document> tickets = database.getCollection("tickets");
Document doc = tickets.find(eq("number", number)).first();
```

The number is used to look up the ticket; if none exists, we create a new ticket and add one inbound message to it:

```java
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
```

As you can see, we store the whole message history in a single Mongo document using an array called `messages`. In the callback for the Mongo insert function we send the ticket confirmation to the user:

```java
// After creating a new ticket, send a confirmation
String shortId = entry.getObjectId("_id").toString().substring(18, 24);

// Notify the user
messageBirdClient.sendMessage(dotenv.get("MESSAGEBIRD_ORIGINATOR"), "Thanks for contacting customer support! Your ticket ID is " + shortId, phones);

```

Let's unpack this. First, we take an excerpt of the autogenerated MongoDB ID because the full ID is too long and the last 6 digits are unique enough for our purpose. Then, we call `messageBirdClient.sendMessage` to send a confirmation message. Three parameters are passed to the API:

* Our configured `originator`, so that the receiver sees a reply from the number which they contacted in the first place.
* The `body` of the message, which contains the ticket ID.
* A `phones` array with the number from the incoming message so that the reply goes back to the right person.

So, what if a ticket already exists? In this case (our `else` block) we'll add a new message to the array and store the updated document; there’s no need to send another confirmation.

```java
else {
    // Add an inbound message to the existing ticket
    JSONObject message = new JSONObject();
    message.put("direction", "in");
    message.put("content", text);
    ArrayList<JSONObject> messages = (ArrayList<JSONObject>) doc.get("messages");
    messages.add(message);

    Bson query = combine(set("open", true), set("messages", messages));
    tickets.updateOne(eq("number", number), query);
}
```


Servers sending webhooks typically expect you to return a response with a default 200 status code to indicate that their webhook request was received, but they don’t parse the response. Therefore, we send the string OK at the end of the route handler, regardless of the case that we handled.

```java
// Return any response, MessageBird won't parse this
res.status(200);
return "";
```

## Reading Messages

Customer support team members can view incoming tickets from an admin view. We have implemented a simple admin view in the `get /admin` route. The approach is straightforward: request all documents representing open tickets from MongoDB, convert IDs as explained above and then pass them to a [Handlebars templates](http://sparkjava.com/documentation#views-and-templates).

The template is stored in `resources/templates/admin.handlebars`. Apart from the HTML that renders the documents, there’s a small Javascript section in it that refreshes the page every 10 seconds; thanks to this, you can keep the page open and will receive messages automatically with only a small delay and without the implementation of Websockets.

This is the implementation of the route:

```java
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
```

## Replying to Messages

The admin template also contains a form for each ticket through which you can send replies. The implementation uses `messageBirdClient.sendMessage`, analogous to the confirmation messages we're sending for new tickets. If you're curious about the details, you can look at the `post /reply` implementation route in `SMSCustomerSupport.java`.

## Testing the Application

Double-check that you’ve set up your number correctly with a flow that forwards incoming messages to a ngrok URL and that the tunnel is still running. Keep in mind that whenever you start a fresh tunnel with the ngrok command, you'll get a new URL, so you have to update it in the flow accordingly.

To start the application, build and run the application through your IDE.

Open http://localhost:4567/admin in your browser. You should see an empty list of tickets. Then, take out your phone, launch the SMS app and send a message to your virtual mobile number; around 10-20 seconds later, you should see your message in the browser! Amazing! Try again with another message which will be added to the ticket, or send a reply.

Use the flow, code snippets and UI examples from this tutorial as an inspiration to build your own SMS Customer Support system. Don't forget to download the code from the [MessageBird Developer Tutorials GitHub repository](https://github.com/messagebirdguides/customer-support-guide-java).

**Nice work!** 🎉

You now have a running SMS Customer Support application!

## Start building!

Want to build something similar but not quite sure how to get started? Feel free to let us know at support@messagebird.com; we'd love to help!
