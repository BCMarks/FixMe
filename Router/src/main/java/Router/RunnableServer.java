package Router;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RunnableServer implements Runnable {
    private ServerSocketChannel channel;
    private Selector selector;
    private int port;
    private String serverType;

    RunnableServer(ServerSocketChannel channel, Selector selector) {
        this.channel = channel;
        this.selector = selector;
        port = channel.socket().getLocalPort();
    }

    @Override
    public void run() {
        try {
            switch (port) {
                case 5000:
                    serverType = "Broker";
                    break;
                case 5001:
                    serverType = "Market";
                    break;
                default:
                    System.out.println("Unchecked port detected. Terminating...");
                    System.exit(0);
            }
            System.out.println("Router listening to "+serverType+"s on port "+port);
            awaitMessages();
        } catch(Exception e) {
            System.out.println("Router: "+e);
        }
    }

    private void awaitMessages() {
        try {
            SelectionKey key = null;
            while (true) {
                if (selector.select() <= 0) {
                    continue;
                }
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                while (iterator.hasNext()) {
                    key = (SelectionKey) iterator.next();
                    iterator.remove();
                    if (key.isAcceptable()) {
                        //on connection
                        try {
                            SocketChannel sc = channel.accept();
                            sc.configureBlocking(false);
                            sc.register(selector, SelectionKey.OP_READ);
                            Client client = new Client(sc, assignID(sc));
                            sendMessage(sc, client.getClientID());
                            Router.addClient(port, client);
                            System.out.println("Connection Accepted from "+serverType+"_"+sc.socket().getPort()+" on "+ sc.getLocalAddress() + "\n");
                        } catch (Exception e) {
                            System.out.println("Server port: "+port+"| Wrong port bub");
                        }
                    }
                    if (key.isReadable()) {
                        //on message
                        SocketChannel sc = (SocketChannel) key.channel();
                        if (port == sc.socket().getLocalPort()) {
                            ByteBuffer bb = ByteBuffer.allocate(1024);
                            sc.read(bb);
                            String result = new String(bb.array()).trim();
                            String response = handleMessage(sc, result);
                            System.out.println("Message received from "+serverType+"_"+sc.socket().getPort()+": "+ result + " Message length= " + result.length());
                            System.out.println(response);
                            if (result.length() <= 0) {
                                sc.close();
                                System.out.println("Connection closed...");
                                System.out.println("Server will keep running. Try running another client to re-establish connection");
                            }
                        } 
                        /*else {
                            System.out.println(serverType+": Its not your time "+sc.socket().getPort());
                        }*/
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Awaiting messages ("+serverType+"): "+e);
        }
    }

    private String handleMessage(SocketChannel sender, String order) {
        //actual order needs to be parsed. this is temporary
        switch (order) {
            case "greeting": //not needed
                try {
                    sendMessage(sender, "welcome");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "welcome";
            case "buy":
            case "sell":
                for (Client client : Router.getClients(5001)) {
                    try {
                        //System.out.println(client.getClientID());
                        sendMessage(client.getSocketChannel(), order);
                    } catch (Exception e) {
            
                    }
                }
                return order;
            case "ok i accept buy":
            case "ok i accept sell":
                for (Client client : Router.getClients(5000)) {
                    try {
                        sendMessage(client.getSocketChannel(), order);
                    } catch (Exception e) {
            
                    }
                }
                return order;
            default:
                try {
                    sendMessage(sender, "INVALID REQUEST");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return "Unrecognized command";
        }
    }

    private String assignID(SocketChannel sc) {
        StringBuilder output = new StringBuilder("");
        int intID = ThreadLocalRandom.current().nextInt(1, 1000000);
        String strID = Integer.toString(intID);
        while (output.length() + strID.length() < 6) {
            output.append("0");
        }
        output.append(strID);
        return output.toString();
        //check for no dupes
    }

    private void sendMessage(SocketChannel recipient, String msg) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        recipient.write(buffer);
    }
}