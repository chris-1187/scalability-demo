package networking.ingress;

import partitioning.PartitionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.protobuf.services.HealthStatusManager;


import java.io.IOException;

public class GrpcServer {

    private static final Logger logger = LoggerFactory.getLogger(GrpcServer.class);
    private final int port;
    private final Server server;
    private final HealthStatusManager healthManager;

    public GrpcServer(int port, PartitionManager partitionManager) {
        this(ServerBuilder.forPort(port), partitionManager, port);
    }

    public GrpcServer(ServerBuilder<?> builder, PartitionManager partitionManager, int port){
        this.port = port;
        this.healthManager = new HealthStatusManager();
        QService qService = new QService(partitionManager);
        server = builder
                .addService(qService)
                .addService(healthManager.getHealthService())
                .build();
    }

    public void start() throws IOException {
        logger.info("Starting Server...");
        if(server != null){
            server.start();
            setServingStatus(false); // Until Raft confirms its ready
            logger.info("gRPC server has started (listerning on port " + port + ")");
        }
    }

    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdownNow();
            server.awaitTermination();
            logger.info("gRPC server shut down");
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public void setServingStatus(boolean isServing) {
        // An empty string "" is the recommended service name for health checking the entire server
        if (isServing) {
            healthManager.setStatus("", HealthCheckResponse.ServingStatus.SERVING);
        } else {
            healthManager.setStatus("", HealthCheckResponse.ServingStatus.NOT_SERVING);
        }
    }

}
