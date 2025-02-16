# École de technologie supérieure
# INT3405 - Technologies des ordinateurs et réseaux
### Session: Hiver 2024
### Travail Pratique 1 - Application de Chat
### Date de remise: [Date]

**Équipe:**
- [Matricule 1] [Nom 1]
- [Matricule 2] [Nom 2]

**Soumis à:** [Nom et prénom du chargé de laboratoire]

---

## 1. Introduction

Ce travail pratique consiste en la réalisation d'une application de chat client-serveur en Java. L'objectif principal est de mettre en pratique les concepts fondamentaux des réseaux informatiques tout en développant une application robuste et fonctionnelle. Le système permet à plusieurs clients de se connecter à un serveur central pour échanger des messages en temps réel, avec des fonctionnalités de gestion des utilisateurs et de persistance des données.

### 1.1 Objectifs Techniques
- Implémentation d'une architecture client-serveur avec sockets TCP/IP
- Gestion de la concurrence pour les multiples connexions
- Persistance des données avec JSON
- Validation et sécurisation des entrées utilisateur
- Gestion efficace des ressources système

## 2. Présentation des Travaux

### 2.1 Architecture du Système

L'application est structurée selon une architecture client-serveur classique avec trois composants principaux :

#### 2.1.1 Client (Client.java)
```java
public class Client extends Thread {
    private final Socket socket;
    private static String serverAddress;
    private static int serverPort;
    private boolean isConnected = false;
    private PrintWriter writer;
    private MessageReceiver messageReceiver;
    
    // Gestion des messages en temps réel
    private static class MessageReceiver extends Thread {
        private final BufferedReader reader;
        private boolean running = true;
        
        @Override
        public void run() {
            // Logique de réception des messages
        }
    }
}
```

Caractéristiques principales :
- Socket TCP pour la communication bidirectionnelle
- Thread dédié pour la réception des messages (MessageReceiver)
- Gestion des états de connexion
- Validation des entrées (IP, port)
- Interface CLI interactive

#### 2.1.2 Serveur (Server.java)
```java
public class Server {
    private static ServerSocket serverSocket;
    private static List<ClientHandler> clients = new ArrayList<>();
    
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private String username;
        
        @Override
        public void run() {
            // Logique de gestion des clients
        }
    }
}
```

Fonctionnalités avancées :
- Gestion thread-safe des clients connectés
- Système de broadcast optimisé
- Persistance JSON avec gestion atomique
- Validation des authentifications
- Historique des messages

#### 2.1.3 Gestion des Constantes (ChatConstants.java)
```java
public class ChatConstants {
    public static final int MIN_PORT = 5000;
    public static final int MAX_PORT = 5050;
    public static final int MAX_MESSAGE_LENGTH = 200;
    public static final String MESSAGE_FORMAT = "[%s- %s:%d- %s]: %s";
    // ... autres constantes
}
```

### 2.2 Implémentation Technique Détaillée

#### 2.2.1 Gestion de la Concurrence
```java
// Exemple de synchronisation des messages
private void broadcastMessage(String message) {
    synchronized(clients) {
        for (ClientHandler client : clients) {
            if (client != this && client.writer != null) {
                client.writer.println(message);
                client.writer.flush();
            }
        }
    }
}
```

#### 2.2.2 Persistance des Données
```java
private static void storeMessage(String username, String message, String formattedMessage) {
    JSONArray messages = readMessagesFromFile();
    JSONObject newMessage = new JSONObject();
    newMessage.put("username", username);
    newMessage.put("message", message);
    newMessage.put("timestamp", System.currentTimeMillis());
    messages.put(newMessage);
    writeMessagesToFile(messages);
}
```

#### 2.2.3 Validation et Sécurité
```java
private static boolean ipValidator(String ip) {
    String[] ipChunks = ip.split("\\.");
    if (ipChunks.length != 4) return false;
    
    for (String chunk : ipChunks) {
        try {
            int value = Integer.parseInt(chunk);
            if (value < 0 || value > 255) return false;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    return true;
}
```

### 2.3 Protocole de Communication

Le système utilise un protocole de communication personnalisé :

1. **Connexion initiale**
   ```
   CLIENT -> SERVER: REGISTER_USER_CMD
   CLIENT -> SERVER: username
   CLIENT -> SERVER: password
   SERVER -> CLIENT: AUTH_SUCCESS/AUTH_FAILED
   ```

2. **Échange de messages**
   ```
   CLIENT -> SERVER: message_content
   SERVER -> ALL_CLIENTS: [username-IP:Port-Timestamp]: message
   ```

3. **Déconnexion**
   ```
   CLIENT -> SERVER: exit
   SERVER: Cleanup resources
   ```

## 3. Difficultés Techniques Rencontrées

### 3.1 Gestion de la Concurrence
- **Problème**: Race conditions dans la liste des clients
- **Solution**: 
  ```java
  private static final List<ClientHandler> clients = 
      Collections.synchronizedList(new ArrayList<>());
  ```

### 3.2 Persistance Atomique
- **Problème**: Corruption des fichiers JSON lors d'écritures simultanées
- **Solution**:
  ```java
  private static final Object fileLock = new Object();
  
  private static void writeMessagesToFile(JSONArray messages) {
      synchronized(fileLock) {
          try (FileWriter writer = new FileWriter(MESSAGE_FILE)) {
              writer.write(messages.toString(4));
          }
      }
  }
  ```

### 3.3 Gestion du Terminal
- **Problème**: Chevauchement des messages dans la CLI
- **Solution**: 
  ```java
  System.out.print("\033[2K"); // Clear line
  System.out.print("\033[1A"); // Move up
  System.out.print("\r" + message);
  ```

## 4. Optimisations et Améliorations Proposées

### 4.1 Améliorations Techniques
1. **Chiffrement des Messages**
   ```java
   public class SecureMessage {
       private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
       // Implémentation du chiffrement
   }
   ```

2. **Support WebSocket**
   ```java
   @ServerEndpoint("/chat")
   public class WebSocketServer {
       // Support web pour plus de flexibilité
   }
   ```

3. **Système de Salons**
   ```java
   public class ChatRoom {
       private String roomId;
       private Set<ClientHandler> members;
       // Gestion des salons
   }
   ```

### 4.2 Suggestions pour le Laboratoire
1. **Tests de Performance**
   - Benchmarking avec JMH
   - Tests de charge avec multiple clients

2. **Sécurité Réseau**
   - Implémentation de TLS/SSL
   - Protection contre les attaques DoS

## 5. Conclusion

Ce projet a permis d'acquérir une expérience pratique approfondie dans :

1. **Programmation Réseau**
   - Sockets TCP/IP
   - Gestion des connexions
   - Protocoles personnalisés

2. **Concurrence**
   - Thread management
   - Synchronisation
   - Collections thread-safe

3. **Architecture Logicielle**
   - Pattern Client-Serveur
   - Gestion des états
   - Persistance des données

Cette expérience constitue une base solide pour le développement d'applications réseau plus complexes et a permis de mettre en pratique des concepts théoriques dans un contexte réel. 