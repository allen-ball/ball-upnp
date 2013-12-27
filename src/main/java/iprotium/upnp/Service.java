/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp;

import java.beans.ConstructorProperties;
import java.net.URI;
import javax.servlet.http.HttpServlet;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP} services.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public abstract class Service {
    private static final String SCPD_XML = "scpd.xml";
    private static final String CTRL = "ctrl";
    private static final String EVT = "evt";

    private static final String SLASH = "/";

    private final Device device;
    private final URI serviceType;
    private final URI serviceId;
    private URI uri = null;

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
        if (device != null) {
            this.device = device;
        } else {
            throw new NullPointerException("device");
        }

        if (serviceType != null) {
            this.serviceType = serviceType;
        } else {
            throw new NullPointerException("serviceType");
        }

        if (serviceId != null) {
            this.serviceId = serviceId;
        } else {
            throw new NullPointerException("serviceId");
        }
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
     * Method to get the {@link URI} to this {@link Service}'s Service
     * Control Protocol Description.
     *
     * @return  The service SCPD {@link URI}.
     */
    protected URI getSCPDURL() { return uri().resolve(SCPD_XML); }

    /**
     * Method to get the {@link HttpServlet} that implements the SCPD.  If
     * this method returns non-{@code null}, the {@link HttpServlet} will be
     * installed at the path specified by {@link #getSCPDURL()}.
     *
     * @return  The service SCPD {@link HttpServlet}.
     */
    protected HttpServlet getSCPDServlet() { return null; }

    private URI uri() {
        synchronized (this) {
            if (uri == null) {
                String name = getClass().getSimpleName();
                int index = device.getServiceList().indexOf(this);

                uri = device.uri().resolve(name + index + SLASH);
            }
        }

        return uri;
    }

    /**
     * Method to get the {@link URI} to this {@link Service}'s control.
     *
     * @return  The service control {@link URI}.
     */
    protected URI getControlURL() { return uri().resolve(CTRL); }

    /**
     * Method to get the {@link HttpServlet} that implements control.  If
     * this method returns non-{@code null}, the {@link HttpServlet} will be
     * installed at the path specified by {@link #getControlURL()}.
     *
     * @return  The service control {@link HttpServlet}.
     */
    protected HttpServlet getControlServlet() { return null; }

    /**
     * Method to get the sub-{@link URI} to this {@link Service}'s evt.
     *
     * @return  The evt sub-{@link URI}.
     */
    protected URI getEventSubURL() { return uri().resolve(EVT); }

    /**
     * Method to get the {@link HttpServlet} that implements evt.  If
     * this method returns non-{@code null}, the {@link HttpServlet} will be
     * installed at the path specified by {@link #getEventSubURL()}.
     *
     * @return  The evt {@link HttpServlet}.
     */
    protected HttpServlet getEventSubServlet() { return null; }

    /**
     * Method to get an {@link Element} that may be serialized with a
     * {@link javax.xml.bind.Marshaller} to XML.
     *
     * @return  The {@link Element} representing this {@link Service}.
     */
    protected Element getElement() { return new Element(this); }

    @Override
    public String toString() { return getServiceType().toString(); }

    /**
     * {@code <service/>}
     */
    @XmlType(propOrder = {
                "serviceType", "serviceId",
                "SCPDURL", "controlURL", "eventSubURL"
             })
    protected static class Element {
        private final Service service;

        /**
         * Sole public constructor.
         *
         * @param       service         The {@link Service}.
         */
        public Element(Service service) {
            if (service != null) {
                this.service = service;
            } else {
                throw new NullPointerException("service");
            }
        }

        private Element() { this(null); }

        @XmlElement
        public URI getServiceType() { return service.getServiceType(); }

        @XmlElement
        public URI getServiceId() { return service.getServiceId(); }

        @XmlElement
        public String getSCPDURL() { return localize(service.getSCPDURL()); }

        private String localize(URI uri) {
            String string = null;

            if (uri != null) {
                URI base = service.getDevice().uri();
                URI relative = base.relativize(uri);

                if (relative.isAbsolute()) {
                    string = uri.toString();
                } else {
                    string = uri.getPath();
                }
            }

            return string;
        }

        @XmlElement
        public String getControlURL() {
            return localize(service.getControlURL());
        }

        @XmlElement
        public String getEventSubURL() {
            return localize(service.getEventSubURL());
        }

        @Override
        public String toString() { return getServiceType().toString(); }
    }
}
