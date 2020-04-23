#!/bin/bash
total=$1

cnt=0
while read p; do
	echo $p
	ssh oversky@$p "rm -rf /tmp/gigapaxos/*" < /dev/null &
	ssh oversky@$p "pkill -f ReconfigurableNode" < /dev/null &
	
done <cl_ssh

