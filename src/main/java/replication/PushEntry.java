package replication;

import java.util.UUID;

public class PushEntry extends LogEntry {
    private final UUID messageId;
    private final String payload;
    private final String queue;

    public PushEntry(String queue, String payload, UUID messageId) {
        this.messageId = messageId;
        this.payload = payload;
        this.queue = queue;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getPayload() {
        return payload;
    }

    public String getQueue() {
        return queue;
    }
}
