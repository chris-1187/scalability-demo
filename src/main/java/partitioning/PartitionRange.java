package partitioning;

public class PartitionRange {
    private final int partitionId;
    private final long startHash;
    private final long endHash;

    public PartitionRange(int partitionId, long startHash, long endHash) {
        this.partitionId = partitionId;
        this.startHash = startHash;
        this.endHash = endHash;
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
        if (partitionId == PartitionManager.getTotalPartitions() - 1) {
            return hashValue >= startHash && hashValue <= endHash;
        }
        return hashValue >= startHash && hashValue < endHash; // Exclusive end for intermediate ranges
    }

    @Override
    public String toString() {
        return "Partition " + partitionId + ": [" + startHash + ", " + endHash + (partitionId == PartitionManager.getTotalPartitions() - 1 ? "]" : ")");
    }
}