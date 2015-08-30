/*
 * $Id$
 *
 * Copyright 2013 - 2015 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import ball.activation.JAXBDataSource;
import ball.io.Directory;
import ball.tomcat.EmbeddedTomcat;
import ball.tomcat.EmbeddedTomcatConfigurator;
import ball.util.UUIDFactory;
import java.beans.ConstructorProperties;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.servlet.Servlet;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Server;
import org.apache.cxf.transport.servlet.CXFServlet;

import static ball.tomcat.EmbeddedTomcat.addServlet;
import static ball.util.StringUtil.NIL;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP} devices.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public abstract class Device implements EmbeddedTomcatConfigurator,
                                        LifecycleListener {
    private static final String HTTP = "http";
    private static final String UUID = "uuid";

    private static final String ASTERISK = "*";
    private static final String COLON = ":";
    private static final String SLASH = "/";

    private final URI deviceType;
    private final UUID uuid = UUIDFactory.getDefault().generateTime();
    private final URI udn;
    private final int port;
    private final URI uri;
    private Server server = null;

    /**
     * Sole constructor.
     *
     * @param   deviceType      The {@link Device} type (URN).
     */
    @ConstructorProperties({ "deviceType" })
    protected Device(URI deviceType) {
        if (deviceType != null) {
            this.deviceType = deviceType;
        } else {
            throw new NullPointerException("deviceType");
        }

        try {
            udn = new URI(UUID, uuid.toString().toUpperCase(), null);
            port = 8080;
            uri =
                new URI(HTTP, null,
                        InetAddress.getLocalHost().getHostAddress(), port,
                        SLASH, null, null);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * Method to get the URN ({@link URI}) describing this {@link Device}'s
     * device type.
     *
     * @return  The device type.
     */
    public URI getDeviceType() { return deviceType; }

    /**
     * Method to get the Unique Device Name (UDN) {@link URI} for this
     * {@link Device}.
     *
     * @return  The UDN {@link URI}.
     */
    public URI getUDN() { return udn; }

    /**
     * Method to get {@link List} of {@link Service}s implemented by this
     * {@link Device}.
     *
     * @return  The {@link List} of {@link Service}s.
     */
    protected abstract List<? extends Service> getServiceList();

    /**
     * Method to get the location {@link URI} for this {@link Device}.
     *
     * @return  The location {@link URI}.
     */
    public URI getLocation() {
        return uri.resolve(getPath() + SLASH + "device.xml");
    }

    private String getPath() {
        return SLASH + getClass().getSimpleName();
    }

    private String getPath(Service service) {
        String name = service.getClass().getSimpleName();
        ArrayList<Service> list = new ArrayList<>(getServiceList());
        Iterator<Service> iterator = list.iterator();

        while (iterator.hasNext()) {
            if (! name.equals(iterator.next().getClass().getSimpleName())) {
                iterator.remove();
            }
        }

        if (list.size() > 1) {
            name += list.indexOf(service);
        }

        return getPath() + SLASH + name;
    }

    private String getSCPDPath(Service service) {
        return getPath(service) + SLASH + "scpd.xml";
    }

    private String getControlPath(Service service) {
        return getPath(service) + SLASH + "control";
    }

    private String getEventPath(Service service) {
        return getPath(service) + SLASH + "event";
    }

    @Override
    public void configure(EmbeddedTomcat tomcat) throws Exception {
        String host = uri.getHost();
        int port = uri.getPort();

        tomcat.setHostname(host);
        tomcat.setPort(port);

        Server server = tomcat.getServer();

        server.setAddress(host);
        server.setPort(port + 1);

        Context context = tomcat.getContext();

        context.addLifecycleListener(this);

        context.getServletContext()
            .setAttribute("device", this);
        /*
         * Device
         *
         * Note: DeviceDescriptionServlet functionality should be included
         * in CXFServlet / Spring implementation.
         */
        addServlet(context, new DeviceDescriptionServlet())
            .addMapping(getLocation().getPath());
        addServlet(context, new CXFServlet())
            .addMapping(getPath() + SLASH + ASTERISK);
        /*
         * Redirect to the Device description
         */
        addServlet(context, new RedirectServlet(getLocation().getPath()))
            .addMapping(NIL);
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getLifecycle() instanceof Context) {
            Context context = (Context) event.getLifecycle();

            switch (context.getState()) {
            case STARTING:
            case STARTED:
                ((SSDP) context.getServletContext().getAttribute("ssdp"))
                    .add(this);
                break;

            case STOPPING:
            case STOPPED:
                ((SSDP) context.getServletContext().getAttribute("ssdp"))
                    .remove(this);
                break;

            default:
                break;
            }
        }
    }

    @Override
    public String toString() { return getDeviceType().toString(); }

    private class DeviceDescriptionServlet extends DataSourceServlet {
        private static final long serialVersionUID = 5705073073449470828L;

        public DeviceDescriptionServlet() { super(null); }

        @Override
        public JAXBDataSource getDataSource() {
            return new JAXBDataSource(new RootElement(Device.this));
        }
    }

    /**
     * {@code <root/>}
     */
    @XmlRootElement(name = "root")
    @XmlType(propOrder = { "specVersion", "device" })
    protected static class RootElement {
        private static final String XMLNS = "urn:schemas-upnp-org:device-1-0";
        private static final SpecVersionElement VERSION =
            new SpecVersionElement(1, 0);

        private final DeviceElement element;

        /**
         * Sole public constructor.
         *
         * @param       device          The {@link Device}.
         */
        public RootElement(Device device) {
            element = (device != null) ? new DeviceElement(device) : null;
        }

        private RootElement() { this(null); }

        @XmlAttribute
        public String getXMLNS() { return XMLNS; }

        @XmlElement
        public SpecVersionElement getSpecVersion() { return VERSION; }

        @XmlElement
        public DeviceElement getDevice() { return element; }

        @Override
        public String toString() { return getXMLNS(); }

        /**
         * {@code <device/>}
         */
        @XmlType(propOrder = { "deviceType", "UDN", "serviceList" })
        protected static class DeviceElement {
            private final Device device;

            /**
             * Sole public constructor.
             *
             * @param   device          The {@link Device}.
             */
            public DeviceElement(Device device) {
                if (device != null) {
                    this.device = device;
                } else {
                    throw new NullPointerException("device");
                }
            }

            private DeviceElement() { this(null); }

            @XmlElement
            public URI getDeviceType() { return device.getDeviceType(); }

            @XmlElement
            public URI getUDN() { return device.getUDN(); }

            @XmlElement
            public ServiceListElement getServiceList() {
                return new ServiceListElement(device);
            }

            @Override
            public String toString() { return getDeviceType().toString(); }
        }

        /**
         * {@code <serviceList/>}
         */
        protected static class ServiceListElement {
            private final Device device;

            /**
             * Sole public constructor.
             *
             * @param   device          The {@link Device}.
             */
            public ServiceListElement(Device device) {
                if (device != null) {
                    this.device = device;
                } else {
                    throw new NullPointerException("device");
                }
            }

            private ServiceListElement() { this(null); }

            @XmlElement
            public List<ServiceElement> getService() {
                ArrayList<ServiceElement> list = new ArrayList<>();

                for (Service service : device.getServiceList()) {
                    list.add(new ServiceElement(service));
                }

                return list;
            }

            @Override
            public String toString() { return super.toString(); }

            /**
             * {@code <service/>}
             */
            @XmlType(propOrder = {
                        "serviceType", "serviceId",
                        "SCPDURL", "controlURL", "eventSubURL"
                     })
            protected static class ServiceElement {
                private final Service service;

                /**
                 * Sole public constructor.
                 *
                 * @param   service         The {@link Service}.
                 */
                public ServiceElement(Service service) {
                    if (service != null) {
                        this.service = service;
                    } else {
                        throw new NullPointerException("service");
                    }
                }

                private ServiceElement() { this(null); }

                @XmlElement
                public URI getServiceType() {
                    return service.getServiceType();
                }

                @XmlElement
                public URI getServiceId() { return service.getServiceId(); }

                @XmlElement
                public String getSCPDURL() {
                    return service.getDevice().getSCPDPath(service);
                }

                @XmlElement
                public String getControlURL() {
                    return service.getDevice().getControlPath(service);
                }

                @XmlElement
                public String getEventSubURL() {
                    return service.getDevice().getEventPath(service);
                }

                @Override
                public String toString() {
                    return getServiceType().toString();
                }
            }
        }
    }
}
