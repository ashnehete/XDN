package edu.umass.cs.xdn.tools;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import edu.umass.cs.xdn.request.XDNAppHttpRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

public class EtherpadFileCreatorClient {

    String serviceName;
    String name;
    String imageName;
    String value;
    boolean coord;
    int numReq;
    String target;

    int id;
    static int received = 0;

    final private static long timeout = 1000;
    XDNAgentClient client;

    private EtherpadFileCreatorClient() throws IOException{
        XDNConfig.load();
        name = XDNConfig.prop.getProperty(XDNConfig.XC.NAME.toString());
        imageName = XDNConfig.prop.getProperty(XDNConfig.XC.IMAGE_NAME.toString());
        value = XDNConfig.prop.getProperty(XDNConfig.XC.VALUE.toString());
        coord = Boolean.parseBoolean(XDNConfig.prop.getProperty(XDNConfig.XC.COORD.toString()));

        numReq = 5;
        if ( XDNConfig.prop.getProperty(XDNConfig.XC.NUM_REQ.toString()) != null)
            numReq = Integer.parseInt(XDNConfig.prop.getProperty(XDNConfig.XC.NUM_REQ.toString()));

        target = null;
        if( XDNConfig.prop.getProperty(XDNConfig.XC.TARGET.toString()) != null)
            target = XDNConfig.prop.getProperty(XDNConfig.XC.TARGET.toString());


        serviceName = XDNConfig.generateServiceName(imageName, name);

        id = (new Random()).nextInt();

        client = new XDNAgentClient();
    }

    private HttpActiveReplicaRequest getRequest(String apiKey) throws Exception {
        // Description of file that will be uploaded
        String padName = "test-xdn";
        String jsonPayload = "{\"apikey\": \"" + apiKey + "\", \"padID\": \" " + padName + " \", \"text\": \"this is written by xdn's client\"}";
        byte[] fileContent = Base64.getEncoder().encode(jsonPayload.getBytes());

        // Http request that will be forwarded to replica
        XDNAppHttpRequest r = new XDNAppHttpRequest();
        r.setPath("/api/1.2.15/createPad");
        r.setMethod("POST");
        r.addHeader("Content-Type", "application/json");
        r.setPayload(fileContent);

        return new HttpActiveReplicaRequest(
            HttpActiveReplicaPacketType.EXECUTE,
            serviceName,
            id++,
            r.toJSONString(),
            coord,
            false,
            0
        );
    }

    private void sendRequest(String apikey) {
        InetSocketAddress addr = null;
        if ( target !=null ){
            addr = PaxosConfig.getActives().get(target);
        }

        System.out.println("EtherpadFileCreatorClient:>> Sending request to target: "+target+", address:"+addr);
        Request result = null;
        try {
            result = client.sendRequest(getRequest(apikey), timeout);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (result == null){
            System.out.println("EtherpadFileCreatorClient:>> Request timed out");
        }
    }

    private void close() {
        client.close();
    }

    public static void main(String[] args) throws IOException {
        EtherpadFileCreatorClient c = new EtherpadFileCreatorClient();
        c.sendRequest(args[0]); // the first args is apikey
        c.close();
    }
}
