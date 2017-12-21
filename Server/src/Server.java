

import DAO.UsersDAO;
import Util.BCrypt;
import connection.MessageContent.UserLogInMsgContent;
import connection.MessageType;
import connection.NetworkMessage;
import exception.DuplicateUserException;
import pojo.Users;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;


public class Server {
    private static final int PORT = 9052;
    private static final Map<Socket, Object> names = new HashMap<>();
    private static Set<ObjectOutputStream> writers = new HashSet<>();

    public static void main (String[] args) throws Exception{
        ServerSocket server = new ServerSocket(PORT);
        try{
            while (true){
                new Handler(server.accept()).start();
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            server.close();
        }
    }

    private static class Handler extends Thread {
        private Socket socket;
        private ObjectInputStream input;
        private OutputStream os;
        private ObjectOutputStream output;
        private InputStream is;

       // public List<Users> userList = UsersDAO.getUserList();
        public Handler(Socket socket) throws IOException{
            this.socket=socket;
        }

        public void run (){
            try{
                is=socket.getInputStream();
                input = new ObjectInputStream(is);
                os= socket.getOutputStream();
                output = new ObjectOutputStream(os);

                NetworkMessage msg = (NetworkMessage) input.readObject();
                if (msg!=null) checkUser(msg);
                writers.add(output);
//                while (socket.isConnected()){
//                    NetworkMessage inputmsg = (NetworkMessage) input.readObject();
//                    if(inputmsg!=null){
//                        if(inputmsg.getType()== MessageType.LOG_IN){
//                        }
//                    }
//                }
            }catch (SocketException e){
                    e.printStackTrace();
            }catch (Exception e){
                    e.printStackTrace();
            }finally {

            }
        }

        private void checkUser (NetworkMessage msg) throws IOException {
//            msg.setType(MessageType.CONNECTED);
//            output.writeObject(msg);
           List<Users> userList = UsersDAO.getUserList();
            UserLogInMsgContent userContent= (UserLogInMsgContent) msg.getContent();
            for (Users u : userList) {
                if (u.getUsername().equals(userContent.getUsername()) && u.getPass().equals(userContent.getPass())) {
                    msg.setType(MessageType.LOG_IN_SUCEEDED);
                    output.writeObject(msg);
//                    if (BCrypt.checkpw(u.getPass(), userContent.getPass())) {
//                        msg.setType(MessageType.CONNECTED);
//                        output.writeObject(msg);
//                    }
                }
            }
        }


        private void checkDuplicateUser (NetworkMessage msg) throws DuplicateUserException {
            if(!names.containsKey(msg.getContent())){
              //  user=new Users();
            }
        }

        private void write(NetworkMessage msg) throws IOException{
            for (ObjectOutputStream writer:writers){
                writer.writeObject(msg);
                writer.reset();
            }
        }
    }
}