package connection;

import java.net.Socket;

public interface MessageCallback {
    void onReceivedMessage(Socket socket, Message msg);
    void onConnectFailed(Socket socket);
}
