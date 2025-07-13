package partitioning;

import com.google.common.hash.Hashing;
import java.util.ArrayList;
import java.util.List;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PartitionManager {

    // Static members for global partition configuration
    private final int numPartitions;
    private static final long MIN_INT_VALUE = (long) Integer.MIN_VALUE;
    private static final long MAX_INT_VALUE = (long) Integer.MAX_VALUE;
    private static final long TOTAL_INT_RANGE_SIZE = (MAX_INT_VALUE - MIN_INT_VALUE) + 1; // 2^32

    // Stores the calculated hash ranges for all partitions.
    private static List<PartitionRange> partitionHashRanges;

    private final ConcurrentHashMap<Integer, Partition> partitionsById = new ConcurrentHashMap<>(); // partitioningRing

    // Static method to initialize the hash ranges
    private void initializePartitionHashRanges() {
        if (partitionHashRanges != null) {
            return; // Already initialized
        }

        partitionHashRanges = new ArrayList<>(numPartitions);
        long rangePerPartition = TOTAL_INT_RANGE_SIZE / numPartitions;
        long remainder = TOTAL_INT_RANGE_SIZE % numPartitions;

        long currentStart = MIN_INT_VALUE;

        for (int pId = 0; pId < numPartitions; pId++) {
            long currentEnd = currentStart + rangePerPartition - 1;
            if (pId < remainder) { // Distribute remainder evenly among the first partitions
                currentEnd++;
            }

            if (pId == numPartitions - 1) {
                currentEnd = MAX_INT_VALUE; // Ensure the last partition covers up to MAX_VALUE
            }

            partitionHashRanges.add(new PartitionRange(pId, currentStart, currentEnd, numPartitions));
            currentStart = currentEnd + 1;
        }

        System.out.println("Initialized Partition Hash Ranges:");
        for (PartitionRange range : partitionHashRanges) {
            System.out.println(range);
        }
        // Make the list immutable after initialization
        partitionHashRanges = Collections.unmodifiableList(partitionHashRanges);
    }

    public PartitionManager(int numPartitions) {
        this.numPartitions = numPartitions;
        initializePartitionHashRanges();
    }

    public int getTotalPartitions() {
        return numPartitions;
    }

    /**
     * Looks up the Partition responsible for a given queue name based on its hash.
     */
    public Partition lookupPartition(String queueName) {
        int hash = hashQueueName(queueName);
        for (PartitionRange range : partitionHashRanges) {
            if (range.contains(hash)) {
                // Return the concrete Partition object (OwnPartition or ForeignPartition)
                // that was registered for this logical partition ID
                Partition partition = partitionsById.get(range.getPartitionId());
                if (partition == null) {
                    throw new IllegalStateException("Partition with ID " + range.getPartitionId() + " not registered.");
                }
                return partition;
            }
        }
        // This should logically not be reached if partitionHashRanges cover the full int range
        throw new IllegalArgumentException("Hash value " + hash + " for queueName '" + queueName + "' does not fall into any defined partition range.");
    }

    /**
     * Registers a concrete Partition instance (OwnPartition or ForeignPartition)
     * with its logical partition ID.
     */
    public void registerPartition(int partitionId, Partition partition){
        partitionsById.put(partitionId, partition);
        System.out.println("Registered Partition ID " + partitionId + " with instance: " + partition.getClass().getSimpleName());
    }

    private int hashQueueName(String queueName) {
        return Hashing.murmur3_32_fixed().hashUnencodedChars(queueName).asInt();
    }
    // Opt
    public Partition getRegisteredPartition(int partitionId) {
        return partitionsById.get(partitionId);
    }
}
