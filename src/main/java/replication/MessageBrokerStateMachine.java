package replication;

import com.alipay.sofa.jraft.Closure;
import com.alipay.sofa.jraft.Iterator;
import com.alipay.sofa.jraft.StateMachine;
import com.alipay.sofa.jraft.Status;
import com.alipay.sofa.jraft.conf.Configuration;
import com.alipay.sofa.jraft.entity.LeaderChangeContext;
import com.alipay.sofa.jraft.error.RaftError;
import com.alipay.sofa.jraft.error.RaftException;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotReader;
import com.alipay.sofa.jraft.storage.snapshot.SnapshotWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.Message;
import queue.MessageQueue;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MessageBrokerStateMachine implements StateMachine {

    public Optional<Message> peekWithTimeout(String queueName, Optional<String> clientToken){
        MessageQueue queue = queues.getOrDefault(queueName, null);
        if(queue != null){
            return queue.peek(clientToken);
        }
        return Optional.empty();
    }

    private static final Logger logger = LoggerFactory.getLogger(MessageBrokerStateMachine.class);


    private final Map<String, MessageQueue> queues = new HashMap<>();


    @Override
    public void onApply(Iterator iterator) {
        System.out.println("APPLY");
        while (iterator.hasNext()){
            LogEntry entry = LogEntry.fromBuffer(iterator.getData());
            switch (entry){
                case PushEntry push -> {
                    System.out.println(iterator.getIndex());
                    Message message = new Message(push.getMessageId(), push.getPayload(), iterator.getIndex());
                    MessageQueue queue = queues.computeIfAbsent(push.getQueue(), k -> new MessageQueue(push.getQueue()));
                    queue.insert(message);

                }
                case PopEntry pop -> {
                    MessageQueue queue = queues.get(pop.getQueue());
                    if(queue != null){
                        queue.commit(pop.getMessageId());
                    }
                }
                default -> throw new RuntimeException("Unknown case in log entry handling");
            }
            if(iterator.done() != null)
                iterator.done().run(Status.OK());
            iterator.next();
            //TODO commit?
        }

    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void onSnapshotSave(SnapshotWriter snapshotWriter, Closure closure) {
        System.out.println("Snapshotting");
        try {
            for (Map.Entry<String, MessageQueue> queueEntry : queues.entrySet()){
                File snapshotFile = new File(snapshotWriter.getPath(), queueEntry.getKey());
                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(snapshotFile))) {
                    out.writeObject(queueEntry.getValue().snapshot());
                }
                snapshotWriter.addFile(snapshotFile.getName());
            }
            closure.run(Status.OK());
            System.out.println("Snapshot ok");
        } catch (IOException e) {
            closure.run(new Status(RaftError.EIO, "Snapshot save failed: %s", e));
        }
    }

    @Override
    public boolean onSnapshotLoad(SnapshotReader snapshotReader) {
        System.out.println("Snapshot load");
        queues.clear();
        Path dir = Paths.get(snapshotReader.getPath());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(entry.toFile()))){
                    String queueName = entry.getFileName().toString();
                    List<Message> snapshot = (List<Message>) in.readObject();
                    queues.put(queueName, new MessageQueue(queueName, snapshot));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public void onLeaderStart(long l) {
    }

    @Override
    public void onLeaderStop(Status status) {
    }

    @Override
    public void onError(RaftException e) {
    }

    @Override
    public void onConfigurationCommitted(Configuration configuration) {
    }

    @Override
    public void onStopFollowing(LeaderChangeContext leaderChangeContext) {
    }

    @Override
    public void onStartFollowing(LeaderChangeContext leaderChangeContext) {
    }

}
