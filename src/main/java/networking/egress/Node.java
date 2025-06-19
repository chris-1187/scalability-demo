package networking.egress;


import io.grpc.ManagedChannel;

public abstract class Node {

    protected abstract void initStub();

    protected String hostname;
    protected ManagedChannel channel;


    public Node(String hostname){
        this.hostname = hostname;
        initStub();
    }

    public void cleanup() {
        channel.shutdown();
    }

}
