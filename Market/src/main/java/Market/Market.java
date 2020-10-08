package Market;

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

public class Market {
    private static int marketPort = 5001;
    private static String marketIP = "127.0.0.1";
    private static String marketID = "";
    private static BufferedReader input = null;
    private PrintWriter output;
    private static SocketChannel sc;
    private ArrayList<Instrument> instruments;

    Market(ArrayList<Instrument> instruments) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
               closeSocketChannel();
            }
        });

        System.out.println("I AM A MARKET!!!");
        this.instruments = instruments;
        try {
            InetSocketAddress addr = new InetSocketAddress(InetAddress.getByName("localhost"), marketPort);
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
            e.printStackTrace();
        }
    }

    public static Boolean processReadySet(Set readySet) throws Exception {
        SelectionKey key = null;
        Iterator iterator = null;
        iterator = readySet.iterator();
        while (iterator.hasNext()) {
            key = (SelectionKey) iterator.next();
            iterator.remove();
        }
        if (key.isConnectable()) {
            Boolean connected = processConnect(key);
            SocketChannel sc = (SocketChannel) key.channel();
            //make connection to router, get id
            if (!connected) {
                return true;
            }
        }
        if (key.isReadable()) {
            //receive message
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = ByteBuffer.allocate(1024);
            sc.read(bb);
            String result = new String(bb.array()).trim();
            System.out.println("Message received from Server: " + result + " Message length= "+ result.length());
            ByteBuffer response;
            if (marketID.equals("")) {
                marketID = result;
            } else {
                switch (result) {
                    case "buy":
                        response = ByteBuffer.wrap("ok i accept buy".getBytes());
                        break;
                    case "sell":
                        response = ByteBuffer.wrap("ok i accept sell".getBytes());
                        break;
                    default:
                        response = ByteBuffer.wrap("REJECTED".getBytes());
                        break;
                }
                sc.write(response);
            }
        }
        /*
        if (key.isWritable()) {
            //send message
            System.out.print("Type a message (type quit to stop): ");
            String msg = input.readLine(); //blocks client from receiving messages
            if (msg.equalsIgnoreCase("quit")) {
                return true;
            }
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer bb = ByteBuffer.wrap(msg.getBytes());
            sc.write(bb);
        }
        */
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
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void closeSocketChannel() {
        try {
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*
    private void sendResponse(String order) {
        try {
            output.println(order);
            System.out.println(input.readLine());
        } catch (Exception e) {
            System.err.println(e);
        }
    }
*/
    public String getMarketID() {
        return marketID;
    }
}