package test;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ReplicableClientRequest;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;

/**
 * java -ea -cp jar/XDN-1.0.jar -Djava.util.logging.config.file=conf/logging.properties -Dlog4j.configuration=conf/log4j.properties -DgigapaxosConfig=conf/xdn.properties test.ReconfigurableServices
 */
public class ReconfigureExpClient {
    final static long interval = 1000;
    static int received = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        String ip = args[0];
        InetSocketAddress addr = new InetSocketAddress(ip, 2000);
        boolean ready = Boolean.parseBoolean(args[1]);

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
                    true,
                    false,
                    0
            );


            if (ready) {
                long start = System.currentTimeMillis();
                try {
                    sent++;
                    // coordinate request through GigaPaxos
                    client.sendRequest(ReplicableClientRequest.wrap(req),
                            // PaxosConfig.getActives().get(node),
                            addr,
                            new RequestCallback() {
                                @Override
                                public void handleResponse(Request response) {
                                    System.out.println((System.currentTimeMillis() - start));
                                    received++;
                                }
                            }
                    );

                } catch (IOException e) {
                    e.printStackTrace();
                    // request coordination failed
                }

                while (received < sent) {
                    Thread.sleep(10);
                }

                long elapsed = System.currentTimeMillis() - start;
                if (interval > elapsed)
                    Thread.sleep(interval - elapsed);
                System.out.println(elapsed);
            } else {

                Thread.sleep(interval);
                System.out.println("0");
            }

            if (i%30 == 0)
                ready = !ready;

        }

        client.close();

    }
}
