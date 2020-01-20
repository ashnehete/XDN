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
    public static boolean largeCheckPointerEnabled = false;
}
