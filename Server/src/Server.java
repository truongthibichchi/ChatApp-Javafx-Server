

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

   /* public void showDialog (String message){
        Platform.runLater(()->{
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("SERVER!");
            alert.setHeaderText(message);
            alert.setContentText("server is running.");
            alert.showAndWait();
        });
    }
*/
    private static class Handler extends Thread {
        //private String name;
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
                os= socket.getOutputStream();
                output = new ObjectOutputStream(os);

                is=socket.getInputStream();
                input = new ObjectInputStream(is);

                //NetworkMessage msg = (NetworkMessage) input.readObject();
                //writers.add(output);
                while (socket.isConnected()){
                    NetworkMessage inputmsg = (NetworkMessage) input.readObject();
                    if(inputmsg!=null){
                        if(inputmsg.getType()== MessageType.LOG_IN){
                            write(inputmsg);
//                            for (Users u: userList){
//                                if(u.getUsername().equals(user.getUsername())){
//                                    if(BCrypt.checkpw(u.getPass(),user.getPass())){
//                                            write(inputmsg);
//                                    }
//                                }
//                            }
                        }
                    }
                }
            }catch (SocketException e){
                    e.printStackTrace();
            }catch (Exception e){
                    e.printStackTrace();
            }finally {

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