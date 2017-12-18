import exception.DuplicateUserException;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 9052;
    private static final Map<Socket, Object> names = new HashMap<>();
    private static Map<Socket, ObjectOutputStream> writers = new HashMap<>();
    //private ArrayList<Users> userList = new ArrayList<>();

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
        //private String name;
        private Socket socket;
        //private Users user;
        private ObjectInputStream input;
        private OutputStream os;
        private ObjectOutputStream output;
        private InputStream is;

        public Handler(Socket socket) throws IOException{
            this.socket=socket;
        }

        public void run (){
            try{
                is=socket.getInputStream();
                input = new ObjectInputStream(is);
                os= socket.getOutputStream();
                output = new ObjectOutputStream(os);

               // Message msg = (Message) input.readObject();

            }catch (SocketException e){

            }catch (Exception e){

            }finally {

            }
        }

        private void checkDuplicateUser (connection.Message msg) throws DuplicateUserException {
            if(!names.containsKey(msg.getContent())){
              //  user=new Users();
            }
        }
    }
}