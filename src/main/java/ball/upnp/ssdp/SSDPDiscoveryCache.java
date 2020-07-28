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
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.http.Header;
import org.apache.http.client.utils.DateUtils;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

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
    private static final long serialVersionUID = -6249849064415060790L;

    /** @serial */ private ScheduledFuture<?> expirer = null;
    /** @serial */ private ScheduledFuture<?> msearch = null;
    /** @serial */ private final List<SSDPDiscoveryService.Listener> listeners =
        List.of(new NOTIFY(), new MSEARCH());

    @Override
    public void register(SSDPDiscoveryService service) {
        if (expirer == null) {
            expirer =
                service.scheduleAtFixedRate(() -> expirer(service),
                                            0, 60, SECONDS);
        }

        if (msearch == null) {
            msearch =
                service.scheduleAtFixedRate(() -> msearch(service),
                                            0, 300, SECONDS);
        }

        listeners.stream().forEach(t -> service.addListener(t));
    }

    @Override
    public void unregister(SSDPDiscoveryService service) {
        ScheduledFuture<?> expirer = this.expirer;

        if (expirer != null) {
            expirer.cancel(true);
        }
        ScheduledFuture<?> msearch = this.msearch;

        if (msearch != null) {
            msearch.cancel(true);
        }
    }

    @Override
    public void sendEvent(SSDPDiscoveryService service,
                          DatagramSocket socket, SSDPMessage message) {
    }

    @Override
    public void receiveEvent(SSDPDiscoveryService service,
                             DatagramSocket socket, SSDPMessage message) {
    }

    private void expirer(SSDPDiscoveryService service) {
        long now = now();
        boolean pending =
            values().stream()
            .mapToLong(t -> MINUTES.convert(t.getExpiration() - now, MILLISECONDS))
            .anyMatch(t -> t <= expirer.getDelay(MINUTES));
        boolean expired = values().removeIf(t -> t.getExpiration() < now);

        if (expired || pending) {
            service.submit(() -> msearch(service));
        }
    }

    private void msearch(SSDPDiscoveryService service) {
        service.multicast(0, SSDPRequest.msearch(SSDPMessage.SSDP_ALL));
    }

    private long now() { return System.currentTimeMillis(); }

    private void update(SSDPMessage message, URI usn) {
        if (usn != null) {
            long time = now();
            long expiration = 0;
            Long maxAge =
                message.getHeaderParameterValue(Long::decode,
                                                SSDPMessage.CACHE_CONTROL,
                                                SSDPMessage.MAX_AGE);

            if (maxAge != null) {
                Date date =
                    message.getHeaderValue(DateUtils::parseDate,
                                           SSDPMessage.DATE);

                if (date != null) {
                    time = date.getTime();
                }

                expiration = time + MILLISECONDS.convert(maxAge, SECONDS);
            }

            if (expiration > time) {
                put(usn, new Value(message, expiration));
            }
        }
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
            this.message = Objects.requireNonNull(message, "message");

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

    @ToString
    private class NOTIFY extends SSDPDiscoveryService.RequestHandler {
        public NOTIFY() { super(SSDPRequest.Method.NOTIFY); }

        @Override
        public void run(SSDPDiscoveryService service,
                        DatagramSocket socket, SSDPRequest request) {
            String nts = request.getHeaderValue(SSDPMessage.NTS);

            if (Objects.equals(SSDPMessage.SSDP_ALIVE, nts)) {
                update(request, request.getUSN());
            } else if (Objects.equals(SSDPMessage.SSDP_BYEBYE, nts)) {
                remove(request.getUSN());
            }
        }
    }

    @NoArgsConstructor @ToString
    private class MSEARCH extends SSDPDiscoveryService.ResponseHandler {
        @Override
        public void run(SSDPDiscoveryService service,
                        DatagramSocket socket, SSDPResponse response) {
            update(response, response.getUSN());
        }
    }
}
