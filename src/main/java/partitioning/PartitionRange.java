package partitioning;

public class PartitionRange {
    private final int partitionId;
    private final long startHash;
    private final long endHash;

    private final int numPartitions;

    public PartitionRange(int partitionId, long startHash, long endHash, int numPartitions) {
        this.partitionId = partitionId;
        this.startHash = startHash;
        this.endHash = endHash;
        this.numPartitions = numPartitions;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public long getStartHash() {
        return startHash;
    }

    public long getEndHash() {
        return endHash;
    }

    public boolean contains(int hashValue) {
        if (partitionId == numPartitions - 1) {
            return hashValue >= startHash && hashValue <= endHash;
        }
        return hashValue >= startHash && hashValue < endHash; // Exclusive end for intermediate ranges
    }

    @Override
    public String toString() {
        return "Partition " + partitionId + ": [" + startHash + ", " + endHash + (partitionId == numPartitions - 1 ? "]" : ")");
    }
}