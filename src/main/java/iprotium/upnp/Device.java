/*
 * $Id$
 *
 * Copyright 2013, 2014 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp;

import iprotium.activation.JAXBDataSource;
import iprotium.io.Directory;
import iprotium.util.NetworkInterfaceUtil;
import iprotium.util.UUIDFactory;
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
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;

import static iprotium.io.Directory.TMPDIR;
//import static iprotium.util.StringUtil.NIL;

/**
 * Abstract base class for {@link.uri http://www.upnp.org/ UPnP} devices.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public abstract class Device extends Tomcat {
    private static final String HTTP = "http";
    private static final String UUID = "uuid";

    private static final String COLON = ":";
    private static final String SLASH = "/";

    private static final String DOT_XML = ".xml";

    private final URI deviceType;
    private final UUID uuid = UUIDFactory.getDefault().generateTime();
    private final URI udn;
    private final URI uri;
    private final SSDPThread ssdp;
    private ContextImpl context = null;

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
            ssdp = new SSDPThread(this);
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
    public URI getLocation() { return uri.resolve(getPath() + DOT_XML); }

    private String getPath() {
        return SLASH + getClass().getSimpleName();
    }

    private String getPath(Service service) {
        String name = service.getClass().getSimpleName();
        ArrayList<Service> list = new ArrayList<Service>(getServiceList());
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
        return getPath(service) + DOT_XML;
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
    protected void initBaseDir() {
        synchronized (this) {
            if (basedir == null) {
                Directory home =
                    TMPDIR.getChildDirectory(getClass().getSimpleName()
                                             + COLON + port);

                home.mkdirs();
                basedir = home.getAbsolutePath();
            }

            super.initBaseDir();
        }
    }

    @Override
    public void start() throws LifecycleException {
        if (context == null) {
            context = new ContextImpl();

            getHost().addChild(context);
        }

        super.start();

        ssdp.alive();

        if (! ssdp.isAlive()) {
            ssdp.start();
        }
    }

    @Override
    public void stop() throws LifecycleException {
        ssdp.byebye();

        super.stop();
    }

    @Override
    public Host getHost() {
        synchronized (this) {
            if (host == null) {
                try {
                    host = super.getHost();

                    InetAddress localhost = InetAddress.getLocalHost();

                    host.addAlias(localhost.getHostAddress());
                    host.addAlias(localhost.getHostName());

                    for (InetAddress address :
                             NetworkInterfaceUtil.getInterfaceInetAddressList()) {
                        if (address.isSiteLocalAddress()) {
                            host.addAlias(address.getHostAddress());
                        }

                        if (address.isLoopbackAddress()) {
                            host.addAlias(address.getHostAddress());
                        }
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            }
        }

        return host;
    }

    @Override
    public Server getServer() {
        synchronized (this) {
            if (server == null) {
                server = super.getServer();
                server.setPort(port + 1);
            }
        }

        return server;
    }

    @Override
    public String toString() { return getDeviceType().toString(); }

    private class LocationServlet extends DataSourceServlet {
        private static final long serialVersionUID = -3666209110730272906L;

        public LocationServlet() { super(null); }

        @Override
        public JAXBDataSource getDataSource() {
            return new JAXBDataSource(new RootElement(Device.this));
        }
    }

    private class ContextImpl extends StandardContext {
        public ContextImpl() {
            super();

            setName(SLASH);
            setPath(SLASH);
            setDocBase(SLASH);
            setConfigFile(getWebappConfigFile(SLASH, SLASH));

            addLifecycleListener(new FixContextListener());

            setSessionTimeout(30);

            add(getLocation().getPath(), new LocationServlet());

            for (Service service : getServiceList()) {
                add(getSCPDPath(service), new SCPDServlet(service));
                add(getControlPath(service), new ControlServlet(service));
                add(getEventPath(service), new EventServlet(service));
            }

            add(SLASH, new RedirectServlet(getLocation().getPath()));
        }

        private void add(String path, Servlet servlet) {
            Tomcat.addServlet(this, path, servlet);
            addServletMapping(path, path);
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

        private Device device = null;
        private final DeviceElement element;

        /**
         * Sole public constructor.
         *
         * @param       device          The {@link Device}.
         */
        public RootElement(Device device) {
            if (device != null) {
                this.device = device;
            } else {
                throw new NullPointerException("device");
            }

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
            private Device device = null;

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
            private Device device = null;

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
                ArrayList<ServiceElement> list =
                    new ArrayList<ServiceElement>();

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
