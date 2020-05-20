package test;

import edu.umass.cs.gigapaxos.interfaces.RequestFuture;
import edu.umass.cs.reconfiguration.ReconfigurableAppClientAsync;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ClientReconfigurationPacket;
import edu.umass.cs.reconfiguration.reconfigurationpackets.RequestActiveReplicas;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RequestActiveReplicaClient {
    static XDNAgentClient client;

    private static ClientReconfigurationPacket requestActiveReplicas(String serviceName) throws IOException, ReconfigurableAppClientAsync.ReconfigurationException, InterruptedException, ExecutionException, TimeoutException {
        RequestActiveReplicas r = new RequestActiveReplicas(serviceName);
        RequestFuture<ClientReconfigurationPacket> future = client.sendRequest(r);
        ClientReconfigurationPacket result = (ClientReconfigurationPacket) future.get(1000, TimeUnit.MILLISECONDS);
        return result;
    }

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException, TimeoutException, ReconfigurableAppClientAsync.ReconfigurationException {
        client = new XDNAgentClient();
        ClientReconfigurationPacket result = requestActiveReplicas("NoopPaxosApp0");
                //XDNConfig.generateServiceName("xdn-demo-app","Alvin"));
        System.out.println(result);
        client.close();
    }

}
