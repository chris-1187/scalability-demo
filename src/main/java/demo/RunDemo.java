package demo;

public class RunDemo {

    public static void main(String[] args) {
        Producer producer = new Producer("127.0.0.1", 50179, 1, "42");

        new Thread(producer).start();

        // new Consumer("192.168.59.100", 32343)
    }
}
