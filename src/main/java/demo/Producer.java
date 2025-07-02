package demo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.qservice.External;
import org.example.qservice.QServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Producer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    String host;
    int port;
    int messageFrequency;
    External.Queue queue;

    public Producer(String host, int port, int messagesSecond, String queue) {
        this.host = host;
        this.port = port;
        this.messageFrequency = messagesSecond;
        this.queue = External.Queue.newBuilder().setName(queue).build();
    }

    @Override
    public void run() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        QServiceGrpc.QServiceBlockingStub syncStub = QServiceGrpc.newBlockingStub(channel);

        External.QueueItem partialQueueItem = External.QueueItem.newBuilder().setQueue(queue).buildPartial();

        int i = 0;
        while (!Thread.currentThread().isInterrupted()) {
            External.QueueItem queueItem = partialQueueItem.toBuilder().setPayload(Integer.toString(i)).build();
            External.EnqueueResponse push = syncStub.push(queueItem);
            logger.info("push result {}", push);

            try {
                Thread.sleep(1000/messageFrequency);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

    }


}
