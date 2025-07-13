#!/bin/bash

nodes="${1:-3}"

# Build image locally
docker build -t scalability-demo:latest .

# Start the minikube cluster with enough resources for up to N Nodes
minikube start --cpus=8 --memory=8192 --disk-size=20g --extra-config=kubelet.max-pods=250

# Init VPA
minikube addons enable metrics-server

if [ ! -d "autoscaler" ]; then
    echo "Cloning autoscaler repository..."
    git clone https://github.com/kubernetes/autoscaler.git
else
    echo "Directory 'autoscaler' already exists. Skipping clone."
fi

cd autoscaler/vertical-pod-autoscaler || exit 1
./hack/vpa-up.sh
cd ../..

# Load the image into minikube
minikube image load scalability-demo:latest

# apply deployment
kubectl apply -f scalability-demo-deployment.yaml
sleep 5

while true; do
    clear
    echo -e "Waiting for $nodes pods to be in 'Running' status...\n"
    kubectl get pods

    running_count=$(kubectl get pods --no-headers | grep 'Running' | wc -l)

    if [ "$running_count" -ge "$nodes" ]; then
        echo -e "\n> $nodes pods are running\n"
        break
    fi

    sleep 1
done

# Expose pod
minikube service dist-msg-queue-client --url
