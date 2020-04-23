#!/bin/bash
total=$1

cnt=0
while read p; do
	echo $p
	ssh oversky@$p "rm -rf /tmp/gigapaxos/*" < /dev/null
	ssh oversky@$p "pkill -f ReconfigurableNode" < /dev/null
	
	# clean up docker 
	docker stop $(docker ps -a -q)
	docker rm -f $(docker ps -a -q)
	docker system prune -f -a
	docker volume rm $(docker volume ls -q)
done <cl_ssh

