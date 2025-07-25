package bootstrap;

import networking.egress.MessageBrokerNode;
import networking.egress.RemoteMessageBrokerNode;
import networking.ingress.GrpcServer;
import org.example.qservice.External;
import partitioning.ForeignPartition;
import partitioning.OwnPartition;
import partitioning.PartitionManager;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;

public class RemoteBootstrapper {

    public static void setup() throws IOException, InterruptedException {
        // Environment and Identity Setup
        String podName = System.getenv("POD_NAME");
        String peerServiceName = System.getenv("PEER_SERVICE_NAME");
        int numNodes = Integer.parseInt(System.getenv("NODE_COUNT"));
        int nodesPerPartition = Integer.parseInt(System.getenv("REPLICATION_FACTOR"));
        int partitionsPerNode = Integer.parseInt(System.getenv("PARTITIONS_PER_NODE"));

        if (numNodes % nodesPerPartition != 0) {
            throw new IllegalStateException("Invalid configuration. Cant evenly distribute partitions onto nodes");
        }

        if (podName == null || peerServiceName == null) {
            throw new IllegalStateException("Missing required environment variables POD_NAME or PEER_SERVICE_NAME");
        }

        int lastDash = podName.lastIndexOf('-');
        if (lastDash == -1) {
            throw new IllegalStateException("Invalid POD_NAME format. Expected format: statefulset-name-ordinal");
        }
        String statefulSetName = podName.substring(0, lastDash);
        int podOrdinal = Integer.parseInt(podName.substring(lastDash + 1));


        int numPartitions = (numNodes / nodesPerPartition)*partitionsPerNode;
        int ownStartPartition = (podOrdinal / nodesPerPartition)*partitionsPerNode;
        Set<Integer> ownPartitionIDs = new HashSet<>();
        for (int i = 0; i < partitionsPerNode; i++) {
            ownPartitionIDs.add(ownStartPartition+i);
        }


        PartitionManager partitionManager = new PartitionManager(numPartitions);

        // Discover and Register All Partitions
        for (int pId = 0; pId < numPartitions; pId++) {
            int firstInGroup = (pId / partitionsPerNode)*nodesPerPartition;
            if (ownPartitionIDs.contains(pId)) {
                // Setup for this node's OWN partition
                List<MessageBrokerNode> allNodesInPartition = new ArrayList<>();
                // Find 'self' node from the list of all nodes in the partition
                String ownHost = podName + "." + peerServiceName + ".default.svc.cluster.local";
                MessageBrokerNode self = new RemoteMessageBrokerNode(ownHost);
                for (int i = 0; i < nodesPerPartition; i++) {
                    int peerOrdinal = firstInGroup + i;
                    String peerHost = statefulSetName + "-" + peerOrdinal + "." + peerServiceName + ".default.svc.cluster.local";
                    if (!peerHost.equals(ownHost)) {
                        allNodesInPartition.add(new RemoteMessageBrokerNode(peerHost));
                    }
                }

                // OwnPartition constructor initializes the Raft group
                OwnPartition partition = new OwnPartition(pId, self, allNodesInPartition, pId);
                partitionManager.registerPartition(pId, partition);

            } else {
                // Setup for FOREIGN partitions
                List<MessageBrokerNode> nodesInForeignPartition = new ArrayList<>();
                for (int i = 0; i < nodesPerPartition; i++) {
                    int peerOrdinal = firstInGroup + i;
                    String peerHost = statefulSetName + "-" + peerOrdinal + "." + peerServiceName + ".default.svc.cluster.local";
                    nodesInForeignPartition.add(new RemoteMessageBrokerNode(peerHost));
                }
                ForeignPartition partition = new ForeignPartition(pId, nodesInForeignPartition);
                partitionManager.registerPartition(pId, partition);
            }
        }

        // Start gRPC Servers
        // Handles client-facing requests
        GrpcServer clientServer = new GrpcServer(8080, partitionManager);
        clientServer.start();

        // Handles internal node-to-node communication (forwarding)
        GrpcServer internalServer = new GrpcServer(8081, partitionManager);
        internalServer.start();

        System.out.println("Node " + podOrdinal + " (Partitions " + ownPartitionIDs.toString() + ") successfully started.");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        setup();
        // Keep the main thread alive
        new CountDownLatch(1).await();
    }
}