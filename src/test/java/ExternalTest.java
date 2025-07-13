import com.google.common.hash.Hashing;
import networking.egress.MessageBrokerNode;
import networking.egress.MockMessageBrokerNode;
import networking.egress.RemoteMessageBrokerNode;
import org.example.qservice.External;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import partitioning.OwnPartition;
import partitioning.PartitionManager;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.*;

public class ExternalTest {


    private final String targetHost = "127.0.0.1";

    private final int targetPort = 56222;

    private MessageBrokerNode entryNode;

    @Before
    public void setUp() throws Exception {
        entryNode = new RemoteMessageBrokerNode(targetHost, targetPort);
    }
    @After
    public void tearDown() throws Exception {

    }


    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateQueueName(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    @Test
    public void apiLevelTest() throws InterruptedException {
        //Message content correctness
        UUID uuid1 = UUID.randomUUID();
        entryNode.push("testQueue", "Hello Queue 1", uuid1);
        assertEquals("Hello Queue 1", entryNode.peek("testQueue", Optional.empty()).get().getMessage().getPayload());
        entryNode.pop("testQueue", uuid1);

        Map<String, Set<UUID>> sentMessages = new HashMap<>();

        List<String> queues = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            queues.add(generateQueueName(16));
        }

        for (int i = 0; i < 100; i++) {
            Collections.shuffle(queues);
            UUID uuid = UUID.randomUUID();
            Set<UUID> queueSet = sentMessages.computeIfAbsent(queues.get(0), k -> new HashSet<>());
            queueSet.add(uuid);
            External.PushResponse response = entryNode.push(queues.get(0), "HelloWorld", uuid).get();
            assertTrue(response.getSuccess());
        }
        Thread.sleep(1000);

        for(String queue : queues) {
            External.PeekResponse response = entryNode.peek(queue, Optional.empty()).get();
            while (response.getFound()){
                Set<UUID> uuidSet = sentMessages.get(queue);
                UUID messageUUID = UUID.fromString(response.getMessage().getMessageId());
                assertTrue(uuidSet.contains(messageUUID));
                uuidSet.remove(messageUUID);
                External.PopResponse popResponse = entryNode.pop(queue, messageUUID).get();
                assertTrue(popResponse.getSuccess());
                Thread.sleep(250); //Pop returns when the log entry was committed by Raft, not when it was applied to the state machine
                response = entryNode.peek(queue, Optional.empty()).get();
            }
        }

        for(var entry : sentMessages.entrySet()){
            assertTrue(entry.getValue().isEmpty());
        }

    }

}
