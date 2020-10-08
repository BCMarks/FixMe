package Broker;

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

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length == 1 && isValidID(args[0])) {
                new Broker(args[0]);
            } else if (args.length == 0) {
                new Broker("");
            } else {
                System.out.println("Usage: java -jar Broker.jar <id (optional)>");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //still need to check that client is connected or not maybe
    private static boolean isValidID(String id) {
        try {
            Integer.parseInt(id);
            if (id.length() != 6) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}