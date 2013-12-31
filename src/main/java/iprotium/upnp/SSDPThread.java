/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
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
    public SSDPDiscoveryCache cache() { return cache; }

    @Override
    protected void ping() {
        long expiration = 0;
        Map.Entry<URI,SSDPDiscoveryCache.Value> entry = cache.firstEntry();

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
     * Method to send {@value iprotium.upnp.ssdp.SSDPMessage#SSDP_BYEBYE}
     * {@link SSDPNotifyRequest}s for each {@link Service} in each
     * {@link Device} known to this {@code this} {@link SSDPThread}.
     */
    protected void byebye() {
        for (Device device : list) {
            for (Service service : device.getServiceList()) {
                queue(new SSDPNotifyByeByeRequest(service));
            }
        }
    }

    @Override
    public void start() {
        for (Device device : list) {
            for (Service service : device.getServiceList()) {
                queue(new SSDPNotifyAliveRequest(service));
            }
        }

        super.start();
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
                    for (Service service : device.getServiceList()) {
                        queue(new SSDPDiscoveryResponse(service));
                    }
                }
            }
        }
    }

    private class SSDPNotifyAliveRequest extends SSDPNotifyRequest {
        public SSDPNotifyAliveRequest(Service service) {
            super();

            addHeader(NT, service.getServiceId().toASCIIString());
            addHeader(NTS, SSDP_ALIVE);
            addHeader(USN, service.getUSN().toASCIIString());
            addHeader(HttpHeaders.LOCATION,
                      service.getDevice().getLocation().toASCIIString());
            addHeader(HttpHeaders.CACHE_CONTROL,
                      MAX_AGE + "=" + String.valueOf(1800));
        }
    }

    private class SSDPNotifyByeByeRequest extends SSDPNotifyRequest {
        public SSDPNotifyByeByeRequest(Service service) {
            super();

            addHeader(NT, service.getServiceId().toASCIIString());
            addHeader(NTS, SSDP_BYEBYE);
            addHeader(USN, service.getUSN().toASCIIString());
        }
    }

    private class SSDPDiscoveryResponse extends SSDPResponse {
        public SSDPDiscoveryResponse(Service service) {
            super(200, "OK");

            addHeader(HttpHeaders.SERVER, "UPnP/1.0");
            addHeader(ST, service.getServiceId().toASCIIString());
            addHeader(HttpHeaders.LOCATION,
                      service.getDevice().getLocation().toASCIIString());
            addHeader(HttpHeaders.CACHE_CONTROL,
                      MAX_AGE + "=" + String.valueOf(1800));
            addHeader(USN, service.getUSN().toASCIIString());
            addHeader(EXT, null);
        }
    }
}
