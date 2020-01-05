package edu.umass.cs.xdn;

import edu.umass.cs.gigapaxos.interfaces.AppRequestParserBytes;
import edu.umass.cs.gigapaxos.interfaces.ClientMessenger;
import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.nio.interfaces.SSLMessenger;
import edu.umass.cs.reconfiguration.examples.AbstractReconfigurablePaxosApp;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.interfaces.Reconfigurable;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class XDNApp extends AbstractReconfigurablePaxosApp<String>
        implements Replicable, Reconfigurable, ClientMessenger, AppRequestParserBytes {

    private String myID;
    private SSLMessenger<?, JSONObject> messenger;

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private static String containerUrl;
    // used to propagate coordinated result to applications
    private static OkHttpClient httpClient;

    private Map<String, String> services;

    public XDNApp() {
        httpClient = new OkHttpClient();

        String cAddr = "localhost";
        if (System.getProperty("container") != null)
            cAddr = System.getProperty("container");
        containerUrl = "http://" + cAddr + "/xdnapp";

        System.out.println("Container URL is:"+containerUrl);
    }

    @Override
    public void setClientMessenger(SSLMessenger<?, JSONObject> msgr) {
        this.messenger = msgr;
        this.myID = msgr.getMyID().toString();
    }

    @Override
    public boolean execute(Request request,
                           boolean doNotReplyToClient) {
        if (request instanceof HttpActiveReplicaRequest) {
            RequestBody body = RequestBody.create(JSON, request.toString());
            okhttp3.Request req = new okhttp3.Request.Builder()
                    .url(containerUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(req).execute()) {
                // System.out.println("Received response from nodejs app:"+response);
                // System.out.println("Content:"+response.body().string());
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public String checkpoint(String name) {
        return null;
    }

    @Override
    public boolean restore(String name, String state) {
        System.out.println("Name:"+name+"\nState: "+state);
        return true;
    }

    @Override
    public boolean execute(Request request) {
        return execute(request,false);
    }

    @Override
    public Request getRequest(String s) throws RequestParseException {
        try {
            JSONObject json = new JSONObject(s);
            return new HttpActiveReplicaRequest(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static HttpActiveReplicaPacketType[] types = HttpActiveReplicaPacketType.values();

    @Override
    public Set<IntegerPacketType> getRequestTypes() {
        return new HashSet<IntegerPacketType>(Arrays.asList(types));
    }



}
