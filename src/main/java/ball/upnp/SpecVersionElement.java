/*
 * $Id$
 *
 * Copyright 2013 - 2019 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import java.beans.ConstructorProperties;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * JAXB annotated {@link Object} used to produce specification version
 * elements in {@link.uri http://www.upnp.org/ UPnP} XML documents.
 *
 * {@bean.info}
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@XmlType(propOrder = { "major", "minor" })
public class SpecVersionElement {
    private final int major;
    private final int minor;

    /**
     * Sole public constructor.
     *
     * @param   major           The version major number.
     * @param   minor           The version minor number.
     */
    @ConstructorProperties({ "major", "minor" })
    public SpecVersionElement(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    private SpecVersionElement() { this(1, 0); }

    @XmlElement
    public int getMajor() { return major; }

    @XmlElement
    public int getMinor() { return minor; }

    @Override
    public String toString() {
        return (String.valueOf(getMajor()) + "." + String.valueOf(getMinor()));
    }
}
