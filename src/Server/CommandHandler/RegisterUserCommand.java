package Server.CommandHandler;

import Server.Server;

import java.net.Socket;

public class RegisterUserCommand implements Command {
    private final String username;
    private final String password;

    public RegisterUserCommand(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void execute(Socket socket)  {

        if (Server.isUsernameTaken(username)) {
            callback.onError("Error: Username already exists.");
        } else {
            Server.storeUser(username, password);
            callback.onSuccess("User " + username + " registered successfully.");
        }
    }
}
