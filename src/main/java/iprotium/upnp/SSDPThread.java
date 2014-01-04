/*
 * $Id$
 *
 * Copyright 2013, 2014 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp;

import iprotium.upnp.ssdp.SSDPDiscoveryCache;
import iprotium.upnp.ssdp.SSDPDiscoveryRequest;
import iprotium.upnp.ssdp.SSDPDiscoveryThread;
import iprotium.upnp.ssdp.SSDPMessage;
import iprotium.upnp.ssdp.SSDPNotifyRequest;
import iprotium.upnp.ssdp.SSDPRequest;
import iprotium.upnp.ssdp.SSDPResponse;
import java.net.SocketException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpHeaders;

/**
 * {@link SSDPDiscoveryThread} implementation that also announces
 * {@link Service}s provided by the specified {@link Device}s.
 *
 * @author {@link.uri mailto:ball@iprotium.com Allen D. Ball}
 * @version $Revision$
 */
public class SSDPThread extends SSDPDiscoveryThread
                                implements SSDPDiscoveryThread.Listener {
    private final List<Device> list;
    private final SSDPDiscoveryCache cache = new SSDPDiscoveryCache();

    /**
     * Sole constructor.
     *
     * @param   devices         The {@link Device}s.
     */
    public SSDPThread(Device... devices) throws SocketException {
        super(60);

        if (devices != null) {
            list = Arrays.asList(devices);

            if (list.isEmpty()) {
                throw new IllegalArgumentException("devices");
            }
        } else {
            throw new NullPointerException("devices");
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

    /**
     * Method to send {@value iprotium.upnp.ssdp.SSDPMessage#SSDP_ALIVE}
     * {@link SSDPNotifyRequest}s for each {@link Service} in each
     * {@link Device} known to this {@code this} {@link SSDPThread}.
     */
    protected void alive() {
        for (Device device : list) {
            queue(new SSDPNotifyAliveRequest(device));

            for (Service service : device.getServiceList()) {
                queue(new SSDPNotifyAliveRequest(service));
            }
        }
    }

    /**
     * Method to send {@value iprotium.upnp.ssdp.SSDPMessage#SSDP_BYEBYE}
     * {@link SSDPNotifyRequest}s for each {@link Service} in each
     * {@link Device} known to this {@code this} {@link SSDPThread}.
     */
    protected void byebye() {
        for (Device device : list) {
            queue(new SSDPNotifyByeByeRequest(device));

            for (Service service : device.getServiceList()) {
                queue(new SSDPNotifyByeByeRequest(service));
            }
        }
    }

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
