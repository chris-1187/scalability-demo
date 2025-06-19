package partitioning;

public abstract class Partition {

    //start position of the partition, when imagining the hash value range as a ring
    protected int ringPosition;

    public int getRingPosition(){
        return ringPosition;
    }

    //TODO provide methods for routing messages

}
