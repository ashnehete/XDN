package edu.umass.cs.xdn.http;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;


@Deprecated
public class XDNHttpServer implements Runnable {

    /**
     * Current architecture:
     * client(e.g., browser) <=> app (container) <=> XDNHttpServer | XDNAgentClient <=> GigaPaxos
     *
     * FIXME: The XDNAgentClient to GigaPaxos communication can be eliminated
     */

    // FIXME: used to send GigaPaxos requests to GigaPaxos for coordination
    private static XDNAgentClient client;

    // Restful API frontend
    private static HttpServer server;

    // API server addr
    private static String apiServerAddr = "localhost";


    public XDNHttpServer(String apiAddr) throws IOException {
        client = new XDNAgentClient();

        apiServerAddr = apiAddr;

        server = HttpServer.create(new InetSocketAddress(apiServerAddr,12416), 0);
        HttpContext context = server.createContext("/");
        context.setHandler(XDNHttpServer::handleRequest);

    }

    @Override
    public void run() {
        System.out.println("Server is listening on port 12416...");
        server.start();
    }

    private static String getRequest(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = null;
        StringBuffer sb = new StringBuffer();

        try {
            while ( (line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private static void handleRequest(HttpExchange exchange) {

        try {
            InputStream is = exchange.getRequestBody();

            // System.out.println(getRequest(is));
            JSONObject req = new JSONObject(getRequest(is));

            // App value to coordinate
            // String value = req.getString("value");
            // int id = req.getInt("id");
            String serviceName = req.getString("serviceName");
            // serviceName = PaxosConfig.getDefaultServiceName();

            // System.out.println("Response:"+req.getString("value")+", ServiceName:"+serviceName);

            // FIXME: Coordinate the request with a GigaPaxos ReconfigurableAppClientAsync
            if ( client.execute(req.toString(), serviceName) ) {
                exchange.sendResponseHeaders(200, req.toString().getBytes().length);
            } else {
                // The server refuses the attempt to brew coffee with a teapot
                exchange.sendResponseHeaders(418, req.toString().getBytes().length);
            }

            // exchange.sendResponseHeaders(200, value.getBytes().length);

            OutputStream os = exchange.getResponseBody();
            os.write(req.toString().getBytes());
            os.flush();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String addr = "localhost";

        if (args.length > 0){
            addr = args[0];
        }

        Thread th = new Thread(new XDNHttpServer(addr));
        th.start();
    }


}
