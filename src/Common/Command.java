package Common;

import java.net.Socket;
public interface Command {
    void execute(Socket socket);
}

