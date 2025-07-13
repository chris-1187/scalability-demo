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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import misc.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import queue.Message;
import queue.MessageQueue;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class MessageBrokerStateMachine implements StateMachine {

    public Optional<Message> peekWithTimeout(String queueName, Optional<String> clientToken){
        MessageQueue queue = queues.getOrDefault(queueName, null);
        if(queue != null){
            return queue.peek(clientToken);
        }
        return Optional.empty();
    }

    public boolean hasSpace(String queue){
        MessageQueue mq = queues.get(queue);
        if(mq != null && mq.getSize() >= Constants.MAX_QUEUE_LENGTH){
            return false;
        }
        return true;
    }

    private static final Logger logger = LoggerFactory.getLogger(MessageBrokerStateMachine.class);

    private final Map<String, MessageQueue> queues = new ConcurrentHashMap<>();

    Cache<UUID, Long> uuidTTLCache;

    public MessageBrokerStateMachine(){
        uuidTTLCache = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }


    @Override
    public void onApply(Iterator iterator) {
        while (iterator.hasNext()){
            LogEntry entry = LogEntry.fromBuffer(iterator.getData());
            switch (entry){
                case PushEntry push -> {
                    Long cacheEntry = uuidTTLCache.getIfPresent(push.getMessageId());
                    if(cacheEntry == null || iterator.getIndex() < cacheEntry){
                        Message message = new Message(push.getMessageId(), push.getPayload(), iterator.getIndex());
                        MessageQueue queue = queues.computeIfAbsent(push.getQueue(), k -> new MessageQueue(push.getQueue()));
                        queue.insert(message);
                        uuidTTLCache.put(message.getMessageId(), message.getLogIndex());
                    }
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
        }

    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void onSnapshotSave(SnapshotWriter snapshotWriter, Closure closure) {
        try {
            for (Map.Entry<String, MessageQueue> queueEntry : queues.entrySet()){
                File snapshotFile = new File(snapshotWriter.getPath(), queueEntry.getKey());
                try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(snapshotFile))) {
                    out.writeObject(queueEntry.getValue().snapshot());
                }
                snapshotWriter.addFile(snapshotFile.getName());
            }
            File cacheFile = new File(snapshotWriter.getPath(), "uuidTTLCache");
            Map<UUID, Long> cacheSnapshot = new HashMap<>(uuidTTLCache.asMap());
            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(cacheFile))) {
                out.writeObject(cacheSnapshot);
            }
            closure.run(Status.OK());
        } catch (IOException e) {
            closure.run(new Status(RaftError.EIO, "Snapshot save failed: %s", e));
        }
    }

    @Override
    public boolean onSnapshotLoad(SnapshotReader snapshotReader) {
        queues.clear();
        uuidTTLCache.cleanUp();
        Path dir = Paths.get(snapshotReader.getPath());
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path entry : stream) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(entry.toFile()))){
                    String queueName = entry.getFileName().toString();
                    List<Message> snapshot = (List<Message>) in.readObject();
                    queues.put(queueName, new MessageQueue(queueName, snapshot));
                }
            }
            File cacheFile = new File(snapshotReader.getPath(), "uuidTTLCache");
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(cacheFile))){
                Map<UUID, Long> cacheSnapshot = (Map<UUID, Long>) in.readObject();
                uuidTTLCache.putAll(cacheSnapshot);
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
