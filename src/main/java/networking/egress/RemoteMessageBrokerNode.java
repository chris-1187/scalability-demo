package networking.egress;

import io.grpc.*;
import misc.Constants;


public class RemoteMessageBrokerNode extends MessageBrokerNode {

    private boolean healthy;

    public RemoteMessageBrokerNode(String hostname) {
        super(hostname);
        healthy = true;
        raftPort = Constants.RAFT_PORT;
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
