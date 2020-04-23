#!/bin/bash
total=$1

cnt=0
while read p; do
	echo $p
	ssh oversky@$p "rm -rf /tmp/gigapaxos/*" < /dev/null &
	ssh oversky@$p "pkill -f ReconfigurableNode" < /dev/null &
	
	# clean up docker 
	ssh oversky@$p "docker stop $(docker ps -a -q)" </dev/null 
	ssh oversky@$p "docker rm -f $(docker ps -a -q)" < /dev/null &
	# docker system prune -f -a
	# docker volume rm $(docker volume ls -q)
done <cl_ssh

