/*
 * $Id$
 *
 * Copyright 2020 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import java.net.URI;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import static lombok.AccessLevel.PROTECTED;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP} services.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@RequiredArgsConstructor(access = PROTECTED)
public abstract class Service implements AnnotatedService {
    @NonNull @Getter
    private final Device device;

    /**
     * Method to get the USN {@link URI}.  The {@link URI} is calculated by
     * combining the {@link Device#getUDN()} and {@link #getServiceId()}.
     *
     * @return  The USN {@link URI}.
     */
    public URI getUSN() {
        return URI.create(getDevice().getUDN() + "::" + getServiceId());
    }

    @Override
    public String toString() { return getServiceType().toString(); }
}