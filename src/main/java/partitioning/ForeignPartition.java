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

            //Iterate to find the first responsive node
            for (MessageBrokerNode node : nodes) {
                if(!node.isHealthy())
                    continue;
                try {
                    RemoteMessageBrokerNode remoteNode = (RemoteMessageBrokerNode) node;
                    Optional<PushResponse> response = remoteNode.sendWithRetry(request, remoteNode.getqServiceStub()::push);
                    if (response.isPresent()) {
                        return response.get().getSuccess() ? Status.OK() : new Status(-1, "Pop request failed server side");
                    } else {
                        System.err.println("Request to " + node.getHostname() + " failed, trying next node.");
                    }
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

            //Iterate to find the first responsive node
            for (MessageBrokerNode node : nodes) {
                if(!node.isHealthy())
                    continue;
                try {
                    RemoteMessageBrokerNode remoteNode = (RemoteMessageBrokerNode) node;
                    Optional<PopResponse> response = remoteNode.sendWithRetry(request, remoteNode.getqServiceStub()::pop);
                    if (response.isPresent()) {
                        return response.get().getSuccess() ? Status.OK() : new Status(-1, "Pop request failed server side");
                    } else {
                        System.err.println("Request to " + node.getHostname() + " failed, trying next node.");
                    }
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

        //Iterate to find the first responsive node
        for (MessageBrokerNode node : nodes) {
            if(!node.isHealthy())
                continue;
            try {
                RemoteMessageBrokerNode remoteNode = (RemoteMessageBrokerNode) node;
                Optional<PeekResponse> optionalResponse = remoteNode.sendWithRetry(request, remoteNode.getqServiceStub()::peek);

                if(optionalResponse.isPresent()){
                    // Reconstruct the Message object from the gRPC response.
                    PeekResponse response = optionalResponse.get();
                    if (response.hasMessage()) {
                        return Optional.of(new Message(
                                UUID.fromString(response.getMessage().getMessageId()),
                                response.getMessage().getPayload(),
                                response.getMessage().getLogIndex()
                        ));
                    } else if(response.getFound()) {
                        return Optional.empty();
                    }
                } else {
                    System.err.println("Request to " + node.getHostname() + " failed, trying next node.");
                }
            } catch(Exception e) {
                System.err.println("An unexpected error occurred while forwarding to " + node.getHostname() + ": " + e.getMessage());
            }
        }

        return Optional.empty();
    }
}
