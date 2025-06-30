package replication;

import com.alipay.sofa.jraft.Node;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.entity.Task;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public abstract class LogEntry implements Serializable {

    //fails if node isnt the leader (anymore)
    public CompletableFuture<Status> submit(Node node){
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(this);
            oos.flush();
        } catch (IOException e) {
            throw new RuntimeException("Serialization failed", e);
        }
        byte[] serializedData = bos.toByteArray();
        Task task = new Task();
        task.setData(ByteBuffer.wrap(serializedData));

        CompletableFuture<Status> futureStatus = new CompletableFuture<>();
        task.setDone(futureStatus::complete);
        node.apply(task);
        return futureStatus;
    }

    public static LogEntry fromBuffer(ByteBuffer buffer){
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer.array()))) {
            return (LogEntry) ois.readObject();
        } catch (IOException | ClassNotFoundException e){
            throw new RuntimeException("deserialization error");
        }
    }

}
