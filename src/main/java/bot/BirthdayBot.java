package bot;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class BirthdayBot {
    public static void main(String[] args) {

        // Establish connection with Discord Bot using token
        DiscordClient client = DiscordClient.create(getTokenFromFile());
        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> Mono.empty());

        login.block();
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
