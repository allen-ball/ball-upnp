/*
 * $Id$
 *
 * Copyright 2013, 2014 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import java.beans.ConstructorProperties;
import java.net.URI;

/**
 * {@link.uri http://www.upnp.org/ UPnP} {@link RootDevice}
 * {@link Service}.
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
public class RootDevice extends Service {
    public static final String TYPE = "upnp:rootdevice";

    /**
     * Sole constructor.
     *
     * @param   device          The {@link Device} that implements this
     *                          {@link Service}.
     */
    @ConstructorProperties({ "device" })
    public RootDevice(Device device) {
        super(device, URI.create(TYPE), URI.create(TYPE));
    }
}
