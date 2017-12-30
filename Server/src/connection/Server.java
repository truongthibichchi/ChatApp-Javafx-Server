package connection;

import DAO.UsersDAO;
import controllers.ServerWindowController;
import pojo.Users;

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
        usersData = cloneUsersData();

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

    private ArrayList<User> cloneUsersData() {
        ArrayList<User> list = new ArrayList<>();
        List<Users> userList = UsersDAO.getUserList();
        for (Users user : userList) {
            User usercopy = new User(user.getUsername(), user.getPassword(), user.getNickname(), user.getAvatar(), Status.DISCONNECT);
            list.add(usercopy);
        }
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
            case CHANGE_INFO:
                onChangeInfo(msg);
                break;
            case CHAT_TEXT:
                onChatTextFromGroup(msg);
                break;
        }
    }

    @Override
    public void onConnectFailed(Socket socket) {
        String username = null;
        String nickname = null;
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            if (entry.getValue().equals(socket)) {
                username = entry.getKey();
                for (User user : usersData) {
                    if (user.getUsername().equals(username)) {
                        user.setStatus(Status.DISCONNECT);
                        nickname = user.getNickname();
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
        byte[] signUpAvatar = msg.getAvatar();

        Users saveUser = new Users(msg.getUserName(), msg.getPass(), msg.getNickname(), msg.getAvatar());
        int notExisted = UsersDAO.InsertUser(saveUser);
        if (notExisted == 1) {
            msg.setType(MessageType.CONNECTED);
            User user = new User(signUpUsername, signUpPass, signUpNickname, signUpAvatar, Status.DISCONNECT);
            usersData.add(user);

            controller.log(msg.getUserName() + " sign up suceeded");
            ThreadPerSocket thread = usersThreads.get(socket);
            thread.send(msg);
            onConnectFailed(socket);

            Message msgToOtherUsers = new Message();
            msgToOtherUsers.setType(MessageType.NEW_USER_CONNECTED);
            msgToOtherUsers.setUserName(signUpUsername);
            msgToOtherUsers.setNickname(signUpNickname);
            msgToOtherUsers.setStatus(Status.DISCONNECT);

            sendToAllUsersExcept(signUpUsername, msgToOtherUsers);


            return;

        } else {
            msg.setType(MessageType.SIGN_UP_FAILED);
            ThreadPerSocket thread = usersThreads.get(socket);
            thread.send(msg);
            onConnectFailed(socket);

        }

    }

    private void onUserAttemptToLogin(Socket socket, Message msg) {
        String loginUserName = msg.getUserName();
        String loginPassword = msg.getPass();
        String nickname = null;
        //byte[] avatar = null;
        if (isCorrectUserInfo(loginUserName, loginPassword)) {
            if (usersSockets.containsKey(msg.getUserName())) {

                msg.setType(MessageType.ALREADY_LOGGED_IN);
                ThreadPerSocket thread = usersThreads.get(socket);
                thread.send(msg);

                controller.log(msg.getUserName() + " has already connected to server, can not log in");
                usersThreads.get(socket).stopThread();
                usersThreads.remove(socket);
                return;
            } else {
                usersSockets.put(loginUserName, socket);
                setUserStatus(loginUserName, Status.ONLINE);

                Message msgBackToUser = new Message();
                for (User user : usersData) {
                    if (loginUserName.equals(user.getUsername())) {
                        nickname = user.getNickname();
                        //   avatar=user.getAvatar();
                    }
                }

                msgBackToUser.setUserName(loginUserName);
                msgBackToUser.setPass(msg.getPass());
                msgBackToUser.setNickname(nickname);
                //msgBackToUser.setAvatar(avatar);
                msgBackToUser.setStatus(Status.ONLINE);
                msgBackToUser.setType(MessageType.CONNECTED);
                msgBackToUser.setUserListData(usersData);
                sendTo(loginUserName, msgBackToUser);

                Message msgToOtherUsers = new Message();
                msgToOtherUsers.setType(MessageType.NEW_USER_CONNECTED);
                msgToOtherUsers.setUserName(loginUserName);
                msgToOtherUsers.setNickname(nickname);
                // msgToOtherUsers.setAvatar(avatar);
                msgToOtherUsers.setStatus(Status.ONLINE);

                sendToAllUsersExcept(loginUserName, msgToOtherUsers);

                controller.log(loginUserName + " has connected to server");
                return;
            }
        }
        if (!isCorrectUserInfo(loginUserName, loginPassword)) {
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


    private void onUserDisconnected(String username, String nickname, Status status) {
        Message msg = new Message();
        msg.setType(MessageType.DISCONNECT);
        msg.setUserName(username);
        msg.setNickname(nickname);
        // msg.setAvatar(avatar);
        msg.setStatus(Status.DISCONNECT);
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            sendTo(entry.getKey(), msg);
        }
    }

    private void onChangeInfo(Message msg) {
        Users user = new Users(msg.getUserName(), msg.getPass(), msg.getNickname(), msg.getAvatar());
        boolean r = UsersDAO.UpdateUser(user);
        if (r == true) {
            msg.setType(MessageType.CHANGE_INFO_SUCCEEDED);
            for (User u : usersData) {
                if (u.getUsername().equals(msg.getUserName())) {
                    u.setPass(msg.getPass());
                    u.setNickname(msg.getNickname());
                    u.setAvatar(msg.getAvatar());
                }
            }
            msg.setUserListData(usersData);
            sendToAll(msg);
            controller.log(msg.getUserName()+"has changed nickname, username to: "+ msg.getNickname()+", "+msg.getPass());
        }
        else{
            msg.setType(MessageType.CHANGE_INFO_FAILED);
            controller.log("Change "+msg.getUserName()+"'s info failed");
            sendTo(msg.getUserName(), msg);
        }
    }

    private void sendTo(String userName, Message msg) {
        Socket socket = usersSockets.get(userName);
        if (socket != null) {
            ThreadPerSocket thread = usersThreads.get(socket);
            thread.send(msg);
        }
    }

    private void sendToAll(Message msg) {
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            sendTo(entry.getKey(), msg);
        }
    }

    private void sendToAllUsersExcept(String userName, Message msg) {
        for (Map.Entry<String, Socket> entry : usersSockets.entrySet()) {
            if (!entry.getKey().equals(userName)) {
                sendTo(entry.getKey(), msg);
            }
        }
    }

    private void onChatTextFromGroup(Message msg) {
        String participants = "";
        ArrayList<User> users = msg.getChatUsers();
        for (User user : users) {
            participants += user.getUsername();
        }
        controller.log(msg.getUserName() + " send a message to " + participants + ": " + msg.getText());

        for (User user : users) {
            sendTo(user.getUsername(), msg);
            controller.log(user.getUsername() + " recieved a message from  " + msg.getUserName() + ": " + msg.getText());

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
