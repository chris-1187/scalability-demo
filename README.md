# scalability-demo
Prototyping Assignment of the 3S Scalability Engineering module.

### How to enable VPA

To use VPA within your Kubernetes/Minikube cluster you first need to have a few prerequisites:
minikube, kubectl installed
```bash
minikube start

  # metrics server enabled
minikube addons enable metrics-server

  # get the autoscaler repo
git clone https://github.com/kubernetes/autoscaler.git
cd autoscaler/vertical-pod-autoscaler

  # install VPA
./hack/vpa-up.sh
```