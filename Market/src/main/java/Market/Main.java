package Market;

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
            if (isValidParameters(args)) {
                ArrayList<Instrument> instruments = createInstrumentList(args);
                new Market(instruments);
            } else {
                System.out.println("Usage: java -jar Market.jar <instrument> <quantity> <price>");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isValidParameters(String[] args) {
        if (args.length % 3 != 0 || args.length == 0) {
            return false;
        }

        for (int i = 0; i < args.length; i++) {
            if (i % 3 == 1) {
                try {
                    int quantity = Integer.parseInt(args[i]);
                    if (quantity < 0) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }

            if (i % 3 == 2) {
                try {
                    float price = Float.parseFloat(args[i]);
                    if (price <= 0) {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        }

        return true;
    }

    private static ArrayList<Instrument> createInstrumentList(String[] args) {
        ArrayList<Instrument> instruments = new ArrayList<Instrument>();
        int i = 0;
        String name;
        int quantity = 0;
        float price = 0;
        while (i < args.length) {
            name = args[i];
            try {
                quantity = Integer.parseInt(args[i + 1]);
                price = Float.parseFloat(args[i + 2]);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(2);
            }
            instruments.add(new Instrument(name, quantity, price));
            i += 3;
        }
        return instruments;
    }

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