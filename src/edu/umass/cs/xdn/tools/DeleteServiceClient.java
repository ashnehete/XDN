package edu.umass.cs.xdn.tools;

import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;

import java.io.IOException;

public class DeleteServiceClient {

    final String name;
    final String imageName;
    final String serviceName;

    XDNAgentClient client;

    static int received = 0;

    public DeleteServiceClient() throws IOException {
        XDNConfig.load();
        name = XDNConfig.prop.getProperty(XDNConfig.XC.NAME.toString());

        imageName = XDNConfig.prop.getProperty(XDNConfig.XC.IMAGE_NAME.toString());
        serviceName = XDNConfig.generateServiceName(imageName, name);

        client = new XDNAgentClient();
    }

    private void sendDeleteServiceRequest() throws InterruptedException, IOException {
        int sent = 1;

        client.sendRequest(new edu.umass.cs.reconfiguration.reconfigurationpackets.DeleteServiceName(serviceName),
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
    }

    private void close(){
        client.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        DeleteServiceClient deleteServiceClient = new DeleteServiceClient();
        deleteServiceClient.sendDeleteServiceRequest();
        deleteServiceClient.close();
    }
}
