package ball.upnp.ssdp;
/*-
 * ##########################################################################
 * UPnP/SSDP Implementation Classes
 * $Id$
 * $HeadURL$
 * %%
 * Copyright (C) 2013 - 2020 Allen D. Ball
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ##########################################################################
 */
import java.net.DatagramSocket;
import java.net.URI;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import org.apache.http.Header;

import static ball.upnp.ssdp.SSDPMessage.MAX_AGE;
import static ball.upnp.ssdp.SSDPMessage.SSDP_BYEBYE;
import static java.util.Objects.requireNonNull;
import static org.apache.http.client.utils.DateUtils.parseDate;

/**
 * SSDP discovery cache implementation.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 * @version $Revision$
 */
@NoArgsConstructor
public class SSDPDiscoveryCache
             extends ConcurrentSkipListMap<URI,SSDPDiscoveryCache.Value>
             implements SSDPDiscoveryService.Listener {
    private static final long serialVersionUID = -8435687218821120293L;

    private ScheduledFuture<?> expirer = null;

    @Override
    public void sendEvent(SSDPDiscoveryService service,
                          DatagramSocket socket,
                          SSDPMessage message) {
        receiveEvent(service, socket, message);
    }

    @Override
    public void receiveEvent(SSDPDiscoveryService service,
                             DatagramSocket socket,
                             SSDPMessage message) {
        if (expirer == null) {
            expirer =
                service.scheduleAtFixedRate(() -> expire(),
                                            0, 60, TimeUnit.SECONDS);
        }

        try {
            long time = now();
            long expiration = 0;
            Header header = message.getFirstHeader(SSDPMessage.CACHE_CONTROL);

            if (header != null) {
                CacheControlDirectiveMap map =
                    new CacheControlDirectiveMap(header.getValue());
                String value = map.get(MAX_AGE);

                if (value != null) {
                    try {
                        header = message.getFirstHeader(SSDPMessage.DATE);

                        if (header != null) {
                            time = parseDate(header.getValue()).getTime();
                        }
                    } catch (Exception exception) {
                    }

                    expiration = time + (Long.decode(value) * 1000);
                }
            } else {
                header = message.getFirstHeader(SSDPMessage.EXPIRES);

                if (header != null) {
                    String value = header.getValue();
                    Date date = parseDate(value);

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

    private void expire() {
        values().removeIf(t -> now() > t.getExpiration());
    }

    /**
     * {@link SSDPDiscoveryCache} {@link java.util.Map} {@link Value}
     * (expiration and {@link SSDPMessage}).
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
