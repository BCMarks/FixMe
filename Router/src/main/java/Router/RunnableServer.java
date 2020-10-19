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
                    serverType = "BROKER";
                    break;
                case 5001:
                    serverType = "MARKET";
                    break;
                default:
                    System.out.println("Unchecked port detected. Terminating...");
                    System.exit(0);
            }
            System.out.println("[ROUTER] Listening to "+serverType+"s on port "+port);
            awaitMessages();
        } catch (Exception e) {
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
                            if (sc != null) {
                                sc.configureBlocking(false);
                                sc.register(selector, SelectionKey.OP_READ);
                                Client client = new Client(sc, assignID(sc));
                                sendMessage(sc, client.getClientID());
                                Router.addClient(port, client);
                                System.out.println("[ROUTER] Connection Accepted from "+serverType+"_"+client.getClientID()+" on port "+port);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (key.isReadable()) {
                        //on message
                        SocketChannel sc = (SocketChannel) key.channel();
                        if (port == sc.socket().getLocalPort()) {
                            ByteBuffer bb = ByteBuffer.allocate(1024);
                            sc.read(bb);
                            String result = new String(bb.array()).trim();
                            if (result.length() <= 0) {
                                Client client = Router.getClientBySocketChannel(port, sc);
                                if (client != null) {
                                    System.out.println("[ROUTER] "+serverType+"_"+client.getClientID()+" has disconnected.");
                                    Router.removeClient(port, client);
                                }
                                sc.close();
                            } else {
                                handleMessage(sc, result);
                                String clientID = extractTag(result, "49=");
                                System.out.println("["+serverType+"_"+clientID+"] "+ result);
                            }                            
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Something went wrong. Terminating...");
            System.exit(1);
        }
    }

    private void handleMessage(SocketChannel sender, String order) throws IOException {
        if (isValidMessage(order)) {
            int destinationPort = 5000;
            if (destinationPort == port) {
                destinationPort += 1;
            }
            String id = extractTag(order, "56=");
            Client recipient = Router.getClient(destinationPort, id);
            if (recipient != null) {
                sendMessage(recipient.getSocketChannel(), order);
            } else {
                sendMessage(sender, "Target market is not online or does not exist.");
            }
        }
    }

    private boolean isValidMessage(String msg) {
        String delim = String.valueOf((char) 1);
        String[] segments = msg.split(delim);
        int total = 0;
        for (int i = 0; i < segments.length - 1; i++) {
            for (char c : segments[i].toCharArray()) {
                total += (int) c;
            }
            total += 1;
        }
        total %= 256;

        int tag = 0;
        try {
            tag = Integer.parseInt(extractTag(msg, "10="));
        } catch (Exception e) {
            return false;
        }

        if (total == tag) {
            return true;
        }
        return false;
    }

    private String extractTag(String msg, String tag) {
        String delim = String.valueOf((char) 1);
        return ((msg.split(tag))[1].split(delim))[0];
    }

    private String assignID(SocketChannel sc) {
        StringBuilder output = new StringBuilder("");
        int intID = ThreadLocalRandom.current().nextInt(1, 1000000);
        String strID = Integer.toString(intID);
        while (output.length() + strID.length() < 6) {
            output.append("0");
        }
        output.append(strID);
        String tempID = output.toString();
        if (isUniqueID(tempID)) {
            return tempID;
        } else {
            return assignID(sc);
        }
    }

    private boolean isUniqueID(String idToCheck) {
        Client client = Router.getClient(port, idToCheck);
        if (client != null) {
            return false;
        }
        return true;
    }

    private void sendMessage(SocketChannel recipient, String msg) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        recipient.write(buffer);
    }
}