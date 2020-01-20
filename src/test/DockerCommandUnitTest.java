package test;

import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.docker.DockerContainer;
import edu.umass.cs.xdn.docker.DockerKeys;
import edu.umass.cs.xdn.util.ProcessResult;
import edu.umass.cs.xdn.util.ProcessRuntime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class DockerCommandUnitTest {

    static Logger log = XDNConfig.log;

    // docker pull oversky710/xdn-demo-app
    private static List<String> getPullCommand(String url) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("pull");
        command.add(url);
        return command;
    }

    // docker run --name xdn-demo-app -p 8080:3000 -e ADDR=172.17.0.1 -d oversky710/xdn-demo-app --ip 172.17.0.100
    private static List<String> getRunCommand(String name, int port, List<String> env, String url) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");

        // name is unique globally, otherwise it won't be created successfully
        command.add("--name");
        command.add(name);

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

    private static boolean restore(String state, String name, String appName) throws JSONException, IOException, InterruptedException {

        JSONObject json = new JSONObject(state);
        int port = json.has(DockerKeys.PORT.toString()) ? json.getInt(DockerKeys.PORT.toString()) : -1;
        String url = json.has(DockerKeys.IMAGE_URL.toString()) ? json.getString(DockerKeys.IMAGE_URL.toString()) : null;
        JSONArray jEnv = json.has(DockerKeys.ENV.toString()) ? json.getJSONArray(DockerKeys.ENV.toString()) : null;
        List<String> env = new ArrayList<>();
        if (jEnv != null) {
            for (int i = 0; i < jEnv.length(); i++) {
                env.add(jEnv.getString(i));
            }
        }

        // 2. Pull service and boot-up
        List<String> pullCommand = getPullCommand(url);
        ProcessRuntime.executeCommand(pullCommand);

        // 3. Boot up the service
        List<String> startCommand = getRunCommand(appName, port, env, url);
        ProcessResult result = null;
        try {
            result = ProcessRuntime.executeCommand(startCommand);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        log.fine("Restore: start app instance command result is "+result);
        if (result == null) {
            // there may be an interruption, let's retry. No need to stop another instance
            try {
                result = ProcessRuntime.executeCommand(startCommand);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            assert (result != null);
            if (result.getRetCode() != 0) {
                // give up and raise an error
                return false;
            } else {
                DockerContainer container = new DockerContainer(appName, url, port, jEnv);
                // updateServiceAndApps(appName, name, container);
                log.fine(">>>>>>>>> Service name " + name + " has been created successfully after retry.");
                return true;
            }
        } else {
            if (result.getRetCode() != 0) {
                try {
                    result = ProcessRuntime.executeCommand(startCommand);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                if (result.getRetCode() == 0) {
                    DockerContainer container = new DockerContainer(appName, url, port, jEnv);
                    // updateServiceAndApps(appName, name, container);
                    log.fine(">>>>>>>>> Service name " + name + " has been created successfully after stop and retry.");
                    return true;
                }
                return false;
            } else {
                // String id = result.getResult().trim();
                DockerContainer container = new DockerContainer(appName, url, port, jEnv);
                // updateServiceAndApps(appName, name, container);
                log.fine(">>>>>>>>> Service name " + name + " has been created successfully.");
                return true;
            }
        }
    }

    public static void main(String[] args) throws JSONException, IOException, InterruptedException {

        JSONObject json = new JSONObject();
        json.put(DockerKeys.NAME.toString(), "xdn-demo-app"+ XDNConfig.xdnServiceDecimal+"Alvin");
        json.put(DockerKeys.IMAGE_URL.toString(), "oversky710/xdn-demo-app");
        json.put(DockerKeys.PORT.toString(), 3000);

        restore(json.toString(), "xdn-demo-app"+ XDNConfig.xdnServiceDecimal+"Alvin", "xdn-demo-app");

    }
}
