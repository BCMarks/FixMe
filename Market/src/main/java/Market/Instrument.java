package Market;

public class Instrument {
    private String name;
    private int quantity;
    private float price;

    Instrument(String name, int quantity, float price) {
        this.name = name;
        this.quantity = quantity;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public float getPrice() {
        return price;
    }

    public void setQuantity(int change) {
        quantity += change;
    }
}