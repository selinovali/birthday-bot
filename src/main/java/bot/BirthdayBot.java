package bot;

import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BirthdayBot {

    public static void main(String[] args) {
        String FORMAT_MSG = "Please tell me your birthday in this format: `.addbirthday MM-dd-YYYY`";
        // Connect to my Discord bot using token, extracted from local file
        DiscordClient client = DiscordClient.create(getTokenFromFile("token"));

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> {
            Mono<Void> printOnLogin = gateway.on(ReadyEvent.class, event ->
                            Mono.fromRunnable(() -> {
                                final User self = event.getSelf();
                                System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());
                            }))
                    .then();

            Mono<Void> handlePingCommand = gateway.on(MessageCreateEvent.class, event -> {
                Message message = event.getMessage();
                String originalMessage = message.getContent();

                // Get user ID
                Snowflake snowflake = message.getAuthor().get().getId();
                String id = snowflake.toString().substring(10,28);

                // message.getChannel().flatMap(channel -> channel.createMessage("Testing mention <@448532794189021185>"));

                if (originalMessage.substring(0,12).equalsIgnoreCase(".addbirthday")) {
                    // If message shorter than min length, message channel with correct format
                    if (originalMessage.length() < 23) {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage(FORMAT_MSG));
                    }

                    // Remove '.addbirthday' and  all trailing spaces from string => "mm-dd-YYYY
                    String birthdayString = originalMessage.substring(12).replaceAll("\\s", "");

                    // If birthday is not a valid date, send format message
                    if(!validateBirthday(birthdayString)) {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage(FORMAT_MSG));
                    }

                    // Add birthday to database, send a message
                    if (insertBirthday(id, birthdayString)) {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage("I'll remember your birthday!"));
                    } else {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage("There was a problem adding your birthday to my list."));
                    }
                }

                return Mono.empty();
            }).then();

            return printOnLogin.and(handlePingCommand);
        });
        login.block();
    }

    /* Checks string against exact expected format: "MM-dd-YYYY"
        Valid from 1940 to now
       Regex tested using regex101: "(0[1-9]|1[012])\-(0[1-9]|[12][0-9]|3[01])\-(19[4-9][0-9]|20[01][0-9]|202[0-3])"
    * */
    public static boolean validateBirthday(String str) {
        Pattern pattern = Pattern.compile("(0[1-9]|1[012])\\-(0[1-9]|[12][0-9]|3[01])\\-(19[4-9][0-9]|20[01][0-9]|202[0-3])", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(str);

        return matcher.find();
    }

    /* Returns date in format: MM-dd-YYYY
    * */
    public static String getDate() {

        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-yyyy");
        Date date = new Date();
        String current_date = formatter.format(date);

        return current_date;
    }

    /* Retrieves the content of a txt file in the working directory and returns as a String
    Used for auth token and database password
    * */
    public static String getTokenFromFile(String file_name) {
        try {
            // Read the token.txt file, which has the Discord bot auth token
            File tokenFile = new File("./src/main/java/bot/" + file_name + ".txt");
            Scanner myReader = new Scanner(tokenFile);

            String token = myReader.nextLine();
            myReader.close();

            return token;
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred reading token.");
            return "";
        }
    }

    /* Connect to the database and insert birthday */
    public static boolean insertBirthday(String id, String birthdayString) {
        try {
            // Establish connection with my database
            String myDriver = "com.mysql.cj.jdbc.Driver";
            String myUrl = "jdbc:mysql://localhost:3306/birthdays";
            Class.forName(myDriver);

            Connection conn = DriverManager.getConnection(myUrl, "root", getTokenFromFile("db_token"));

            // Create INSERT query
            String query = "INSERT INTO user (userID, birthday)" + " VALUES (" + id + ",'" + birthdayString + "')";
            Statement st = conn.createStatement();

            st.executeUpdate(query);
            conn.close();
            return true;
        } catch (Exception e) {

            System.out.println("An error occurred while inserting to database.");
            System.err.println(e.getMessage());
            return false;
        }

    }
}
