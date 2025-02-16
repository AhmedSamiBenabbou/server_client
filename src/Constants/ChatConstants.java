package Constants;

public class ChatConstants {
    // Network Constants
    public static final int MIN_PORT = 5000;
    public static final int MAX_PORT = 5050;
    public static final int MAX_MESSAGE_LENGTH = 200;

    // File Paths
    public static final String DATA_FOLDER_PATH = "data";
    public static final String USERS_FILE = "users.json";
    public static final String MESSAGES_FILE = "messages.json";

    // Commands
    public static final String REGISTER_USER_CMD = "REGISTER_USER";
    public static final String AUTH_SUCCESS = "AUTH_SUCCESS";
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String END_HISTORY = "END_HISTORY";
    public static final String EXIT_COMMAND = "exit";
    
    // Message Format
    public static final String MESSAGE_FORMAT = "[%s- %s:%d- %s]: %s";
    public static final String DATE_FORMAT = "yyyy-MM-dd@HH:mm:ss";
} 