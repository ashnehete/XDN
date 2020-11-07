package edu.umass.cs.xdn.dns;

import edu.umass.cs.reconfiguration.dns.DnsTrafficPolicy;

import java.io.*;
import java.net.InetAddress;
import java.util.*;

/*
 * This policy can only be used for name servers in a same Cloudlab region.
 */
public class CloudlabDnsTrafficPolicy implements DnsTrafficPolicy {

    private final static String CLOUDLAB_SHARED_FOLDER = "/proj/lsn-PG0/groups/";

    private static String path = CLOUDLAB_SHARED_FOLDER+"seed.txt";

    @Override
    public Set<InetAddress> getAddresses(Set<InetAddress> addresses, InetAddress source) {
        Set<InetAddress> result = new HashSet<>();
        List<InetAddress> targetList = new ArrayList<>(addresses);

        int num = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(new File(path)))) {
            num = Integer.parseInt(reader.readLine());
            System.out.println(num);
        } catch (IOException e) {
            e.printStackTrace();
        }

        int idx = num % targetList.size();
        if (idx < 0) idx += targetList.size();

        result.add(targetList.get(idx));

        return result;
    }

    public static void main(String[] args) {
        String filePath = path; // "tmp.txt"

        String content = ""+(new Random().nextInt());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(filePath)))) {
            writer.write(content);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(new File(filePath)))) {
            int num = Integer.parseInt(reader.readLine());
            System.out.println(num);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
