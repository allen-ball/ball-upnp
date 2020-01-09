/*
 * $Id$
 *
 * Copyright 2020 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import ball.upnp.annotation.DeviceType;
import java.net.URI;

/**
 * {@link Device} support for {@link DeviceType} and related annotations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface AnnotatedDevice {

    /**
     * Method to get the URN ({@link URI}) describing this
     * {@link AnnotatedDevice}'s device type.
     *
     * @return  The service type.
     */
    default URI getDeviceType() {
        DeviceType annotation = getClass().getAnnotation(DeviceType.class);

        return (annotation != null) ? URI.create(annotation.value()) : null;
    }
}
