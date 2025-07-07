package demo;

import com.google.common.util.concurrent.ListenableFuture;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.qservice.External;
import org.example.qservice.QServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class Producer implements ProducerI, Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    String host;
    int port;
    int messageFrequency;
    String queue_name;
    QServiceGrpc.QServiceFutureStub futureStub;


    public Producer(String host, int port, int messagesSecond, String queue_name) {
        this.host = host;
        this.port = port;
        this.messageFrequency = messagesSecond;
        this.queue_name = queue_name;
    }

    @Override
    public void run() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
        futureStub = QServiceGrpc.newFutureStub(channel);

        int i = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                pushMessage(Integer.toString(i++));
                Thread.sleep(1000 / messageFrequency);
            } catch (ExecutionException e) {
                logger.error(e.getCause().getMessage(), e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean pushMessage(String message) throws ExecutionException {
        String uuid = java.util.UUID.randomUUID().toString();
        External.PushRequest pushRequest = External.PushRequest.newBuilder()
                .setMessageContent(message)
                .setMessageId(uuid)
                .build();

        External.PushResponse pushResponse;
        boolean success = false;
        while (!success && !Thread.currentThread().isInterrupted()) {
            ListenableFuture<External.PushResponse> push = futureStub.push(pushRequest);
            try {
                pushResponse = push.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            success = pushResponse.getSuccess();
        }
        return true;
    }


}
