package edu.umass.cs.xdn.dns;

import edu.umass.cs.reconfiguration.dns.DnsTrafficPolicy;

import java.net.InetAddress;
import java.util.*;

/**
 *
 */
public class BlindTestAwareDnsTrafficPolicy implements DnsTrafficPolicy {

    private static long lastQueriedTime = System.currentTimeMillis();
    private static InetAddress lastReturnValue = null;

    final private static long defaultTTLForReturnValue = 10*60*1000; // 10 minutes

    private static Random random = new Random();

    @Override
    public Set<InetAddress> getAddresses(Set<InetAddress> addresses, InetAddress source) {
        Set<InetAddress> result = new HashSet<>();
        long now = System.currentTimeMillis();
        if(now - lastQueriedTime >= defaultTTLForReturnValue || lastReturnValue==null){
            List<InetAddress> targetList = new ArrayList<>(addresses);
            // randomly generate a new record to return
            lastReturnValue = targetList.get(random.nextInt(targetList.size()));
            // update lastQueriedTime
            lastQueriedTime = now;
        } // else no need to update return value

        result.add(lastReturnValue);

        return result;
    }
}
