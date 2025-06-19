package networking.egress;

import io.grpc.inprocess.InProcessChannelBuilder;

public class MockNode extends Node {

    public MockNode(String name) {
        super(name);
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
}
