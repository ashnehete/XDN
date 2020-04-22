Prerequisites: `Linux`, `Java 1.8+`, `ant`, `bash`, `docker`, `criu`

Obtaining XDN
-------------
Source:
```
git clone https://github.com/ZhaoyuUmass/XDN
cd XDN
ant jar
```

Overview
-------------
Service dispersibility refers to the ability to programmatically sprinkle replicas of lightweight services anywhere and everywhere based on system objectives such as customization, or the flexibility to tailor service replica groups to individual end-users based on their access patterns, availability, or performance requirements.

`XDN` is a heterogeneous system that tightly integrates edge and cloud platform to make service dispersibility easier, especially for stateful services.


Getting Started Quickly
-----------------------
`XDN` is built on top of [GigaPaxos](<https://github.com/MobilityFirst/gigapaxos>), a group-scalable replicated state machine (RSM) system, which allows applications to easily create and manage a very large number of separate RSMs. Please go through [GigaPaxos tutorial](<https://github.com/MobilityFirst/gigapaxos/wiki>) first to get familiar with the concepts mentioned in this tutorial.

### Tutorial 1: Standalone Setup
To start up XDN servers, you need to specify a config file. In this tutorial, we'll use the config file <tt>conf/xdn.local.properties</tt>.

    # XDN config file
    APPLICATION=edu.umass.cs.xdn.XDNApp
    
    DISABLE_RECONFIGURATION=true
    ENABLE_ACTIVE_REPLICA_HTTP=true
    GIGAPAXOS_DATA_DIR=/tmp/gigapaxos
    
    # format: active.<active_server_name>=host:port
    active.AR0=127.0.0.1:2000
    
    # format: reconfigurator.<active_server_name>=host:port
    reconfigurator.RC=127.0.0.1:5000

Run the servers as follows from the top-level directory:
```
script/gpServer.sh -DgigapaxosConfig=conf/xdn.local.properties start all
```

Run the client to create an XDN application:
```
script/gpClient.sh -DgigapaxosConfig=conf/xdn.local.properties test.CreateServices
```

It creates an stateful counter app wrapped in docker called xdn-demo-app. You can find its docker image on DockerHub: [xdn-demo-app docker image](https://hub.docker.com/repository/docker/oversky710/xdn-demo-app).
Open your browser to check the counter's current value: [http://127.0.0.1/xdnapp](http://127.0.0.1/xdnapp).

Run the client to send a request:
```
script/gpClient.sh -DgigapaxosConfig=conf/xdn.local.properties test.ExecuteServices
```

The client will send a request with a value "1", the the underlying app [xdn-demo-app](https://github.com/ZhaoyuUmass/xdn-demo-app) add value 1 to its current state.
Open your browser to check the counter's current value after operation: [http://127.0.0.1/xdnapp](http://127.0.0.1/xdnapp).

You may also issue a request directly to our HTTP API with `curl`:

```
curl "http://127.0.0.1:2300?name=xdn-demo-app_xdn_Alvin&qval=1"
```

Or open the following link with your browser to send a request: [http://127.0.0.1:2300?name=xdn-demo-app_xdn_Alvin&qval=1](http://127.0.0.1:2300?name=xdn-demo-app_xdn_Alvin&qval=1).

Find out more usage instructions with our HTTP APIs on [GigaPaxos Wiki](http://github.com/MobilityFirst/gigapaxos/wiki).

To clear up:
```
script/gpServer.sh -DgigapaxosConfig=conf/xdn.local.properties forceclear all
script/cleanup.sh
```


### Tutorial 2: Distributed Setup

Let's use the config file <tt>conf/exp.properties</tt>
    
    # XDN config file
    APPLICATION=edu.umass.cs.xdn.XDNApp
    DEMAND_PROFILE_TYPE=edu.umass.cs.xdn.policy.DemoDemandProfile
        
    ENABLE_ACTIVE_REPLICA_HTTP=true
    
    GIGAPAXOS_DATA_DIR=/tmp/gigapaxos
    
    # format: active.<active_server_name>=host:port
    active.AR0=node-0:2000
    active.AR1=node-1:2000
    
    # format: reconfigurator.<active_server_name>=host:port
    reconfigurator.RC=node-2:5000

Here, <i>node-0</i>,<i>node-1</i>, and <i>node-2</i> must be different servers so that they have different docker runtimes to create new app instances.
The demand profile `edu.umass.cs.xdn.policy.DemoDemandProfile` reconfigures an app on every request, it moves an app between <i>node-0</i> and <i>node-1</i>.

Run the servers as follows from the top-level directory:
```
script/gpServer.sh -DgigapaxosConfig=conf/exp.properties start all
```

Run the client to create an XDN application:
```
script/gpClient.sh -DgigapaxosConfig=conf/exp.properties test.CreateServices
```

Run the client to send a request:
```
script/gpClient.sh -DgigapaxosConfig=conf/exp.properties test.ExecuteServices
```

Run it multiple times, you will see that the service app is moved back-and-forth between <i>node-0</i> and <i>node-1</i> on every request.

Shutdown the service:
```
script/gpServer.sh -DgigapaxosConfig=conf/exp.properties stop all
```

Run command to clean up on every node:
```
script/cleanup.sh
```