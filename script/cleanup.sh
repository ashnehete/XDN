#!/bin/bash
sudo rm -rf /tmp/gigapaxos gigapaxos.log* derby.log

# exit

docker stop -t 0 $(docker ps -a -q)
docker rm -f $(docker ps -a -q)
#docker system prune -f -a
docker volume rm $(docker volume ls -q)


# docker stop -t 0 $(docker ps -a -q) && docker rm -f $(docker ps -a -q) && docker volume rm $(docker volume ls -q)
