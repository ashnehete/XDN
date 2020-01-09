package edu.umass.cs.xdn;

import edu.umass.cs.gigapaxos.interfaces.AppRequestParserBytes;
import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.reconfiguration.examples.AbstractReconfigurablePaxosApp;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.interfaces.Reconfigurable;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import edu.umass.cs.xdn.docker.DockerKeys;
import edu.umass.cs.xdn.docker.DockerService;
import edu.umass.cs.xdn.interfaces.Service;
import edu.umass.cs.xdn.util.ProcessResult;
import edu.umass.cs.xdn.util.ProcessRuntime;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * This is XDN
 */
public class XDNApp extends AbstractReconfigurablePaxosApp<String>
        implements Replicable, Reconfigurable, AppRequestParserBytes {

    private String myID;

    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private static String containerUrl;

    // used to propagate coordinated result to applications
    private static OkHttpClient httpClient;

    /**
     *
     */
    private Map<String, Service> services;

    /**
     * 
     */
    public XDNApp() {
        httpClient = new OkHttpClient();

        String cAddr = "localhost";
        if (System.getProperty("container") != null)
            cAddr = System.getProperty("container");

        containerUrl = "http://" + cAddr + XDNConfig.xdnRoute;

        System.out.println("Container URL is:"+containerUrl);

        services = new HashMap<>();
    }

    @Override
    public boolean execute(Request request,
                           boolean doNotReplyToClient) {
        if (request instanceof HttpActiveReplicaRequest) {
            HttpActiveReplicaRequest r = (HttpActiveReplicaRequest) request;
            if ( HttpActiveReplicaPacketType.EXECUTE.equals(r.getRequestType()) ) {
                RequestBody body = RequestBody.create(JSON, request.toString());
                okhttp3.Request req = new okhttp3.Request.Builder()
                        .url(containerUrl)
                        .post(body)
                        .build();

                try (Response response = httpClient.newCall(req).execute()) {
                    // System.out.println("Received response from nodejs app:"+response);
                    // System.out.println("Content:"+response.body().string());
                    ((HttpActiveReplicaRequest) request).setResponse(response.body().string());
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else if (HttpActiveReplicaPacketType.SNAPSHOT.equals(r.getRequestType())) {
                String name = r.getServiceName();
                List<String> command = getCheckpointCommand(name);
                return run(command);
            } else if (HttpActiveReplicaPacketType.RECOVER.equals(r.getRequestType())) {
                String name = r.getServiceName();
                List<String> command = getRestoreCommand(name);
                return run(command);
            }
            // else, unrecognized request type
            return false;
        }
        return false;
    }

    @Override
    public String checkpoint(String name) {
        // Checkpoint the state of the corresponding name
        return null;
    }

    @Override
    public boolean restore(String name, String state) {
        // System.out.println("Name:"+name+"\nState: "+state);
        if (services.containsKey(name)){
            // this is a registered service
            Service service = services.get(name);
        } else {
            /*
             * This is not a registered service, follow the steps to set up the service
             * 1. Extract the initial service information:
             * docker image name on Docker hub, docker runtime command, file path, etc.
             * 2. Pull the image
             * 3. Boot up the service, if succeed, then add it to runtime manager and service map, return true.
             * 4. If service boot-up failed, try to terminate some running instance to yield some resources and retry.
             * If succeed, add the service to runtime manager and service map, return true.
             * If still unable to boot-up the service, return false.
             */
            try {
                /**
                 * 1. Extract the initial service information
                 */
                JSONObject json = new JSONObject(state);
                int port = json.has(DockerKeys.PORT.toString())? json.getInt(DockerKeys.PORT.toString()): -1;
                String url = json.has(DockerKeys.IMAGE_URL.toString())? json.getString(DockerKeys.IMAGE_URL.toString()):null;
                JSONArray jEnv = json.has(DockerKeys.ENV.toString())? json.getJSONArray(DockerKeys.ENV.toString()):null;
                List<String> env = new ArrayList<>();
                if (jEnv != null){
                    for (int i=0; i<jEnv.length(); i++){
                        env.add(jEnv.getString(i));
                    }
                }

                /**
                 * 2. Pull service and boot-up
                 */
                List<String> pullCommand = getPullCommand(url);
                boolean pulled = run(pullCommand);
                if (!pulled)
                    return false;

                /**
                 * 3. Boot up the service
                 */
                List<String> startCommand = getStartCommand(name, port, env, url);
                ProcessResult result;
                try {
                    result = ProcessRuntime.executeCommand(startCommand);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    return false;
                }
                if (result == null) {
                    // TODO: retry

                } else {
                    if (result.getRetCode() != 0) {
                        // TODO: retry
                    } else {
                        String id = result.getResult().trim();
                        DockerService service = new DockerService(name, id, url, port);
                        services.put(name, service);
                        return true;
                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    // docker run -p 8080:3000 -e ADDR=172.17.0.1 -d oversky710/xdn-demo-app --ip 172.17.0.100
    private List<String> getStartCommand(String name, int port, List<String> env, String url) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");

        //FIXME: only works on cloud node
        if (port > 0){
            command.add("-p");
            command.add("80:"+port);
        }

        if (env != null ){
            for (String e:env) {
                command.add("-e");
                command.add(e);
            }
        }

        command.add("-d");
        command.add(url);

        return command;
    }

    // docker checkpoint create --leave-running=true --checkpoint-dir=/tmp/test 9ac1990b3d76 test
    private List<String> getCheckpointCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("checkpoint");
        command.add("create");
        command.add("--leave-running=true");
        command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        command.add(name);
        return command;
    }

    // docker start --checkpoint-dir=/tmp/test 9ac1990b3d76
    private List<String> getRestoreCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("start");
        command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        return command;
    }

    // docker pull oversky710/xdn-demo-app
    private List<String> getPullCommand(String url) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("pull");
        command.add(url);
        return command;
    }

    private List<String> getStopCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("stop");
        command.add(name);
        return command;
    }

    private boolean run(List<String> command) {
        ProcessResult result;
        try {
            result = ProcessRuntime.executeCommand(command);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return result.getRetCode() == 0;
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
            throw new RequestParseException(e);
        }
    }

    private static HttpActiveReplicaPacketType[] types = HttpActiveReplicaPacketType.values();

    @Override
    public Set<IntegerPacketType> getRequestTypes() {
        return new HashSet<>(Arrays.asList(types));
    }

    public static void main(String[] args) {

    }
}
