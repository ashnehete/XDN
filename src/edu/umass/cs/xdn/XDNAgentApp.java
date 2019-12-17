package edu.umass.cs.xdn;

import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.examples.AppRequest;
import edu.umass.cs.reconfiguration.examples.noopsimple.NoopApp;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Set;

/**
 *
 */
public class XDNAgentApp implements Replicable {

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private static String containerUrl;
    // used to propagate coordinated result to applications
    private static OkHttpClient httpClient;

    public XDNAgentApp() {
        httpClient = new OkHttpClient();

        String cAddr = "localhost";
        if (System.getProperty("container") != null)
            cAddr = System.getProperty("container");
        containerUrl = "http://" + cAddr + XDNConfig.xdnRoute;

        System.out.println("Container URL is:"+containerUrl);
    }

    @Override
    public boolean execute(Request request) {
        // System.out.println("Request "+request+" has been coordinated successfully!");

        AppRequest gReq = ((AppRequest) request);
        String value = gReq.getValue();
        System.out.println(">>>>>>>>>> Value:"+value);

        String str = "";
        try {
            JSONObject json = new JSONObject(value);
            // json.put("value", gReq.getValue());
            str = json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println(">>>>>>>>>>> JSON:"+str);

        // post to app with a HttpClient
        RequestBody body = RequestBody.create(JSON, str);
        okhttp3.Request req = new okhttp3.Request.Builder()
                .url(containerUrl)
                .post(body)
                .build();

        try (Response response = httpClient.newCall(req).execute()) {

            // System.out.println("Received response from nodejs app:"+response);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Request getRequest(String stringified) throws RequestParseException {
        try {
            return NoopApp.staticGetRequest(stringified);
        } catch (RequestParseException | JSONException e) {
            // do nothing by design
        }
        return null;
    }

    @Override
    public Set<IntegerPacketType> getRequestTypes() {
        return NoopApp.staticGetRequestTypes();
    }

    @Override
    public String checkpoint(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean execute(Request request, boolean doNotReplyToClient) {
        // execute request without replying back to client

        // identical to above unless app manages its own messaging
        return this.execute(request);
    }

    @Override
    public boolean restore(String name, String state) {
        // TODO Auto-generated method stub
        return true;
    }
}
