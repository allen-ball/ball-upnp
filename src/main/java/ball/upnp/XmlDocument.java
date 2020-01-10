/*
 * $Id$
 *
 * Copyright 2020 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import javax.xml.bind.annotation.XmlRootElement;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * Support for {@link XmlRootElement} and related annotations.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public interface XmlDocument {

    /**
     * Method to get the {@link XmlRootElement} annotation.
     *
     * @return  The {@link XmlRootElement} annotation.
     */
    default XmlRootElement getXmlRootElement() {
        return AnnotationUtils.findAnnotation(getClass(),
                                              XmlRootElement.class);
    }
}
