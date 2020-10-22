package edu.umass.cs.xdn;

import edu.umass.cs.gigapaxos.PaxosConfig;
import edu.umass.cs.gigapaxos.interfaces.AppRequestParserBytes;
import edu.umass.cs.gigapaxos.interfaces.ClientMessenger;
import edu.umass.cs.gigapaxos.interfaces.Replicable;
import edu.umass.cs.gigapaxos.interfaces.Request;
import edu.umass.cs.gigapaxos.paxosutil.LargeCheckpointer;
import edu.umass.cs.nio.JSONPacket;
import edu.umass.cs.nio.interfaces.IntegerPacketType;
import edu.umass.cs.nio.interfaces.SSLMessenger;
import edu.umass.cs.nio.nioutils.NIOHeader;
import edu.umass.cs.reconfiguration.examples.AbstractReconfigurablePaxosApp;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import edu.umass.cs.reconfiguration.interfaces.Reconfigurable;
import edu.umass.cs.reconfiguration.reconfigurationutils.RequestParseException;
import edu.umass.cs.xdn.dns.LocalDNSResolver;
import edu.umass.cs.xdn.docker.DockerKeys;
import edu.umass.cs.xdn.docker.DockerContainer;
import edu.umass.cs.xdn.util.ProcessResult;
import edu.umass.cs.xdn.util.ProcessRuntime;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  XDNApp is a GigaPaxos application used for
 */
