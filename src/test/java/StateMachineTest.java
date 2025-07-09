import networking.egress.MockMessageBrokerNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import partitioning.OwnPartition;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class StateMachineTest {

    OwnPartition node;

    @Before
    public void setUp() throws Exception {
        node = new OwnPartition(0, new MockMessageBrokerNode("localhost", 9991), List.of());
    }
    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testCorrectness() throws InterruptedException {
        Thread.sleep(5000); //wait for JRaft to figure everything out
        assertFalse(node.peek("queue1", Optional.of("42")).getFound());

        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        UUID uuid4 = UUID.randomUUID();

        assertTrue(node.push("queue1", "HelloWorld!1", uuid1).isOk());
        assertTrue(node.push("queue1", "HelloWorld!2", uuid2).isOk());
        assertTrue(node.push("queue1", "HelloWorld!3", uuid3).isOk());
        assertTrue(node.push("queue2", "HelloWorld!6", uuid4).isOk());

        //simulate push retry with same idempotency UUID
        assertTrue(node.push("queue1", "HelloWorld!2", uuid2).isOk());
        assertTrue(node.push("queue1", "HelloWorld!2", uuid2).isOk());
        assertTrue(node.push("queue1", "HelloWorld!2", uuid2).isOk());


        assertEquals("HelloWorld!1", node.peek("queue1", Optional.of("42")).getMessage().getPayload());
        assertFalse(node.peek("queue1", Optional.of("41")).getFound()); //avoid early redelivery

        assertTrue(node.pop("queue1", uuid2).isOk());
        assertTrue(node.pop("queue1", uuid1).isOk());

        //simulate pop retry with same idempotency UUID
        assertTrue(node.pop("queue1", uuid1).isOk());
        assertTrue(node.pop("queue1", uuid1).isOk());
        assertTrue(node.pop("queue1", uuid1).isOk());


        assertEquals("HelloWorld!2", node.peek("queue1", Optional.of("42")).getMessage().getPayload());
        assertTrue(node.pop("queue1", uuid2).isOk());

        assertEquals("HelloWorld!3", node.peek("queue1", Optional.of("42")).getMessage().getPayload());
        assertTrue(node.pop("queue1", uuid3).isOk());

        assertFalse(node.peek("queue1", Optional.of("42")).getFound());
    }


}
