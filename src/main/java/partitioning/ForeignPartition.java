package partitioning;

import networking.egress.MessageBrokerNode;

import java.util.List;

public class ForeignPartition extends Partition {

    private final List<MessageBrokerNode> nodes;

    public ForeignPartition(int ringPosition, List<MessageBrokerNode> nodes){
        this.ringPosition = ringPosition;
        this.nodes = nodes;
    }

}
