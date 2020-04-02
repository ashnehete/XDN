package test;

import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.interfaces.RequestCallback;
import edu.umass.cs.reconfiguration.examples.AppRequest;
import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ExecuteServices {
    static int received = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        XDNAgentClient client = new XDNAgentClient();
        String testServiceName = "xdn-demo-app"+ XDNConfig.xdnServiceDecimal+"Alvin";

        JSONObject json = new JSONObject();
        try {
            json.put("value", "1");
            json.put("id", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final int sent = 1;
        
        AppRequest request = new AppRequest(testServiceName, json.toString(), AppRequest.PacketType.DEFAULT_APP_REQUEST, false);

        try {
            // coordinate request through GigaPaxos
            client.sendRequest(request
                    , new RequestCallback() {
                        @Override
                        public void handleResponse(Request response) {
                            System.out.println("Response received:"+response);
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
            // request coordination failed

        }

        while (sent < received) {
            Thread.sleep(500);
        }

        client.close();
    }
}
