package Router;

import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;

public class Router {
    private static String routerIP = "127.0.0.1";
    private static int brokerPort = 5000;
    private static int marketPort = 5001;
    private ServerSocketChannel brokerServer;
    private ServerSocketChannel marketServer;
    private Selector selector;
    private static ArrayList<Client> brokerClients = new ArrayList<Client>();
    private static ArrayList<Client> marketClients = new ArrayList<Client>();

    Router() {
        try {
            selector = Selector.open();
            brokerServer = ServerSocketChannel.open();
            marketServer = ServerSocketChannel.open();
            brokerServer.configureBlocking(false);
            marketServer.configureBlocking(false);
            brokerServer.socket().bind(new InetSocketAddress(routerIP, brokerPort));
            marketServer.socket().bind(new InetSocketAddress(routerIP, marketPort));
            brokerServer.register(selector, SelectionKey.OP_ACCEPT);
            marketServer.register(selector, SelectionKey.OP_ACCEPT);
            startRouter();
        } catch (Exception e) {
            System.out.println("The Router is already running somewhere.");
            stopRouter();
        }
    }

    public void startRouter() {
        System.out.println("[ROUTER] Starting...");
        ExecutorService threads = Executors.newFixedThreadPool(2);
        threads.submit(new RunnableServer(marketServer, selector));
        threads.submit(new RunnableServer(brokerServer, selector));
        threads.shutdown();
    }

    public void stopRouter() {
        try {
            brokerServer.close();
            marketServer.close();
        } catch (Exception e) {
            //handle failed closure
        }
    }

    public static void addClient(int port, Client client) {
        if (port == 5000) {
            brokerClients.add(client);
        } else {
            marketClients.add(client);
        }
    }

    public static void removeClient(int port, Client client) {
        if (port == 5000) {
            brokerClients.remove(client);
        } else {
            marketClients.remove(client);
        }
    }

    public static ArrayList<Client> getClients(int port) {
        if (port == 5000) {
            return brokerClients;
        } else {
            return marketClients;
        }
    }

    public static Client getClient(int port, String id) {
        ArrayList<Client> list = getClients(port);
        for (Client client : list) {
            if (client.getClientID().equals(id)) {
                return client;
            }
        }
        return null;
    }

    public static Client getClientBySocketChannel(int port, SocketChannel sc) {
        ArrayList<Client> list = getClients(port);
        for (Client client : list) {
            if (client.getSocketChannel().equals(sc)) {
                return client;
            }
        }
        return null;
    }
}