# Scalability Demo: A Distributed Message Broker
Prototyping Assignment of the 3S Scalability Engineering module (Group 9).

This is a distributed message broker. It manages multiple named queues (FIFO) and allows 
to enqueue and dequeue messages with at-least-once processing through client commits.
It's able to scale to a huge number of queues, provides high availability and 
message durability and tolerates single node failures.

### Setup

To deploy this distributed message broker, a docker and minikube installation is required (docker driver recommended). 
To set up and start the system, you can run the setup script at the root of this project. Note: First setups will take 
some time as they download the VPA repository. To start the application run:

```bash
  ./setup.sh
```

You can configure the scaling behavior by changing the env variables in the scalability-demo-deployment.yaml file before running the setup script.
You can adjust the following parameters:
* NODE_COUNT: cluster size; needs to be identical to the number of replicas, make sure they are the same
* REPLICATION_FACTOR: number of nodes per partition
* PARTITIONS_PER_NODE: number of partitions per node (vertical scaling factor)

### Code Overview
Here is a summary of the Java packages that you can find in our code 

* bootstrap: contains the logic to bootstrap the cluster by reading env variables provided by kubernetes and calculating the cluster topology
* partitioning: for managing OwnPartitions(held by the node running the code) and ForeignPartitions(held only by other nodes), as well as the logic to look them up
* replication: implementation of replication on top of JRaft with a state machine that processes push and pop entries
* queue: the internal message and message queue data structures holding the actual data
* networking: implementation of the gRPC API (ingress) and node objects for contacting other nodes in the cluster (egress)
* demo: contains demo producer and consumer clients to enqueue and dequeue messages 

We provide two high level tests for validating the implementation
* StateMachineTest: tests only the state machine logic  with regard to e.g. message order, idempotency, etc. by running a single raft node in-process
* ExternalTest: runs against a deployed system, enqueues a larger amount of random messages, clears all queues, and checks that all messages have been retrieved successfully, in order to validate the API and routing logic (you have to manually enter hostname and port before)