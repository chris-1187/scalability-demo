package bootstrap;

import networking.egress.MessageBrokerNode;
import networking.egress.MockMessageBrokerNode;
import networking.ingress.GrpcServer;
import io.grpc.inprocess.InProcessServerBuilder;
import partitioning.OwnPartition;
import partitioning.PartitionManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class MockBootstrapper { // TODO

    public static class MockCluster {
        public List<MockMessageBrokerNode> nodes;
        public List<PartitionManager> partitionManagers;
        public List<GrpcServer> servers;
    }

    public static MockCluster setup(int nodesPerPartition) throws IOException {
        List<MockMessageBrokerNode> allNodes = new ArrayList<>();
        Map<Integer, List<MockMessageBrokerNode>> partitionNodes = new HashMap<>();

        for (int i = 0; i < 3; i++) {
            List<MockMessageBrokerNode> nodes = new ArrayList<>();
            for (int j = 0; j < nodesPerPartition; j++) {
                String serverName = InProcessServerBuilder.generateName();
                // FIX: Provide a unique port for each node's Raft instance
                MockMessageBrokerNode node = new MockMessageBrokerNode(serverName, 9000 + (i * nodesPerPartition) + j);
                nodes.add(node);
                allNodes.add(node);
            }
            partitionNodes.put(i, nodes);
        }

        List<PartitionManager> partitionManagers = new ArrayList<>();
        List<GrpcServer> servers = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            List<MockMessageBrokerNode> currentPartitionNodes = partitionNodes.get(i);
            for (int j = 0; j < nodesPerPartition; j++) {
                MockMessageBrokerNode self = currentPartitionNodes.get(j);
                // FIX: Create a list of the correct supertype, containing all nodes in the partition
                List<MessageBrokerNode> allNodesInPartition = new ArrayList<>(currentPartitionNodes);

                PartitionManager partitionManager = new PartitionManager();
                // Pass the list with all nodes (including self) to the partition
                OwnPartition partition = new OwnPartition(i, self, allNodesInPartition);
                partitionManager.registerPartition(i, partition);
                partitionManagers.add(partitionManager);

                GrpcServer server = new GrpcServer(InProcessServerBuilder.forName(self.getHostname()).directExecutor(), partitionManager, j);
                servers.add(server);
                server.start();
            }
        }

        MockCluster cluster = new MockCluster();
        cluster.nodes = allNodes;
        cluster.partitionManagers = partitionManagers;
        cluster.servers = servers;
        return cluster;
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        setup(3);
        new CountDownLatch(1).await();
    }
}