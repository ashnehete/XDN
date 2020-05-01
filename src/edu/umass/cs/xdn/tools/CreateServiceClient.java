package edu.umass.cs.xdn.tools;

import edu.umass.cs.reconfiguration.ReconfigurableAppClientAsync;
import edu.umass.cs.reconfiguration.reconfigurationpackets.ClientReconfigurationPacket;
import edu.umass.cs.reconfiguration.reconfigurationpackets.CreateServiceName;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import edu.umass.cs.xdn.docker.DockerKeys;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class CreateServiceClient {

    String serviceName;
    String name;
    String imageName;
    String imageUrl;
    int port;

    final private static long timeout = 1000;

    XDNAgentClient client;

    public CreateServiceClient() throws IOException {
        XDNConfig.load();
        name = XDNConfig.prop.getProperty(XDNConfig.XC.NAME.toString());

        imageName = XDNConfig.prop.getProperty(XDNConfig.XC.IMAGE_NAME.toString());
        imageUrl = XDNConfig.prop.getProperty(XDNConfig.XC.IMAGE_URL.toString());
        port = Integer.parseInt(XDNConfig.prop.getProperty(XDNConfig.XC.DOCKER_PORT.toString()));

        serviceName = XDNConfig.generateServiceName(imageName, name);

        client = new XDNAgentClient();
    }

    private CreateServiceName generateCreateServiceNamePacket() throws JSONException {
        JSONObject state = new JSONObject();
        state.put(DockerKeys.NAME.toString(), this.imageName);
        state.put(DockerKeys.IMAGE_URL.toString(), this.imageUrl);
        // json.put(DockerKeys.ENV.toString(), null);
        state.put(DockerKeys.PORT.toString(), this.port);
        state.put(DockerKeys.VOL.toString(), this.imageName);
        return new CreateServiceName(serviceName, state.toString());
    }

    private void close(){
        client.close();
    }

    private void sendCreateServiceName() throws JSONException, IOException, ReconfigurableAppClientAsync.ReconfigurationException {
        long createTime = System.currentTimeMillis();
        ClientReconfigurationPacket packet = client.sendRequest(generateCreateServiceNamePacket(), timeout);
        System.out.println("Response to create service name ="
                + (packet.getResponseMessage())
                + " received in "
                + (System.currentTimeMillis() - createTime)
                + "ms");
    }

    public static void main(String[] args) throws IOException, JSONException, ReconfigurableAppClientAsync.ReconfigurationException {
        CreateServiceClient c = new CreateServiceClient();
        c.sendCreateServiceName();
        c.close();
    }
}
