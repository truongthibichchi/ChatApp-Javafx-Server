package connection;

import java.net.Socket;

public interface ConnectionCallBack {
    void onListenerOpenFailed();
    void onListenerOpenSucceeded();

    void onConnectionReceived(Socket socketToClient);

    void onConnectToServerSucceeded(Socket socketToServer);
    void onConnectToServerFailed();

    void onConnectionToAClientLost(Socket socketToClient);
    void onConnectionToServerLost(Socket socketToServer);

    void onMsgReceived(Object msg, Socket fromSocket);
}
