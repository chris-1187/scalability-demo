package partitioning;

import com.alipay.sofa.jraft.Status;
import io.grpc.StatusRuntimeException;
import networking.egress.MessageBrokerNode;
import networking.egress.RemoteMessageBrokerNode;
import org.example.qservice.External.PushRequest;
import org.example.qservice.External.PushResponse;
import org.example.qservice.External.PopRequest;
import org.example.qservice.External.PopResponse;
import org.example.qservice.External.PeekRequest;
import org.example.qservice.External.PeekResponse;
import queue.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class ForeignPartition extends Partition {

    private final List<MessageBrokerNode> nodes;

    public ForeignPartition(int ringPosition, List<MessageBrokerNode> nodes){
        this.ringPosition = ringPosition;
        this.nodes = nodes;
    }

    @Override
    public CompletableFuture<Status> push(String queueName, String messageContent, UUID messageId) {

        return CompletableFuture.supplyAsync(() -> {
            PushRequest request = PushRequest.newBuilder()
                    .setQueueName(queueName)
                    .setMessageContent(messageContent)
                    .setMessageId(messageId.toString())
                    .build();

            // Iterate through the nodes of the foreign partition to find the leader and process the request.
            for (MessageBrokerNode node : nodes) {
                try {
                    RemoteMessageBrokerNode remoteNode = (RemoteMessageBrokerNode) node;

                    PushResponse response = remoteNode.getqServiceStub().push(request);

                    // A success response means we found the leader and the operation was accepted.
                    // A failure response likely means this node was not the leader, so we continue the loop.
                    if (response.getSuccess()) {
                        return Status.OK();
                    }
                } catch (StatusRuntimeException e) {

                    System.err.println("Request to " + node.getHostname() + " failed, trying next node. Reason: " + e.getStatus());
                } catch(Exception e) {
                    System.err.println("An unexpected error occurred while forwarding to " + node.getHostname() + ": " + e.getMessage());
                }
            }

            return new Status(-1, "Failed to forward push request to partition " + ringPosition);
        });
    }

    @Override
    public CompletableFuture<Status> pop(String queueName, UUID messageId) {
        return CompletableFuture.supplyAsync(() -> {
            PopRequest request = PopRequest.newBuilder()
                    .setQueueName(queueName)
                    .setMessageId(messageId.toString())
                    .build();

            for (MessageBrokerNode node : nodes) {
                try {
                    RemoteMessageBrokerNode remoteNode = (RemoteMessageBrokerNode) node;
                    // TODO: Add a getter for the gRPC stub in RemoteMessageBrokerNode
                    // For example: public QServiceGrpc.QServiceBlockingStub getqServiceStub() { return qServiceStub; }
                    PopResponse response = remoteNode.getqServiceStub().pop(request);
                    if (response.getSuccess()) {
                        return Status.OK();
                    }
                } catch (StatusRuntimeException e) {
                    System.err.println("Request to " + node.getHostname() + " failed, trying next node. Reason: " + e.getStatus());
                } catch(Exception e) {
                    System.err.println("An unexpected error occurred while forwarding to " + node.getHostname() + ": " + e.getMessage());
                }
            }
            return new Status(-1, "Failed to forward pop request to partition " + ringPosition);
        });
    }

    @Override
    public Optional<Message> peek(String queueName, Optional<String> clientToken) {

        PeekRequest request = PeekRequest.newBuilder()
                .setQueueName(queueName)
                .setClientToken(clientToken.orElse(""))
                .build();

        for (MessageBrokerNode node : nodes) {
            try {
                RemoteMessageBrokerNode remoteNode = (RemoteMessageBrokerNode) node;
                PeekResponse response = remoteNode.getqServiceStub().peek(request);

                // Reconstruct the Message object from the gRPC response.
                if (response.hasMessage()) {
                    return Optional.of(new Message(
                            UUID.fromString(response.getMessage().getMessageId()),
                            response.getMessage().getPayload(),
                            response.getMessage().getLogIndex()
                    ));
                } else if(response.getFound()) {
                    return Optional.empty();
                }

            } catch (StatusRuntimeException e) {
                System.err.println("Request to " + node.getHostname() + " failed, trying next node. Reason: " + e.getStatus());
            } catch(Exception e) {
                System.err.println("An unexpected error occurred while forwarding to " + node.getHostname() + ": " + e.getMessage());
            }
        }

        return Optional.empty();
    }
}
