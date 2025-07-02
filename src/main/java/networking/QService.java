package networking;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.example.qservice.External.PopRequest;
import org.example.qservice.External.PopResponse;
import org.example.qservice.External.PeekRequest;
import org.example.qservice.External.PeekResponse;
import org.example.qservice.External.PushRequest;
import org.example.qservice.External.PushResponse;
import org.example.qservice.QServiceGrpc;
import partitioning.Partition;
import partitioning.PartitionManager;
import queue.Message;

import java.util.Optional;
import java.util.UUID;

public class QService extends QServiceGrpc.QServiceImplBase {

    private final PartitionManager partitionManager;

    public QService(PartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    @Override
    public void push(PushRequest request, StreamObserver<PushResponse> responseObserver) {
        try {
            String queueName = request.getQueueName();
            Partition partition = partitionManager.lookupPartition(queueName);
            UUID messageId = UUID.fromString(request.getMessageId());

            // Delegate the call to the appropriate partition object.
            // It will handle applying to Raft (OwnPartition) or forwarding (ForeignPartition).
            partition.push(request.getQueueName(), request.getMessageContent(), messageId)
                    .whenComplete((status, throwable) -> {
                        if (throwable != null) {
                            responseObserver.onError(Status.INTERNAL
                                    .withDescription("An unexpected error occurred: " + throwable.getMessage())
                                    .withCause(throwable)
                                    .asRuntimeException());
                            return;
                        }

                        // The response now signals success or failure, which is crucial for client-side forwarding logic.
                        PushResponse response = PushResponse.newBuilder().setSuccess(status.isOk()).build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    });
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error processing request: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void peek(PeekRequest request, StreamObserver<PeekResponse> responseObserver) {
        try {
            String queueName = request.getQueueName();
            Partition partition = partitionManager.lookupPartition(queueName);
            Optional<String> clientToken = request.getClientToken().isEmpty()
                    ? Optional.empty()
                    : Optional.of(request.getClientToken());

            Optional<Message> messageOpt = partition.peek(queueName, clientToken);

            PeekResponse.Builder responseBuilder = PeekResponse.newBuilder();
            if (messageOpt.isPresent()) {

                Message msg = messageOpt.get();

                org.example.qservice.External.Message protoMessage =
                        org.example.qservice.External.Message.newBuilder()
                                .setMessageId(msg.getMessageId().toString())
                                .setPayload(msg.getPayload())
                                .setLogIndex(msg.getLogIndex())
                                .build();

                responseBuilder.setMessage(protoMessage).setFound(true);

            } else {
                responseBuilder.setFound(false);
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("An unexpected error occurred: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }


    @Override
    public void pop(PopRequest request, StreamObserver<PopResponse> responseObserver) {
        try {
            String queueName = request.getQueueName();
            Partition partition = partitionManager.lookupPartition(queueName);
            UUID messageId = UUID.fromString(request.getMessageId());

            partition.pop(queueName, messageId)
                    .whenComplete((status, throwable) -> {
                        if (throwable != null) {
                            responseObserver.onError(Status.INTERNAL
                                    .withDescription("An unexpected error occurred: " + throwable.getMessage())
                                    .withCause(throwable)
                                    .asRuntimeException());
                            return;
                        }

                        PopResponse response = PopResponse.newBuilder().setSuccess(status.isOk()).build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    });
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error processing request: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}