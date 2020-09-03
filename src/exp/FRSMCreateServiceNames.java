package exp;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.reconfigurationpackets.CreateServiceName;
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

    static final int total = 10;

    static int received = 0;

    final static String serviceNamePrefix = "test";
    final static String imageName = "xdn-test-app";
    final static String imageUrl = "oversky710/xdn-test-app";
    final static int port = 3000;
    final static int exposePort = 80;

    public static void main(String[] args) throws JSONException, IOException, InterruptedException {

        XDNAgentClient client = new XDNAgentClient();

        Map<String, InetSocketAddress> servers = PaxosConfig.getActives();

        int cnt = 0;

        for (int i=0; i<total; i++) {
            String serviceName = serviceNamePrefix+i;
            Set<InetSocketAddress> initGroup = new HashSet<>();

            // FIXME: works only for config file conf/exp/test.properties on Sep 3rd, 2020
            initGroup.add(servers.get("AR0"));
            for (int k=0; k<3; k++) {
                if( cnt%3 != k ) {
                    initGroup.add(servers.get("AR" + (k+1)));
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
