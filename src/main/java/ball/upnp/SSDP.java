/*
 * $Id$
 *
 * Copyright 2013 - 2015 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

import ball.annotation.ServiceProviderFor;
import ball.tomcat.EmbeddedContextConfigurator;
import ball.tomcat.EmbeddedLifecycleListener;
import ball.upnp.ssdp.SSDPDiscoveryCache;
import ball.upnp.ssdp.SSDPDiscoveryRequest;
import ball.upnp.ssdp.SSDPDiscoveryThread;
import ball.upnp.ssdp.SSDPMessage;
import ball.upnp.ssdp.SSDPNotifyRequest;
import ball.upnp.ssdp.SSDPRequest;
import ball.upnp.ssdp.SSDPResponse;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.catalina.Context;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.Server;
import org.apache.http.HttpHeaders;

/**
 * {@link SSDPDiscoveryThread} implementation that also announces
 * {@link Service}s provided by the specified {@link Device}s.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
@ServiceProviderFor({ EmbeddedContextConfigurator.class, EmbeddedLifecycleListener.class })
@EmbeddedLifecycleListener.For({ Server.class })
public class SSDP extends SSDPDiscoveryThread
                  implements EmbeddedContextConfigurator,
                             EmbeddedLifecycleListener,
                             SSDPDiscoveryThread.Listener {
    private final ArrayList<Device> list = new ArrayList<>();
    private final SSDPDiscoveryCache cache = new SSDPDiscoveryCache();

    /**
     * No-argument constructor.
     */
    public SSDP() throws SocketException { this((Device[]) null); }

    /**
     * @param   devices         The {@link Device}s.
     */
    public SSDP(Device... devices) throws SocketException {
        super(60);

        if (devices != null) {
            add(devices);
        }

        addListener(cache);
        addListener(this);
    }

    /**
     * Method to get the {@link SSDPDiscoveryCache} managed by this
     * {@link SSDPDiscoveryThread}.
     *
     * @return  The {@link SSDPDiscoveryThread}.
     */
    public SSDPDiscoveryCache getSSDPDiscoveryCache() { return cache; }

    /**
     * Method to add local {@link Device}s.  Sends
     * {@value ball.upnp.ssdp.SSDPMessage#SSDP_ALIVE}
     * {@link SSDPNotifyRequest}s for each {@link Service} in each
     * {@link Device} added,
     *
     * @param   devices         The {@link Device}s to add.
     */
    protected void add(Device... devices) {
        for (Device device : devices) {
            if (! list.contains(device)) {
                list.add(device);

                queue(new SSDPNotifyAliveRequest(device));

                for (Service service : device.getServiceList()) {
                    queue(new SSDPNotifyAliveRequest(service));
                }
            }
        }
    }

    /**
     * Method to remove local {@link Device}s.  Sends
     * {@value ball.upnp.ssdp.SSDPMessage#SSDP_BYEBYE}
     * {@link SSDPNotifyRequest}s for each {@link Service} in each
     * {@link Device} removed.
     *
     * @param   devices         The {@link Device}s to remove.
     */
    protected void remove(Device... devices) {
        for (Device device : devices) {
            if (list.contains(device)) {
                list.remove(device);

                queue(new SSDPNotifyByeByeRequest(device));

                for (Service service : device.getServiceList()) {
                    queue(new SSDPNotifyByeByeRequest(service));
                }
            }
        }
    }

    @Override
    public void configure(Context context) throws Exception {
        start();

        context.getServletContext()
            .setAttribute("ssdp", this);
        context.getServletContext()
            .setAttribute("cache", getSSDPDiscoveryCache());

        context.setConfigured(true);
    }

    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (event.getLifecycle() instanceof Server) {
            Server server = (Server) event.getLifecycle();

            if (Lifecycle.BEFORE_STOP_EVENT.equals(event.getType())) {
                remove(list.toArray(new Device[] { }));
            }
        }
    }

    @Override
    protected void ping() {
        SSDPDiscoveryCache cache = getSSDPDiscoveryCache();
        Map.Entry<URI,SSDPDiscoveryCache.Value> entry = cache.firstEntry();
        long expiration = 0;

        if (entry != null) {
            expiration = entry.getValue().getExpiration();

            for (SSDPDiscoveryCache.Value value : cache.values()) {
                expiration = Math.min(expiration, value.getExpiration());
            }
        }

        if (expiration < (now() + (2 * interval * 1000))) {
            super.ping();
        }
    }

    private long now() { return System.currentTimeMillis(); }

    @Override
    public void sendEvent(SSDPDiscoveryThread thread, SSDPMessage message) {
        receiveEvent(thread, message);
    }

    @Override
    public void receiveEvent(SSDPDiscoveryThread thread, SSDPMessage message) {
        if (message instanceof SSDPRequest) {
            SSDPRequest request = (SSDPRequest) message;
            String method = request.getRequestLine().getMethod();
            /*
             * Should check MAN and ST values, too.
             */
            if (SSDPDiscoveryRequest.METHOD.equalsIgnoreCase(method)) {
                for (Device device : list) {
                    queue(new SSDPDiscoveryResponse(device));

                    for (Service service : device.getServiceList()) {
                        queue(new SSDPDiscoveryResponse(service));
                    }
                }
            }
        }
    }

    private class SSDPNotifyAliveRequest extends SSDPNotifyRequest {
        public SSDPNotifyAliveRequest(Device device) {
            this(device, device.getUDN(), device.getUDN());
        }

        public SSDPNotifyAliveRequest(Service service) {
            this(service.getDevice(),
                 service.getServiceId(), service.getUSN());
        }

        private SSDPNotifyAliveRequest(Device device, URI nt, URI usn) {
            super();

            addHeader(NT, nt.toASCIIString());
            addHeader(NTS, SSDP_ALIVE);
            addHeader(USN, usn.toASCIIString());
            addHeader(HttpHeaders.LOCATION.toUpperCase(),
                      device.getLocation().toASCIIString());
            addHeader(HttpHeaders.CACHE_CONTROL.toUpperCase(),
                      MAX_AGE + "=" + String.valueOf(1800));
        }
    }

    private class SSDPNotifyByeByeRequest extends SSDPNotifyRequest {
        public SSDPNotifyByeByeRequest(Device device) {
            this(device.getUDN(), device.getUDN());
        }

        public SSDPNotifyByeByeRequest(Service service) {
            this(service.getServiceId(), service.getUSN());
        }

        private SSDPNotifyByeByeRequest(URI nt, URI usn) {
            super();

            addHeader(NT, nt.toASCIIString());
            addHeader(NTS, SSDP_BYEBYE);
            addHeader(USN, usn.toASCIIString());
        }
    }

    private class SSDPDiscoveryResponse extends SSDPResponse {
        public SSDPDiscoveryResponse(Device device) {
            this(device, device.getUDN(), device.getUDN());
        }

        public SSDPDiscoveryResponse(Service service) {
            this(service.getDevice(),
                 service.getServiceId(), service.getUSN());
        }

        private SSDPDiscoveryResponse(Device device, URI st, URI usn) {
            super(200, "OK");

            addHeader(HttpHeaders.SERVER.toUpperCase(), "UPnP/1.0");
            addHeader(ST, st.toASCIIString());
            addHeader(HttpHeaders.LOCATION.toUpperCase(),
                      device.getLocation().toASCIIString());
            addHeader(HttpHeaders.CACHE_CONTROL.toUpperCase(),
                      MAX_AGE + "=" + String.valueOf(1800));
            addHeader(USN, usn.toASCIIString());
            addHeader(EXT, null);
        }
    }
}
