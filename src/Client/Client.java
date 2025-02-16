package Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import Constants.ChatConstants;

public class Client extends Thread {
    private final Socket socket;
    private static String serverAddress;
    private static int serverPort;
    private boolean isConnected = false;
    private PrintWriter writer;
    private MessageReceiver messageReceiver;

    public Client(Socket socket) {
        this.socket = socket;
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
            if (port < ChatConstants.MIN_PORT || port > ChatConstants.MAX_PORT) {
                throw new IllegalArgumentException("Le port doit être compris entre " + ChatConstants.MIN_PORT + " et " + ChatConstants.MAX_PORT + ".");
            }
            return true;
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Le port doit être un entier valide.");
        }
    }

    private static void userInput() {
        Scanner scanner = new Scanner(System.in);
        boolean ipValid = false;
        boolean portValid = false;

        while (!ipValid) {
            System.out.print("Please enter server IP address: ");
            String ipInput = scanner.nextLine().trim();
            try {
                ipValid = ipValidator(ipInput);
                serverAddress = ipInput;
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }

        while (!portValid) {
            System.out.print("Please enter server port: ");
            String portInput = scanner.nextLine().trim();
            try {
                portValid = portValidator(portInput);
                serverPort = Integer.parseInt(portInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private static class MessageReceiver extends Thread {
        private final BufferedReader reader;
        private boolean running = true;

        public MessageReceiver(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            try {
                String message;
                while (running && (message = reader.readLine()) != null) {
                    if (message.startsWith("CLIENT_NUMBER:")) {
                        int clientNumber = Integer.parseInt(message.split(":")[1]);
                        System.out.println("Connected as client #" + clientNumber);
                        continue;
                    }
                    if ("END_HISTORY".equals(message)) {
                        System.out.println("===================\n");
                        System.out.println("Message (max 200 characters, 'exit' to quit)");
                        System.out.print("> ");
                        continue;
                    }
                    // Remove the extra newline and ensure consistent formatting
                    System.out.print("\033[2K"); // Clear the current line
                    System.out.print("\r" + message + "\n> ");
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error receiving messages: " + e.getMessage());
                }
            }
        }

        public void stopReceiving() {
            running = false;
        }
    }

    @Override
    public void run() {
        try {
            writer = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner scanner = new Scanner(System.in);

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            writer.println(ChatConstants.REGISTER_USER_CMD);
            writer.println(username);
            writer.println(password);

            String response = reader.readLine();
            if (ChatConstants.AUTH_FAILED.equals(response)) {
                System.out.println("Erreur dans la saisie du mot de passe");
                return;
            } else if (ChatConstants.AUTH_SUCCESS.equals(response)) {
                isConnected = true;
            } else {
                System.out.println("Erreur de connexion inattendue");
                return;
            }

            messageReceiver = new MessageReceiver(reader);
            messageReceiver.start();

            System.out.println("\n=== Chat History ===");
            
            String message;
            while (isConnected) {
                message = scanner.nextLine().trim();

                if (ChatConstants.EXIT_COMMAND.equalsIgnoreCase(message)) {
                    isConnected = false;
                    continue;
                }

                if (message.length() > ChatConstants.MAX_MESSAGE_LENGTH) {
                    System.out.println("Error: Message too long (max " + ChatConstants.MAX_MESSAGE_LENGTH + " characters)");
                    continue;
                }

                // Clear the current line and move cursor up
                System.out.print("\033[1A"); // Move up one line
                System.out.print("\033[2K"); // Clear the line

                // Format own message before sending
                String formattedMessage = String.format(ChatConstants.MESSAGE_FORMAT,
                    username,
                    socket.getLocalAddress().getHostAddress(),
                    socket.getLocalPort(),
                    new java.text.SimpleDateFormat(ChatConstants.DATE_FORMAT).format(new java.util.Date()),
                    message
                );
                
                // Display formatted message locally
                System.out.println(formattedMessage);
                System.out.print("> ");

                // Send original message to server
                writer.println(message);
            }

        } catch (IOException e) {
            System.err.println("Communication error: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void disconnect() {
        try {
            isConnected = false;
            if (messageReceiver != null) {
                messageReceiver.stopReceiving();
            }
            if (writer != null) {
                writer.println(ChatConstants.EXIT_COMMAND);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Disconnected from server.");
            }
        } catch (IOException e) {
            System.err.println("Error during disconnection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        userInput();

        try {
            Socket socket = new Socket(serverAddress, serverPort);
            System.out.println("Connecté au serveur " + serverAddress + ":" + serverPort);

            Client client = new Client(socket);
            client.start();

        } catch (IOException e) {
            System.err.println("Erreur de connexion au serveur: " + e.getMessage());
        }
    }
}
