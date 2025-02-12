package server_client;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

//J'ai configuré le serveur pour qu'il se lance sur le port 0(wild card) et l'adresse IP 127.0.0.1 (localhost).
//Mon serveur gère beaucoup d'erreurs, j'ai mis des blocs try-catch un peu partout. J'ai remarqué que le serveur est vraiment facile à mettre en place, 
//il n'y a pas de logique complexe. Du coup, je vais essayer de commencer le document demain après avoir parlé avec le chargé. 
//Je n'ai aucune idée de comment le commencer pour le moment, mais si tu peux, Yasmine, créer un document pour que vous puissiez suivre l'avancement de mon travail.
//À part ça, étant donné que mon serveur gère beaucoup d'exceptions, dites-moi si vous êtes bloqués sur un point du serveur. On peut déboguer ça si besoin.
//Sûrement, je n'ai pas utilisé ChatGPT pour corriger mes erreurs de français.
public class test {
	  private static String ipAdress;
	  private static int port;
	  private static ServerSocket serverSocket;
	  private static Scanner scanner = new Scanner(System.in);
	  private static int clientNumber = 0;
	  private static String serverAddress = "127.0.0.1"; // localHost
	  private static int serverPort = 0;
	  	
	   // validation je pense cote client
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
	   
	   // validation je pense cote client
	   private static boolean portValidator(String portInput) {
		    try {
		        int port = Integer.parseInt(portInput);
		        System.out.print(port);
		        if (5050 < port || port < 5000) {
		            throw new IllegalArgumentException("Le port doit être compris entre 5000 et 5050.");
		        }
		        return true;
		    } catch (NumberFormatException e) {
		        throw new NumberFormatException("Le port doit être un entier valide.");
		    }
		}
	   
	   // validation je pense cote client
	   private static void userInput() {
		    boolean ipValid = false;
		    boolean portValid = false;

		    while (!ipValid) {
		        System.out.print("Veuillez entrer l'adresse IP du serveur: ");
		        String ipInput = scanner.nextLine().trim();

		        try {
		            ipValid = ipValidator(ipInput);
		            ipAdress = ipInput;
		        } catch (IllegalArgumentException  e) {
		            System.out.println("Erreur : " + e.getMessage());
		        }
		    }

		    while (!portValid) {
		        System.out.print("Veuillez entrer le port du serveur: ");
		        String portInput = scanner.nextLine().trim();

		        try {
		            portValid = portValidator(portInput);
		            port = Integer.parseInt(portInput);
		        } catch (IllegalArgumentException  e) {
		            System.out.println("Erreur : " + e.getMessage());
		        }
		    }
		}
		
	   
	   // Ça, c'est tout le serveur.
	   public static void main(String[] args) {
	        // Demande à l'utilisateur de saisir l'adresse IP et le port
	        userInput();
	        scanner.close();

	        // Affiche l'adresse IP et le port saisis    
	        System.out.format("\nVous avez saisi l'adresse IP et le port suivants :\n");
	        System.out.format("IP:Port -> %s:%s\n", ipAdress, port);
	        System.out.println("Merci d'avoir entré ces informations.");
	        
	        try {
	            // Création d'un socket serveur avec option de réutilisation d'adresse
	            serverSocket = new ServerSocket();
	            serverSocket.setReuseAddress(true);

	            // Résolution de l'adresse IP et liaison au port spécifié
	            InetAddress serverIP = InetAddress.getByName(serverAddress);
	            serverSocket.bind(new InetSocketAddress(serverIP, serverPort));

	            System.out.format("\nServeur créé -> %s:%d%n\n", serverAddress, port);

	            // Boucle pour accepter les connexions clients
	            boolean serverIsRunning = true;
	            while (serverIsRunning) {
	                try {
	                    // Acceptation d'un client et démarrage de son gestionnaire
	                    new ClientHandler(serverSocket.accept(), ++clientNumber).start();
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
	            // Fermeture du serveur proprement
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
		
		private static class ClientHandler extends Thread {

			public ClientHandler(Socket socket, int clientNumber) {
				// TODO Auto-generated constructor stub
			}

			public void run() {
				System.out.println("Client 1 lancee");
				}
			}
			
}

