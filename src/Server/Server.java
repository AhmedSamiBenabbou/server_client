package Server;

import Client.Client;
import Common.CommandCallback;
import Server.CommandHandler.RegisterUserCommand;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.List;

import Server.CommandHandler.SendMessageCommand;
import org.json.JSONArray;
import org.json.JSONObject;

public class Server {
    private static String serverAddress;
    private static int serverPort;
    private static ServerSocket serverSocket;
    private static Scanner scanner = new Scanner(System.in);
    private static int clientNumber = 0;
    private static List<Client> clients = new ArrayList<>();
    private static final File DATA_FOLDER = new File("data");
    private static final File USER_FILE = new File(DATA_FOLDER, "users.json");
    private static final File MESSAGE_FILE = new File(DATA_FOLDER, "messages.json");

    private static void setupFolder(File folder) {
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (created) {
                System.out.println("Folder " + folder.getPath() + " created successfully.");
            } else {
                System.out.println("Failed to create folder " + folder.getPath());
            }
        }
    }

    private static void setupDataFolder() {
        setupFolder(DATA_FOLDER);
    }

    public static void storeMessage(String username, String messageContent) {
        setupDataFolder();
        JSONArray messagesArray = readMessagesFromFile();

        JSONObject message = new JSONObject();
        message.put("username", username);
        message.put("message", messageContent);
        message.put("timestamp", System.currentTimeMillis());

        messagesArray.put(message);
        writeMessagesToFile(messagesArray);
    }

    private static JSONArray readMessagesFromFile() {
        try {
            if (!MESSAGE_FILE.exists()) {
                return new JSONArray();
            }

            String content = new String(Files.readAllBytes(MESSAGE_FILE.toPath()));
            return new JSONArray(content);
        } catch (IOException e) {
            System.err.println("Error reading message file: " + e.getMessage());
            return new JSONArray();
        }
    }

    private static void writeMessagesToFile(JSONArray messagesArray) {
        try (FileWriter fileWriter = new FileWriter(MESSAGE_FILE)) {
            fileWriter.write(messagesArray.toString(4));
        } catch (IOException e) {
            System.err.println("Error writing message file: " + e.getMessage());
        }
    }

    public static void storeUser(String username, String password) {
        setupDataFolder();

        JSONArray usersArray = readUsersFromFile();

        for (int i = 0; i < usersArray.length(); i++) {
            JSONObject user = usersArray.getJSONObject(i);
            if (user.getString("username").equals(username)) {
                System.out.println("Error: Username already exists.");
                return;
            }
        }

        JSONObject newUser = new JSONObject();
        newUser.put("username", username);
        newUser.put("password", password);

        usersArray.put(newUser);

        writeUsersToFile(usersArray);
        System.out.println("User " + username + " registered successfully.");
    }

    private static JSONArray readUsersFromFile() {
        try {
            if (!USER_FILE.exists()) {
                return new JSONArray();
            }

            String content = new String(Files.readAllBytes(USER_FILE.toPath()));
            return new JSONArray(content);
        } catch (IOException e) {
            System.err.println("Error reading user file: " + e.getMessage());
            return new JSONArray();
        }
    }

    private static void writeUsersToFile(JSONArray usersArray) {
        try (FileWriter fileWriter = new FileWriter(USER_FILE)) {
            fileWriter.write(usersArray.toString(4));
        } catch (IOException e) {
            System.err.println("Error writing user file: " + e.getMessage());
        }
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
            System.out.print(port);
            if (port < 5000 || 5050 < port) {
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

        while (!ipValid) {
            System.out.print("Veuillez entrer l'adresse IP du serveur: ");
            String ipInput = scanner.nextLine().trim();

            try {
                ipValid = ipValidator(ipInput);
                serverAddress = ipInput;
            } catch (IllegalArgumentException  e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }

        while (!portValid) {
            System.out.print("Veuillez entrer le port du serveur: ");
            String portInput = scanner.nextLine().trim();

            try {
                portValid = portValidator(portInput);
                serverPort = Integer.parseInt(portInput);
            } catch (IllegalArgumentException  e) {
                System.out.println("Erreur : " + e.getMessage());
            }
        }
    }

    public static void handleClient(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            String commandType = reader.readLine();

            if ("REGISTER_USER".equals(commandType)) {
                String username = reader.readLine();
                String password = reader.readLine();

                RegisterUserCommand registerCommand = new RegisterUserCommand(username, password);
                registerCommand.execute(socket, new CommandCallback() {
                    @Override
                    public void onFailure(String errorMessage) {
                        writer.println("Error: " + errorMessage);
                    }

                    @Override
                    public void onSuccess(String successMessage) {
                        writer.println(successMessage);
                    }
                });
            }


        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            //userInput();
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);

            String localServerHost ="127.0.0.1";
            int localServerPort = 5020;

            // InetAddress serverIP = InetAddress.getByName(serverAddress);
            // serverSocket.bind(new InetSocketAddress(serverIP, serverPort));
            // System.out.format("\nServeur créé -> %s:%d%n\n", serverAddress, serverPort);

            InetAddress serverIP = InetAddress.getByName(localServerHost);
            serverSocket.bind(new InetSocketAddress(serverIP, localServerPort));
            System.out.format("\nServeur créé -> %s:%d%n\n", localServerHost, localServerPort);

            while (true) {
                try {
                     Client client = new Client(serverSocket.accept(), ++clientNumber);
                        client.start();
                } catch (IOException e) {
                    System.err.println("Erreur lors de l'acceptation d'un client : " + e.getMessage());
                }
            }

        } catch (BindException e) {
            System.err.println("Erreur : L'adresse et le port sont déjà utilisés.");
        } catch (UnknownHostException e) {
            System.err.println("Erreur : Adresse IP invalide.");
        } catch (IOException e) {
            System.err.println("Erreur lors de la création du serveur : " + e.getMessage());
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    System.out.println("Serveur arrêté.");
                } catch (IOException e) {
                    System.err.println("Erreur lors de la fermeture du serveur : " + e.getMessage());
                }
            }
        }
    }
}



