package edu.umass.cs.xdn.tools;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Random;

public class ExecuteRequestClient {

    String serviceName;
    String name;
    String imageName;
    String value;
    boolean coord;
    int numReq;
    String target;

    int id;

    final private static long timeout = 1000;

    XDNAgentClient client;

    private ExecuteRequestClient() throws IOException {
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

    private void close(){
        client.close();
    }

    private HttpActiveReplicaRequest getRequest() {
        return new HttpActiveReplicaRequest(HttpActiveReplicaPacketType.EXECUTE,
                serviceName,
                id++,
                value,
                coord,
                false,
                0
                );
    }

    private void sendRequest() {
        InetSocketAddress addr = null;
        if ( target !=null ){
            addr = PaxosConfig.getActives().get(target);
        }

        System.out.println("Start sending request to address:"+addr);

        for (int i=0; i<numReq; i++){
            Request result = null;
            long start = System.currentTimeMillis();
            if (target == null)
                try {
                    result = client.sendRequest(getRequest(), timeout);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            else{
                try {
                    result = client.sendRequest(getRequest(), addr, timeout);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (result == null){
                System.out.println(i+"th request:"+"timed out");
                return;
            }
            System.out.println(i+"th request:"+(System.currentTimeMillis()-start)+"ms "+(target!=null? target: "no target speciied"));
        }
    }

    public static void main(String[] args) throws IOException {
        ExecuteRequestClient c = new ExecuteRequestClient();
        c.sendRequest();
        c.close();

    }
}
