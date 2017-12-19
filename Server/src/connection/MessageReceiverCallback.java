package connection;

import java.net.Socket;

public interface MessageReceiverCallback {
    void onMsgReceived(Object msg, Socket fromSocket);
    void onStreamClosed(Socket socket);
}
