package exp;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.reconfigurationpackets.CreateServiceName;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import edu.umass.cs.xdn.docker.DockerKeys;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FRSMCreateServiceNames {

    static final int total = 1000;

    static int received = 0;

    final static String serviceNamePrefix = "test";
    final static String imageName = "xdn-test-app";
    final static String imageUrl = "oversky710/xdn-test-app";
    final static int port = 3000;
    final static int exposePort = 80;

    final static int numCloudServers = 3;

    public static void main(String[] args) throws JSONException, IOException, InterruptedException {

        XDNAgentClient client = new XDNAgentClient();

        Map<String, InetSocketAddress> servers = PaxosConfig.getActives();

        int cnt = 0;

        Map<String, InetSocketAddress> actives = PaxosConfig.getActives();

        // 3 replicas are for cloud servers, the rest are edge servers
        int size = actives.size() - numCloudServers;

        for (int i=0; i<total; i++) {
            String serviceName = XDNConfig.generateServiceName(imageName, serviceNamePrefix+i);
            Set<InetSocketAddress> initGroup = new HashSet<>();


            // initGroup.add(servers.get("AR0"));
            initGroup.add(servers.get("AR"+(numCloudServers + cnt%size)));
            for (int k=0; k<numCloudServers; k++) {
                if( cnt%numCloudServers != k ) {
                    initGroup.add(servers.get("AR" + k));
                }
            }
            cnt++;

            JSONObject state = new JSONObject();
            state.put(DockerKeys.NAME.toString(), imageName);
            state.put(DockerKeys.IMAGE_URL.toString(), imageUrl);
            // json.put(DockerKeys.ENV.toString(), null);
            state.put(DockerKeys.PORT.toString(), port);
            state.put(DockerKeys.VOL.toString(), imageName);
            state.put(DockerKeys.PUBLIC_EXPOSE_PORT.toString(), exposePort);

            CreateServiceName packet = new CreateServiceName(serviceName, state.toString(), initGroup);
            System.out.println("About to send create service name request packet:"+packet);

            received = 0;
            client.sendRequest(packet, new RequestCallback() {
                final long createTime = System.currentTimeMillis();
                @Override
                public void handleResponse(Request response) {
                    System.out.println("Response to create service name ="
                            + (response)
                            + " received in "
                            + (System.currentTimeMillis() - createTime)
                            + "ms");
                    received += 1;
                }
            });

            while (received < 1) {
                Thread.sleep(500);
            }
        }

        client.close();
    }
}
