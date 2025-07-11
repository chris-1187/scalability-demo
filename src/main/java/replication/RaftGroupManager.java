package replication;

import com.alipay.sofa.jraft.*;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.option.NodeOptions;
import misc.Constants;
import networking.egress.MockMessageBrokerNode;
import networking.egress.MessageBrokerNode;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RaftGroupManager {


    private static String parseNode(MessageBrokerNode node){
        return node.getHostname() + ":" + node.getRaftPort();
    }

    public static void resetDirectory(String dirPath)  {
        Path path = Paths.get(dirPath);

        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }

            Files.createDirectories(path);
        } catch (Exception e){
            System.err.println("Error resetting directory " + dirPath + ": " + e.getMessage());
        }

    }

    public static Node setup(MessageBrokerNode self, List<MessageBrokerNode> peers) {
        NodeOptions nodeOptions = new NodeOptions();
        nodeOptions.setElectionTimeoutMs(Constants.ELECTION_TIMEOUT_MS);
        nodeOptions.setDisableCli(false);
        nodeOptions.setSnapshotIntervalSecs(Constants.SNAPSHOT_INTERVAL_SEC);

        String baseStoragePath = System.getenv("RAFT_STORAGE_BASE_PATH");
        if (baseStoragePath == null || baseStoragePath.isEmpty()) {
            baseStoragePath = "/var/data/dist-msg-queue";
            System.out.println("RAFT_STORAGE_BASE_PATH environment variable not set, defaulting to: " + baseStoragePath);
        }

        String snapshotPath = baseStoragePath + "/snaps/" + self.getRaftPort();
        resetDirectory(snapshotPath);
        nodeOptions.setSnapshotUri(snapshotPath);

        String logPath = baseStoragePath + "/logs/" + self.getRaftPort();
        resetDirectory(logPath);
        nodeOptions.setLogUri(logPath);

        String metaPath = baseStoragePath + "/meta/" + self.getRaftPort();
        resetDirectory(metaPath);
        nodeOptions.setRaftMetaUri(metaPath);

        StateMachine sm = new MessageBrokerStateMachine();
        nodeOptions.setFsm(sm);

        Configuration configuration = new Configuration();
        configuration.addPeer(PeerId.parsePeer(parseNode(self)));
        for(MessageBrokerNode node : peers){
            if(node == self)
                continue;
            PeerId peerId = PeerId.parsePeer(parseNode(node));
            configuration.addPeer(peerId);
            node.setRaftPeerID(peerId);
        }

        nodeOptions.setInitialConf(configuration);

        PeerId selfId = PeerId.parsePeer(parseNode(self));
        self.setRaftPeerID(selfId);

        RaftGroupService raftGroupService = new RaftGroupService("partition", selfId, nodeOptions);
        return raftGroupService.start();
    }


    public static void main(String[] args) throws InterruptedException {
        MessageBrokerNode[] nodes = new MessageBrokerNode[]{new MockMessageBrokerNode("localhost", 9991), new MockMessageBrokerNode("localhost", 9992), new MockMessageBrokerNode("localhost", 9993)};

        Node n1 = RaftGroupManager.setup(nodes[0], List.of(nodes));
        Node n2 = RaftGroupManager.setup(nodes[1], List.of(nodes));
        Node n3 = RaftGroupManager.setup(nodes[2], List.of(nodes));

        Node leader = null;
        while (leader == null) {
            if (n1.isLeader()) leader = n1;
            else if (n2.isLeader()) leader = n2;
            else if (n3.isLeader()) leader = n3;
            Thread.sleep(500);
        }

        System.out.println("Leader is: " + leader.getLeaderId());

        while (leader.isLeader()){
            PushEntry insert = new PushEntry( "queue1", "Hello World!", UUID.randomUUID());
            CompletableFuture<Status> futureStatus = insert.submit(leader);
            try {
                Status status = futureStatus.get();
                if (status.isOk()) {
                    System.out.println("status ok");
                } else {
                    System.out.println("status not ok");
                }
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }

            Thread.sleep(500);

        }

    }
}
