/*
 * $Id$
 *
 * Copyright 2020 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import ball.util.UUIDFactory;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP} devices.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor(access = PROTECTED)
public abstract class Device implements AnnotatedDevice {
    @Getter
    private final UUID UUID = UUIDFactory.getDefault().generateTime();
    @Getter
    private final List<Service> serviceList = new LinkedList<>();

    /**
     * Method to get {@link.this} {@link Service}s UDN.
     *
     * @return  The UDN {@link URI}.
     */
    public URI getUDN() {
        return URI.create("uuid:" + getUUID().toString().toUpperCase());
    }

    @Override
    public String toString() { return getDeviceType().toString(); }
}
