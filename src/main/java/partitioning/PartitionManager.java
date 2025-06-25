package partitioning;

import com.google.common.hash.Hashing;
import java.util.ArrayList;
import java.util.List;

import java.util.*;

public class PartitionManager {

    private final TreeMap<Integer, Partition> partitionRing = new TreeMap<>();
    private final List<String> peerFqdns; // Can be used to initialize JRaft Nodes

    public PartitionManager() {
        int peerPort = 8081; // TODO: Maybe get from GrpcServer ?
        // Read k8s env variables
        String replicaCountStr = System.getenv("REPLICA_COUNT");
        String podName = System.getenv("POD_NAME");
        String serviceName = System.getenv("PEER_SERVICE_NAME");
        String namespace = System.getenv("NAMESPACE");

        if (replicaCountStr == null || podName == null || serviceName == null || namespace == null) {
            throw new IllegalStateException("Missing required environment variables.");
        }

        int replicaCount;
        try {
            replicaCount = Integer.parseInt(replicaCountStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("REPLICA_COUNT must be an integer.", e);
        }

        // Extract pod base name (e.g., "kv-store" from "kv-store-0")
        String basePodName = podName.contains("-") ? podName.substring(0, podName.lastIndexOf('-')) : podName;

        // Generate FQDNs (endpoints) for all peers
        this.peerFqdns = new ArrayList<>();
        for (int i = 0; i < replicaCount; i++) {
            String peerEndpoints = String.format("%s-%d.%s.%s.svc.cluster.local:%d", basePodName, i, serviceName, namespace, peerPort);
            peerFqdns.add(peerEndpoints);
        }
        System.out.println("Peer FQDNs: " + peerFqdns);
    }

    public List<String> getPeerFqdns() {
        return peerFqdns;
    }

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
