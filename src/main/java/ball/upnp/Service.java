/*
 * $Id$
 *
 * Copyright 2013 - 2018 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import java.beans.ConstructorProperties;
import java.net.URI;

import static java.util.Objects.requireNonNull;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP} services.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public abstract class Service {
    private static final String COLON = ":";
    private static final String SLASH = "/";

    private final Device device;
    private final URI serviceType;
    private final URI serviceId;
    private final URI usn;

    /**
     * Sole constructor.
     *
     * @param   device          The {@link Device} that implements this
     *                          {@link Service}.
     * @param   serviceType     The {@link Service} type (URN).
     * @param   serviceId       The {@link Service} ID (URN).
     */
    @ConstructorProperties({ "device", "serviceType", "serviceId" })
    protected Service(Device device, URI serviceType, URI serviceId) {
        this.device = requireNonNull(device, "device");
        this.serviceType = requireNonNull(serviceType, "serviceType");
        this.serviceId = requireNonNull(serviceId, "serviceId");
        this.usn =
            URI.create(getDevice().getUDN().toString()
                       + COLON + COLON + getServiceId().toString());
    }

    /**
     * Method to access the {@link Device} that implements this
     * {@link Service}.
     *
     * @return  The {@link Device}.
     */
    public Device getDevice() { return device; }

    /**
     * Method to get the URN {{@link URI}) describing this {@link Service}'s
     * service type.
     *
     * @return  The service type.
     */
    public URI getServiceType() { return serviceType; }

    /**
     * Method to get this {@link Service}'s service ID ({@link URI}).
     *
     * @return  The service ID {@link URI}.
     */
    public URI getServiceId() { return serviceId; }

    /**
     * Method to get the USN {@link URI}.  The {@link URI} is calculated by
     * combining the {@link Device#getUDN()} and {@link #getServiceId()}.
     *
     * @return  The USN {@link URI}.
     */
    public URI getUSN() { return usn; }

    @Override
    public String toString() { return getServiceType().toString(); }
}
