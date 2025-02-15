package Client;

import Common.Command;
import Common.CommandCallback;
import Server.CommandHandler.RegisterUserCommand;
import Server.CommandHandler.SendMessageCommand;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client extends Thread {
    private Socket socket;
    static private int clientNumber = 1;
    private static String serverAddress;
    private static int serverPort;

    public Client(Socket socket, int clientNumber) {
        this.socket = socket;
        Client.clientNumber = clientNumber;
    }

    private static boolean ipValidator(String ip) {
        String[] ipChunks = ip.split("\\.");

        if (ipChunks.length != 4) {
            throw new IllegalArgumentException("L'adresse IP doit contenir exactement 4 octets.");
        }

        for (String chunk : ipChunks) {
            try {
                int ipChunk = Integer.parseInt(chunk);
                if (ipChunk < 0 || ipChunk > 255) {
                    throw new IllegalArgumentException("Chaque octet de l'adresse IP doit être compris entre 0 et 255.");
                }
            } catch (NumberFormatException e) {
                throw new NumberFormatException("Chaque octet de l'adresse IP doit être un entier valide.");
            }
        }

        return true;
    }

    private static boolean portValidator(String portInput) {
        try {
            int port = Integer.parseInt(portInput);
            if (port < 5000 || port > 5050) {
                throw new IllegalArgumentException("Le port doit être compris entre 5000 et 5050.");
            }
            return true;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Le port doit être un entier valide.");
        }
    }

    private static void userInput() {
        boolean ipValid = false;
        boolean portValid = false;
        Scanner scanner = new Scanner(System.in);

        while (!ipValid) {
            System.out.print("Veuillez entrer l'adresse IP du serveur: ");
            String ipInput = scanner.nextLine().trim();

            try {
                ipValid = ipValidator(ipInput);
                serverAddress = ipInput;
            } catch (IllegalArgumentException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }

        while (!portValid) {
            System.out.print("Veuillez entrer le port du serveur: ");
            String portInput = scanner.nextLine().trim();

            try {
                portValid = portValidator(portInput);
                serverPort = Integer.parseInt(portInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    @Override
    public void run() {
        try {
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();
            Scanner scanner = new Scanner(System.in);

            String username = null;
            String password = null;

            while (username == null || isValidUsername(username)) {
                System.out.print("Enter username:");
                username = scanner.nextLine().trim();

                if (isValidUsername(username)) {
                    System.out.println("Invalid username! Must be 4-12 characters, no spaces, and no special characters.");
                }
            }

            while (password == null || isValidPassword(password)) {
                System.out.print("Enter password:");
                password = scanner.nextLine().trim();

                if (isValidPassword(password)) {
                    System.out.println("Invalid password! Must be 6-20 characters, no spaces, and include at least one uppercase, one lowercase, and one digit.");
                }
            }

            System.out.println("You entered username: " + username);
            System.out.println("You entered password: " + password);


            outputStream.write("User registered successfully.\n".getBytes());

            System.out.print("Enter message to send (type 'exit' to quit): ");
            String message = null;
            while (!"exit".equalsIgnoreCase(message)) {

                message = scanner.nextLine().trim();

                if (!"exit".equalsIgnoreCase(message)) {
                    Command sendMessageCommand = new SendMessageCommand(username, message);
                    sendMessageCommand.execute(socket);
                }
            }

            System.out.println("Exiting chat...");

        } catch (IOException e) {
            System.err.println("Error handling client " + clientNumber + ": " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket for client " + clientNumber);
            }
        }
    }

    private static boolean isValidUsername(String username) {
        return username.length() < 4 || username.length() > 12 ||
                !username.matches("^[a-zA-Z0-9]+$");
    }

    private static boolean isValidPassword(String password) {
        return password.length() < 6 || password.length() > 20 ||
                !password.matches(".*[A-Z].*") ||
                !password.matches(".*[a-z].*") ||
                !password.matches(".*\\d.*") ||
                password.contains(" ");
    }

    public static void main(String[] args) {
        userInput();

        try {
            Socket socket = new Socket(serverAddress, serverPort);
            System.out.println("Connected to server at " + serverAddress + ":" + serverPort);

            Client client = new Client(socket, clientNumber);
            client.start();

            clientNumber++;
        } catch (IOException e) {
            System.err.println("Error connecting to the server: " + e.getMessage());
        }
    }
}
