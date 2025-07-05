package partitioning;

import com.alipay.sofa.jraft.Status;
import networking.egress.MessageBrokerNode;
import org.example.qservice.External.PushResponse;
import org.example.qservice.External.PopResponse;
import org.example.qservice.External.PeekResponse;

import java.util.*;


public class ForeignPartition extends Partition {

    private final List<MessageBrokerNode> nodes;

    public ForeignPartition(int ringPosition, List<MessageBrokerNode> nodes){
        this.ringPosition = ringPosition;
        this.nodes = nodes;
    }

    @Override
    public Status push(String queueName, String messageContent, UUID messageId) {
        List<MessageBrokerNode> shuffledNodes = new ArrayList<>(nodes);
        Collections.shuffle(shuffledNodes);
        //Iterate to find the first responsive node
        for (MessageBrokerNode node : shuffledNodes) {
            Optional<PushResponse> response = node.push(queueName, messageContent, messageId);
            if (response.isPresent()) {
                return response.get().getSuccess() ? Status.OK() : new Status(-1, "Push request failed server side");
            } else {
                System.err.println("Request to " + node.getHostname() + " failed, trying next node.");
            }
        }
        return new Status(-1, "Failed to forward push request to partition " + ringPosition);

    }

    @Override
    public Status pop(String queueName, UUID messageId) {
        List<MessageBrokerNode> shuffledNodes = new ArrayList<>(nodes);
        Collections.shuffle(shuffledNodes);
        //Iterate to find the first responsive node
        for (MessageBrokerNode node : shuffledNodes) {
            Optional<PopResponse> response = node.pop(queueName, messageId);
            if (response.isPresent()) {
                return response.get().getSuccess() ? Status.OK() : new Status(-1, "Pop request failed server side");
            } else {
                System.err.println("Request to " + node.getHostname() + " failed, trying next node.");
            }
        }
        return new Status(-1, "Failed to forward pop request to partition " + ringPosition);
    }

    @Override
    public PeekResponse peek(String queueName, Optional<String> clientToken) {
        List<MessageBrokerNode> shuffledNodes = new ArrayList<>(nodes);
        Collections.shuffle(shuffledNodes);
        //Iterate to find the first responsive node
        for (MessageBrokerNode node : shuffledNodes) {
            Optional<PeekResponse> optionalResponse = node.peek(queueName, clientToken);

            if(optionalResponse.isPresent()){
                return optionalResponse.get();
            } else {
                System.err.println("Request to " + node.getHostname() + " failed, trying next node.");
            }
        }
        return PeekResponse.newBuilder().setFound(false).build();
    }
}
