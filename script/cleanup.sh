#!/bin/bash
sudo rm -r /tmp/gigapaxos gigapaxos.log* derby.log

docker stop $(docker ps -a -q)
docker rm -f $(docker ps -a -q)
docker system prune -f -a
