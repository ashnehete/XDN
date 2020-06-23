#!/bin/sh

# 0. pre-requisites
sudo swapoff -a

# 1. Install docker
sudo apt-get install apt-transport-https ca-certificates curl software-properties-common -y

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -

sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"

sudo apt-get update -y

sudo apt-get install docker-ce -y

sudo systemctl enable docker && sudo systemctl start docker

sudo usermod -a -G docker $USER

# 2. Install Kubernetes
curl -s https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key add -

echo 'deb http://apt.kubernetes.io/ kubernetes-xenial main' | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update -y
 
sudo apt-get install kubelet kubeadm kubectl -y
 
sudo systemctl enable kubelet && sudo systemctl start kubelet

