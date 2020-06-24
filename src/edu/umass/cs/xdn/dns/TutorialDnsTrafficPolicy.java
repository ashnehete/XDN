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
public class TutorialDnsTrafficPolicy implements DnsTrafficPolicy {

    private static String sourceIP = "/128.105.145.106";

    @Override
    public Set<InetAddress> getAddresses(Set<InetAddress> addresses, InetAddress source) {
        Set<InetAddress> result = new HashSet<>();
        // convert the set to a list
        List<InetAddress> targetList = new ArrayList<>(addresses);

        boolean found = false;

        for (int i=1; i<targetList.size(); i++) {
            if (targetList.get(i).toString().contains(sourceIP)){
                Set<InetAddress> r = new HashSet<>();
                r.add(targetList.get(i));
                return r;
            }
            result.add(targetList.get(i));
        }

        return result;
    }

}
