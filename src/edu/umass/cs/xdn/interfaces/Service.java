package edu.umass.cs.xdn.interfaces;

import java.util.List;

/**
 *
 */
public interface Service {

    /**
     * @param name
     * @return
     */
    public List<String> getStartCommand(String name);

    /**
     * @param name
     * @return
     */
    public List<String> getCheckpointCommand(String name);

    /**
     * @param name
     * @return
     */
    public List<String> getRestoreCommand(String name);

    /**
     * @param name
     * @return
     */
    public List<String> getStopCommand(String name);

    /**
     * @param name the unique name or id of the service
     * @param exists indicate that whether the image associated with the name already exists
     * @return
     */
    public List<String> getPullCommand(String name, boolean exists);
}
