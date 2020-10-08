package Router;

import java.nio.channels.SocketChannel;

public class Client {
    private SocketChannel channel;
    private String id;

    Client(SocketChannel sc, String clientID) {
        channel = sc;
        id = clientID;
    }

    public String getClientID() {
        return id;
    }

    public SocketChannel getSocketChannel() {
        return channel;
    }
}