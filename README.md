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