package connection;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Connector {
    private static final int PORT = 9052;
    private ServerSocket server = null;
    private Listener listenerThread = null;
    private ArrayList<Socket> sockets = new ArrayList<>();

    private static final Map<Socket, Object> messageReceivers = new HashMap<>();
    private static Map<Socket, ObjectOutputStream> outputStreams = new HashMap<>();
    private ConnectionCallBack connectionCallBack;

    public Connector(ConnectionCallBack callBack){this.connectionCallBack=callBack;}

    public synchronized void openListener(){
        try{
            //server = new ServerSocket(PORT);
            listenerThread = new Listener();
            listenerThread.setListener(server);
            listenerThread.setListenCallBack((ListenCallBack) this);
            listenerThread.start();

            Thread thread = new Thread(()-> connectionCallBack.onListenerOpenSucceeded());
            thread.start();
        }catch(Exception e){
            connectionCallBack.onListenerOpenFailed();
        }
    }


    class Listener extends Thread{
        private ServerSocket listener = null;

        private ListenCallBack listenCallBack;

        public void run (){
            try{
                if(listener==null){
                    listener=new ServerSocket(PORT);
                }
                while (true){
                    Socket socket = listener.accept();
                    Thread callbackThread = new Thread(()-> listenCallBack.onConnectionReceived(socket));
                    callbackThread.start();
                }
            }catch (Exception e){}
        }

        public synchronized void stopListen (){
            try{
                listener.close();
            }catch (Exception e){}
        }

        public void setListener(ServerSocket listener) {
            this.listener = listener;
        }

        public void setListenCallBack(ListenCallBack callBack){this.listenCallBack=callBack;}
    }

    class MessageReceiver extends Thread{
        private Socket socket;
        private ObjectInputStream inputStream;
        private MessageReceiverCallback msgCallback;

        public MessageReceiver(MessageReceiverCallback msgCallback, Socket socket){
            this.msgCallback=msgCallback;
            this.socket=socket;
        }

        public void run(){
            try{
                inputStream = new ObjectInputStream(socket.getInputStream());
            }catch(Exception e){
                Thread callBackThread = new Thread(()->msgCallback.onStreamClosed(socket));
                callBackThread.start();
                return;
            }
            while (true){
                try{
                    Object msg = inputStream.readObject();
                    Thread callBackThread = new Thread(()->msgCallback.onMsgReceived(msg,socket));
                    callBackThread.start();
                }catch(Exception e){
                    Thread callBackThread = new Thread (()-> msgCallback.onStreamClosed(socket));
                    callBackThread.start();
                    break;
                }
            }
        }
    }
}
