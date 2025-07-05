package networking.egress;


import com.alipay.sofa.jraft.entity.PeerId;
import io.grpc.ManagedChannel;

public abstract class MessageBrokerNode {

    protected abstract void initStub();

    protected String hostname;
    protected int raftPort;

    //TODO Hostname port stuff
    protected ManagedChannel channel;

    protected PeerId raftPeerID;


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

    public PeerId getRaftPeerID() {
        return raftPeerID;
    }

    public void setRaftPeerID(PeerId peerID){
        raftPeerID = peerID;
    }

    public void cleanup() {
        channel.shutdown();
    }

    public abstract boolean isHealthy();

}
