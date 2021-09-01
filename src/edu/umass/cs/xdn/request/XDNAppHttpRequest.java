package edu.umass.cs.xdn.tools;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// XDNAppHttpRequest is a wrapper class to help XDN Java Client sending
// HttpRequest to XDNApp that further will be forwarded to ActiveReplica
public class XDNAppHttpRequest {
    // path that will be forwarded to all replicas, start with "/", exclude the host
    String path = "/";

    // http method: GET, POST, PUT, DELETE
    String method = "GET";

    // header of the http request
    Map<String, List<String>> headers = new HashMap<>();

    // body of the http request in array of bytes
    byte[] payload;

    public static XDNAppHttpRequest initFromJSONString(String json) {
        return new Gson().fromJson(json, XDNAppHttpRequest.class);
    }

    public String toJSONString() {
        return new Gson().toJson(this);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public void addHeader(String key, String value) {
        List<String> values = headers.get(key);
        if (values == null) {
            values = new ArrayList<>();
        }
        values.add(value);
        headers.put(key, values);
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public byte[] getPayload() {
        return payload;
    }
}
