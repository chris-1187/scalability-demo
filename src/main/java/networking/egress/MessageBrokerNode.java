package networking.egress;


import com.alipay.sofa.jraft.entity.PeerId;
import com.google.protobuf.GeneratedMessageV3;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import misc.Constants;
import org.example.qservice.External;
import org.example.qservice.QServiceGrpc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class MessageBrokerNode {

    protected abstract void initStub();

    protected volatile boolean healthy = true;
    protected volatile Instant ignoreTimeout = Instant.MIN;
    protected String hostname;
    protected int raftPort;

    //TODO Hostname port stuff
    protected ManagedChannel channel;
    protected QServiceGrpc.QServiceBlockingStub qServiceStub;

    protected PeerId raftPeerID;


    public MessageBrokerNode(String hostname){
        this.hostname = hostname;
        initStub();
    }

    public QServiceGrpc.QServiceBlockingStub getqServiceStub() {
        return qServiceStub.withDeadlineAfter(Constants.TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
    }

    //usage: sendWithRetry<RequestProtobufType, ResponseProtobufType>(request, qServiceStub::pop)
    public <RequestT extends GeneratedMessageV3, ResponseT extends GeneratedMessageV3>
    Optional<ResponseT> sendWithRetry(RequestT requestMessage, Function<RequestT, ResponseT> stub) {
        System.out.println("called other node");
        if(!healthy && ignoreTimeout.compareTo(Instant.now()) > 0){ //should usually not happen if one calls isHealthy() before
            return Optional.empty();
        }
        int i = -1;
        try {
            while (i < Constants.RETRIES){
                i++;
                try {
                    ResponseT response = stub.apply(requestMessage);
                    healthy = true;
                    return Optional.of(response);
                } catch (StatusRuntimeException e){
                    long waitTime = (long) (Constants.INITIAL_BACKOFF_MILLIS * Math.pow(Constants.BACKOFF_MULTIPLIER, i)
                            + ThreadLocalRandom.current().nextLong(-Constants.JITTER_MAX_MILLIS, Constants.JITTER_MAX_MILLIS));
                    e.printStackTrace();
                    Thread.sleep(waitTime);
                }
            }
        } catch (InterruptedException e){
            return Optional.empty();
        }
        healthy = false;
        ignoreTimeout = Instant.now().plus(Constants.UNHEALTHY_NODE_IGNORE_TIMEOUT_MILLIS);
        return Optional.empty();
    }

    public Optional<External.PushResponse> push(String queueName, String messageContent, UUID messageId){
        if(!isHealthy())
            return Optional.empty();
        External.Message message = External.Message.newBuilder()
                .setMessageId(messageId.toString())
                .setPayload(messageContent)
                .build();
        External.PushRequest request = External.PushRequest.newBuilder()
                .setQueueName(queueName)
                .setMessage(message)
                .build();
        return sendWithRetry(request, getqServiceStub()::push);
    }

    public Optional<External.PopResponse> pop(String queueName, UUID messageId){
        if(!isHealthy())
            return Optional.empty();
        External.PopRequest request = External.PopRequest.newBuilder()
                .setQueueName(queueName)
                .setMessageId(messageId.toString())
                .build();
        return sendWithRetry(request, getqServiceStub()::pop);
    }

    public Optional<External.PeekResponse> peek(String queueName, Optional<String> clientToken){
        if(!isHealthy())
            return Optional.empty();
        External.PeekRequest request = External.PeekRequest.newBuilder()
                .setQueueName(queueName)
                .setClientToken(clientToken.orElse(""))
                .build();
        return sendWithRetry(request, getqServiceStub()::peek);
    }

    public String getHostname() {
        return hostname;
    }

    public int getRaftPort() {
        return raftPort;
    }

    public PeerId getRaftPeerID() {
        return raftPeerID;
    }

    public void setRaftPeerID(PeerId peerID){
        raftPeerID = peerID;
    }

    public void cleanup() {
        channel.shutdown();
    }

    public abstract boolean isHealthy();

}
