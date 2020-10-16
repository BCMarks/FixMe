package Broker;

import java.io.BufferedReader;
import java.io.IOException;

public class FIXMessage {
    private static String delim = String.valueOf((char) 1);
    private static String beginString = "8=FIX.4.2".concat(delim); //8
    private static int bodyLength; //9
    private String msgType; //35 (3=reject, 5=logout, 8=Execution report, D=single order)
    private String senderID; //49
    private String targetID; //56
    private String side; //54 (1=buy, 2=sell)
    private String symbol; //55 (Instrument name)
    private String quantity; //38
    private String price; //44
    private String status; //39 (0=new, 2=accepted, 8=rejected)
    private static String checksum; //10
    private String msg;

    //For when the broker sends a buy/sell request
    FIXMessage(BufferedReader input, String clientID) {
        try {
            msgType = "D".concat(delim);
            senderID = clientID.concat(delim);
            status = "0".concat(delim);
            side(input);
            symbol(input);
            quantity(input);
            targetID(input);
            price(input);
            constructMessage();
        } catch (Exception e) {
            //null pointer exception from disconnecting
        }
    }

    FIXMessage(String msg) {
        this.msg = msg;
        String[] msgSplit = msg.split(delim);
        msgType = (msgSplit[2].split("="))[1].concat(delim);
        targetID = (msgSplit[3].split("="))[1].concat(delim);
        senderID = (msgSplit[4].split("="))[1].concat(delim);
        side = (msgSplit[5].split("="))[1].concat(delim);
        symbol = (msgSplit[6].split("="))[1].concat(delim);
        quantity = (msgSplit[7].split("="))[1].concat(delim);
        price = (msgSplit[8].split("="))[1].concat(delim);
        status = (msgSplit[9].split("="))[1].concat(delim);
    }

    private void side(BufferedReader input) throws IOException {
        System.out.print("Do you wish to Buy or Sell? ");
        String txt = input.readLine(); //blocks client from receiving messages
        switch (txt.toLowerCase()) {
            case "buy":
                side = "1".concat(delim);
                break;
            case "sell":
                side = "2".concat(delim);
                break;
            default:
                System.out.println("Invalid Response: You may only buy or sell.");
                side(input);
                break;
        }
    }

    private void symbol(BufferedReader input) throws IOException {
        System.out.print("Which Instrument? ");
        String txt = input.readLine(); //blocks client from receiving messages
        if (txt.matches("^[a-zA-Z0-9]*$")) {
            symbol = txt.concat(delim);
        } else {
            System.out.println("Invalid Response: Instrument name must only contain alphanumeric characters.");
            symbol(input);
        }
    }

    private void quantity(BufferedReader input) throws IOException {
        System.out.print("How much? ");
        String txt = input.readLine(); //blocks client from receiving messages
        try {
            int quantity = Integer.parseInt(txt);
            if (quantity < 0) {
                System.out.println("Invalid Response: Quantity must be a positive integer.");
                quantity(input);
            } else {
                this.quantity = txt.concat(delim);
            }
        } catch (Exception e) {
            System.out.println("Invalid Response: Quantity must be a positive integer.");
            quantity(input);
        }
    }

    private void targetID(BufferedReader input) throws IOException {
        System.out.print("Which market ID? ");
        String txt = input.readLine(); //blocks client from receiving messages
        if (isValidID(txt)) {
            targetID = txt.concat(delim);
        } else {
            System.out.println("Invalid Response: A valid ID is 6 digits long.");
            targetID(input);
        }
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

    private void price(BufferedReader input) throws IOException {
        System.out.print("For what price per instrument? ");
        String txt = input.readLine(); //blocks client from receiving messages
        try {
            float price = Float.parseFloat(txt);
            if (price <= 0) {
                System.out.println("Invalid Response: Price may not be negative");
                price(input);
            } else {
                this.price = txt.concat(delim);
            }
        } catch (Exception e) {
            System.out.println("Invalid Response: Price must be a valid float.");
            price(input);
        }
    }

    private void calculateBodyLength() {
        bodyLength = msgType.length() + senderID.length() + targetID.length() + side.length() +
            symbol.length() + quantity.length() + price.length() + status.length() + 24; // 8 tags of length 3 each
    }

    private void calculateChecksum(String msg) {
        int total = 0;
        for (char c : msg.toCharArray()) {
            total += (int) c;
        }
        total %= 256;
        StringBuilder result = new StringBuilder("");
        String strTotal = Integer.toString(total);
        while (result.length() + strTotal.length() < 3) {
            result.append("0");
        }
        result.append(strTotal);
        checksum = result.toString();
    }

    private void constructMessage() {
        calculateBodyLength();
        StringBuilder msg = new StringBuilder("");
        msg.append(beginString);
        msg.append("9=".concat(String.valueOf(bodyLength)).concat(delim));
        msg.append("35=".concat(msgType));
        msg.append("49=".concat(senderID));
        msg.append("56=".concat(targetID));
        msg.append("54=".concat(side));
        msg.append("55=".concat(symbol));
        msg.append("38=".concat(quantity));
        msg.append("44=".concat(price));
        msg.append("39=".concat(status));
        calculateChecksum(msg.toString());
        msg.append("10=".concat(checksum));
        this.msg = msg.toString();
    }

    public String getMessage() {
        return msg;
    }

    public void printStatus() {
        if ("2".equals(status.trim())) {
            System.out.println("Order has been Accepted.");
        } else if ("8".equals(status.trim())) {
            System.out.println("Order has been Rejected.");
        } else {
            System.out.println("Order could not be processed.");
        }
    }
}