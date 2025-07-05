package networking.egress;

import com.google.protobuf.GeneratedMessageV3;
import io.grpc.*;
import misc.Constants;
import org.example.qservice.QServiceGrpc;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


public class RemoteMessageBrokerNode extends MessageBrokerNode {

    private volatile boolean healthy = true;
    private volatile Instant ignoreTimeout = Instant.MIN;

    private QServiceGrpc.QServiceBlockingStub qServiceStub;


    //usage: sendWithRetry<RequestProtobufType, ResponseProtobufType>(request, qServiceStub::pop)
    public <RequestT extends GeneratedMessageV3, ResponseT extends GeneratedMessageV3>
    Optional<ResponseT> sendWithRetry(RequestT requestMessage, Function<RequestT, ResponseT> stub) {
        if(!healthy && ignoreTimeout.compareTo(Instant.now()) > 0){ //should usually not happen if one calls isHealthy() before
            return Optional.empty();
        }
        int i = -1;
        try {
            while (i < Constants.retries){
                i++;
                try {
                    ResponseT response = stub.apply(requestMessage);
                    healthy = true;
                    return Optional.of(response);
                } catch (StatusRuntimeException e){
                    long waitTime = (long) (Constants.initialBackoffMillis * Math.pow(Constants.backoffMultiplier, i)
                            + ThreadLocalRandom.current().nextLong(-Constants.jitterMaxMillis, Constants.jitterMaxMillis));
                    Thread.sleep(waitTime);
                }
            }
        } catch (InterruptedException e){
            return Optional.empty();
        }
        healthy = false;
        ignoreTimeout = Instant.now().plus(Constants.unhealthyNodeIgnoreTimeoutMillis);
        return Optional.empty();
    }


    public RemoteMessageBrokerNode(String hostname) {
        super(hostname);
        healthy = true;
        raftPort = Constants.RAFT_PORT;
    }

    @Override
    protected void initStub(){
        // Connect to the internal gRPC port 8081
        String target = hostname + ":8081";
        channel = ManagedChannelBuilder
                .forTarget(target)
                .usePlaintext()
                .build();
        //TODO enable retries with exponential backoff (set gRPC channel config)

        // Create a blocking stub for the QService
        qServiceStub = QServiceGrpc.newBlockingStub(channel).withDeadline(Deadline.after(Constants.timeoutMillis, TimeUnit.MILLISECONDS));

    }

    @Override
    public boolean isHealthy() {
        return healthy || ignoreTimeout.compareTo(Instant.now()) < 0;
    }

    public QServiceGrpc.QServiceBlockingStub getqServiceStub() {
        return qServiceStub;
    }

}

