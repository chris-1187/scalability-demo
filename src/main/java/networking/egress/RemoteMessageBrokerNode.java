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

    private int servicePort = 8081;

    public RemoteMessageBrokerNode(String hostname) {
        super(hostname);
        healthy = true;
        raftPort = Constants.RAFT_PORT;
        initStub();
    }

    public RemoteMessageBrokerNode(String hostname, int forcePort) {
        super(hostname);
        healthy = true;
        raftPort = Constants.RAFT_PORT;
        servicePort = forcePort;
        initStub();
    }

    @Override
    protected void initStub(){
        // Connect to the internal gRPC port 8081
        String target = hostname + ":" + servicePort;
        channel = ManagedChannelBuilder
                .forTarget(target)
                .usePlaintext()
                .build();

        // Create a blocking stub for the QService
        qServiceStub = QServiceGrpc.newBlockingStub(channel);
    }

    @Override
    public boolean isHealthy() {
        return healthy || ignoreTimeout.compareTo(Instant.now()) < 0;
    }



}

