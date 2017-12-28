package connection;

import controllers.ServerWindowController;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
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
        User user1 = new User("chi", "chi", "Mi Ri Ki", Status.DISCONNECT);
        User user2 = new User("khanh", "khanh", "Trương Duy Khánh", Status.DISCONNECT);
        User user3 = new User("quang", "quang", "Đức Quang", Status.DISCONNECT);
        User user4 = new User("dim", "dim", "Ngọc Diễm", Status.DISCONNECT);
        User user5 = new User("thanh", "thanh", "Hoài Thanh", Status.DISCONNECT);
        list.add(user1);
        list.add(user2);
        list.add(user3);
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
                onSignUp(socket, msg);
                break;
            case CHAT_TEXT:
                    onChatTextFromGroup(msg);
                break;
        }
    }

    @Override
    public void onConnectFailed(Socket socket) {
        String username = null;
        String nickname= null;
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            if (entry.getValue().equals(socket)) {
                username = entry.getKey();
                for (User user : usersData) {
                    if (user.getUsername().equals(username)) {
                        user.setStatus(Status.DISCONNECT);
                        nickname=user.getNickname();
                        break;
                    }
                }
            }
        }
        usersThreads.remove(socket);
        usersSockets.remove(username);
        onUserDisconnected(username, nickname, Status.DISCONNECT);
        controller.log(username + " disconnected!");
    }

    public void onOpenServerFailed() {
        controller.log("Failed to open server");
    }

    public void onNetworkError() {
        controller.log("Network Error");
    }

    private void onSignUp(Socket socket, Message msg) {
        String signUpUsername = msg.getUserName();
        String signUpNickname = msg.getNickname();
        String signUpPass = msg.getPass();
        for (User user : usersData) {
            if (user.getUsername().equals(signUpUsername)) {
                msg.setType(MessageType.SIGN_UP_FAILED);
                ThreadPerSocket thread = usersThreads.get(socket);
                thread.send(msg);
                usersThreads.remove(socket);
                return;
            }
        }
        msg.setType(MessageType.CONNECTED);
        usersSockets.put(signUpUsername, socket);
        User user = new User(signUpUsername, signUpPass, signUpNickname, Status.ONLINE);
        usersData.add(user);
        msg.setUserListData(usersData);
        sendTo(msg.getUserName(), msg);


        Message msgToOtherUsers = new Message();
        msgToOtherUsers.setType(MessageType.NEW_USER_CONNECTED);
        msgToOtherUsers.setUserName(signUpUsername);
        msgToOtherUsers.setNickname(signUpNickname);
        msgToOtherUsers.setStatus(Status.ONLINE);

        sendToAllUsersExcept(signUpUsername, msgToOtherUsers);
        controller.log(msg.getUserName() + " sign up suceeded");

    }

    private void onUserAttemptToLogin(Socket socket, Message msg) {
        String loginUserName = msg.getUserName();
        String loginPassword = msg.getPass();
        String nickname=null;
        if(isCorrectUserInfo(loginUserName, loginPassword)){
            if (usersSockets.containsKey(msg.getUserName())) {

                msg.setType(MessageType.ALREADY_LOGGED_IN);
                ThreadPerSocket thread = usersThreads.get(socket);
                thread.send(msg);

                controller.log(msg.getUserName() + " has already connected to server, can not log in");
                usersThreads.get(socket).stopThread();
                usersThreads.remove(socket);
                return;
            }
            else{
                usersSockets.put(loginUserName, socket);
                setUserStatus(loginUserName, Status.ONLINE);

                Message msgBackToUser = new Message();
                for (User user: usersData){
                    if (loginUserName.equals(user.getUsername())){
                        nickname=user.getNickname();
                    }
                }

                msgBackToUser.setUserName(loginUserName);
                msgBackToUser.setNickname(nickname);
                msgBackToUser.setType(MessageType.CONNECTED);
                msgBackToUser.setUserListData(usersData);
                sendTo(loginUserName, msgBackToUser);

                Message msgToOtherUsers = new Message();
                msgToOtherUsers.setType(MessageType.NEW_USER_CONNECTED);
                msgToOtherUsers.setUserName(loginUserName);
                msgToOtherUsers.setNickname(nickname);
                msgToOtherUsers.setStatus(Status.ONLINE);

                sendToAllUsersExcept(loginUserName, msgToOtherUsers);

                controller.log(loginUserName + " has connected to server");
                return;
            }
        }
        if(!isCorrectUserInfo(loginUserName, loginPassword)){
            msg.setType(MessageType.WRONG_INFO);
            ThreadPerSocket thread = usersThreads.get(socket);
            thread.send(msg);
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

    private void onUserDisconnected(String username, String nickname, Status status) {
        Message msg = new Message();
        msg.setType(MessageType.DISCONNECT);
        msg.setUserName(username);
        msg.setNickname(nickname);
        msg.setStatus(Status.DISCONNECT);
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            sendTo(entry.getKey(), msg);
        }
    }

    private void sendTo(String userName, Message msg) {
        Socket socket = usersSockets.get(userName);
        if(socket!=null){
            ThreadPerSocket thread = usersThreads.get(socket);
            thread.send(msg);
        }
    }

    private void onChatTextFromGroup(Message msg) {
        String participants= "";
        ArrayList<User> users = msg.getChatUsers();
        for(User user: users){
            participants+=user.getUsername();
        }
        controller.log(msg.getUserName()+ " send a message to "+ participants + ": "+msg.getText());

        for(User user: users){
            sendTo(user.getUsername(),msg);
            controller.log(user.getUsername()+" recieved a message from  "+ msg.getUserName()+ ": "+msg.getText());
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
