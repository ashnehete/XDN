package test;

import edu.umass.cs.xdn.XDNConfig;
import edu.umass.cs.xdn.deprecated.XDNAgentClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * java -ea -cp jar/XDN-1.0.jar -Djava.util.logging.config.file=conf/logging.properties -Dlog4j.configuration=conf/log4j.properties -DgigapaxosConfig=conf/xdn.properties test.ReconfigurableServices
 */
public class ReconfigureServices {

    public static void main(String[] args) throws IOException {
        XDNAgentClient client = new XDNAgentClient();

        String testServiceName = "xdn-demo-app"+ XDNConfig.xdnServiceDecimal+"Alvin";

        JSONObject json = new JSONObject();
        try {
            json.put("value", "1");
            json.put("id", 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        final int total = 100;
        int id = 0;


    }
}
