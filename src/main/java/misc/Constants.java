package misc;

import java.time.Duration;

public class Constants {

    public final static Duration REDELIVERY_TIMEOUT = Duration.ofSeconds(1);

    public final static int ELECTION_TIMEOUT_MS = 1000;

    public final static int SNAPSHOT_INTERVAL_SEC = 1;

    public final static Duration LEASE_TIMEOUT_MS = Duration.ofMillis(500);
    public final static int LEASE_REFRESH_MS = 100;

    public final static int RAFT_PORT = 9999;

    public final static long timeoutMillis = 100;
    public final static int retries = 3;
    public final static long initialBackoffMillis = 100;
    public final static double backoffMultiplier = 2;
    public final static long jitterMaxMillis = 50;
    public final static Duration unhealthyNodeIgnoreTimeoutMillis = Duration.ofMillis(10000);

}
