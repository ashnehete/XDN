package edu.umass.cs.xdn.dns;

import edu.umass.cs.reconfiguration.dns.DnsTrafficPolicy;
import edu.umass.cs.xdn.XDNConfig;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This policy is used for a blind test demo only
 */
public class EdgeAwareDnsTrafficPolicy implements DnsTrafficPolicy {

    private static String sourceIP = XDNConfig.prop.getProperty(XDNConfig.XC.EDGE_ADDR.toString());

    @Override
    public Set<InetAddress> getAddresses(Set<InetAddress> addresses, InetAddress source) {
        Set<InetAddress> result = new HashSet<>();
        // convert the set to a list
        List<InetAddress> targetList = new ArrayList<>(addresses);

        if ( source.toString().equals(sourceIP) ) {
            result.add(targetList.get(0));
        } else if (targetList.size() > 1) {
            for (int i=1; i<targetList.size(); i++) {
                result.add(targetList.get(i));
            }
        }
        return result;
    }

}
