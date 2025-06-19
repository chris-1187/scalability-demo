package networking.egress;

import io.grpc.*;


public class RemoteNode extends Node {

    private boolean healthy;

    public RemoteNode(String hostname) {
        super(hostname);
        healthy = true;
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
