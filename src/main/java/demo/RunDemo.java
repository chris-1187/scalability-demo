package demo;

public class RunDemo {

    public static void main(String[] args) {
        Producer producer = new Producer("192.168.59.100", 32343, 1, "42");

        new Thread(producer).start();

        // new Consumer("192.168.59.100", 32343)
    }
}
