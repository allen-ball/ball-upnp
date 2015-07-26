/*
 * $Id$
 *
 * Copyright 2013 - 2015 Allen D. Ball.  All rights reserved.
 */
package ball.upnp;

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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.DateUtils;

/**
 * {@link SSDPDiscoveryThread} implementation that also announces
 * {@link Service}s provided by the specified {@link Device}s.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class SSDP extends SSDPDiscoveryThread
                  implements SSDPDiscoveryThread.Listener {

    /**
     * Singleton instance of {@link SSDP}.
     */
    public static final SSDP INSTANCE;

    static {
        try {
            INSTANCE = new SSDP();
            INSTANCE.start();
        } catch (Exception exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final ArrayList<Device> list = new ArrayList<>();
    private final SSDPDiscoveryCache cache = new SSDPDiscoveryCache();

    private SSDP() throws SocketException {
        super(60);

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
        synchronized (list) {
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
        synchronized (list) {
            for (Device device : devices) {
                if (list.remove(device)) {
                    for (Service service : device.getServiceList()) {
                        queue(new SSDPNotifyByeByeRequest(service));
                    }

                    queue(new SSDPNotifyByeByeRequest(device));
                }
            }

            list.removeAll(Arrays.asList(devices));
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
            addHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(0));
            addHeader(HttpHeaders.DATE, DateUtils.formatDate(new Date(now())));
        }
    }
}
