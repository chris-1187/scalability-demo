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
        qServiceStub = QServiceGrpc.newBlockingStub(channel);//.withDeadline(Deadline.after(Constants.timeoutMillis, TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean isHealthy() {
        return true || healthy || ignoreTimeout.compareTo(Instant.now()) < 0;
    }



}

