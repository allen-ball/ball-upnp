/*
 * $Id$
 *
 * Copyright 2013 - 2015 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import ball.annotation.ServiceProviderFor;
import ball.tomcat.EmbeddedContextConfigurator;
import ball.tomcat.EmbeddedLifecycleListener;
import ball.tomcat.EmbeddedTomcatConfigurator;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.apache.catalina.Server;

/**
 * {@link.uri http://www.upnp.org/ UPnP} {@link MediaServer}
 * {@link Device} which implements {@link ContentDirectory} and
 * {@link ConnectionManager} {@link Service}s.
 *
 * <table>
 *   <tr><td>{@link #TYPE}:</td><td>{@value #TYPE}</td></tr>
 * </table>
 *
 * {@bean-info}
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ EmbeddedTomcatConfigurator.class, EmbeddedContextConfigurator.class, EmbeddedLifecycleListener.class })
@EmbeddedLifecycleListener.For({ Server.class })
public class MediaServer extends Device {
    public static final String TYPE =
        "urn:schemas-upnp-org:device:MediaServer:4";

    private final RootDevice root;
    private final ContentDirectory directory;
    private final ConnectionManager manager;
    private final List<? extends Service> list;

    /**
     * Sole constructor.
     */
    public MediaServer() {
        super(URI.create(TYPE));

        root = new RootDevice(this);
        directory = new ContentDirectory(this);
        manager = new ConnectionManager(this);

        list = Arrays.asList(root, directory, manager);
    }

    @Override
    public List<? extends Service> getServiceList() { return list; }
}
