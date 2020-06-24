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

    private static String sourceIP = "128.105.145.106";

    @Override
    public Set<InetAddress> getAddresses(Set<InetAddress> addresses, InetAddress source) {
        Set<InetAddress> result = new HashSet<>();
        // convert the set to a list
        List<InetAddress> targetList = new ArrayList<>(addresses);

        System.out.println(">>>>>>>>>>> "+targetList);

        for (int i=0; i<targetList.size(); i++) {
            System.out.println(">>>>>>>>>>> "+ targetList.get(i)+","+sourceIP);
            if (targetList.get(i).toString().contains(sourceIP)){
                System.out.println(targetList.get(i)+","+sourceIP);
                Set<InetAddress> r = new HashSet<>();
                r.add(targetList.get(i));
                return r;
            }
            result.add(targetList.get(i));
        }

        System.out.println(">>>>>>>>>>> "+result);

        return result;
    }

}