public class XDNApp extends AbstractReconfigurablePaxosApp<String>
        implements Replicable, Reconfigurable, AppRequestParserBytes, ClientMessenger {

    // used by execute method to post coordinated requests to underlying app
    private static final String USER_AGENT = "Mozilla/5.0";

    private String myID;
    private static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");

    private static Logger log = PaxosConfig.getLogger();

    // used to propagate coordinated result to applications
    private static OkHttpClient httpClient;

    private final boolean isLinux;

    /**
     * A map of app name to containerizedApps
     */
    private Map<String, DockerContainer> containerizedApps;

    public boolean nameExists(String name) {
        return containerizedApps.containsKey(name);
    }

    public String getIpAddrForDomainName(String name) {
        return containerizedApps.get(name).getAddr();
    }

    /**
     * A map of service name to container app name
     */
    private Map<String, String> serviceNames;

    private Set<String> runningApps;

    public boolean isRunning(String name) {
        return runningApps.contains(name);
    }

    private String gatewayIPAddress;

    private enum XDNAppKeys {
        APP,
        SERVICE_NAME
    }

    Level DEBUG_LEVEL = Level.INFO;

    private static boolean DEBUG = true;

    /**
     * 
     */
    public XDNApp() {
        httpClient = new OkHttpClient();

        gatewayIPAddress = "172.17.0.1";
        if (System.getProperty("gateway") != null)
            gatewayIPAddress = System.getProperty("gateway");

        try {
            ProcessResult r= ProcessRuntime.executeCommand(getBridgeInspectCommand());
            // a valid result is returned
            if(r.getRetCode() == 0) {
                JSONArray arr = new JSONArray(r.getResult());
                // get IP address management info from the result
                JSONObject json = arr.getJSONObject(0).getJSONObject("IPAM");
                String ipAddr = json.getJSONArray("Config").getJSONObject(0).getString("Gateway");
                if (ipAddr != null)
                    gatewayIPAddress = ipAddr;
            }
        } catch (IOException | InterruptedException | JSONException e) {
            e.printStackTrace();
        }

        isLinux = System.getProperty("os.name").equals("Linux");

        containerizedApps = new ConcurrentHashMap<>();
        // avoid throwing an exception when bootup
        containerizedApps.put(PaxosConfig.getDefaultServiceName(),
                new DockerContainer(PaxosConfig.getDefaultServiceName(),
                        null, -1, -1, null, ""));
        // TODO: change HashSet to a sorted list to track resource usage
        runningApps = new HashSet<>();

        serviceNames = new ConcurrentHashMap<>();

        // customized checkpoint directory does not work for criu restore,
        // use the default directory (/var/lib/docker/containers)

        File checkpointFolder = new File(XDNConfig.checkpointDir);
        if (!checkpointFolder.exists()) {
            boolean created = checkpointFolder.mkdir();
            if (!created)
                log.log(Level.FINE,
                        "{0} failed to create checkpoint folder!",
                        new Object[]{this});
        }

        if (XDNConfig.isEdgeNode) {
            /**
             * Check whether the current process has root privilege to bind to port 53 for DNS.
             * If not, exit as edge server must bind to port 53.
             */
            List<String> whoCommand = new ArrayList<>();
            whoCommand.add("whoami");
            ProcessResult result = null;
            try {
                result = ProcessRuntime.executeCommand(whoCommand);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

            assert (result != null);

            if ( !result.getResult().trim().equals("root") && XDNConfig.largeCheckPointerEnabled ) {
                // if largeCheckPointerEnabled is enabled but the program is not running with root privilege, log a severe error and exit, because checkpoint won't work.
                log.severe("LargeCheckpointer is enabled, must run with root privilege, please restart with root privilege.");
                System.exit(1);
            }

            // start LDNS server on the edge node
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    LocalDNSResolver resolver = new LocalDNSResolver(XDNApp.this);
                }
            };
            new Thread(runnable).start();
        }
    }

    private String getContainerUrl(String addr) {
        return "http://" + addr + XDNConfig.xdnRoute;
    }

    private String printMap(Map m){
        StringBuilder sb = new StringBuilder();
        sb.append("#############\n");
        for (Object key: m.keySet()){
            sb.append(key);
            sb.append(":");
            sb.append(m.get(key));
            sb.append("\n");
        }
        sb.append("#############\n");
        return sb.toString();
    }

    @Override
    public boolean execute(Request request,
                           boolean doNotReplyToClient) {

        log.log(DEBUG_LEVEL, "XDNApp execute request:{0}", new Object[]{request});
        if (XDNConfig.noopEnabled){

            if (request instanceof HttpActiveReplicaRequest)
                ((HttpActiveReplicaRequest) request).setResponse("");
            else {
                System.out.println("Unrecognized request type: "+request);
            }
            return true;
        }

        if (request instanceof HttpActiveReplicaRequest) {
            HttpActiveReplicaRequest r = (HttpActiveReplicaRequest) request;
            String name = r.getServiceName();
            String containerUrl = null;
            // TODO: check whether app is running, if not, return false
            if (serviceNames.containsKey(name) && containerizedApps.containsKey(serviceNames.get(name))) {
                DockerContainer dc = containerizedApps.get(serviceNames.get(name));
                // Note, this url only works on Linux. Since MacOS does not have docker0 bridge running on the host machine, therefore, the following implementation won't work for MacOS
                // {@url https://docs.docker.com/docker-for-mac/networking/#:~:text=There%20is%20no%20docker0%20bridge,docker0%20interface%20on%20the%20host}
                if(isLinux)
                    containerUrl = getContainerUrl(dc.getAddr()+":"+dc.getPort());
                else
                    containerUrl = getContainerUrl("localhost:"+dc.getExposePort());

                log.log(DEBUG_LEVEL, "############## Container URL:+{0}+\n DockerContainer:{1}\n containerizedApps:{2}\n serviceNames:{3}",
                        new Object[]{containerUrl, dc, printMap(containerizedApps), printMap(serviceNames)});
            }

            if (containerUrl == null)
                return false;

            log.log(DEBUG_LEVEL,"Execute request {0} for service name {1} running at address {2}",
                    new Object[]{r, name, containerUrl});


            if ( HttpActiveReplicaPacketType.EXECUTE.equals(r.getRequestType()) ) {
                // use HttpURLConnection to maintain a persistent connection with underlying HTTP app automatically
                URL url = null;
                try {
                    url = new URL(containerUrl);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("POST");
                    con.setRequestProperty("User-Agent", USER_AGENT);
                    con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    con.setRequestProperty("Accept", "application/json");
                    con.setDoOutput(true);
                    OutputStream os = con.getOutputStream();
                    os.write(r.toString().getBytes());
                    os.flush();
                    os.close();

                    int responseCode = con.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) { //success
                        BufferedReader in = new BufferedReader(new InputStreamReader(
                                con.getInputStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();

                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        // TODO: check whether the request comes from HttpActiveReplica
                        ((HttpActiveReplicaRequest) request).setResponse(response.toString());
                        log.log(Level.INFO, "{0} received response from underlying app {1}: {2}",
                                new Object[]{this, name, response});

                        return true;

                    } else {
                        return false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


            }

        }
        return false;
    }

    @Override
    public String checkpoint(String name) {
        if (DEBUG)
            return "";

        long start = System.currentTimeMillis();

        log.log(Level.INFO, ">>>>>>> Checkpoint ServiceName={0}", new Object[]{name});

        if (name.equals(PaxosConfig.getDefaultServiceName())){
            // return empty string for the default app
            return "";
        }
        // handle checkpoint for XDNApp's default user name, which represents a unique device, e.g., XDNApp0_AlvinRouter
        else if (name.startsWith(PaxosConfig.getDefaultServiceName())){
            /*
              Checkpoint XDN Agent state of this specific node: serviceNames and containerizedApps.
              Though containerizedApps can be retrieved from docker command, it's better to keep a copy by XDN itself.
             */
            JSONObject xdnState = new JSONObject();
            JSONObject apps = new JSONObject();
            JSONObject services = new JSONObject();
            try {
                for (String appName: containerizedApps.keySet()){
                        apps.put(appName, containerizedApps.get(appName).toJSONObject());
                }
                xdnState.put(XDNAppKeys.APP.toString(), apps);

                for (String serviceName: serviceNames.keySet()) {
                    services.put(serviceName, serviceNames.get(serviceName));
                }
                xdnState.put(XDNAppKeys.SERVICE_NAME.toString(), services);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return xdnState.toString();
        }

        // Now let's handle app state and user state event
        if (XDNConfig.largeCheckPointerEnabled) {
            // String[] nameResult = XDNConfig.extractNamesFromServiceName(name);
            // String userName = nameResult[0];
            // String appName = nameResult[1];
            String appName = containerizedApps.get(name).getName();

            log.log(Level.FINE,
                    ">>>>>>>> About to checkpoint for appName:{0}",
                    new Object[]{appName});

            if (XDNConfig.volumeCheckpointEnabled) {
                // checkpoint volume
                String volume = getVolumeDir(appName);
                List<String> tarCommand = getTarCommand(appName + ".tar.gz", volume, XDNConfig.checkpointDir);
                // assert (run(tarCommand));
                run(tarCommand);
                File cp = new File(XDNConfig.checkpointDir + appName + ".tar.gz");

                String chkp = LargeCheckpointer.createCheckpointHandle(cp.getAbsolutePath());
                // String chkp = cp.getAbsolutePath();
                JSONObject json = null;

                json = DockerContainer.dockerToJsonState(containerizedApps.get(serviceNames.get(name)));

                JSONObject checkpointJson = null;
                try {
                    checkpointJson = new JSONObject(chkp);
                    Iterator key = checkpointJson.keys();
                    while(key.hasNext()){
                        String k = (String) key.next();
                        json.put(k, checkpointJson.get(k));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                log.log(Level.FINE,
                        ">>>>>>>>> Checkpoint volume: {0}",
                        new Object[]{json});
                return json.toString();

            } else {
                // use {@link LargeCheckpointer} to checkpoint
                List<String> checkpointListCommand = getCheckpointListCommand(appName);
                ProcessResult result = null;
                try {
                    result = ProcessRuntime.executeCommand(checkpointListCommand);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                assert (result != null);

                // checkpoint exists
                boolean exists = result.getResult().contains(appName);
                if (exists) {
                    // remove the previous checkpoint
                    List<String> checkpointRemoveCommand = getCheckpointRemoveCommand(appName);

                    if (!run(checkpointRemoveCommand)) {
                        // checkpoint has not been removed successfully
                        return null;
                    }
                }

                List<String> checkpointCreateCommand = getCheckpointCreateCommand(appName, true);
                if (!run(checkpointCreateCommand)) {
                    // checkpoint container failed
                    return null;
                }
                // assert(cp.exists());

                // Note: this only works with root privilege
                String image = XDNConfig.defaultCheckpointDir + containerizedApps.get(appName).getID() + "/checkpoints/" + appName;
                List<String> tarCommand = getTarCommand(appName + ".tar.gz", image, XDNConfig.checkpointDir);
                assert (run(tarCommand));
                File cp = new File(XDNConfig.checkpointDir + appName + ".tar.gz");

                String chkp = LargeCheckpointer.createCheckpointHandle(cp.getAbsolutePath());
                JSONObject json = null;

                json = DockerContainer.dockerToJsonState(containerizedApps.get(serviceNames.get(name)));

                JSONObject checkpointJson = null;
                try {
                    checkpointJson = new JSONObject(chkp);
                    Iterator key = checkpointJson.keys();
                    while(key.hasNext()){
                        String k = (String) key.next();
                        json.put(k, checkpointJson.get(k));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                log.log(Level.FINE,
                        "Checkpoint: LargeCheckpointer {0}",
                        new Object[]{json});

                return json.toString();
            }

        } else {
            // TODO: UNTESTED PATH
            // send checkpoint request to the underlying app
            String containerUrl = null;
            if (serviceNames.containsKey(name) && containerizedApps.containsKey(serviceNames.get(name)))
                containerUrl = getContainerUrl(containerizedApps.get(serviceNames.get(name)).getAddr());

            if (containerUrl == null)
                return null;

            // send checkpoint request to the underlying app
            JSONObject json = new JSONObject();
            try {
                json.put(HttpActiveReplicaRequest.Keys.NAME.toString(), name);
                json.put(JSONPacket.PACKET_TYPE.toUpperCase(), HttpActiveReplicaPacketType.SNAPSHOT.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestBody body = RequestBody.create(JSON, json.toString());
            okhttp3.Request req = new okhttp3.Request.Builder()
                    .url(containerUrl)
                    .post(body)
                    .build();

            try (Response response = httpClient.newCall(req).execute()) {
                log.log(Level.INFO,
                        "Checkpoint: received response from app:{0}",
                        new Object[]{response});
                // System.out.println("Content:"+response.body().string());

                return response.body()!=null? response.body().string(): "";
            } catch (IOException e) {
                e.printStackTrace();
            }
            log.log(Level.WARNING,
                    "Checkpoint: something wrong with underlying app, no checkpoint is taken.");
            long elapsed = System.currentTimeMillis() - start;
            System.out.println(">>>>>>> It takes "+elapsed+"ms to checkpoint.");
            // underlying app may not implement checkpoint, return an empty string as a checkpoint
            return "";
        }
    }

    /**
     * name is the serviceName, state is the state associated with the serviceName
     * If state is null, that means we are about to delete the serviceName.
     * Otherwise we need to restore the state for serviceName.
     */
    @Override
    public boolean restore(String name, String state) {

        // String appName = name.split(XDNConfig.xdnServiceDecimal)[0];
        String appName = name;
        // FIXME: don not derive appName based on name (serviceName)
        if (name.contains(XDNConfig.xdnDomainName)){
            String[] nameResult = XDNConfig.extractNamesFromServiceName(name);
            // String userName = nameResult[0];
            appName = nameResult[1];
        }

        log.log(DEBUG_LEVEL,
                ">>>>>> Restore request:  Name: {0}\nAppName: {1}\nState: {2}",
                new Object[]{name, appName, state});

        // handle XDN service restore
        if (name.equals(PaxosConfig.getDefaultServiceName())){
            // Don't do any thing, this is the default app
            return true;
        } else if (name.startsWith(PaxosConfig.getDefaultServiceName())){
            // this is the device service name, restore serviceNames and containerizedApps
            if (state == null) {
                // clean up the state of this XDN app
                containerizedApps.clear();
                serviceNames.clear();
            } else {
                try {
                    JSONObject xdnState = new JSONObject(state);
                    JSONObject apps = xdnState.getJSONObject(XDNAppKeys.APP.toString());
                    Iterator<?> iter = apps.keys();
                    while(iter.hasNext()) {
                        String containerizedAppName = iter.next().toString();
                        containerizedApps.put(containerizedAppName,
                                new DockerContainer(apps.getJSONObject(containerizedAppName)));
                    }

                    JSONObject services = xdnState.getJSONObject(XDNAppKeys.SERVICE_NAME.toString());
                    iter = services.keys();
                    while(iter.hasNext()){
                        String serviceName = iter.next().toString();
                        serviceNames.put(serviceName, services.getString(serviceName));
                    }

                    return true;
                } catch (JSONException e) {
                    // unable to process this restore event, quit and raise an error
                    return false;
                }
            }
        }

        log.log(DEBUG_LEVEL,
                ">>>>>>>> XDN containerized app to restore:{0}\n >>>>>>>> serviceNames:{1}\n>>>>>>>> runningApps:{2}",
                new Object[]{name, serviceNames, runningApps});

        // Handle serviceName (name) restore
        if (serviceNames.containsKey(name)){
            // if service name exists, app name must also exist
            assert(containerizedApps.containsKey(appName));
            // this is a registered service name
            DockerContainer container = containerizedApps.get(appName);
            assert (container != null);

            if ( state == null ){
                // we do not need to remove name from serviceNames table
                // serviceNames.remove(name);
                // remove pointer from service name list in containerizedApps, must succeed
                boolean cleared = container.removeServiceName(name);
                // serviceName must be successfully removed from service list in container class
                assert (cleared);
                if (container.isEmpty()) {
                    // If service list is empty, we need to clean up the container state on this node
                    long stopTime = System.currentTimeMillis();
                    // Stop the container
                    List<String> stopCommand = getStopCommand(container.getName());
                    boolean stopped = run(stopCommand);
                    // container must be stopped successfully
                    assert (stopped);
                    System.out.println(" >>>>>>>>> It takes "+(System.currentTimeMillis() - stopTime)+"ms to stop app "+container.getName());

                    // Remove the container's checkpoint
                    /*
                    List<String> removeCheckpointCommand = getCheckpointRemoveCommand(container.getName());
                    boolean removed = run(removeCheckpointCommand);
                    assert (removed);
                    */

                    // We do not want to remove the image as in the future, we may still need to use it
                    /**
                    List<String> removeImageCommand = getRemoveImageCommand(container.getUrl());
                    removed = run(removeImageCommand);
                    assert (removed);
                     */

                    // Note: we must keep the docker info in containerizedApps
                    // Because the associated information can only be acquired upon service name creation
                    // containerizedApps.remove(appName);

                    runningApps.remove(appName);
                }
                return true;
            } else {
                System.out.println(">>>>>>>>>>>>>>>>>>> Restore from a non-empty state: "+state);

                // restore from a checkpoint which is either a docker checkpoint or a volume checkpoint
                if (XDNConfig.largeCheckPointerEnabled) {

                    // If the instance is running, stop and prepare to restart
                    if (runningApps.contains(appName)) {
                        List<String> stopCommand = getStopCommand(appName);
                        assert (run(stopCommand));
                        List<String> rmCommand = getRemoveCommand(appName);
                        assert (run(rmCommand));
                    }

                    String dest;
                    long checkpointTime = System.currentTimeMillis();

                    if (XDNConfig.volumeCheckpointEnabled){
                        // checkpoint the external volume
                        // If the instance is running, stop and prepare to restart
                        dest = getVolumeDir(appName);
                    } else {
                        // checkpoint docker directly
                        /*
                        Here is the restore complexity:
                        Current version of docker (19.03) does not support restore from a customized directory.
                        Therefore, we have to copy it to the corresponding location of the docker image.
                        */
                        dest = XDNConfig.defaultCheckpointDir + containerizedApps.get(appName).getID() + "/checkpoints/";
                    }


                    String filename = XDNConfig.checkpointDir + appName + ".tar.gz";
                    File cp = new File(filename);
                    LargeCheckpointer.restoreCheckpointHandle(state, cp.getAbsolutePath());
                    List<String> unTarCommand = getUntarCommand(filename, dest);
                    assert (run(unTarCommand));
                    // System.out.println(">>>>>>>>> It takes "+(System.currentTimeMillis()-checkpointTime)+"ms to get checkpoint for app "+container.getName());
                    log.log(DEBUG_LEVEL, ">>>>>>>>> It takes {0}ms to get checkpoint for app {1}",
                            new Object[]{(System.currentTimeMillis()-checkpointTime), container.getName()});

                    long startTime = System.currentTimeMillis();
                    List<String> startCommand = getStartCommand(appName);
                    assert (run(startCommand));
                    DockerContainer c = containerizedApps.get(appName);
                    // System.out.println(" >>>>>>>>> It takes "+(System.currentTimeMillis()-startTime)+"ms to start app "+container.getName());

                    updateServiceAndApps(appName, name, c);

                } else {
                    // TODO: UNTESTED PATH
                    // send restore request to the underlying app
                    String containerUrl = null;
                    if (serviceNames.containsKey(name) && containerizedApps.containsKey(serviceNames.get(name)))
                        containerUrl = getContainerUrl(containerizedApps.get(serviceNames.get(name)).getAddr());

                    if (containerUrl == null)
                        return false;

                    // send checkpoint request to the underlying app
                    JSONObject json = new JSONObject();
                    try {
                        json.put(HttpActiveReplicaRequest.Keys.NAME.toString(), name);
                        json.put(JSONPacket.PACKET_TYPE.toUpperCase(), HttpActiveReplicaPacketType.RECOVER.toString());
                        json.put(HttpActiveReplicaRequest.Keys.QVAL.toString(), state);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    RequestBody body = RequestBody.create(JSON, json.toString());
                    okhttp3.Request req = new okhttp3.Request.Builder()
                            .url(containerUrl)
                            .post(body)
                            .build();

                    try (Response response = httpClient.newCall(req).execute()) {
                        log.log(DEBUG_LEVEL,
                                "Restore: received response from app: {0}",
                                new Object[]{response});

                        return response.code()==200;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // underlying app may not implement checkpoint, return an empty string as a checkpoint
                }
                return true;
            }

        } else {
            log.log(DEBUG_LEVEL,
                    "Restore: service name {0} does not exist.",
                    new Object[]{name});

            assert(state != null);
            JSONObject json = null;
            try {
                json = new JSONObject(state);
                appName = json.getString(DockerKeys.NAME.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            log.log(DEBUG_LEVEL, "########## JSON from state:{0}", new Object[]{state});

            /*
             * This is not a registered service name, follow the steps to set up the service if the app does not exist yet
             * 1. Extract the initial service information:
             * docker image name on Docker hub, docker runtime command, file path, etc.
             * 2. Pull the image
             * 3. Boot up the service, if succeed, then add it to runtime manager and service map, return true.
             * 4. If service boot-up failed, try to terminate some running instance to yield some resources and retry.
             * If succeed, add the service to runtime manager and service map, return true.
             * If still unable to boot-up the service, return false.
             * 5. restore user state
             */
            if ( !containerizedApps.containsKey(appName) ) {
                try {
                    assert (json != null);
                    // 1. Extract the initial service information
                    log.log(DEBUG_LEVEL,
                            "Restore: app {0} does not exist, restore from a new image.",
                            new Object[]{appName});

                    DockerContainer dockerContainer = null;
                    try {
                        dockerContainer = DockerContainer.stateToDockerContainer(json);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    log.log(DEBUG_LEVEL, "########## Docker from JSON {0}",
                            new Object[]{dockerContainer});

                    int port = dockerContainer.getPort();
                    String url = dockerContainer.getUrl();
                    String vol = dockerContainer.getVolume();
                    int exposePort = dockerContainer.getExposePort();
                    JSONArray jEnv = dockerContainer.getEnv();

                    List<String> env = new ArrayList<>();
                    if (jEnv != null) {
                        for (int i = 0; i < jEnv.length(); i++) {
                            env.add(jEnv.getString(i));
                        }
                    }

                    log.log(DEBUG_LEVEL, "\n >>>>>>>>>> container info: name={0},state={1},json={2}\n",
                            new String[]{name, state, json.toString()});

                    // 2. Pull service and boot-up
                    List<String> pullCommand = getPullCommand(url);
                    ProcessRuntime.executeCommand(pullCommand);

                    // 3. Boot up the service
                    List<String> startCommand = getRunCommand(appName, port, exposePort, env, url, vol);
                    ProcessResult result = null;
                    try {
                        result = ProcessRuntime.executeCommand(startCommand);
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }

                    // need to start the docker first, then download the checkpoint
                    if (XDNConfig.largeCheckPointerEnabled) {

                        if (XDNConfig.volumeCheckpointEnabled){
                            // Check whether it's a large checkpoint
                            if(LargeCheckpointer.isCheckpointHandle(json.toString())){
                                String dest = getVolumeDir(appName);
                                String filename = XDNConfig.checkpointDir + appName + ".tar.gz";
                                log.log(DEBUG_LEVEL,
                                        "Extract state from volume {0} to {1}",
                                        new Object[]{filename, dest});

                                File cp = new File(filename);
                                LargeCheckpointer.restoreCheckpointHandle(state, cp.getAbsolutePath());
                                List<String> unTarCommand = getUntarCommand(filename, dest);
                                assert (run(unTarCommand));

                                // state is restored successfully, we need to restart the docker
                                List<String> restartCommand = getRestartCommand(appName);
                                run(restartCommand);
                            } // else: new state
                            else {
                                // This is OK as it only happens when the service name is first time created
                                log.log(DEBUG_LEVEL, "Not a valid checkpoint {0}", new Object[]{json});
                            }
                        } else {
                            if(LargeCheckpointer.isCheckpointHandle(json.toString())){
                                String dest = XDNConfig.defaultCheckpointDir + containerizedApps.get(appName).getID() + "/checkpoints/";
                                String filename = XDNConfig.checkpointDir + appName + ".tar.gz";
                                log.log(DEBUG_LEVEL,
                                        "Extract state from checkpoint {0} to {1}",
                                        new Object[]{filename, dest});
                                File cp = new File(filename);
                                LargeCheckpointer.restoreCheckpointHandle(state, cp.getAbsolutePath());
                                List<String> unTarCommand = getUntarCommand(filename, dest);
                                assert (run(unTarCommand));

                                // state is restored successfully, we need to restart the docker
                                List<String> restartCommand = getRestartCommand(appName);
                                run(restartCommand);
                            }
                            else {
                                // This is OK as it only happens when the service name is first time created
                                log.log(DEBUG_LEVEL, "Not a valid checkpoint {0}",
                                        new Object[]{json});
                            }
                        }


                    }

                    log.log(DEBUG_LEVEL, "Restore: start app instance command result is {0}",
                            new Object[]{result});
                    if (result == null) {
                        // there may be an interruption, let's retry.
                        try {
                            result = ProcessRuntime.executeCommand(startCommand);
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                        assert (result != null);
                        if (result.getRetCode() != 0) {
                            // give up and raise an error
                            log.log(Level.SEVERE, "unable to restart app for service name {0}: {1}",
                                    new Object[]{name, startCommand.toString()});
                            return false;
                        } else {
                            DockerContainer container = new DockerContainer(appName, url, port, exposePort, jEnv, vol);
                            updateServiceAndApps(appName, name, container);
                            log.log(DEBUG_LEVEL,
                                    ">>>>>>>>> Service name {0} has been created successfully after retry.\n appName: {1}",
                                    new Object[]{name, appName});
                            return true;
                        }
                    } else {
                        if (result.getRetCode() != 0) {
                            // error or no enough resource, stop an unused container and retry
                            if (!selectAndStopContainer()){
                                // if no container is stopped , then give up and return an error
                                return false;
                            }
                            try {
                                result = ProcessRuntime.executeCommand(startCommand);
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                            if (result.getRetCode() == 0 ) {
                                DockerContainer container = new DockerContainer(appName, url, port, exposePort, jEnv, vol);
                                updateServiceAndApps(appName, name, container);
                                log.log(Level.FINE,
                                        ">>>>>>>>> Service name {0} has been created successfully after stop and retry.\n appName: {1}",
                                        new Object[]{name, appName});
                                return true;
                            }
                            return false;
                        } else {
                            // String id = result.getResult().trim();
                            DockerContainer container = new DockerContainer(appName, url, port, exposePort, jEnv, vol);
                            updateServiceAndApps(appName, name, container);
                            log.log(DEBUG_LEVEL,
                                    ">>>>>>>>> Service name {0} has been created successfully.\n appName:{1}",
                                    new Object[]{name, appName});
                            return true;
                        }

                    }

                } catch (Exception e) {
                    // catch all exceptions
                    e.printStackTrace();
                }
                return false;
            } else if ( !runningApps.contains(appName) ) {

                // there is already an app instance, if it's not running, boot it up
                List<String> startCommand = getStartCommand(appName);
                assert (run(startCommand));
                DockerContainer c = containerizedApps.get(appName);

                updateServiceAndApps(appName, name, c);
                return true;
            } else {

                DockerContainer c = containerizedApps.get(appName);
                updateServiceAndApps(appName, name, c);
                // c.addServiceName(name);
                // containerizedApps.put(appName, c);
                log.log(Level.INFO, "App {0} is already running, no need to spawn or restart the app.",
                        new Object[]{appName});
            }

        }

        return true;
    }

    /**
     * The method needs to be synchronized because we have a few map to update
     */
    private synchronized void updateServiceAndApps(String appName, String name, DockerContainer container){
        if (container != null) {
            List<String> inspectCommand = getInspectCommand(appName);
            try {
                ProcessResult result = ProcessRuntime.executeCommand(inspectCommand);
                JSONArray arr = new JSONArray(result.getResult());
                JSONObject json = arr.getJSONObject(0);
                // "id"
                String id = json.getString("Id");
                container.setID(id);
                // "NetworkSettings" -> "IPAddress"
                String ipAddr = json.getJSONObject("NetworkSettings").getString("IPAddress");
                container.setAddr(ipAddr);
            } catch (Exception e) {
                e.printStackTrace();
            }
            container.addServiceName(name);
            containerizedApps.put(appName, container);
            runningApps.add(appName);
        }
        serviceNames.put(name, appName);
    }

    private boolean selectAndStopContainer() {
        // select a container to stop
        Iterator<String> iter = runningApps.iterator();
        // TODO: select a running container based on the scheduling policy (rather than the first one) and stop it
        if (iter.hasNext()){
            DockerContainer container = containerizedApps.get(iter.next());
            List<String> stopCommand = getStopCommand(container.getName());
            return run(stopCommand);
        }
        return false;
    }

    /* ========================= docker command ============================ */
    /**
     * Inspect a docker to get the information such as ip address
     */
    private List<String> getInspectCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("inspect");
        command.add(name);
        return command;
    }

    private List<String> getBridgeInspectCommand() {
        List<String> command = new ArrayList<>();

        command.add("docker");
        command.add("network");
        command.add("inspect");
        // inspect the virtual bridge
        command.add("bridge");

        return command;
    }

    /**
     * Run command is the command to run a docker for the first time
     */
    // docker run --name xdn-demo-app -p 8080:3000 -e ADDR=172.17.0.1 -d oversky710/xdn-demo-app --ip 172.17.0.100
    private List<String> getRunCommand(String name, int port, int exportPort, List<String> env, String url, String vol) {
        return getRunCommand(name, port, exportPort, env, url, vol, 2.0, 8);
    }

    private List<String> getRunCommand(String name, int port, int exportPort,
                                       List<String> env, String url, String vol,
                                       double cpus, int memory) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");

        // name is unique globally, otherwise it won't be created successfully
        command.add("--name");
        command.add(name);

        if (vol != null) {
            command.add("-v");
            command.add(vol+":/tmp");
        }

        // FIXME: cpu and memory limit
        if(cpus > 0) {
            // command.add("--cpus=\"" + cpus + "\"");
            command.add("--cpuset-cpus=0-3");
        }
        // command.add("-m");
        // command.add("8G");

        //FIXME: only works on cloud node
        if (port > 0){
            command.add("-p");
            command.add(exportPort+":"+port);
        }

        if (env != null ){
            for (String e:env) {
                command.add("-e");
                command.add(e);
            }
        }

        command.add("-e");
        command.add("HOST="+gatewayIPAddress);

        command.add("-e");
        command.add("HOSTNAME="+myID);

        command.add("-d");
        command.add(url);

        return command;
    }

    private List<String> getCheckpointCreateCommand(String name){
        return getCheckpointCreateCommand(name, false);
    }

    // docker checkpoint create --leave-running=true --checkpoint-dir=/tmp/test name test
    private List<String> getCheckpointCreateCommand(String name, boolean leaveRunning) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("checkpoint");
        command.add("create");
        if (leaveRunning)
            command.add("--leave-running=true");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        command.add(name);
        return command;
    }

    private List<String> getCheckpointRemoveCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("checkpoint");
        command.add("rm");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        command.add(name);
        return command;
    }

    private List<String> getCheckpointListCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("checkpoint");
        command.add("ls");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);

        return command;
    }

    List<String> getRemoveImageCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("image");
        command.add("rm");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        return command;
    }

    /**
     * Start command is the command to start a docker when the previous docker has not been removed
     */
    // docker start --checkpoint=xdn-demo-app xdn-demo-app
    private List<String> getStartCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("start");
        // command.add("--checkpoint-dir="+XDNConfig.checkpointDir+name);
        command.add(name);
        return command;
    }

    // docker restart xdn-demo-app
    private List<String> getRestartCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("restart");
        // restart the docker immediately
        command.add("-t");
        command.add("0");
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
        command.add("-t");
        command.add("0"); // time to kill docker immediately
        command.add(name);
        return command;
    }

    private List<String> getRemoveCommand(String name) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("rm");
        command.add(name);
        return command;
    }

    /* ================== End of docker command ==================== */

    private List<String> getTar() {
        List<String> command = new ArrayList<>();
        command.add("sudo");
        command.add("tar");
        return command;
    }

    private List<String> getTarCommand(String filename, String path, String dest) {
        List<String> command = getTar();
        command.add("zcf");
        command.add(dest+"/"+filename);
        command.add("-C");
        command.add(path);
        command.add(".");
        return command;
    }

    private List<String> getUntarCommand(String filename, String dest) {
        List<String> command = getTar();
        command.add("zxf");
        command.add(filename);
        command.add("-C");
        command.add(dest);
        return command;
    }

    private boolean run(List<String> command) {
        log.log(Level.FINE, "Command: {0}", new Object[]{command});
        ProcessResult result;
        try {
            result = ProcessRuntime.executeCommand(command);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        log.log(Level.FINE, "Command return value: {0}", new Object[]{result});
        return result.getRetCode() == 0;
    }

    private String getVolumeDir(String appName) {
        return XDNConfig.defaultVolumeDir+appName+"/_data/";
    }

    @Override
    public boolean execute(Request request) {
        return execute(request,false);
    }

    @Override
    public Request getRequest(byte[] message, NIOHeader header)
            throws RequestParseException{
        try {
            return new HttpActiveReplicaRequest(message);
        } catch (UnsupportedEncodingException | UnknownHostException e) {
            e.printStackTrace();
            return this.getRequest(new String(message));
        }
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

    // only need to set id
    @Override
    public void setClientMessenger(SSLMessenger<?, JSONObject> msgr) {
        this.myID = msgr.getMyID().toString();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()+"("+myID+")";
    }

    public static void main(String[] args) {

    }
}
