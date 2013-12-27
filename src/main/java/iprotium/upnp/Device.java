/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp;

import iprotium.io.Directory;
import iprotium.io.IOUtil;
import iprotium.upnp.ssdp.SSDPDiscoveryCache;
import iprotium.upnp.ssdp.SSDPDiscoveryThread;
import iprotium.upnp.ssdp.SSDPMessage;
import iprotium.util.NetworkInterfaceUtil;
import iprotium.util.UUIDFactory;
import iprotium.xml.bind.JAXBDataSource;
import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.activation.DataSource;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import static iprotium.io.Directory.TMPDIR;
import static iprotium.util.StringUtil.NIL;

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

    private static final InetAddress LOCALHOST;

    static {
        try {
            LOCALHOST = InetAddress.getLocalHost();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final URI udn;
    private final URI uri;
    private final SSDPDiscoveryCache cache = new SSDPDiscoveryCache();
    private final SSDPDiscoveryThread thread;
    private final URI deviceType;
    private Context context = null;

    {
        try {
            udn =
                new URI(UUID,
                        UUIDFactory.getDefault().generateTime().toString(),
                        null);
            uri =
                new URI(HTTP, null, LOCALHOST.getHostAddress(), port,
                        SLASH, null, null);
            thread = new SSDPDiscoveryThread(60);
            thread.addListener(new ListenerImpl());
            thread.addListener(cache);
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * Sole constructor.
     *
     * @param   deviceType              The {@link Device} type (URN).
     */
    @ConstructorProperties({ "deviceType" })
    protected Device(URI deviceType) {
        super();

        enableNaming();

        if (deviceType != null) {
            this.deviceType = deviceType;
        } else {
            throw new NullPointerException("deviceType");
        }
    }

    /**
     * Method to get the URN {{@link URI}) describing this {@link Device}'s
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
    protected abstract List<Service> getServiceList();

    /**
     * Method to get the location {@link URI} for this {@link Device}.
     *
     * @return  The location {@link URI}.
     */
    public URI getLocation() {
        return uri().resolve(getClass().getSimpleName() + DOT_XML);
    }

    /**
     * Method to get the base {@link URI} of this {@link Device}.
     *
     * @return  The base {@link URI} of this {@link Device}.
     */
    protected URI uri() { return uri; }

    /**
     * Method to get the {@link SSDPDiscoveryCache} associated with this
     * {@link Device}.
     *
     * @return  The {@link SSDPDiscoveryCache}.
     */
    public SSDPDiscoveryCache getSSDPDiscoveryCache() { return cache; }

    /**
     * Method to get the {@link SSDPDiscoveryThread} associated with this
     * {@link Device}.
     *
     * @return  The {@link SSDPDiscoveryThread}.
     */
    public SSDPDiscoveryThread getSSDPDiscoveryThread() { return thread; }

    /**
     * Method to get the {@link Context} for this {@link Tomcat} instance.
     *
     * @return  The {@link Context}.
     */
    public Context context() {
        synchronized (this) {
            if (context == null) {
                initBaseDir();
                context = addContext(NIL, basedir);
            }
        }

        return context;
    }

    /**
     * Method to get a {@link Document} that may be serialized with a
     * {@link javax.xml.bind.Marshaller} to XML.
     *
     * @return  The {@link Document} representing this {@link Device}.
     */
    protected Document getDocument() { return new Document(this); }

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
    public void init() throws LifecycleException {
        try {
            host.addAlias(LOCALHOST.getHostAddress());
            host.addAlias(LOCALHOST.getHostName());

            for (InetAddress address :
                     NetworkInterfaceUtil.getInterfaceInetAddressList()) {
                if (address.isSiteLocalAddress()) {
                    host.addAlias(address.getHostAddress());
                }

                if (address.isLoopbackAddress()) {
                    host.addAlias(address.getHostAddress());
                }
            }

            super.init();
        } catch (LifecycleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LifecycleException(exception);
        }
    }

    @Override
    public void start() throws LifecycleException {
        try {
            addServlet(getLocation(), new JAXBDocumentServlet());

            for (Service service : getServiceList()) {
                addServlet(service.getSCPDURL(),
                           service.getSCPDServlet());
                addServlet(service.getControlURL(),
                           service.getControlServlet());
                addServlet(service.getEventSubURL(),
                           service.getEventSubServlet());
            }

            addServlet(NIL, new RedirectServlet(getLocation().getPath()));

            super.start();

            thread.start();
        } catch (LifecycleException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new LifecycleException(exception);
        }
    }

    private void addServlet(URI uri, HttpServlet servlet) {
        if (servlet != null) {
            addServlet(uri.getPath(), servlet);
        }
    }

    private void addServlet(String path, HttpServlet servlet) {
        addServlet(context(), path, servlet);
        context().addServletMapping(path, path);
    }

    @Override
    public String toString() { return getDeviceType().toString(); }

    private class ListenerImpl implements SSDPDiscoveryThread.Listener {
        public ListenerImpl() { }

        @Override
        public void sendEvent(SSDPMessage message) {
        }

        @Override
        public void receiveEvent(SSDPMessage message) {
        }

        @Override
        public String toString() { return getClass().getCanonicalName(); }
    }

    private class JAXBDocumentServlet extends HttpServlet {
        private static final long serialVersionUID = 6867002874183364142L;

        public JAXBDocumentServlet() { super(); }

        @Override
        protected void doGet(HttpServletRequest request,
                             HttpServletResponse response) throws IOException,
                                                                  ServletException {
            InputStream in = null;
            ServletOutputStream out = null;

            try {
                DataSource ds = new JAXBDataSource(getDocument());

                response.setContentType(ds.getContentType());

                in = ds.getInputStream();
                out = response.getOutputStream();
                IOUtil.copy(in, out);
            } finally {
                IOUtil.close(in);
                IOUtil.close(out);
            }
        }

        @Override
        public String toString() { return getClass().getSimpleName(); }
    }

    /**
     * {@code <root/>}
     */
    @XmlRootElement(name = "root")
    @XmlType(propOrder = { "specVersion", "device" })
    protected static class Document {
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
        public Document(Device device) {
            if (device != null) {
                this.device = device;
            } else {
                throw new NullPointerException("device");
            }

            element = (device != null) ? new DeviceElement(device) : null;
        }

        private Document() { this(null); }

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
            public List<Service.Element> getService() {
                ArrayList<Service.Element> list =
                    new ArrayList<Service.Element>();

                for (Service service : device.getServiceList()) {
                    list.add(service.getElement());
                }

                return list;
            }

            @Override
            public String toString() { return super.toString(); }
        }
    }
}
