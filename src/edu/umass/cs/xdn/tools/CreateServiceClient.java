package edu.umass.cs.xdn.tools;

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
import java.util.*;

public class CreateServiceClient {

    final String serviceName;
    final String name;
    final String imageName;
    final String imageUrl;
    final int port;
    final int exposePort;

    Set<InetSocketAddress> initGroup;
    static int received = 0;

    // final private static long timeout = 30000;

    XDNAgentClient client;

    public CreateServiceClient() throws IOException {
        XDNConfig.load();

        // FIXME: get these values from XDNConfig directly
        XDNConfig.xdnDomainName = XDNConfig.prop.getProperty(XDNConfig.XC.XDN_DOMAIN_NAME.toString()) == null?
                XDNConfig.xdnDomainName :
                XDNConfig.prop.getProperty(XDNConfig.XC.XDN_DOMAIN_NAME.toString());

        name = XDNConfig.prop.getProperty(XDNConfig.XC.NAME.toString());

        imageName = XDNConfig.prop.getProperty(XDNConfig.XC.IMAGE_NAME.toString());
        imageUrl = XDNConfig.prop.getProperty(XDNConfig.XC.IMAGE_URL.toString());
        port = Integer.parseInt(XDNConfig.prop.getProperty(XDNConfig.XC.DOCKER_PORT.toString()));
        exposePort = Integer.parseInt(XDNConfig.prop.getProperty(XDNConfig.XC.PUBLIC_EXPOSE_PORT.toString()));
        serviceName = XDNConfig.generateServiceName(imageName, name);

        String initGroupString = XDNConfig.prop.getProperty(XDNConfig.XC.INIT_GROUP.toString()) == null?
                "ALL" :
                XDNConfig.prop.getProperty(XDNConfig.XC.INIT_GROUP.toString());

        Map<String, InetSocketAddress> servers = PaxosConfig.getActives();

        initGroup = new HashSet<>();
        if (initGroupString.equals("ALL")) {
            for(String name: servers.keySet()){
                initGroup.add(servers.get(name));
            }
        } else {
            String[] srvs = initGroupString.split(",");
            for (String name: srvs) {
                if (servers.containsKey(name))
                    initGroup.add(servers.get(name));
                else
                    System.err.println("The key "+name+" in INIT_GROUP does not exist.");
            }
        }

        System.out.println("initGroupString:"+initGroupString);
        System.out.println("initGroup:"+initGroup);

        client = new XDNAgentClient();
    }

    private CreateServiceName generateCreateServiceNamePacket() throws JSONException {
        JSONObject state = new JSONObject();
        state.put(DockerKeys.NAME.toString(), this.imageName);
        state.put(DockerKeys.IMAGE_URL.toString(), this.imageUrl);
        // json.put(DockerKeys.ENV.toString(), null);
        state.put(DockerKeys.PORT.toString(), this.port);
        state.put(DockerKeys.VOL.toString(), this.imageName);
        state.put(DockerKeys.PUBLIC_EXPOSE_PORT.toString(), this.exposePort);

        return new CreateServiceName(this.serviceName, state.toString(), this.initGroup);
    }

    private void close(){
        client.close();
    }

    private void sendCreateServiceName() throws JSONException, IOException, InterruptedException {
        final int sent = 1;

        CreateServiceName packet = generateCreateServiceNamePacket();

        System.out.println("About to send create service name request packet:"+packet);

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
        while (sent > received) {
            Thread.sleep(500);
        }
    }

    public static void main(String[] args) throws IOException, JSONException, InterruptedException {
        CreateServiceClient c = new CreateServiceClient();
        c.sendCreateServiceName();
        c.close();
    }
}
