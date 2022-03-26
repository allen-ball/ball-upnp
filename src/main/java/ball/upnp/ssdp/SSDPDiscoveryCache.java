package ball.upnp.ssdp;
/*-
 * ##########################################################################
 * UPnP/SSDP Implementation Classes
 * %%
 * Copyright (C) 2013 - 2022 Allen D. Ball
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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.hc.core5.http.Header;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * SSDP discovery cache implementation.
 *
 * @author {@link.uri mailto:ball@hcf.dev Allen D. Ball}
 */
@NoArgsConstructor
public class SSDPDiscoveryCache extends ConcurrentSkipListMap<URI,SSDPMessage> implements SSDPDiscoveryService.Listener {
    private static final long serialVersionUID = 2743071044637511801L;

    /** @serial */ private ScheduledFuture<?> expirer = null;
    /** @serial */ private ScheduledFuture<?> msearch = null;
    /** @serial */ private final List<SSDPDiscoveryService.Listener> listeners =
        Arrays.asList(new NOTIFY(), new MSEARCH());

    @Override
    public void register(SSDPDiscoveryService service) {
        if (expirer == null) {
            expirer = service.scheduleAtFixedRate(() -> expirer(service), 0, 60, SECONDS);
        }

        if (msearch == null) {
            msearch = service.scheduleAtFixedRate(() -> msearch(service), 0, 300, SECONDS);
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
    public void sendEvent(SSDPDiscoveryService service, DatagramSocket socket, SSDPMessage message) {
    }

    @Override
    public void receiveEvent(SSDPDiscoveryService service, DatagramSocket socket, SSDPMessage message) {
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
        service.msearch(15, SSDPMessage.SSDP_ALL);
    }

    private long now() { return System.currentTimeMillis(); }

    private void update(URI usn, SSDPMessage message) {
        if (usn != null) {
            long time = now();

            if (message.getExpiration() > time) {
                put(usn, message);
            }
        }
    }

    @ToString
    private class NOTIFY extends SSDPDiscoveryService.RequestHandler {
        public NOTIFY() { super(SSDPRequest.Method.NOTIFY); }

        @Override
        public void run(SSDPDiscoveryService service, DatagramSocket socket, SSDPRequest request) {
            String nts = request.getHeaderValue(SSDPMessage.NTS);

            if (Objects.equals(SSDPMessage.SSDP_ALIVE, nts)) {
                update(request.getUSN(), request);
            } else if (Objects.equals(SSDPMessage.SSDP_UPDATE, nts)) {
                /* update(request.getUSN(), request); */
            } else if (Objects.equals(SSDPMessage.SSDP_BYEBYE, nts)) {
                remove(request.getUSN());
            }
        }
    }

    @NoArgsConstructor @ToString
    private class MSEARCH extends SSDPDiscoveryService.ResponseHandler {
        @Override
        public void run(SSDPDiscoveryService service, DatagramSocket socket, SSDPResponse response) {
            update(response.getUSN(), response);
        }
    }
}
