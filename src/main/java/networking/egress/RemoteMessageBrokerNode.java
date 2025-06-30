package networking.egress;

import io.grpc.*;


public class RemoteMessageBrokerNode extends MessageBrokerNode {

    private static final int RAFT_PORT = 9999;
    private boolean healthy;

    public RemoteMessageBrokerNode(String hostname) {
        super(hostname);
        healthy = true;
        raftPort = RAFT_PORT;
    }

    @Override
    protected void initStub(){
        channel = ManagedChannelBuilder
                .forTarget(hostname)
                .usePlaintext()
                .build();
        //TODO enable retries with exponential backoff (set gRPC channel config)
        //TODO create stub
    }

}
