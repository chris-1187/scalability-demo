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
import java.util.concurrent.locks.ReentrantLock;

public class OwnPartition extends Partition {

    private final List<MessageBrokerNode> peers;
    private final Node raftNode;
    private final MessageBrokerStateMachine stateMachine;
    private ReentrantLock leaseLock = new ReentrantLock();
    private volatile Instant leaseTimeout = Instant.MIN;

    public OwnPartition(int ringPosition, MessageBrokerNode self, List<MessageBrokerNode> peers){
        this.peers = peers;
        this.ringPosition = ringPosition;
        raftNode = RaftGroupManager.setup(self, peers);
        stateMachine = (MessageBrokerStateMachine) raftNode.getOptions().getFsm();
        (new LeaseRenewalThread()).start();
    }

    public CompletableFuture<Status> push(String queueName, String messageContent, UUID messageId){
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
        return CompletableFuture.completedFuture(new Status(-1, "Not a leader"));
    }

    public CompletableFuture<Status> pop(String queueName, UUID messageId){
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
        return CompletableFuture.completedFuture(new Status(-1, "Not a leader"));
    }

    @Override
    public Optional<queue.Message> peek(String queueName, Optional<String> clientToken) {
        // We don't need to check leadership for reads, as we use readIndex in the lease mechanism
        // The lease mechanism ensures we don't serve stale reads
        leaseLock.lock();
        try {
            if (Instant.now().compareTo(leaseTimeout) < 0) {
                return stateMachine.peekWithTimeout(queueName, clientToken);
            }
            // We don't have a valid lease, return empty to avoid serving potentially stale data
            return Optional.empty();
        } finally {
            leaseLock.unlock();
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
                    Instant leaseBegin = Instant.now();
                    raftNode.readIndex("ctx".getBytes(StandardCharsets.UTF_8), new ReadIndexClosure() {
                        @Override
                        public void run(Status status, long index, byte[] reqCtx) {
                            if (status.isOk()) {
                                leaseLock.lock();
                                Instant leaseEnd = leaseBegin.plus(Constants.LEASE_TIMEOUT_MS);
                                if(leaseEnd.compareTo(leaseTimeout) > 0)
                                    leaseTimeout = leaseEnd;
                                leaseLock.unlock();
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
