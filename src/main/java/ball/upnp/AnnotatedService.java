/*
 * $Id$
 *
 * Copyright 2020 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import ball.upnp.annotation.ServiceId;
import ball.upnp.annotation.ServiceType;
import java.net.URI;

/**
 * {@link Service} support for {@link ServiceType} and related annotations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface AnnotatedService {

    /**
     * Method to get the URN {{@link URI}) describing this
     * {@link AnnotatedService}'s service type.
     *
     * @return  The service type.
     */
    default URI getServiceType() {
        ServiceType annotation = getClass().getAnnotation(ServiceType.class);

        return (annotation != null) ? URI.create(annotation.value()) : null;
    }

    /**
     * Method to get the URN {{@link URI}) describing this
     * {@link AnnotatedService}'s service ID.
     *
     * @return  The service type.
     */
    default URI getServiceId() {
        ServiceId annotation = getClass().getAnnotation(ServiceId.class);

        return (annotation != null) ? URI.create(annotation.value()) : getServiceType();
    }
}
