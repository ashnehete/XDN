package edu.umass.cs.xdn.tools;

import edu.umass.cs.reconfiguration.http.HttpActiveReplicaPacketType;
import edu.umass.cs.reconfiguration.http.HttpActiveReplicaRequest;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpBenchmarkClient {
    private static final String USER_AGENT = "Mozilla/5.0";

    private static String TEST_URL = "http://localhost:3000";
    private static String NAME = "";

    private static String post(JSONObject json){
        StringBuilder response = new StringBuilder();

        URL url = null;
        try {
            url = new URL(TEST_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", USER_AGENT);
            con.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            OutputStream os = con.getOutputStream();
            os.write(json.toString().getBytes());
            os.flush();
            os.close();



            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { //success
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
            }
            // System.out.println(response.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response.toString();
    }

    public static void main(String[] args){
        if (args.length == 1){
            TEST_URL = args[0];
        } else if (args.length == 2){
            TEST_URL = args[0];
            NAME = args[1];
        }

        JSONObject json = new JSONObject();
        try {
            json.put("value", "1");
            json.put("id", 0);
            json.put("name", NAME);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        for (int i =0; i<1100; i++) {
            long start = System.nanoTime();
            post(json);
            long elapsed = System.nanoTime() - start;
            System.out.println(String.format("%,.4f", elapsed/1000.0/1000.0));
        }
    }
}
