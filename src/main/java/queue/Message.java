package queue;

import java.io.Serializable;
import java.util.UUID;

public class Message implements Serializable {
    private final UUID messageId;
    private final String payload;
    private final long logIndex;

    public Message(UUID messageId, String payload, long logIndex) {
        this.messageId = messageId;
        this.payload = payload;
        this.logIndex = logIndex;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getPayload() {
        return payload;
    }

    public long getLogIndex() {
        return logIndex;
    }
}
