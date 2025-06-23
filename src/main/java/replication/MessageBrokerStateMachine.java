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
import queue.MessageQueue;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MessageBrokerStateMachine implements StateMachine {

    private final Map<String, MessageQueue> queues = new HashMap<>(); //TODO this will probably be moved somewhere else

    @Override
    public void onApply(Iterator iterator) {
        while (iterator.hasNext()){
            //TODO deserialize log entries and apply to queues (either message insertion or commit)
        }
    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void onSnapshotSave(SnapshotWriter snapshotWriter, Closure closure) {
        try {
            for (Map.Entry<String, MessageQueue> queue : queues.entrySet()){
                File snapshotFile = new File(snapshotWriter.getPath(), queue.getKey() + "-snapshot.txt");
                try (BufferedWriter out = new BufferedWriter(new FileWriter(snapshotFile))) {
                    //TODO serialize queue to file
                }
                snapshotWriter.addFile("queue-snapshot.txt");
            }
            closure.run(Status.OK());
        } catch (IOException e) {
            closure.run(new Status(RaftError.EIO, "Snapshot save failed: %s", e));
        }


    }

    @Override
    public boolean onSnapshotLoad(SnapshotReader snapshotReader) {
        for (Map.Entry<String, MessageQueue> queue : queues.entrySet()){
            File snapshotFile = new File(snapshotReader.getPath(), queue.getKey() + "-snapshot.txt");
            if (!snapshotFile.exists()) return false;

            try (BufferedReader in = new BufferedReader(new FileReader(snapshotFile))) {
                //TODO clear message queue
                //TODO deserialize queue from file
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onLeaderStart(long l) {
        //TODO set application state to leader and start accepting requests to append to log
    }

    @Override
    public void onLeaderStop(Status status) {
        //TODO set application state to no leader and stop accepting requests temporarily
    }

    @Override
    public void onError(RaftException e) {
        //e.g. error notifications
    }

    @Override
    public void onConfigurationCommitted(Configuration configuration) {
        //update e.g. cluster membership information
    }

    @Override
    public void onStopFollowing(LeaderChangeContext leaderChangeContext) {
        //TODO stop routing requests and reject them (system becomes unavailable until new leader is elected)
    }

    @Override
    public void onStartFollowing(LeaderChangeContext leaderChangeContext) {
        //TODO find out the leader node to route future requests to
    }

}
