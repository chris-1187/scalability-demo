package partitioning;

import com.alipay.sofa.jraft.Status;
import queue.Message;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class Partition {

    protected int ringPosition;

    public int getRingPosition(){
        return ringPosition;
    }

    public abstract Optional<Message> peek(String queueName, Optional<String> clientToken);

    /**
     * Asynchronously pushes a message to a queue within the partition.
     * In an OwnPartition, this will be applied to the Raft group.
     * In a ForeignPartition, this will be forwarded to a node in that partition.
     *
     * @param queueName The name of the queue.
     * @param messageContent The content of the message.
     * @param messageId The unique ID of the message.
     * @return A CompletableFuture that completes with the Status of the operation.
     */
    public abstract CompletableFuture<Status> push(String queueName, String messageContent, UUID messageId);

    /**
     * Asynchronously pops (commits) a message from a queue within the partition.
     * In an OwnPartition, this will be applied to the Raft group.
     * In a ForeignPartition, this will be forwarded to a node in that partition.
     *
     * @param queueName The name of the queue.
     * @param messageId The unique ID of the message to commit.
     * @return A CompletableFuture that completes with the Status of the operation.
     */
    public abstract CompletableFuture<Status> pop(String queueName, UUID messageId);
}