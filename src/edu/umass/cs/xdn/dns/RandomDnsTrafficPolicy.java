package edu.umass.cs.xdn.dns;

import edu.umass.cs.reconfiguration.dns.DnsTrafficPolicy;

import java.net.InetAddress;
import java.util.*;

public class RandomDnsTrafficPolicy implements DnsTrafficPolicy {
    static Random rand = new Random();

    @Override
    public Set<InetAddress> getAddresses(Set<InetAddress> addresses, InetAddress source) {
        Set<InetAddress> result = new HashSet<>();
        List<InetAddress> targetList = new ArrayList<>(addresses);
        result.add(targetList.get(rand.nextInt(targetList.size())));

        return result;
    }
}
