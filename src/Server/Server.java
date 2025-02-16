package Server;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import Constants.ChatConstants;

public class Server {
    private static String serverAddress;
    private static int serverPort;
    private static ServerSocket serverSocket;
    private static final File DATA_FOLDER = new File(ChatConstants.DATA_FOLDER_PATH);
    private static final File USER_FILE = new File(DATA_FOLDER, ChatConstants.USERS_FILE);
    private static final File MESSAGE_FILE = new File(DATA_FOLDER, ChatConstants.MESSAGES_FILE);
    private static List<ClientHandler> clients = new ArrayList<>();
    private static int clientCounter = 0; // Counter for client numbers

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private String username;
        private final int clientNumber; // Unique number for this client

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.clientNumber = ++clientCounter; // Assign unique number
        }

        @Override
        public void run() {
            try {
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Authentication
                String command = reader.readLine();
                if (ChatConstants.REGISTER_USER_CMD.equals(command)) {
                    username = reader.readLine();
                    String password = reader.readLine();
                    handleAuthentication(username, password);
                }

                // Send client number confirmation
                writer.println("CLIENT_NUMBER:" + clientNumber);

                // Envoi de l'historique
                sendChatHistory();

                // Boucle de réception des messages
                String message;
                while ((message = reader.readLine()) != null) {
                    if (ChatConstants.EXIT_COMMAND.equalsIgnoreCase(message)) {
                        break;
                    }
                    handleMessage(message);
                }
            } catch (IOException e) {
                System.err.println("Erreur client: " + e.getMessage());
            } finally {
                disconnect();
            }
        }

        private void handleAuthentication(String username, String password) {
            JSONArray users = readUsersFromFile();
            boolean userExists = false;

            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                if (user.getString("username").equals(username)) {
                    userExists = true;
                    if (user.getString("password").equals(password)) {
                        writer.println(ChatConstants.AUTH_SUCCESS);
                    } else {
                        writer.println(ChatConstants.AUTH_FAILED);
                        disconnect();
                    }
                    break;
                }
            }

            if (!userExists) {
                // Auto-create account
                JSONObject newUser = new JSONObject();
                newUser.put("username", username);
                newUser.put("password", password);
                users.put(newUser);
                writeUsersToFile(users);
                writer.println(ChatConstants.AUTH_SUCCESS);
            }
        }

        private void sendChatHistory() {
            JSONArray messages = readMessagesFromFile();
            // Calculate starting index to get last 15 messages
            int startIndex = Math.max(0, messages.length() - 15);
            
            for (int i = startIndex; i < messages.length(); i++) {
                JSONObject msg = messages.getJSONObject(i);
                String formattedMessage = msg.has("formattedMessage") ? 
                    msg.getString("formattedMessage") : 
                    String.format("[%s]: %s", msg.getString("username"), msg.getString("message"));
                writer.println(formattedMessage);
            }
            writer.println("END_HISTORY");
        }

        private void handleMessage(String message) {
            if (message.length() > ChatConstants.MAX_MESSAGE_LENGTH) {
                writer.println("Error: Message too long (max " + ChatConstants.MAX_MESSAGE_LENGTH + " characters)");
                return;
            }

            String formattedMessage = String.format(ChatConstants.MESSAGE_FORMAT,
                username,
                socket.getInetAddress().getHostAddress(),
                socket.getPort(),
                new java.text.SimpleDateFormat(ChatConstants.DATE_FORMAT).format(new java.util.Date()),
                message
            );

            // Store message
            storeMessage(username, message, formattedMessage);

            // Real-time display on server
            System.out.println(formattedMessage);

            // Broadcast to all clients except sender
            broadcastMessage(formattedMessage);
        }

        private void broadcastMessage(String message) {
            for (ClientHandler client : clients) {
                // Skip the sender to avoid echo
                if (client != this && client.writer != null) {
                    client.writer.println(message);
                }
            }
        }

        private void disconnect() {
            clients.remove(this);
            try {
                if (writer != null) writer.close();
                if (reader != null) reader.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
            }
        }
    }

    private static void setupDataFolder() {
        if (!DATA_FOLDER.exists()) {
            DATA_FOLDER.mkdirs();
        }
    }

    private static void storeMessage(String username, String message, String formattedMessage) {
        setupDataFolder();
        JSONArray messages = readMessagesFromFile();
        
        JSONObject newMessage = new JSONObject();
        newMessage.put("username", username);
        newMessage.put("message", message);
        newMessage.put("formattedMessage", formattedMessage);
        newMessage.put("timestamp", System.currentTimeMillis());
        
        messages.put(newMessage);
        writeMessagesToFile(messages);
    }

    private static JSONArray readMessagesFromFile() {
        try {
            if (!MESSAGE_FILE.exists()) {
                return new JSONArray();
            }
            return new JSONArray(new String(Files.readAllBytes(MESSAGE_FILE.toPath())));
        } catch (IOException e) {
            System.err.println("Erreur lecture messages: " + e.getMessage());
            return new JSONArray();
        }
    }

    private static void writeMessagesToFile(JSONArray messages) {
        try (FileWriter writer = new FileWriter(MESSAGE_FILE)) {
            writer.write(messages.toString(4));
        } catch (IOException e) {
            System.err.println("Erreur écriture messages: " + e.getMessage());
        }
    }

    private static JSONArray readUsersFromFile() {
        try {
            if (!USER_FILE.exists()) {
                return new JSONArray();
            }
            return new JSONArray(new String(Files.readAllBytes(USER_FILE.toPath())));
        } catch (IOException e) {
            System.err.println("Erreur lecture utilisateurs: " + e.getMessage());
            return new JSONArray();
        }
    }

    private static void writeUsersToFile(JSONArray users) {
        try (FileWriter writer = new FileWriter(USER_FILE)) {
            writer.write(users.toString(4));
        } catch (IOException e) {
            System.err.println("Erreur écriture utilisateurs: " + e.getMessage());
        }
    }

    private static void userInput() {
        Scanner scanner = new Scanner(System.in);
        boolean validInput = false;

        while (!validInput) {
            try {
                System.out.print("Adresse IP du serveur: ");
                serverAddress = scanner.nextLine().trim();
                if (!serverAddress.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                    throw new IllegalArgumentException("Format d'IP invalide");
                }

                System.out.print("Port du serveur (" + ChatConstants.MIN_PORT + "-" + ChatConstants.MAX_PORT + "): ");
                serverPort = Integer.parseInt(scanner.nextLine().trim());
                if (serverPort < ChatConstants.MIN_PORT || serverPort > ChatConstants.MAX_PORT) {
                    throw new IllegalArgumentException("Port hors limites (" + ChatConstants.MIN_PORT + "-" + ChatConstants.MAX_PORT + ")");
                }

                validInput = true;
            } catch (Exception e) {
                System.out.println("Erreur: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        setupDataFolder();
        userInput();

        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(serverAddress, serverPort));
            System.out.println("Serveur démarré sur " + serverAddress + ":" + serverPort);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
                System.out.println("Nouveau client connecté: " + clientSocket.getInetAddress().getHostAddress());
            }
        } catch (IOException e) {
            System.err.println("Erreur serveur: " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("Serveur arrêté.");
                } catch (IOException e) {
                    System.err.println("Erreur fermeture serveur: " + e.getMessage());
                }
            }
        }
    }
}



