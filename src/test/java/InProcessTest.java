import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import static org.junit.jupiter.api.Assertions.*;
import io.grpc.testing.GrpcCleanupRule;

import java.util.*;

@RunWith(JUnit4.class)
public class InProcessTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Before
    public void setUp() throws Exception {
        //TODO bootstrap in-process nodes, and store as variable
    }
    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void helloWold(){
        assertTrue(true);
    }

}
