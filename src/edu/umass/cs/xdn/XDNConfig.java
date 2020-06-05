package edu.umass.cs.xdn;

import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;

public class XDNConfig {

    public static void load() {
        String filename = defaultConfigFileName;
        if (System.getProperty(appConfigName)!=null) {
            filename = System.getProperty(appConfigName);
        }

        File f = new File(filename);
        if (!f.exists()) {
            System.out.println("Config file "+filename+" does not exist");
            System.exit(0);
        }

        try {
            InputStream input = new FileInputStream(filename);
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties prop = new Properties();

    static{
        load();
    }

    public enum XC {
        /**
         * a name that is unique for an app, i.e., IMAGE_NAME
         */
        NAME("Umass"),
        /**
         *
         */
        IMAGE_NAME("xdn-demo-app"),

        /**
         * URL to fetch docker image from DockerHub
         */
        IMAGE_URL("oversky710/xdn-demo-app"),
        /**
         * Docker port number
         */
        DOCKER_PORT(3000),
        /**
         * public exposed port number
         */
        PUBLIC_EXPOSE_PORT(8080),
        /**
         * Value of a request to send to XDN
         */
        VALUE("1"),
        /**
         *
         */
        COORD(true),
        /**
         *
         */
        NUM_REQ(1),
        /**
         *
         */
        EDGE_ADDR("127.0.0.1"),
        ;

        final Object defaultValue;

        XC(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

    }


    public static Logger log = Logger.getLogger(XDNConfig.class.getName());
    /**
     *
     */
    final public static String xdnRoute = "/xdnapp";

    /**
     * Indicate whether this node is edge node, default is false, means it is a cloud node
     */
    public static boolean isEdgeNode = false;

    /**
     *
     */
    public static String checkpointDir =  "checkpoints/";

    /**
     * Docker's default checkpoint location
     */
    public final static String defaultCheckpointDir =  "/var/lib/docker/containers/";

    public final static String defaultVolumeDir = "/var/lib/docker/volumes/";

    /**
     *
     */
    private static String xdnServiceDecimal = ".";

    /**
     * form service in a DNS domain name style to be compatible with DNS query
     */
    public static String xdnDomainName = "xdnbest.xyz";

    /**
     * If true, XDN will fetch docker checkpoint directly from a remote node.
     * Otherwise, XDN just uses stringified checkpoint.
     */
    public static boolean largeCheckPointerEnabled = true;

    /**
     * If true, all state is kept in a volume on a docker host.
     * checkpoint needs to copy the volume,
     * while restore needs to transfer the volume.
     */
    public static boolean volumeCheckpointEnabled = true;

    /**
     * Used to test overhead with noop for XDNApp
     */
    public static boolean noopEnabled = false;

    final public static String appConfigName = "appConfig";

    final public static String defaultConfigFileName = "conf/app/service.properties";


    public static String generateServiceName(String imageName, String name){
        // FIXME: imageName or name could be null
        return name+xdnServiceDecimal+imageName+xdnServiceDecimal+xdnDomainName;
        // return imageName+xdnServiceDecimal+name;
    }

    /**
     *
     * @param serviceName
     * @return a String array:
     */
    public static String[] extractNamesFromServiceName(String serviceName){
        String[] result = new String[]{null, null};
        serviceName = serviceName.replace(xdnServiceDecimal+xdnDomainName, "");
        String[] subs = serviceName.split(getEscapeDecimal(xdnServiceDecimal));
        // FIXME: imageName or name could be null
        // name
        result[0] = subs[0];
        // imageName
        result[1] = subs[1];

        return result;
    }

    private static String getEscapeDecimal(String decimal){
        return "\\"+decimal;
    }

    public static void main(String[] args) throws IOException {
        String filename = defaultConfigFileName;
        if (System.getProperty(appConfigName)!=null) {
            filename = System.getProperty(appConfigName);
        }

        File f = new File(filename);
        if (!f.exists()) {
            System.out.println("Config file "+filename+" does not exist");
            System.exit(0);
        }

        InputStream input = new FileInputStream(filename);

        Properties prop = new Properties();
        prop.load(input);
        System.out.println(prop);
    }
}
