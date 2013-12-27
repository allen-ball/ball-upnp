/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp;

import java.beans.ConstructorProperties;
import java.net.URI;

/**
 * {@link.uri http://www.upnp.org/ UPnP} {@link ContentDirectory}
 * {@link Service}.
 *
 * <table>
 *   <tr><td>{@link #TYPE}:</td><td>{@value #TYPE}</td></tr>
 *   <tr><td>{@link #ID}:</td><td>{@value #ID}</td></tr>
 * </table>
 *
 * {@bean-info}
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class ContentDirectory extends Service {
    public static final String TYPE =
        "urn:schemas-upnp-org:service:ContentDirectory:1";
    public static final String ID =
        "urn:upnp-org:serviceId:ContentDirectory";

    /**
     * Sole constructor.
     *
     * @param   device          The {@link Device} that implements this
     *                          {@link Service}.
     */
    @ConstructorProperties({ "device" })
    public ContentDirectory(Device device) {
        super(device, URI.create(TYPE), URI.create(ID));
    }
}
