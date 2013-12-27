/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp.ant.taskdefs;

import iprotium.annotation.AntTask;
import iprotium.upnp.MediaServer;
import iprotium.upnp.ssdp.SSDPDiscoveryThread;
import iprotium.upnp.ssdp.SSDPMessage;
import iprotium.util.ant.taskdefs.AbstractClasspathTask;
import org.apache.tools.ant.BuildException;

/**
 * {@link.uri http://ant.apache.org/ Ant} {@link org.apache.tools.ant.Task}
 * to run a {@link MediaServer}.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
@AntTask("media-server")
public class MediaServerTask extends AbstractClasspathTask
                             implements SSDPDiscoveryThread.Listener {
    private Integer port = null;
    private boolean verbose = false;

    /**
     * Sole constructor.
     */
    public MediaServerTask() { super(); }

    protected Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    protected boolean isVerbose() { return verbose; }
    public void setVerbose(boolean verbose) { this.verbose = verbose; }

    @Override
    public void execute() throws BuildException {
        if (getPort() == null) {
            throw new BuildException("`port' attribute must be specified");
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(getClassLoader());

            MediaServer server = new MediaServer();

            server.context().setParentClassLoader(getClassLoader());

            if (isVerbose()) {
                server.getSSDPDiscoveryThread().addListener(this);
            }

            server.start();

            log(String.valueOf(server.getLocation()));

            server.getServer().await();
        } catch (BuildException exception) {
            throw exception;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new BuildException(throwable);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }

    @Override
    public void sendEvent(SSDPMessage message) {
        log(String.valueOf(message));
    }

    @Override
    public void receiveEvent(SSDPMessage message) {
        log(String.valueOf(message));
    }
}
