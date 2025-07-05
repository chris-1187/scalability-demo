package networking.egress;

import io.grpc.inprocess.InProcessChannelBuilder;

public class MockMessageBrokerNode extends MessageBrokerNode {

    public MockMessageBrokerNode(String name, int raftPort) {
        super(name);
        this.raftPort = raftPort;
    }

    @Override
    protected void initStub() {
        channel = InProcessChannelBuilder
                .forName(hostname)
                .usePlaintext()
                .directExecutor()
                .build();
        //TODO create stub
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}
