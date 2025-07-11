#!/bin/bash

# For local testing: Build image locally; Use when something changed
docker build -t scalability-demo:latest .

# Start the minikube cluster with enough resources for up to 9 Nodes
minikube start --cpus=4 --memory=8192 --disk-size=20g --extra-config=kubelet.max-pods=250

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
# minikube image load ghcr.io/chris-1187/scalability-demo:latest

# apply deployment
kubectl apply -f scalability-demo-deployment.yaml
sleep 5

while true; do
    clear
    echo -e "Waiting for 9 pods to be in 'Running' status...\n"
    kubectl get pods

    running_count=$(kubectl get pods --no-headers | grep 'Running' | wc -l)

    if [ "$running_count" -ge 9 ]; then
        echo -e "\n> 9 pods are running\n"
        break
    fi

    sleep 1
done

# Expose pod
minikube service dist-msg-queue-client --url