package Market;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

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

    //For when the market sends a accepted/rejected response
    FIXMessage(String msg) {
        String[] msgSplit = msg.split(delim);
        targetID = (msgSplit[3].split("="))[1].concat(delim);
        senderID = (msgSplit[4].split("="))[1].concat(delim);
        side = (msgSplit[5].split("="))[1].concat(delim);
        symbol = (msgSplit[6].split("="))[1].concat(delim);
        quantity = (msgSplit[7].split("="))[1].concat(delim);
        price = (msgSplit[8].split("="))[1].concat(delim);
    }

    public void prepareResponse(ArrayList<Instrument> instruments) {
        msgType = "8".concat(delim);
        status = processRequest(instruments).concat(delim);
        constructMessage();
    }

    private String processRequest(ArrayList<Instrument> instruments) {
        Instrument requestedInstrument = null;
        int quantityValue = 0;
        float priceValue = 0;
        try {
            quantityValue = Integer.parseInt(quantity.trim());
            priceValue = Float.parseFloat(price.trim());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
        for (Instrument instrument : instruments) {
            //nested if statements sies
            if (instrument.getName().equals(symbol.trim())) {
                if ("1".equals(side.trim())) {
                    if (instrument.getQuantity() >= quantityValue && instrument.getPrice() <= priceValue) {
                        instrument.setQuantity(-quantityValue);
                        return "2";
                    } else {
                        return "8";
                    }
                } else if ("2".equals(side.trim())) {
                    if (quantityValue > 0 && instrument.getPrice() >= priceValue) {
                        instrument.setQuantity(quantityValue);
                        return "2";
                    } else {
                        return "8";
                    }
                }
            }
        }
        return "8";
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
        if (status == "2") {
            System.out.println("Order has been Accepted.");
        } else if (status == "8") {
            System.out.println("Order has been Rejected.");
        } else {
            System.out.println("Order could not be processed.");
        }
    }
}