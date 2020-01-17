#!/bin/bash

sudo apt update
# sudo apt install criu

# sudo cp daemon.json /etc/docker/
sudo dockerd --config-file /etc/docker/daemon.json

# setup Java
cp /proj/lsn-PG0/groups/jdk-8u181-linux-x64.tar.gz .
tar zxf jdk-8u181-linux-x64.tar.gz
cp /proj/lsn-PG0/groups/.profile .
source .profile


# get XDN code
cp -r /proj/lsn-PG0/groups/XDN .

# compile code
cd XDN
ant jar
cd ~
