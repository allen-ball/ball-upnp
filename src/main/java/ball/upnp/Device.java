/*
 * $Id$
 *
 * Copyright 2013 - 2015 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import ball.activation.JAXBDataSource;
import ball.io.Directory;
import ball.tomcat.EmbeddedTomcat;
import ball.util.UUIDFactory;
import java.beans.ConstructorProperties;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
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
import org.apache.velocity.tools.view.VelocityViewServlet;

import static ball.util.StringUtil.NIL;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP} devices.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public abstract class Device extends EmbeddedTomcat {
    private static final String HTTP = "http";
    private static final String UUID = "uuid";

    private static final String ASTERISK = "*";
    private static final String COLON = ":";
    private static final String SLASH = "/";

    private final URI deviceType;
    private final UUID uuid = UUIDFactory.getDefault().generateTime();
    private final URI udn;
    private final URI uri;
    private final SSDPThread ssdp;

    /**
     * Sole constructor.
     *
     * @param   deviceType      The {@link Device} type (URN).
     */
    @ConstructorProperties({ "deviceType" })
    protected Device(URI deviceType) {
        super();

        if (deviceType != null) {
            this.deviceType = deviceType;
        } else {
            throw new NullPointerException("deviceType");
        }

        try {
            udn = new URI(UUID, uuid.toString().toUpperCase(), null);
            uri =
                new URI(HTTP, null,
                        InetAddress.getLocalHost().getHostAddress(), port,
                        SLASH, null, null);
            ssdp = new SSDPThread();
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

    /**
     * Method to get the {@link SSDPThread} associated with this
     * {@link Device}.
     *
     * @return  The {@link SSDPThread}.
     */
    public SSDPThread getSSDPThread() { return ssdp; }

    @Override
    public Server getServer() {
        synchronized (this) {
            if (server == null) {
                server = super.getServer();
                server.addLifecycleListener(new LifecycleListenerImpl());
                server.addLifecycleListener(ssdp);
            }
        }

        return server;
    }

    @Override
    public String toString() { return getDeviceType().toString(); }

    private class LifecycleListenerImpl implements LifecycleListener {
        public LifecycleListenerImpl() { }

        @Override
        public void lifecycleEvent(LifecycleEvent event) {
            if (Lifecycle.BEFORE_START_EVENT.equals(event.getType())) {
                Context context = getContext();

                context.getServletContext()
                    .setAttribute("device", Device.this);
                /*
                 * Device
                 *
                 * Note: DeviceDescriptionServlet functionality should be
                 * included in CXFServlet / Spring implementation.
                 */
                addServlet(context, new DeviceDescriptionServlet())
                    .addMapping(getLocation().getPath());
                addServlet(context, new CXFServlet())
                    .addMapping(getPath() + SLASH + ASTERISK);
                /*
                 * SSDP Cache
                 */
                context.getServletContext()
                    .setAttribute("ssdp", ssdp);
                context.getServletContext()
                    .setAttribute("cache", ssdp.getSSDPDiscoveryCache());
                /*
                 * Resources
                 */
                addServlet(context, VelocityViewServlet.class)
                    .addMapping("*.html");
                /*
                 * Redirect to the Device description
                 */
                addServlet(context,
                           new RedirectServlet(getLocation().getPath()))
                    .addMapping(NIL);
                /*
                 * Set the port for the shutdown command
                 */
                getServer().setPort(port + 1);
            }

            if (Lifecycle.AFTER_START_EVENT.equals(event.getType())) {
                ssdp.add(Device.this);
            }

            if (Lifecycle.BEFORE_STOP_EVENT.equals(event.getType())) {
                ssdp.remove(Device.this);
            }
        }

        @Override
        public String toString() { return getClass().getName(); }
    }

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
