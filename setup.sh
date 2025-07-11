#!/bin/bash

# Build image locally; Use when something changed
# docker build -t scalability-demo:latest .

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

# apply deployment
kubectl apply -f scalability-demo-deployment.yaml

for i in {1..10}; do
    kubectl get pods
    sleep 1
done

# Expose pod
minikube service kv-store-client --url