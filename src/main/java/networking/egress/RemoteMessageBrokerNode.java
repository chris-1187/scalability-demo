package networking.egress;

import io.grpc.*;
import misc.Constants;
import org.example.qservice.QServiceGrpc;


public class RemoteMessageBrokerNode extends MessageBrokerNode {

    private boolean healthy;
    private QServiceGrpc.QServiceBlockingStub qServiceStub;


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
        qServiceStub = QServiceGrpc.newBlockingStub(channel);

    }

    public QServiceGrpc.QServiceBlockingStub getqServiceStub() {
        return qServiceStub;
    }

}

