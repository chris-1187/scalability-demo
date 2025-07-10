package demo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.qservice.External;
import org.example.qservice.QServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer implements ProducerI, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    String host;
    int port;
    int messageFrequency;
    String queue_name;
    QServiceGrpc.QServiceBlockingStub blockingStub;


    public Producer(String host, int port, int messagesSecond, String queue_name) {
        this.host = host;
        this.port = port;
        this.messageFrequency = messagesSecond;
        this.queue_name = queue_name;
    }

    @Override
    public void run() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        blockingStub = QServiceGrpc.newBlockingStub(channel);

        int i = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                push(Integer.toString(i++));
                Thread.sleep(1000 / messageFrequency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean push(String payload) {
        String uuid = java.util.UUID.randomUUID().toString();
        External.Message message = External.Message.newBuilder()
                .setMessageId(uuid)
                .setPayload(payload)
                .build();
        External.PushRequest pushRequest = External.PushRequest.newBuilder()
                .setMessage(message)
                .build();

        boolean success = false;
        while (!success && !Thread.currentThread().isInterrupted()) {
            logger.info("Thread: {} - pushing message {}", Thread.currentThread().getName(), pushRequest);
            success = blockingStub.push(pushRequest).getSuccess();
        }
        return success;
    }


}
