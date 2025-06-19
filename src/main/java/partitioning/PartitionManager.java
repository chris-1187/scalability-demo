package partitioning;

import com.google.common.hash.Hashing;

import java.util.*;

public class PartitionManager {

    private final TreeMap<Integer, Partition> partitionRing = new TreeMap<>();

    public Partition lookupPartition(String queueName) {
        assert !partitionRing.isEmpty();
        int hash = hashQueueName(queueName);
        var entry = partitionRing.floorEntry(hash);
        if(entry != null){
            return entry.getValue();
        } else {
            return partitionRing.get(partitionRing.lastKey());
        }
    }

    public void registerPartition(int ringPos, Partition partition){
        partitionRing.put(ringPos, partition);
    }

    private int hashQueueName(String queueName) {
        return Hashing.murmur3_32_fixed().hashUnencodedChars(queueName).asInt();
    }


}
