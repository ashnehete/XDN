package test;

import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.examples.AppRequest;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ReplicableClientRequest;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ExecuteServices {
    static int received = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        XDNAgentClient client = new XDNAgentClient();
        String testServiceName = "xdn-demo-app"+ XDNConfig.xdnServiceDecimal+"Alvin";

        JSONObject json = new JSONObject();
        try {
            json.put("value", "1");
            json.put("id", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final int total = 100;
        int id = 0;

        System.out.println("Start testing... ");
        for (int i=0; i<1000; i++) {
            int sent = 0;
            HttpActiveReplicaRequest req = new HttpActiveReplicaRequest(HttpActiveReplicaPacketType.EXECUTE,
                    testServiceName,
                    id++,
                    json.toString(),
                    true,
                    false,
                    0
                    );
            AppRequest request = new AppRequest(testServiceName, json.toString(), AppRequest.PacketType.DEFAULT_APP_REQUEST, false);
            System.out.println("About to send "+i+"th request.");

            long start = System.currentTimeMillis();
            try {
                // coordinate request through GigaPaxos
                client.sendRequest(ReplicableClientRequest.wrap(req)
                        , new RequestCallback() {
                            @Override
                            public void handleResponse(Request response) {
                                System.out.println(System.currentTimeMillis()-start);
                                received = 1;
                            }
                        });

            } catch (IOException e) {
                e.printStackTrace();
                // request coordination failed

            }
            while (received < sent ) {
                Thread.sleep(500);
            }
            received = 0;
        }

        client.close();
    }
}
