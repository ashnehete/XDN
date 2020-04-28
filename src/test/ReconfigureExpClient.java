package test;

import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ReplicableClientRequest;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * java -ea -cp jar/XDN-1.0.jar -Djava.util.logging.config.file=conf/logging.properties -Dlog4j.configuration=conf/log4j.properties -DgigapaxosConfig=conf/xdn.properties test.ReconfigurableServices
 */
public class ReconfigureExpClient {
    final static long interval = 1000;
    // final static long timeout = 1000;

    static int received = 0;
    static Map<Integer, Long> result = new HashMap<>();

    public static void main(String[] args) throws IOException, InterruptedException {

        boolean ready = Boolean.parseBoolean(args[0]);

        InetSocketAddress addr = null;
        if (args.length > 1) {
            String ip = args[1];
            addr = new InetSocketAddress(ip, 2100);
        }

        XDNAgentClient client = new XDNAgentClient();

        String testServiceName = "xdn-demo-app"+ XDNConfig.xdnServiceDecimal+"Alvin";


        int total = 100;

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
                final int index = i;
                sent++;
                try {
                    // coordinate request through GigaPaxos
                    if (addr != null)
                        client.sendRequest(ReplicableClientRequest.wrap(req),
                                // PaxosConfig.getActives().get(node),
                                addr,
                                new RequestCallback() {
                                    @Override
                                    public void handleResponse(Request response) {
                                        result.put(index, (System.currentTimeMillis() - start));
                                        System.out.println((System.currentTimeMillis() - start));
                                        received++;
                                    }
                                }
                        );
                    else
                        client.sendRequest(ReplicableClientRequest.wrap(req),
                                // PaxosConfig.getActives().get(node),
                                new RequestCallback() {
                                    @Override
                                    public void handleResponse(Request response) {
                                        result.put(index, (System.currentTimeMillis() - start));
                                        System.out.println((System.currentTimeMillis() - start));
                                        received++;
                                    }
                                }
                        );

                } catch (IOException e) {
                    e.printStackTrace();
                    // request coordination failed
                }

                long elapsed = System.currentTimeMillis() - start;
                if (interval > elapsed)
                    Thread.sleep(interval - elapsed);
                // System.out.println(elapsed);
            } else {

                Thread.sleep(interval);
                // System.out.println("0");
            }

//            if (i%30 == 0)
//                ready = !ready;

        }

        while(received < sent){
            System.out.println("Received:"+received);
            Thread.sleep(500);
        }
        client.close();

    }
}
