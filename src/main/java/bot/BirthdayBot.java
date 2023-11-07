package bot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class BirthdayBot {
    public static void main(String[] args) {

        // Establish connection with Discord Bot using token
        DiscordClient client = DiscordClient.create(getTokenFromFile());
        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> {
            // ReadyEvent example
            Mono<Void> printOnLogin = gateway.on(ReadyEvent.class, event ->
                            Mono.fromRunnable(() -> {
                                final User self = event.getSelf();
                                System.out.printf("Logged in as %s#%s%n", self.getUsername(), self.getDiscriminator());
                            }))
                    .then();

            // MessageCreateEvent example
            Mono<Void> handlePingCommand = gateway.on(MessageCreateEvent.class, event -> {
                Message message = event.getMessage();

                if (message.getContent().equalsIgnoreCase(".addbirthday")) {
                    // Validate birthday in message string
                    // If wrong, print birthday format
                    return message.getChannel()
                            .flatMap(channel -> channel.createMessage("I'll remember your birthday!"));
                }

                return Mono.empty();
            }).then();

            // combine them!
            return printOnLogin.and(handlePingCommand);
        });
        login.block();
    }

    /* Returns date in format: YYYY-MM-DD */
    public String getDate() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        String date = java.time.LocalDate.now().toString();
        return date;
    }

    /* Function retrieves the token from txt file and returns as a String */
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
