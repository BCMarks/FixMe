package Broker;

import java.net.*;
import java.util.List;
import java.io.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class Broker {
    private static int brokerPort = 5000;
    private static String brokerIP = "127.0.0.1";
    private static String brokerID = "";
    private static BufferedReader input = null;
    private PrintWriter output;
    private static SocketChannel sc;

    Broker() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                closeSocketChannel();
            }
        });
        try {
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("localhost"), brokerPort);
            Selector selector = Selector.open();
            sc = SocketChannel.open();
            sc.configureBlocking(false);
            sc.connect(addr);
            sc.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            input = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                if (selector.select() > 0) {
                    Boolean doneStatus = processReadySet(selector.selectedKeys());
                    if (doneStatus) {
                        break;
                    }
                }
            }
            sc.close();
        } catch (Exception e) {
            System.out.println("Connection issues have arisen.");
        }
    }

    public static Boolean processReadySet(Set readySet) throws Exception {
        SelectionKey key = null;
        Iterator iterator = null;
        boolean awaitingResponse = true;
        iterator = readySet.iterator();
        while (iterator.hasNext()) {
            key = (SelectionKey) iterator.next();
            iterator.remove();
        }
        if (key.isConnectable()) {
            Boolean connected = processConnect(key);
            SocketChannel sc = (SocketChannel) key.channel();
            if (!connected) {
                return true;
            }
            awaitingResponse = false;
        }
        if (key.isReadable()) {
            //receive message
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = ByteBuffer.allocate(1024);
            sc.read(bb);
            String result = new String(bb.array()).trim();
            if (brokerID.equals("")) {
                brokerID = result;
            } else {
                System.out.println("[ROUTER] " + result);
                try {
                    FIXMessage msg = new FIXMessage(result);
                    msg.printStatus();
                } catch (Exception e) {
                    // Router messages do not follow fix format
                }
            }
            awaitingResponse = false;
        }
        if (key.isWritable()) {
            //send message
            if (!awaitingResponse) {
                try {
                    FIXMessage msg = new FIXMessage(input, brokerID);
                    SocketChannel sc = (SocketChannel) key.channel();
                    ByteBuffer bb = ByteBuffer.wrap((msg.getMessage()).getBytes());
                    sc.write(bb);
                    awaitingResponse = true;
                } catch (IOException e) {
                    System.out.println("Connection issues have arisen.");
                    System.exit(1);
                } catch (Exception e) {
                    System.exit(1);
                }
                
            }
        }
        return false;
    }

    public static Boolean processConnect(SelectionKey key) {
        SocketChannel sc = (SocketChannel) key.channel();
        try {
            while (sc.isConnectionPending()) {
                sc.finishConnect();
            }
        } catch (IOException e) {
            key.cancel();
            System.out.println("Router is offline.");
            return false;
        }
        return true;
    }

    public static void closeSocketChannel() {
        try {
            sc.close();
            System.out.println("\nDisconnecting...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getBrokerID() {
        return brokerID;
    }

    private void sendMessage(SocketChannel recipient, String msg) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(msg.getBytes());
        recipient.write(buffer);
    }
}