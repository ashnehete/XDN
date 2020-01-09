package edu.umass.cs.xdn.docker;

import edu.umass.cs.xdn.interfaces.Service;

import java.net.InetAddress;
import java.util.List;

public class DockerService implements Service {

    final String name;

    /**
     * Docker id
     */
    final String id;

    /**
     * Docker hub url
     */
    final String imageUrl;

    /**
     * The port number exposed to the public network
     * TODO: may need to expose multiple ports in the future
     */
    final int port;

    /**
     *
     */
    InetAddress addr;

    public DockerService(String name, String id, String imageUrl, int port) {
        this.name = name;
        this.id = id;
        this.imageUrl = imageUrl;
        this.port = port;
    }

    public void setAddr(InetAddress addr) {
        this.addr = addr;
    }

    @Override
    public List<String> getStartCommand(String name) {
        return null;
    }

    @Override
    public List<String> getCheckpointCommand(String name) {
        return null;
    }

    @Override
    public List<String> getRestoreCommand(String name) {
        return null;
    }

    @Override
    public List<String> getStopCommand(String name) {
        return null;
    }

    @Override
    public List<String> getPullCommand(String name, boolean exists) {
        return null;
    }


    public static void main(String[] args) {

    }

}
