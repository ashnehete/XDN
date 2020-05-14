package edu.umass.cs.xdn.dns;

import edu.umass.cs.reconfiguration.dns.DnsTrafficPolicy;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This policy uses a MD5 deterministic hash to return a random IP address based on the current timestamp
 */
public class HashBasedBlindTestDnsTrafficPolicy implements DnsTrafficPolicy {

    final private static long DEFAULT_INTERVAL_FOR_RETURN_VALUE = 10*60*1000;
    private static long lastQueriedTime = System.currentTimeMillis()/ DEFAULT_INTERVAL_FOR_RETURN_VALUE -1;
    private static InetAddress lastReturnValue = null;

    static MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<InetAddress> getAddresses(Set<InetAddress> addresses, InetAddress source) {
        Set<InetAddress> result = new HashSet<>();
        long now = System.currentTimeMillis()/ DEFAULT_INTERVAL_FOR_RETURN_VALUE; // current time divided by interval

        if (now != lastQueriedTime || lastReturnValue == null) {
            List<InetAddress> targetList = new ArrayList<>(addresses);

            byte[] b = ByteUtils.longToBytes(now);
            byte[] r = md.digest(b);
            int num = ByteUtils.bytesToLong(r).intValue();

            lastReturnValue = targetList.get(num % targetList.size());

            lastQueriedTime = now;
        }
        result.add(lastReturnValue);

        return result;
    }

    public static class ByteUtils {
        private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

        public static byte[] longToBytes(long x) {

            buffer.putLong(0, x);
            return buffer.array();
        }

        public static Long bytesToLong(byte[] bytes) {
            buffer.clear();
            buffer.put(bytes, 0, Long.BYTES);
            buffer.flip();//need flip
            return buffer.getLong();
        }

    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        int total = 10;
        Long now = System.currentTimeMillis()/1000;
        System.out.println(now);
        MessageDigest md = MessageDigest.getInstance("MD5");

        for (int i=0; i<total; i++){
            byte[] b = ByteUtils.longToBytes(now);
            byte[] r = md.digest(b);
            // System.out.println(new String(r));
            long num = ByteUtils.bytesToLong(r);

            System.out.println(num+","+num%2);
        }

    }

}
