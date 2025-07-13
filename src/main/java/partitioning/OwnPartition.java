package partitioning;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.closure.ReadIndexClosure;
import com.alipay.sofa.jraft.entity.PeerId;
import misc.Constants;
import networking.egress.MessageBrokerNode;
import org.example.qservice.External;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.Message;
import replication.MessageBrokerStateMachine;
import replication.PopEntry;
import replication.PushEntry;
import replication.RaftGroupManager;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

public class OwnPartition extends Partition {

    private static final Logger logger = LoggerFactory.getLogger(OwnPartition.class);
    private final List<MessageBrokerNode> peers;
    private final Node raftNode;
    private final MessageBrokerStateMachine stateMachine;
    private ReentrantLock leaseLock = new ReentrantLock();
    private volatile Instant leaseTimeout = Instant.MIN;

    public OwnPartition(int ringPosition, MessageBrokerNode self, List<MessageBrokerNode> peers, int partitionID){
        this.peers = peers;
        this.ringPosition = ringPosition;
        raftNode = RaftGroupManager.setup(self, peers, partitionID);
        stateMachine = (MessageBrokerStateMachine) raftNode.getOptions().getFsm();
        (new LeaseRenewalThread()).start();
    }

    public Status push(String queueName, String messageContent, UUID messageId){
        if (queueName == null || queueName.isEmpty()){
            logger.warn("""
                    Error while inserting message: ID:"{}", Payload:"{}"
                    Expected target queue to be a non empty string but was:"{}". Message dropped!
                    """, messageId, queueName, queueName);
            return new Status(-1, "Appending entry failed");
        }
        if(raftNode.isLeader()){ //!! this information may be stale, and entries may get rejected (in very rare cases)
            if(!stateMachine.hasSpace(queueName))
                return new Status(-1, "Queue length limit reached. Try again later");
            PushEntry entry = new PushEntry(queueName, messageContent, messageId);
            CompletableFuture<Status> futureStatus = entry.submit(raftNode);
            try {
                return futureStatus.get();
            } catch (InterruptedException | ExecutionException e) {
                return new Status(-1, "Appending push entry failed");
            }
        } else {
            Optional<MessageBrokerNode> leaderNode = getLeaderNode();
            if(leaderNode.isPresent()){
                Optional<External.PushResponse> response = leaderNode.get().push(queueName, messageContent, messageId);
                if (response.isPresent()) {
                    return response.get().getSuccess() ? Status.OK() : new Status(-1, "Push request failed server side");
                } else {
                    System.err.println("Request to current leader failed");
                }
            }
        }
        return new Status(-1, "Leader not found");
    }

    public Status pop(String queueName, UUID messageId){
        if(raftNode.isLeader()){ //!! this information may be stale, and entries may get rejected (in very rare cases)
            PopEntry entry = new PopEntry(queueName, messageId);
            CompletableFuture<Status> futureStatus = entry.submit(raftNode);
            try {
                return futureStatus.get();
            } catch (InterruptedException | ExecutionException e) {
                return new Status(-1, "Appending pop entry failed");
            }
        } else {
            Optional<MessageBrokerNode> leaderNode = getLeaderNode();
            if(leaderNode.isPresent()){
                Optional<External.PopResponse> response = leaderNode.get().pop(queueName, messageId);
                if (response.isPresent()) {
                    return response.get().getSuccess() ? Status.OK() : new Status(-1, "Pop request failed server side");
                } else {
                    System.err.println("Request to current leader failed");
                }
            }
        }
        return new Status(-1, "Leader not found");
    }

    @Override
    public External.PeekResponse peek(String queueName, Optional<String> clientToken) {
        // Instead of checking for leadership for every peek, we use a lease provided through read index, which confirms leadership of the current node
        leaseLock.lock();
        try {
            if(raftNode.isLeader() && Instant.now().compareTo(leaseTimeout) < 0){
                Optional<Message> message = stateMachine.peekWithTimeout(queueName, clientToken);
                if(message.isPresent()){
                    External.Message messageResponse = External.Message.newBuilder().
                            setMessageId(message.get().getMessageId().toString()).
                            setPayload(message.get().getPayload()).
                            build();
                    return External.PeekResponse.newBuilder().setFound(true).setMessage(messageResponse).build();
                }
            // Else, we don't have a valid lease, return empty to avoid serving potentially stale data
            } else {
                Optional<MessageBrokerNode> leaderNode = getLeaderNode();
                if(leaderNode.isPresent()){
                    Optional<External.PeekResponse> optionalResponse = leaderNode.get().peek(queueName, clientToken);
                    if(optionalResponse.isPresent())
                        return optionalResponse.get();
                }
            }

        } finally {
            leaseLock.unlock();
        }
        return External.PeekResponse.newBuilder().setFound(false).build();
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
