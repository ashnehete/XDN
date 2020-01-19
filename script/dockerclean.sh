#!/bin/bash

docker stop $(docker ps -a -q)
docker rm -f $(docker ps -a -q)
docker system prune -f -a
