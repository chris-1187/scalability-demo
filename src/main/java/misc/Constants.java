package misc;

import java.time.Duration;

public class Constants {

    public final static Duration REDELIVERY_TIMEOUT = Duration.ofSeconds(1);

    public final static int ELECTION_TIMEOUT_MS = 500;

    public final static int SNAPSHOT_INTERVAL_SEC = 1;

    public final static Duration LEASE_TIMEOUT_MS = Duration.ofMillis(500);
    public final static int LEASE_REFRESH_MS = 100;

    public final static int RAFT_PORT = 9999;

    public final static int MAX_QUEUE_LENGTH = 100;
    public final static long TIMEOUT_MILLIS = 1000;
    public final static int RETRIES = 3;
    public final static long INITIAL_BACKOFF_MILLIS = 200;
    public final static double BACKOFF_MULTIPLIER = 2;
    public final static long JITTER_MAX_MILLIS = 50;
    public final static Duration UNHEALTHY_NODE_IGNORE_TIMEOUT_MILLIS = Duration.ofMillis(10000);

}
