package replication;

import java.util.UUID;

public class PopEntry extends LogEntry {
    private final UUID messageId;

    private final String queue;


    public PopEntry(String queue, UUID messageId) {
        this.messageId = messageId;
        this.queue = queue;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getQueue() {
        return queue;
    }
}
