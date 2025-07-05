package partitioning;

import com.alipay.sofa.jraft.Status;
import org.example.qservice.External;
import queue.Message;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class Partition {

    protected int ringPosition;

    public int getRingPosition(){
        return ringPosition;
    }

    public abstract External.PeekResponse peek(String queueName, Optional<String> clientToken);

    /**
     * Pushes a message to a queue within the partition.
     * In an OwnPartition, this will be applied to the Raft group.
     * In a ForeignPartition, this will be forwarded to a node in that partition.
     *
     * @param queueName The name of the queue.
     * @param messageContent The content of the message.
     * @param messageId The unique ID of the message.
     * @return The status of the completed operation
     */
    public abstract Status push(String queueName, String messageContent, UUID messageId);

    /**
     * Pops (commits) a message from a queue within the partition.
     * In an OwnPartition, this will be applied to the Raft group.
     * In a ForeignPartition, this will be forwarded to a node in that partition.
     *
     * @param queueName The name of the queue.
     * @param messageId The unique ID of the message to commit.
     * @return The status of the completed operation
     */
    public abstract Status pop(String queueName, UUID messageId);
}