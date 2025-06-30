package partitioning;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.closure.ReadIndexClosure;
import com.alipay.sofa.jraft.entity.PeerId;
import com.alipay.sofa.jraft.util.Endpoint;
import misc.Constants;
import networking.egress.MessageBrokerNode;
import queue.Message;
import replication.MessageBrokerStateMachine;
import replication.PopEntry;
import replication.PushEntry;
import replication.RaftGroupManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class OwnPartition extends Partition {

    private final List<MessageBrokerNode> peers;
    private final Node raftNode;
    private final MessageBrokerStateMachine stateMachine;
    private volatile Instant leaseTimeout = Instant.MIN;

    public OwnPartition(int ringPosition, MessageBrokerNode self, List<MessageBrokerNode> peers){
        this.peers = peers;
        this.ringPosition = ringPosition;
        raftNode = RaftGroupManager.setup(self, peers);
        stateMachine = (MessageBrokerStateMachine) raftNode.getOptions().getFsm();
        (new LeaseRenewalThread()).start();
    }

    public void push(){
        if(raftNode.isLeader()){ //!! this information may be stale, and entries may get rejected (in very rare cases)
            PushEntry entry = new PushEntry("queue1", "Hello World!", UUID.randomUUID());
            CompletableFuture<Status> futureStatus = entry.submit(raftNode);
            //TODO await future and return status for client response
        } else {
            Optional<MessageBrokerNode> leaderNode = getLeaderNode();
            if(leaderNode.isPresent()){
                //TODO forward call to leaderNode
            }
        }
    }

    public void pop(){
        if(raftNode.isLeader()){ //!! this information may be stale, and entries may get rejected (in very rare cases)
            PopEntry entry = new PopEntry("queue1", UUID.randomUUID());
            CompletableFuture<Status> futureStatus = entry.submit(raftNode);
            //TODO await future and return status for client response
        } else {
            Optional<MessageBrokerNode> leaderNode = getLeaderNode();
            if(leaderNode.isPresent()){
                //TODO forward call to leaderNode
            }
        }
    }

    public void peek(){
        if(leaseTimeout.compareTo(Instant.now()) > 0){
            //peekWithTimeout will prevent early redeliveries of messages that were already delivered to another client
            //can submit a client token for fault tolerance, so that the same client can read the message again if neccessary
            Optional<Message> message = stateMachine.peekWithTimeout("queue1", Optional.of("some_client_ID_or_token"));
            //TODO client response
        } else {
            Optional<MessageBrokerNode> leaderNode = getLeaderNode();
            if(leaderNode.isPresent()){
                //TODO forward call to leaderNode
            }
        }
    }

    private Optional<MessageBrokerNode> getLeaderNode(){
        PeerId leader = raftNode.getLeaderId(); //This info can also be stale in theory
        if(leader != null) {
            for (MessageBrokerNode peer : peers) {
                if (leader.equals(peer.getRaftPeerID())) {
                    return Optional.of(peer);
                }
            }
        }
        return Optional.empty();
    }

    //periodically acquires short running leadership leases (far shorter than the election timeout and therefore almost always safe)
    private class LeaseRenewalThread extends Thread{
        @Override
        public void run() {
            try {
                while (true){
                    Thread.sleep(Constants.LEASE_REFRESH_MS);
                    if(!raftNode.isLeader())
                        continue;
                    raftNode.readIndex("ctx".getBytes(StandardCharsets.UTF_8), new ReadIndexClosure() {
                        @Override
                        public void run(Status status, long index, byte[] reqCtx) {
                            if (status.isOk()) {
                                leaseTimeout = Instant.now().plus(Constants.LEASE_TIMEOUT_MS);
                            }

                        }
                    });
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
