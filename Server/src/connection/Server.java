package connection;

import controllers.ServerWindowController;

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
    private ArrayList<User> usersData = null;
    private ServerWindowController controller;

    public Server(int port) {
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
        usersData = createFakeData();

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


    private ArrayList<User> createFakeData() {
        ArrayList<User> list = new ArrayList<>();
        User user1 = new User("chi", "chi", "chi", Status.DISCONNECT);
//            User user2 = new User("khanh", "khanh", "khanh", Status.DISCONNECT);
//            User user3 = new User("quang", "quang", "quang", Status.DISCONNECT);
        User user4 = new User("dim", "dim", "dim", Status.DISCONNECT);
        User user5 = new User("thanh", "thanh", "thanh", Status.DISCONNECT);
        list.add(user1);
//            list.add(user2);
//            list.add(user3);
        list.add(user4);
        list.add(user5);
        return list;
    }

    @Override
    public void onReceivedMessage(Socket socket, Message msg) {
        switch (msg.getType()) {
            case LOGIN:
                onUserAttemptToLogin(socket, msg);
                break;
            case SIGN_UP:
                SignUp(socket, msg);
                break;

            case CHAT_TEXT:

                break;
        }
    }

    @Override
    public void onConnectFailed(Socket socket) {
        String username = null;
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            if (entry.getValue().equals(socket)) {
                username = entry.getKey();
                for (User user : usersData) {
                    if (user.getUsername().equals(username)) {
                        user.setStatus(Status.DISCONNECT);
                    }
                }
            }
        }
        usersThreads.remove(socket);
        usersSockets.remove(username);
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            resetUserList(entry.getKey());
        }
        controller.log(username + " disconnected!");
    }

    public void onOpenServerFailed() {
        controller.log("Failed to open server");
    }

    public void onNetworkError() {
        controller.log("Network Error");
    }

    private void SignUp(Socket socket, Message msg) {
        for (User user : usersData) {
            if (msg.getUserName() == user.getUsername()) {
                msg.setType(MessageType.SIGN_UP_FAILED);
                sendTo(msg.getUserName(), msg);
                return;
            }
        }
        msg.setType(MessageType.CONNECTED);
        usersSockets.put(msg.getUserName(), socket);
        User user = new User(msg.getUserName(), msg.getPass(), msg.getNickname());
        user.setStatus(Status.ONLINE);
        usersData.add(user);

        msg.setUserListData(usersData);
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            sendTo(entry.getKey(), msg);
        }
        sendTo(msg.getUserName(), msg);
        controller.log(msg.getUserName() + " sign up suceeded");

    }

    private void onUserAttemptToLogin(Socket socket, Message msg) {
        if (usersSockets.containsKey(msg.getUserName())) {
            msg.setType(MessageType.LOG_IN_FAILED);
            sendTo(msg.getUserName(), msg);
            controller.log(msg.getUserName() + " is already connected to server, can not log in");

            usersThreads.get(socket).stopThread();
            usersThreads.remove(socket);
            usersSockets.remove(msg.getUserName());

            return;
        }

        String loginUserName = msg.getUserName();
        String loginPassword = msg.getPass();

        if (!usersSockets.containsKey(msg.getUserName())) {
            usersSockets.put(loginUserName, socket);
        }

        if (isCorrectUserInfo(loginUserName, loginPassword)) {
            setUserStatus(loginUserName, Status.ONLINE);

            Message msgBackToUser = new Message();
            msgBackToUser.setType(MessageType.CONNECTED);
            msgBackToUser.setUserListData(usersData);
            sendTo(loginUserName, msgBackToUser);

            Message msgToOtherUsers = new Message();
            msgToOtherUsers.setType(MessageType.NEW_USER_CONNECTED);
            msgToOtherUsers.setUserListData(usersData);
            sendToAllUsersExcept(loginUserName, msgToOtherUsers);

            controller.log(loginUserName + " has logged in to server");
        }
    }

    private void setUserStatus(String loginUserName, Status status) {
        for (User user : usersData) {
            if (user.getUsername().equals(loginUserName)) {
                user.setStatus(status);
                break;
            }
        }
    }

    private boolean isCorrectUserInfo(String loginUserName, String loginPassword) {
        for (User user : usersData) {
            if (user.getUsername().equals(loginUserName)) {
                if (user.getPass().equals(loginPassword)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return false;
    }

    private void sendToAllUsersExcept(String userName, Message msg) {
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            if (!entry.getKey().equals(userName)) {
                sendTo(entry.getKey(), msg);
            }
        }
    }

    private void resetUserList(String username) {
        Message msg = new Message();
        msg.setType(MessageType.DISCONNECT);
        msg.setUserListData(usersData);
        for (User user : usersData) {
            sendTo(user.getUsername(), msg);
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
            System.err.println(e);
            messageCallback.onConnectFailed(this.socket);
        }
    }

    public void send(Message msg) {
        try {
            outputStream.writeObject(msg);
        } catch (Exception e) {
            System.err.println(e);
            messageCallback.onConnectFailed(this.socket);
        }
    }

    public void stopThread() {
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (Exception ignored) {

        }
    }
}
