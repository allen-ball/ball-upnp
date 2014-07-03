/*
 * $Id$
 *
 * Copyright 2014 Allen D. Ball.  All rights reserved.
 */
package ball.upnp.ant.taskdefs;

import ball.annotation.AntTask;
import ball.util.Factory;
import ball.util.ant.taskdefs.AbstractClasspathTask;
import java.io.File;
import org.apache.catalina.Server;
import org.apache.catalina.startup.Tomcat;
import org.apache.tools.ant.BuildException;

/**
 * {@link.uri http://ant.apache.org/ Ant} {@link org.apache.tools.ant.Task}
 * to run a {@link Tomcat} instance.
 *
 * {@bean-info}
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
@AntTask("tomcat")
public class TomcatTask extends AbstractClasspathTask {
    private String type = Tomcat.class.getName();
    private boolean silent = false;
    private File basedir = null;
    private Integer port = null;

    /**
     * Sole constructor.
     */
    public TomcatTask() { super(); }

    protected String getType() { return type; }
    public void setType(String type) { this.type = type; }

    protected boolean isSilent() { return silent; }
    public void setSilent(boolean silent) { this.silent = silent; }

    protected File getBasedir() { return basedir; }
    public void setBasedir(File basedir) { this.basedir = basedir; }

    protected Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }

    @Override
    public void execute() throws BuildException {
        if (getPort() == null) {
            throw new BuildException("`port' attribute must be specified");
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(getClassLoader());

            Class<? extends Tomcat> type =
                Class.forName(getType()).asSubclass(Tomcat.class);
            Tomcat tomcat = new Factory<Tomcat>(type).getInstance();

            tomcat.setSilent(isSilent());

            if (getBasedir() != null) {
                tomcat.setBaseDir(getBasedir().getAbsolutePath());
            }

            tomcat.setPort(getPort());
            tomcat.getServer().setParentClassLoader(getClassLoader());
            tomcat.start();

            if (tomcat.getServer().getPort() > 0) {
                log(String.valueOf(tomcat.getServer().getAddress())
                    + ":" + String.valueOf(tomcat.getServer().getPort())
                    + " " + String.valueOf(tomcat.getServer().getShutdown()));
            }

            tomcat.getServer().await();
        } catch (BuildException exception) {
            throw exception;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new BuildException(throwable);
        } finally {
            Thread.currentThread().setContextClassLoader(loader);
        }
    }
}
