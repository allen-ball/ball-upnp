/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

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
public class MediaServer extends Device {
    public static final String TYPE =
        "urn:schemas-upnp-org:device:MediaServer:1";

    private final ContentDirectory directory;
    private final ConnectionManager manager;
    private final List<Service> list;

    /**
     * Sole constructor.
     */
    public MediaServer() {
        super(URI.create(TYPE));

        directory = new ContentDirectory(this);
        manager = new ConnectionManager(this);

        list = Arrays.asList(directory, manager);
    }

    @Override
    public List<Service> getServiceList() { return list; }
}
