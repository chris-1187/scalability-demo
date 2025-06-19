package partitioning;

import networking.egress.Node;

import java.util.List;

public class ForeignPartition extends Partition {

    private final List<Node> nodes;

    public ForeignPartition(int ringPosition, List<Node> nodes){
        this.ringPosition = ringPosition;
        this.nodes = nodes;
    }

}
