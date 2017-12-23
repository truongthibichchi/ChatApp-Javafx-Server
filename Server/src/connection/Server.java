package connection;

import controllers.ServerWindowController;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server extends Thread implements MessageCallback {
    private int port;

    private HashMap<String, Socket> usersSockets = new HashMap<>();
    private HashMap<Socket, ThreadPerSocket> usersThreads = new HashMap<>();
    private ArrayList<User> users;
    private ArrayList<User> usersData=null;
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
        usersData=createFakeData();

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


    private ArrayList<User> createFakeData(){
        ArrayList<User> list = new ArrayList<>();
            User user1 = new User("nhi", "nhi", "nhi", Status.ONLINE);
            User user2 = new User("me", "me", "me", Status.ONLINE);
            User user3 = new User("be", "be", "be", Status.ONLINE);
            list.add(user1);
            list.add(user2);
            list.add(user3);
        return list;
    }
    @Override
    public void onReceivedMessage(Socket socket, Message msg) {
        switch (msg.getType()) {
            case LOGIN:
                    checkDuplicateUser(socket, msg);
                break;
            case SIGN_UP:
                

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
        String username=null;
        for(Map.Entry<String, Socket> entry :usersSockets.entrySet()){
            if(entry.getValue().equals(socket)){
                username=entry.getKey();
            }
        }
        usersThreads.remove(socket);
        if(username!=null) usersSockets.remove(username);
        controller.log(username+" disconnected!");
    }

    public void onOpenServerFailed() {
        controller.log("Failed to open server");
    }

    public void onNetworkError() {
        controller.log("Network Error");
    }

    private void checkDuplicateUser(Socket socket, Message msg){
        if (!usersSockets.containsKey(msg.getUserName())){
            msg.setType(MessageType.LOGIN_SUCCEEDED);
            msg.setNickname(msg.getUserName());
            msg.setUserListData(usersData);
            usersSockets.put(msg.getUserName(), socket);
//            User user = new User();
//            user.setUsername(msg.getUserName());
//            user.setNickname(msg.getNickname());
//            user.setPass(msg.getPass());
//            user.setStatus(Status.ONLINE);
//
//            users.add(user);
            //msg.setUserList(users);
            sendTo(msg.getUserName(), msg);
            controller.log(msg.getUserName()+ " has connected to server");
        }
        else {
            controller.log(msg.getUserName()+ " is already connected to server, can't not log in");
        }
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
                messageCallback.onReceivedMessage(this.socket, msg);
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
