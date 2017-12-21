package connection;

import java.net.Socket;

public interface MessageCallback {
    void onReceivedMessage (Message msg);
    void onConnectFailed (Socket socket);
}
