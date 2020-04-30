package test;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.gigapaxos.interfaces.RequestFuture;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.ReconfigurableAppClientAsync;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.interfaces.ReconfigurableRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ClientReconfigurationPacket;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ReplicableClientRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.RequestActiveReplicas;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * java -ea -cp jar/XDN-1.0.jar -Djava.util.logging.config.file=conf/logging.properties -Dlog4j.configuration=conf/log4j.properties -DgigapaxosConfig=conf/xdn.properties test.ReconfigurableServices
 */
public class ReconfigureExpClient {
    final static long interval = 1000;
    final static long timeout = 1000;

    static int received = 0;
    static synchronized void incr(){
        received++;
    }

    static XDNAgentClient client;

    private String requestActiveReplicas(String serviceName) throws IOException, ReconfigurableAppClientAsync.ReconfigurationException, InterruptedException, ExecutionException, TimeoutException {
        RequestActiveReplicas r = new RequestActiveReplicas(serviceName);
        RequestFuture<ClientReconfigurationPacket> future = client.sendRequest(r);
        ClientReconfigurationPacket result = (ClientReconfigurationPacket) future.get(1000, TimeUnit.MILLISECONDS);
        return result.getResponseMessage();
    }

    protected static class RequestRunnable implements Runnable {

        RequestRunnable(){
        }

        @Override
        public void run() {

        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        boolean ready = Boolean.parseBoolean(args[0]);

        InetSocketAddress addr = null;
        if (args.length > 1) {
            String ip = args[1];
            addr = new InetSocketAddress(ip, 2100);
        }

        client = new XDNAgentClient();

        String testServiceName = CreateServices.imageName+ XDNConfig.xdnServiceDecimal+"Alvin";

        int total = 10;

        int id = (new Random()).nextInt();

        int sent = 0;
        
        // System.out.println("Start testing... ");
        for (int i=0; i<total; i++) {
            HttpActiveReplicaRequest req = new HttpActiveReplicaRequest(HttpActiveReplicaPacketType.EXECUTE,
                    testServiceName,
                    id++,
                    "1",
                    false,
                    false,
                    0
            );


            if (ready) {
                final long start = System.currentTimeMillis();

                sent++;

                Request result = null;

                // coordinate request through GigaPaxos
                if (addr != null)
                    result = client.sendRequest(ReplicableClientRequest.wrap(req),
                            addr
                            , timeout
//                                (RequestCallback) response -> {
//                                    // result.put(index, (System.currentTimeMillis() - start));
//                                    System.out.println(index+","+(System.currentTimeMillis() - start));
//                                    incr();
//                                }
                    );
                else
                    result = client.sendRequest(ReplicableClientRequest.wrap(req)
                            ,timeout
//                                (RequestCallback) response -> {
//                                    // result.put(index, (System.currentTimeMillis() - start));
//                                    System.out.println(index+","+(System.currentTimeMillis() - start));
//                                    incr();
//                                }

                    );


                System.out.println(result);

                long elapsed = System.currentTimeMillis() - start;

                if (interval > elapsed)
                    Thread.sleep(interval - elapsed);

                System.out.println(elapsed);
            }

//            if (i%30 == 0)
//                ready = !ready;

        }

        client.close();

    }
}
