package misc;

import java.time.Duration;

public class Constants {

    public static final int RAFT_PORT = 9999;

    public final static Duration REDELIVERY_TIMEOUT = Duration.ofSeconds(1);

    public final static int ELECTION_TIMEOUT_MS = 5000;

    public final static int SNAPSHOT_INTERVAL_SEC = 1;

    public final static Duration LEASE_TIMEOUT_MS = Duration.ofMillis(500);
    public final static int LEASE_REFRESH_MS = 100;

}
