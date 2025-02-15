package Server.CommandHandler;

import Common.Command;
import Common.CommandCallback;
import Common.CommandExecutionException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class SendMessageCommand implements Command {
    private final String username;
    private final String message;

    public SendMessageCommand(String username, String message) {
        this.username = username;
        this.message = message;
    }

    @Override
    public void execute(Socket socket, CommandCallback callback){
        try {
            OutputStream outputStream = socket.getOutputStream();
            String messageToSend = username + ": " + message + "\n";
            outputStream.write(messageToSend.getBytes());
        } catch (IOException e) {
            throw new CommandExecutionException("Error sending message: " + e.getMessage(), e);
        }
    }
}
