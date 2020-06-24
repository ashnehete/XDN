package test;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.reconfigurationpackets.CreateServiceName;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.docker.DockerKeys;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DemoCreateServices {

    String serviceName;
    String name;
    String imageName;
    String imageUrl;
    int port;
    int exposePort;
    static int received = 0;

    public static final String xdnDomainName = "xdnedge.xyz";

    public DemoCreateServices() throws IOException {
        XDNConfig.load();
        name = XDNConfig.prop.getProperty(XDNConfig.XC.NAME.toString());

        imageName = XDNConfig.prop.getProperty(XDNConfig.XC.IMAGE_NAME.toString());
        imageUrl = XDNConfig.prop.getProperty(XDNConfig.XC.IMAGE_URL.toString());
        port = Integer.parseInt(XDNConfig.prop.getProperty(XDNConfig.XC.DOCKER_PORT.toString()));
        exposePort = Integer.parseInt(XDNConfig.prop.getProperty(XDNConfig.XC.PUBLIC_EXPOSE_PORT.toString()));
        serviceName = name+'.'+imageName+'.'+xdnDomainName; // XDNConfig.generateServiceName(imageName, name);

    }

    private CreateServiceName generateCreateServiceNamePacket(Set<InetSocketAddress> initGroup) throws JSONException {
        JSONObject state = new JSONObject();
        state.put(DockerKeys.NAME.toString(), this.imageName);
        state.put(DockerKeys.IMAGE_URL.toString(), this.imageUrl);
        // json.put(DockerKeys.ENV.toString(), null);
        state.put(DockerKeys.PORT.toString(), this.port);
        state.put(DockerKeys.VOL.toString(), this.imageName);
        state.put(DockerKeys.PUBLIC_EXPOSE_PORT.toString(), this.exposePort);
        return new CreateServiceName(serviceName, state.toString(), initGroup);
    }

    public static void main(String[] args) throws IOException, InterruptedException, JSONException {
        DemoCreateServices services = new DemoCreateServices();

        Map<String, InetSocketAddress> servers = PaxosConfig.getActives();

        Set<InetSocketAddress> initGroup = new HashSet<>();
        for(String name: servers.keySet()){
            initGroup.add(servers.get(name));
            break; // only need one replica
        }

        System.out.println("InitGroup:"+initGroup);
        XDNAgentClient client = new XDNAgentClient();

        final int sent = 1;

        //client.sendRequest(new CreateServiceName(testServiceName,
        //                json.toString(), initGroup),
        client.sendRequest(services.generateCreateServiceNamePacket(initGroup),
                new RequestCallback() {
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
                }
        );

        while (sent > received) {
            Thread.sleep(500);
        }

        Thread.sleep(1000);

        System.out.println("Service name created successfully.");
        client.close();

    }
}
