package connection;

import controllers.ServerWindowController;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class Server extends Thread implements MessageCallback {
    private int port;

    private HashMap<String, Socket> usersSockets = new HashMap<>();
    private HashMap<Socket, ThreadPerSocket> usersThreads = new HashMap<>();
    private ServerWindowController controller;

    public Server (int port) {
        this.port = port;
    }
    public void setController(ServerWindowController controller) {
        this.controller = controller;
    }

    @Override
    public void run() {
        super.run();
        startServer(port);
    }
    private void startServer(int port) {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
        } catch (Exception e) {
            onOpenServerFailed();
        }
        controller.log("Server is now Ready!");

        while (true) {
            Socket socket = null;
            try {
                socket = serverSocket.accept();
            } catch (Exception e) {
                onNetworkError();
            }
            ThreadPerSocket thread = new ThreadPerSocket(socket, this);
            thread.start();

            usersThreads.put(socket, thread);
        }
    }


    @Override
    public void onReceivedMessage(Message msg) {
        switch (msg.getType()) {
            case LOGIN:

                break;

            case LOGOUT:

                break;

            case CHAT_TEXT:

                break;

            case LOGIN_SUCCEEDED:

                break;
        }
    }

    @Override
    public void onConnectFailed(Socket socket) {
        usersThreads.remove(socket);

        controller.log("A Client disconnected!");
    }

    public void onOpenServerFailed() {
        controller.log("Failed to open server");
    }

    public void onNetworkError() {
        controller.log("Network Error");
    }



    private void sendTo(String userName, Message msg) {
        Socket socket = usersSockets.get(userName);
        ThreadPerSocket thread = usersThreads.get(socket);
        thread.send(msg);
    }

    private void sendToGroup(List<String> userNames, Message msg) {
        for (String userName : userNames) {
            sendTo(userName, msg);
        }
    }
}

class ThreadPerSocket extends Thread {
    private Socket socket;
    private MessageCallback messageCallback;

    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    ThreadPerSocket(Socket socket, MessageCallback callback) {
        this.socket = socket;
        this.messageCallback = callback;
    }

    @Override
    public void run() {
        super.run();

        try {
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Message msg = (Message) inputStream.readObject();
                messageCallback.onReceivedMessage(msg);
            }
        } catch (Exception e) {
            messageCallback.onConnectFailed(this.socket);
        }
    }

    public void send(Message msg) {
        try {
            outputStream.writeObject(msg);
        } catch (Exception e) {
            messageCallback.onConnectFailed(this.socket);
        }
    }
}
