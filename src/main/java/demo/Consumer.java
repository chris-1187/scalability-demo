package demo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.qservice.External;
import org.example.qservice.QServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Consumer implements ConsumerI, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Consumer.class);
    String host;
    int port;
    QServiceGrpc.QServiceBlockingStub blockingStub;
    String queue_name;
    private final External.PeekRequest peekRequest;

    public Consumer(String host, int port, String queue_name) {
        this.host = host;
        this.port = port;
        this.queue_name = queue_name;
        this.peekRequest = External.PeekRequest.newBuilder().setQueueName(queue_name).build();
    }

    @Override
    public void run() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        blockingStub = QServiceGrpc.newBlockingStub(channel);

        while (!Thread.currentThread().isInterrupted()) {
            External.Message message = peek();
            try {
                logger.info("Thread: {} - recevied message: {}", Thread.currentThread().getName(), message);
                long sleep = Long.parseLong(message.getPayload());
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (NumberFormatException ignore) {
            }

            boolean popSuccess = false;
            while (!popSuccess && !Thread.currentThread().isInterrupted()) {
                popSuccess = pop(message.getMessageId());
                logger.info("Thread: {} - pop response {}", Thread.currentThread().getName(), popSuccess);
            }
        }
    }

    @Override
    public External.Message peek() {
        return blockingStub.peek(peekRequest).getMessage();
    }

    @Override
    public boolean pop(String messageId) {
        External.PopRequest popRequest = External.PopRequest.newBuilder()
                .setQueueName(queue_name)
                .setMessageId(messageId)
                .build();
        return blockingStub.pop(popRequest).getSuccess();
    }
}
