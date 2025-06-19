package partitioning;

import networking.egress.Node;
import queue.MessageQueue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OwnPartition extends Partition {


    //The node that stores this object belongs to the partition, so we only store the peers
    private final List<Node> peers;

    private final Map<String, MessageQueue> queues = new HashMap<>();

    public OwnPartition(int ringPosition, List<Node> peers){
        this.ringPosition = ringPosition;
        this.peers = peers;
    }


    //TODO implement replication strategy


}
