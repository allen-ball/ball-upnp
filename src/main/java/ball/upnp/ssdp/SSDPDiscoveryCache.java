/*
 * $Id$
 *
 * Copyright 2013 - 2019 Allen D. Ball.  All rights reserved.
 */
package ball.upnp.ssdp;

import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.DateUtils;

import static ball.upnp.ssdp.SSDPMessage.MAX_AGE;
import static ball.upnp.ssdp.SSDPMessage.SSDP_BYEBYE;
import static java.util.Objects.requireNonNull;

/**
 * SSDP discovery cache implementation.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
public class SSDPDiscoveryCache
             extends ConcurrentSkipListMap<URI,SSDPDiscoveryCache.Value>
             implements SSDPDiscoveryThread.Listener {
    private static final long serialVersionUID = -383765398333867476L;

    /**
     * Sole constructor.
     */
    public SSDPDiscoveryCache() {
        super();

        new Thread() {
            { setDaemon(true); }

            @Override
            public void run() {
                for (;;) {
                    try {
                        sleep(60 * 1000);
                    } catch (InterruptedException exception) {
                    }

                    Iterator<Value> iterator = values().iterator();

                    while (iterator.hasNext()) {
                        Value value = iterator.next();

                        if (now() > value.getExpiration()) {
                            iterator.remove();
                        }
                    }
                }
            }
        }.start();
    }

    @Override
    public void sendEvent(SSDPDiscoveryThread thread, SSDPMessage message) {
        receiveEvent(thread, message);
    }

    @Override
    public void receiveEvent(SSDPDiscoveryThread thread, SSDPMessage message) {
        try {
            long time = now();
            long expiration = 0;
            Header header = message.getFirstHeader(HttpHeaders.CACHE_CONTROL);

            if (header != null) {
                CacheControlDirectiveMap map =
                    new CacheControlDirectiveMap(header.getValue());
                String value = map.get(MAX_AGE);

                if (value != null) {
                    try {
                        header = message.getFirstHeader(HttpHeaders.DATE);

                        if (header != null) {
                            time =
                                DateUtils.parseDate(header.getValue())
                                .getTime();
                        }
                    } catch (Exception exception) {
                    }

                    expiration = time + (Long.decode(value) * 1000);
                }
            } else {
                header = message.getFirstHeader(HttpHeaders.EXPIRES);

                if (header != null) {
                    String value = header.getValue();
                    Date date = DateUtils.parseDate(value);

                    if (date != null) {
                        expiration = date.getTime();
                    }
                }
            }

            if (expiration > time) {
                put(message.getUSN(), new Value(message, expiration));
            }
        } catch (Exception exception) {
        }

        if (message instanceof SSDPRequest) {
            SSDPRequest request = (SSDPRequest) message;
            String method = request.getRequestLine().getMethod();

            if (SSDPNotifyRequest.METHOD.equals(method)) {
                Header header = message.getFirstHeader(SSDPMessage.NTS);

                if (header != null && SSDP_BYEBYE.equals(header.getValue())) {
                    remove(request.getUSN());
                }
            }
        }
    }

    private long now() { return System.currentTimeMillis(); }

    /**
     * {@link SSDPDiscoveryCache} {@link java.util.Map} {@link Value}
     * (expiration and {@link SSDPMessage})
     *
     * {@bean.info}
     */
    public class Value {
        private final SSDPMessage message;
        private long expiration = 0;

        private Value(SSDPMessage message, long expiration) {
            this.message = requireNonNull(message, "message");

            setExpiration(expiration);
        }

        public SSDPMessage getSSDPMessage() { return message; }

        public long getExpiration() { return expiration; }
        public void setExpiration(long expiration) {
            if (expiration > 0) {
                this.expiration = expiration;
            } else {
                throw new IllegalArgumentException("expiration=" + expiration);
            }
        }

        @Override
        public String toString() { return message.toString(); }
    }

    private class CacheControlDirectiveMap extends TreeMap<String,String> {
        private static final long serialVersionUID = -7901522510091761313L;

        public CacheControlDirectiveMap(String string) {
            super(String.CASE_INSENSITIVE_ORDER);

            for (String directive : string.trim().split(Pattern.quote(";"))) {
                String[] pair = directive.split(Pattern.quote("="), 2);

                put(pair[0].trim(), (pair.length > 1) ? pair[1].trim() : null);
            }
        }
    }
}
