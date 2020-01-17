package test;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.reconfigurationpackets.CreateServiceName;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.docker.DockerKeys;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreateServices {

    static int received = 0;

    public static void main(String[] args) throws IOException, InterruptedException, JSONException {

        String testServiceName = PaxosConfig.getDefaultServiceName()+0; // PaxosConfig.getDefaultServiceName();

        Map<String, InetSocketAddress> servers = PaxosConfig.getActives();

        if (args.length > 0) {
            testServiceName = args[0];
        }

        Set<InetSocketAddress> initGroup = new HashSet<>();
        for(String name: servers.keySet()){
            initGroup.add(servers.get(name));
        }

        XDNAgentClient client = new XDNAgentClient();

        JSONArray arr = new JSONArray();
        arr.put("");

        JSONObject json = new JSONObject();
        json.put(DockerKeys.NAME.toString(), "xdn-demo-app"+ XDNConfig.xdnServiceDecimal+"Alvin");
        json.put(DockerKeys.IMAGE_URL.toString(), "oversky710/xdn-demo-app");
        // json.put(DockerKeys.ENV.toString(), null);
        json.put(DockerKeys.PORT.toString(), 3000);

        final int sent = 1;

        client.sendRequest(new CreateServiceName(testServiceName,
                        json.toString(), initGroup),
                new RequestCallback() {
                    final long createTime = System.currentTimeMillis();
                    @Override
                    public void handleResponse(Request response) {
                        System.out.println("Response to create service name ="
                                + (response)
                                + " received in "
                                + (System.currentTimeMillis() - createTime)
                                + "ms");
                        // received += 1;
                    }
                }
        );

        while (sent < received) {
            Thread.sleep(500);
        }
    }
}
