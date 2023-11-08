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
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BirthdayBot {
    public static void main(String[] args) {
        // Establish connection with Discord Bot using token
        DiscordClient client = DiscordClient.create(getTokenFromFile());
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

                // Get user ID }
                Snowflake snowflake = message.getAuthor().get().getId();
                String id = snowflake.toString().substring(10,28);
                // message.getChannel().flatMap(channel -> channel.createMessage("Testing mention <@448532794189021185>"));

                if (originalMessage.substring(0,12).equalsIgnoreCase(".addbirthday")) {
                    // If message shorter than min length, message channel with correct format
                    if (originalMessage.length() < 23) {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage("Please tell me your birthday in this format: `.addbirthday MM-dd-YYYY`"));
                    }

                    // Remove '.addbirthday' and  all trailing spaces from string => "mm-dd-YYYY
                    String birthdayString = originalMessage.substring(12).replaceAll("\\s", "");

                    // Check if the birthday string is formatted correctly
                    if(validateBirthday(birthdayString)) {
                        System.out.println(birthdayString);
                    } else {
                        return message.getChannel()
                                .flatMap(channel -> channel.createMessage("Please tell me your birthday in this format: `.addbirthday MM-dd-YYYY`"));
                    }

                    // TO DO: Add birthday to database

                    return message.getChannel()
                            .flatMap(channel -> channel.createMessage("I'll remember your birthday!"));
                }

                return Mono.empty();
            }).then();

            return printOnLogin.and(handlePingCommand);
        });
        login.block();
    }

    /* Checks string against exact expected format: "MM-dd-YYYY"
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

    /* Function retrieves the token from txt file and returns as a String
    * */
    public static String getTokenFromFile() {
        try {
            // Read the token.txt file, which has the Discord bot auth token
            File tokenFile = new File("./src/main/java/bot/token.txt");
            Scanner myReader = new Scanner(tokenFile);

            String token = myReader.nextLine();
            myReader.close();

            return token;
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred reading token.");
            return "";
        }
    }
}
