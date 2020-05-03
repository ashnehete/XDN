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
import java.util.Random;

/**
 * java -ea -cp jar/XDN-1.0.jar -Djava.util.logging.config.file=conf/logging.properties -Dlog4j.configuration=conf/log4j.properties -DgigapaxosConfig=conf/xdn.properties test.ExecuteServices
 */
public class ExecuteServices {
    static int received = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        XDNAgentClient client = new XDNAgentClient();
        String testServiceName = XDNConfig.generateServiceName(CreateServices.imageName, "Alvin");

        Integer addValue = 1;
        int total = 1;
        String node = null;
        if (System.getProperty("node")!=null) {
            node = System.getProperty("node");
            // if node does not exist, reset it to null
            if (!PaxosConfig.getActives().containsKey(node)) {
                System.out.println("Node ID "+node+" does not exist.");
                node = null;
            }
        }

        int id = (new Random()).nextInt();
        int sent = 0;
        // System.out.println("Start testing... ");
        for (int i=0; i<total; i++) {
            sent++;
            HttpActiveReplicaRequest req = new HttpActiveReplicaRequest(HttpActiveReplicaPacketType.EXECUTE,
                    testServiceName,
                    id++,
                    addValue.toString(),
                    true,
                    false,
                    0
                    );
            // AppRequest request = new AppRequest(testServiceName, json.toString(), AppRequest.PacketType.DEFAULT_APP_REQUEST, false);
            // System.out.println("About to send "+i+"th request.");
            long start = System.currentTimeMillis();
            if (node == null) {
                try {
                    // coordinate request through GigaPaxos
                    client.sendRequest(ReplicableClientRequest.wrap(req)
                            , new RequestCallback() {
                                @Override
                                public void handleResponse(Request response) {
                                    System.out.println((System.currentTimeMillis() - start));
                                    received++;
                                }
                            });

                } catch (IOException e) {
                    e.printStackTrace();
                    // request coordination failed
                }

                while (received < sent) {
                    Thread.sleep(500);
                }
            } else {

                try {
                    // send request to a specific node
                    client.sendRequest(ReplicableClientRequest.wrap(req),
                            PaxosConfig.getActives().get(node)
                            );

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }

        client.close();
    }
}
