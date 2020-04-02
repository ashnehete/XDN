package edu.umass.cs.xdn;

import edu.umass.cs.gigapaxos.PaxosConfig;

import java.util.logging.Logger;

public class XDNConfig {

    public static Logger log = Logger.getLogger(XDNConfig.class.getName());

    /**
     *
     */
    public static String xdnRoute = "/xdnapp";

    /**
     * Indicate whether this node is edge node, default is false, means it is a cloud node
     */
    public static boolean isEdgeNode = false;

    /**
     *
     */
    public static String checkpointDir =  "checkpoints/"; // customized location does not work: "/users/oversky/checkpoint/";

    public static String defaultCheckpointDir =  "/var/lib/docker/containers/";
    /**
     *
     */
    public static String xdnServiceDecimal = "_xdn_";

    /**
     * If true, XDN will fetch docker checkpoint directly from a remote node.
     * Otherwise, XDN just uses stringified checkpoint.
     */
    public static boolean largeCheckPointerEnabled = true; //false;

    /**
     * Used to test overhead with noop for XDNApp
     */
    public static boolean noopEnabled = true;
}
