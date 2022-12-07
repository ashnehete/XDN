package edu.umass.cs.xdn.tools;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class MySQLQueryClient {
    String serviceName;
    String name;
    String imageName;
    String value;
    boolean coord;
    int numReq;
    String target;
    String database;
    String query;

    int id;
    static int received = 0;

    final private static long timeout = 1000;
    private final static String formatHeader = "XDN_FORMAT:MYSQL;";

    XDNAgentClient client;

    private MySQLQueryClient() throws IOException {
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

        System.out.println("Start sending request to target: "+target+", address:"+addr);

        int sent = 0;
        for (int i=0; i<numReq; i++){
            Request result = null;
            long start = System.nanoTime(); // System.currentTimeMillis();
            if (target == null) {
                try {
                    result = client.sendRequest(getRequest(), timeout);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (result == null){
                    System.out.println(i+"th request:"+"timed out");
                    return;
                }
                // System.out.println(i+"th request:"+(System.currentTimeMillis()-start)+"ms");
                System.out.println(String.format("Request: %,.4f", (System.nanoTime()-start)/1000.0/1000.0));

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            } else {
                sent++;
                try {
                    client.sendRequest(getRequest(), addr, new RequestCallback() {
                        @Override
                        public void handleResponse(Request response) {
                            System.out.println("Response to create service name =" + (response));
                            System.out.println(String.format("Response: %,.4f", (System.nanoTime()-start)/1000.0/1000.0));
                            received += 1;
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while(received < sent ){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, JSONException {
        MySQLQueryClient c = new MySQLQueryClient();
        c.sendRequest();
        c.close();
    }
}
