#!/bin/bash

minikube start --cpus=4 --memory=8192 --disk-size=20g --extra-config=kubelet.max-pods=250

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

minikube image load scalability-demo:latest

kubectl apply -f scalability-demo-deployment.yaml

for i in {1..5}; do
    kubectl get pods
    sleep 1
done

minikube service kv-store-client --url