/*
 * $Id$
 *
 * Copyright 2013 Allen D. Ball.  All rights reserved.
 */
package iprotium.upnp.ssdp;

import java.net.URI;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.DateUtils;

/**
 * SSDP discovery cache implementation.
 *
 * @author <a href="mailto:ball@iprotium.com">Allen D. Ball</a>
 * @version $Revision$
 */
public class SSDPDiscoveryCache
             extends ConcurrentSkipListMap<URI,SSDPDiscoveryCache.Value>
             implements SSDPDiscoveryThread.Listener {
    private static final long serialVersionUID = 3201991262995832734L;

    private static final String MAX_AGE = "max-age";

    private static final String BYEBYE = "ssdp:byebye";

    /**
     * Sole constructor.
     */
    public SSDPDiscoveryCache() {
        super();

        new ExpiryThread().start();
    }

    @Override
    public void sendEvent(SSDPMessage message) { receiveEvent(message); }

    @Override
    public void receiveEvent(SSDPMessage message) {
        long expiration = 0;

        try {
            Header header = message.getFirstHeader(HttpHeaders.CACHE_CONTROL);

            if (header != null) {
                CacheControlDirectiveMap map =
                    new CacheControlDirectiveMap(header.getValue());
                String value = map.get(MAX_AGE);

                if (value != null) {
                    expiration = now() + (Long.decode(value) * 1000);
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
        } catch (Exception exception) {
        }

        if (expiration > now()) {
            put(message.getUSN(), new Value(message, expiration));
        }

        if (message instanceof SSDPRequest) {
            SSDPRequest request = (SSDPRequest) message;
            String method = request.getRequestLine().getMethod();

            if (SSDPNotifyRequest.METHOD.equals(method)) {
                Header header = message.getFirstHeader(SSDPMessage.NTS);

                if (header != null && BYEBYE.equals(header.getValue())) {
                    remove(request.getUSN());
                }
            }
        }
    }

    private long now() { return System.currentTimeMillis(); }

    /**
     * {@link SSDPDiscoveryCache} {@link java.util.Map} {@link Value}
     * (expiration and {@link SSDPMessage})
     */
    public class Value {
        private final SSDPMessage message;
        private long expiration = 0;

        private Value(SSDPMessage message, long expiration) {
            if (message != null) {
                this.message = message;
            } else {
                throw new NullPointerException("message");
            }

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

    private class ExpiryThread extends Thread {
        public ExpiryThread() {
            super();

            setDaemon(true);
            setName(getClass().getName());
        }

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
    }

    private class CacheControlDirectiveMap extends TreeMap<String,String> {
        private static final long serialVersionUID = 6243101589812178932L;

        public CacheControlDirectiveMap(String string) {
            super(String.CASE_INSENSITIVE_ORDER);

            for (String directive : string.trim().split(Pattern.quote(";"))) {
                String[] pair = directive.split(Pattern.quote("="), 2);

                put(pair[0].trim(), (pair.length > 1) ? pair[1].trim() : null);
            }
        }
    }
}
