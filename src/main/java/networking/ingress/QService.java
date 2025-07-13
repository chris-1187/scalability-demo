package networking.ingress;

import com.alipay.sofa.jraft.Status;
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
        String queueName = request.getQueueName();
        Partition partition = partitionManager.lookupPartition(queueName);
        UUID messageId = UUID.fromString(request.getMessage().getMessageId());

        Status status = partition.push(request.getQueueName(), request.getMessage().getPayload(), messageId);

        PushResponse response = PushResponse.newBuilder().setSuccess(status.isOk()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void peek(PeekRequest request, StreamObserver<PeekResponse> responseObserver) {
        String queueName = request.getQueueName();
        Partition partition = partitionManager.lookupPartition(queueName);
        Optional<String> clientToken = request.getClientToken().isEmpty()
                ? Optional.empty()
                : Optional.of(request.getClientToken());

        PeekResponse response = partition.peek(queueName, clientToken);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    @Override
    public void pop(PopRequest request, StreamObserver<PopResponse> responseObserver) {
        String queueName = request.getQueueName();
        Partition partition = partitionManager.lookupPartition(queueName);
        UUID messageId = UUID.fromString(request.getMessageId());

        Status status = partition.pop(queueName, messageId);

        PopResponse response = PopResponse.newBuilder().setSuccess(status.isOk()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}