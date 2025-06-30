package networking.egress;


import io.grpc.ManagedChannel;

public abstract class MessageBrokerNode {

    protected abstract void initStub();

    protected String hostname;
    protected int raftPort;

    //TODO Hostname port stuff
    protected ManagedChannel channel;


    public MessageBrokerNode(String hostname){
        this.hostname = hostname;
        initStub();
    }

    public String getHostname() {
        return hostname;
    }

    public int getRaftPort() {
        return raftPort;
    }

    public void cleanup() {
        channel.shutdown();
    }

}
